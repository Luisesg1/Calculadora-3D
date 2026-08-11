package com.print3d.calculator.data.repo

import com.print3d.calculator.data.local.ClientDao
import com.print3d.calculator.data.local.MachineDao
import com.print3d.calculator.data.local.MaterialDao
import com.print3d.calculator.data.local.QuotationDao
import com.print3d.calculator.data.local.TemplateDao
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
class QuotationRepository @Inject constructor(private val dao: QuotationDao) {
    val all: Flow<List<Quotation>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    fun search(q: String): Flow<List<Quotation>> =
        dao.search(q).map { list -> list.map { it.toDomain() } }
    suspend fun get(id: Long): Quotation? = dao.getById(id)?.toDomain()
    suspend fun save(quotation: Quotation): Long = dao.upsert(quotation.toEntity())
    suspend fun update(quotation: Quotation) = dao.update(quotation.toEntity())
    suspend fun delete(quotation: Quotation) = dao.delete(quotation.toEntity())
    suspend fun nextNumber(): String {
        val n = dao.count() + 1
        return "COT-" + n.toString().padStart(4, '0')
    }
    suspend fun count(): Int = dao.count()
}

@Singleton
class TemplateRepository @Inject constructor(private val dao: TemplateDao) {
    val all: Flow<List<QuoteTemplate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun save(template: QuoteTemplate): Long = dao.upsert(template.toEntity())
    suspend fun delete(template: QuoteTemplate) = dao.delete(template.toEntity())
    suspend fun count(): Int = dao.count()
}
