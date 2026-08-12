package com.print3d.calculator.data.sync

import com.print3d.calculator.data.local.ClientDao
import com.print3d.calculator.data.local.ClientEntity
import com.print3d.calculator.data.local.MachineDao
import com.print3d.calculator.data.local.MachineEntity
import com.print3d.calculator.data.local.MaterialDao
import com.print3d.calculator.data.local.MaterialEntity
import com.print3d.calculator.data.local.MaterialMovementDao
import com.print3d.calculator.data.local.MaterialMovementEntity
import com.print3d.calculator.data.local.QuotationDao
import com.print3d.calculator.data.local.QuotationEntity
import com.print3d.calculator.data.local.QuoteEventDao
import com.print3d.calculator.data.local.QuoteEventEntity
import com.print3d.calculator.data.local.TemplateDao
import com.print3d.calculator.data.local.TemplateEntity
import com.print3d.calculator.data.repo.AppJson
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Whole-account snapshot of every local table. Serialized to one JSON blob in the cloud. */
@Serializable
data class BackupData(
    val materials: List<MaterialEntity> = emptyList(),
    val machines: List<MachineEntity> = emptyList(),
    val clients: List<ClientEntity> = emptyList(),
    val quotations: List<QuotationEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val movements: List<MaterialMovementEntity> = emptyList(),
    val quoteEvents: List<QuoteEventEntity> = emptyList()
)

/** Row in the Supabase `user_backups` table. One per user, keyed by user_id. */
@Serializable
data class BackupRow(
    @SerialName("user_id") val userId: String,
    val payload: String
)

/**
 * Snapshot backup of the whole local dataset to Supabase, keyed by user_id.
 *
 * Model is last-write-wins per whole account (not per-row): simple, no schema migration, and
 * enough for a single maker on one or two devices. On login we [pull] the cloud snapshot into
 * Room; local writes are auto-[push]ed (debounced) and pushed once more on sign-out.
 *
 * All network work is wrapped so a failure never corrupts local data or blocks auth.
 */
@Singleton
class CloudSync @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val materialDao: MaterialDao,
    private val machineDao: MachineDao,
    private val clientDao: ClientDao,
    private val quotationDao: QuotationDao,
    private val templateDao: TemplateDao,
    private val movementDao: MaterialMovementDao,
    private val quoteEventDao: QuoteEventDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Local writes before this timestamp are ignored by the auto-push observer (echo guard). */
    @Volatile
    private var suppressPushUntil = 0L

    init { observeLocalChanges() }

    /** Watches every table; after a quiet period, mirrors the local snapshot to the cloud. */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeLocalChanges() {
        scope.launch {
            combine(
                listOf(
                    materialDao.observeAll(),
                    machineDao.observeAll(),
                    clientDao.observeAll(),
                    quotationDao.observeAll(),
                    templateDao.observeAll(),
                    movementDao.observeAll(),
                    quoteEventDao.observeAll()
                )
            ) { }
                .drop(1) // skip the initial load emission at app start
                .debounce(2500)
                .collect {
                    if (System.currentTimeMillis() >= suppressPushUntil) push()
                }
        }
    }

    /**
     * Uploads the current local snapshot. Returns true if it reached the cloud. No-op (false)
     * when signed out or on network failure.
     */
    suspend fun push(): Boolean {
        val uid = auth.currentUserOrNull()?.id ?: return false
        val data = BackupData(
            materials = materialDao.getAllOnce(),
            machines = machineDao.getAllOnce(),
            clients = clientDao.getAllOnce(),
            quotations = quotationDao.getAllOnce(),
            templates = templateDao.getAllOnce(),
            movements = movementDao.getAllOnce(),
            quoteEvents = quoteEventDao.getAllOnce()
        )
        val payload = AppJson.encodeToString(BackupData.serializer(), data)
        return runCatching { postgrest.from("user_backups").upsert(BackupRow(uid, payload)) }.isSuccess
    }

    /**
     * Wipes all local tables. Called on sign-out so the next account on this device can't see the
     * previous user's data. Mutes the auto-push observer so the wipe doesn't overwrite the cloud
     * backup with an empty snapshot.
     */
    suspend fun clearLocal() {
        suppressPushUntil = System.currentTimeMillis() + 6000
        materialDao.clearAll()
        machineDao.clearAll()
        clientDao.clearAll()
        quotationDao.clearAll()
        templateDao.clearAll()
        movementDao.clearAll()
        quoteEventDao.clearAll()
    }

    /**
     * Restores the cloud snapshot into Room, replacing local contents. No-op if the user has no
     * cloud backup yet (first login) — local data is preserved and pushed up by the observer.
     */
    suspend fun pull() {
        val uid = auth.currentUserOrNull()?.id ?: return
        val row = runCatching {
            postgrest.from("user_backups")
                .select { filter { eq("user_id", uid) } }
                .decodeSingleOrNull<BackupRow>()
        }.getOrNull() ?: return
        val data = runCatching {
            AppJson.decodeFromString(BackupData.serializer(), row.payload)
        }.getOrNull() ?: return

        // Mute the observer while we rewrite Room so the restore doesn't echo back as a push.
        suppressPushUntil = System.currentTimeMillis() + 6000

        materialDao.clearAll(); materialDao.insertAll(data.materials)
        machineDao.clearAll(); machineDao.insertAll(data.machines)
        clientDao.clearAll(); clientDao.insertAll(data.clients)
        quotationDao.clearAll(); quotationDao.insertAll(data.quotations)
        templateDao.clearAll(); templateDao.insertAll(data.templates)
        movementDao.clearAll(); movementDao.insertAll(data.movements)
        quoteEventDao.clearAll(); quoteEventDao.insertAll(data.quoteEvents)
    }
}
