package com.print3d.calculator.data.repo

import com.print3d.calculator.data.local.ClientDao
import com.print3d.calculator.data.local.MachineDao
import com.print3d.calculator.data.local.MaterialDao
import com.print3d.calculator.data.local.MaterialMovementDao
import com.print3d.calculator.data.local.QuotationDao
import com.print3d.calculator.data.local.QuoteEventDao
import com.print3d.calculator.data.local.TemplateDao
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.MaterialMovement
import com.print3d.calculator.domain.model.MovementReason
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteEvent
import com.print3d.calculator.domain.model.QuoteStatus
import com.print3d.calculator.domain.model.QuoteTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialRepository @Inject constructor(private val dao: MaterialDao) {
    val all: Flow<List<Material>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun get(id: Long): Material? = dao.getById(id)?.toDomain()
    suspend fun save(material: Material): Long = dao.upsert(material.toEntity())
    suspend fun delete(material: Material) = dao.delete(material.toEntity())
}

@Singleton
class ClientRepository @Inject constructor(private val dao: ClientDao) {
    val all: Flow<List<Client>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun getByName(name: String): Client? = dao.getByName(name)?.toDomain()
    suspend fun get(id: Long): Client? = dao.getById(id)?.toDomain()
    suspend fun save(client: Client): Long = dao.upsert(client.toEntity())
    suspend fun delete(client: Client) = dao.delete(client.toEntity())

    /** Create a client from a quote's name if none exists yet. Keeps the directory in sync. */
    suspend fun ensureExists(name: String, phone: String = "", email: String = "") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (dao.getByName(trimmed) == null) {
            dao.upsert(Client(name = trimmed, phone = phone, email = email).toEntity())
        }
    }
}

@Singleton
class MachineRepository @Inject constructor(private val dao: MachineDao) {
    val all: Flow<List<Machine>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun get(id: Long): Machine? = dao.getById(id)?.toDomain()
    suspend fun save(machine: Machine): Long = dao.upsert(machine.toEntity())
    suspend fun delete(machine: Machine) = dao.delete(machine.toEntity())
}

@Singleton
class QuotationRepository @Inject constructor(
    private val dao: QuotationDao,
    private val eventDao: QuoteEventDao,
    private val inventory: InventoryManager,
    private val billing: com.print3d.calculator.data.billing.BillingRepository
) {
    val all: Flow<List<Quotation>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    fun search(q: String): Flow<List<Quotation>> =
        dao.search(q).map { list -> list.map { it.toDomain() } }
    suspend fun get(id: Long): Quotation? = dao.getById(id)?.toDomain()
    suspend fun save(quotation: Quotation): Long = dao.upsert(quotation.toEntity())
    suspend fun update(quotation: Quotation) = dao.update(quotation.toEntity())
    suspend fun delete(quotation: Quotation) {
        dao.delete(quotation.toEntity())
        eventDao.clearForQuote(quotation.id)
    }
    suspend fun nextNumber(): String {
        val n = dao.count() + 1
        return "COT-" + n.toString().padStart(4, '0')
    }
    suspend fun count(): Int = dao.count()

    /** Quotes created in the current calendar month — powers the free monthly cap. */
    suspend fun countThisMonth(): Int = dao.countSince(startOfMonth())

    /** Status-change audit trail for one quote. */
    fun events(quotationId: Long): Flow<List<QuoteEvent>> =
        eventDao.observeForQuote(quotationId).map { list -> list.map { it.toDomain() } }

    /**
     * Move a quote to [newStatus]: stamps the matching lifecycle timestamp, bumps [Quotation.updatedAt],
     * persists, and appends a [QuoteEvent]. Returns the updated quote so the caller can react
     * (e.g. trigger inventory consumption). No-op timestamps when the state is revisited.
     */
    suspend fun updateStatus(quote: Quotation, newStatus: QuoteStatus, note: String = ""): Quotation {
        val now = System.currentTimeMillis()
        var updated = quote.copy(status = newStatus, updatedAt = now)
        updated = when (newStatus) {
            QuoteStatus.SENT -> updated.copy(sentAt = updated.sentAt ?: now)
            QuoteStatus.VIEWED -> updated.copy(viewedAt = updated.viewedAt ?: now)
            QuoteStatus.ACCEPTED -> updated.copy(acceptedAt = updated.acceptedAt ?: now)
            QuoteStatus.IN_PRODUCTION -> updated.copy(productionStartedAt = updated.productionStartedAt ?: now)
            QuoteStatus.DELIVERED -> updated.copy(deliveredAt = updated.deliveredAt ?: now)
            QuoteStatus.REJECTED -> updated.copy(rejectedAt = updated.rejectedAt ?: now)
            else -> updated
        }
        // Auto-deduct inventory when a job enters production — Pro only, and only once per quote.
        if (newStatus == QuoteStatus.IN_PRODUCTION && billing.isSubscribed.value) {
            updated = inventory.applyConsumption(updated)
        }
        dao.update(updated.toEntity())
        eventDao.upsert(QuoteEvent(quotationId = quote.id, status = newStatus, timestamp = now, note = note).toEntity())
        return updated
    }

    /** Append a lifecycle event without changing the quote (e.g. the initial DRAFT on creation). */
    suspend fun logEvent(quotationId: Long, status: QuoteStatus, note: String = "") {
        eventDao.upsert(
            QuoteEvent(quotationId = quotationId, status = status, timestamp = System.currentTimeMillis(), note = note).toEntity()
        )
    }

    /** True when the current user is entitled to Pro (unlimited + auto-inventory). */
    val isPro: Boolean get() = billing.isSubscribed.value

    private fun startOfMonth(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}

@Singleton
class MaterialMovementRepository @Inject constructor(private val dao: MaterialMovementDao) {
    val all: Flow<List<MaterialMovement>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
    fun forMaterial(materialId: Long): Flow<List<MaterialMovement>> =
        dao.observeForMaterial(materialId).map { list -> list.map { it.toDomain() } }
}

/**
 * Stock engine. Applies material consumption when a job enters production and records every change
 * in the movement ledger. Never touches [com.print3d.calculator.domain.calc.CalculationEngine].
 */
@Singleton
class InventoryManager @Inject constructor(
    private val materialDao: MaterialDao,
    private val movementDao: MaterialMovementDao
) {
    /**
     * Deduct the grams quoted per material line for [quote], once. Returns the quote flagged
     * [Quotation.stockDeducted] = true (or unchanged if it was already deducted / has no lines).
     */
    suspend fun applyConsumption(quote: Quotation): Quotation {
        if (quote.stockDeducted) return quote
        val now = System.currentTimeMillis()
        for (line in quote.input.effectiveMaterialLines) {
            val id = line.materialId ?: continue
            if (line.grams <= 0.0) continue
            val entity = materialDao.getById(id) ?: continue
            val prev = entity.currentWeightG
            val next = prev - line.grams
            materialDao.upsert(entity.copy(currentWeightG = next))
            movementDao.upsert(
                MaterialMovement(
                    materialId = id,
                    delta = -line.grams,
                    reason = MovementReason.CONSUMPTION,
                    previousWeightG = prev,
                    newWeightG = next,
                    timestamp = now,
                    quotationId = quote.id
                ).toEntity()
            )
        }
        return quote.copy(stockDeducted = true)
    }

    /** Manual stock change (+/-). Records a ledger movement with the given [reason]. */
    suspend fun adjustStock(materialId: Long, delta: Double, reason: MovementReason, note: String = "") {
        val entity = materialDao.getById(materialId) ?: return
        val prev = entity.currentWeightG
        val next = prev + delta
        materialDao.upsert(entity.copy(currentWeightG = next))
        movementDao.upsert(
            MaterialMovement(
                materialId = materialId,
                delta = delta,
                reason = reason,
                previousWeightG = prev,
                newWeightG = next,
                timestamp = System.currentTimeMillis(),
                note = note
            ).toEntity()
        )
    }
}

@Singleton
class TemplateRepository @Inject constructor(private val dao: TemplateDao) {
    val all: Flow<List<QuoteTemplate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun save(template: QuoteTemplate): Long = dao.upsert(template.toEntity())
    suspend fun delete(template: QuoteTemplate) = dao.delete(template.toEntity())
    suspend fun count(): Int = dao.count()
}
