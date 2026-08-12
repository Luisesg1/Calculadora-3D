package com.print3d.calculator.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getById(id: Long): MaterialEntity?

    @Query("SELECT * FROM materials")
    suspend fun getAllOnce(): List<MaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MaterialEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MaterialEntity>)

    @Query("DELETE FROM materials")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: MaterialEntity)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): ClientEntity?

    @Query("SELECT * FROM clients")
    suspend fun getAllOnce(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ClientEntity>)

    @Query("DELETE FROM clients")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: ClientEntity)
}

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE id = :id")
    suspend fun getById(id: Long): MachineEntity?

    @Query("SELECT * FROM machines")
    suspend fun getAllOnce(): List<MachineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MachineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MachineEntity>)

    @Query("DELETE FROM machines")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: MachineEntity)
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY isFavorite DESC, createdAt DESC")
    fun observeAll(): Flow<List<QuotationEntity>>

    @Query(
        "SELECT * FROM quotations WHERE clientName LIKE '%' || :q || '%' " +
            "OR projectName LIKE '%' || :q || '%' OR number LIKE '%' || :q || '%' " +
            "ORDER BY isFavorite DESC, createdAt DESC"
    )
    fun search(q: String): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE id = :id")
    suspend fun getById(id: Long): QuotationEntity?

    @Query("SELECT * FROM quotations")
    suspend fun getAllOnce(): List<QuotationEntity>

    @Query("SELECT COUNT(*) FROM quotations")
    suspend fun count(): Int

    /** Quotes created at or after [since] — used for the free monthly cap. */
    @Query("SELECT COUNT(*) FROM quotations WHERE createdAt >= :since")
    suspend fun countSince(since: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<QuotationEntity>)

    @Query("DELETE FROM quotations")
    suspend fun clearAll()

    @Update
    suspend fun update(entity: QuotationEntity)

    @Delete
    suspend fun delete(entity: QuotationEntity)
}

@Dao
interface MaterialMovementDao {
    @Query("SELECT * FROM material_movements ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MaterialMovementEntity>>

    @Query("SELECT * FROM material_movements WHERE materialId = :materialId ORDER BY timestamp DESC")
    fun observeForMaterial(materialId: Long): Flow<List<MaterialMovementEntity>>

    @Query("SELECT * FROM material_movements")
    suspend fun getAllOnce(): List<MaterialMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MaterialMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MaterialMovementEntity>)

    @Query("DELETE FROM material_movements")
    suspend fun clearAll()

    @Query("DELETE FROM material_movements WHERE materialId = :materialId")
    suspend fun clearForMaterial(materialId: Long)
}

@Dao
interface QuoteEventDao {
    @Query("SELECT * FROM quote_events WHERE quotationId = :quotationId ORDER BY timestamp ASC")
    fun observeForQuote(quotationId: Long): Flow<List<QuoteEventEntity>>

    @Query("SELECT * FROM quote_events")
    fun observeAll(): Flow<List<QuoteEventEntity>>

    @Query("SELECT * FROM quote_events")
    suspend fun getAllOnce(): List<QuoteEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuoteEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<QuoteEventEntity>)

    @Query("DELETE FROM quote_events")
    suspend fun clearAll()

    @Query("DELETE FROM quote_events WHERE quotationId = :quotationId")
    suspend fun clearForQuote(quotationId: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Query("SELECT * FROM templates")
    suspend fun getAllOnce(): List<TemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TemplateEntity>)

    @Query("DELETE FROM templates")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: TemplateEntity)
}
