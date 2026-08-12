package com.print3d.calculator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MaterialEntity::class, MachineEntity::class, QuotationEntity::class, ClientEntity::class,
        TemplateEntity::class, MaterialMovementEntity::class, QuoteEventEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun machineDao(): MachineDao
    abstract fun quotationDao(): QuotationDao
    abstract fun clientDao(): ClientDao
    abstract fun templateDao(): TemplateDao
    abstract fun materialMovementDao(): MaterialMovementDao
    abstract fun quoteEventDao(): QuoteEventDao

    companion object {
        const val NAME = "print3d.db"

        /** v2: printers gained a dedicated brand column. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE machines ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v3: dedicated clients table. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS clients (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, " +
                        "address TEXT NOT NULL, notes TEXT NOT NULL)"
                )
            }
        }

        /** v4: quotes gained a lifecycle status (draft/sent/accepted/rejected). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotations ADD COLUMN status TEXT NOT NULL DEFAULT 'DRAFT'")
            }
        }

        /** v5: reusable quote templates. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS templates (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, inputJson TEXT NOT NULL)"
                )
            }
        }

        /** v6: CRM lifecycle (due date + per-state timestamps + event log) and material inventory. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Quotations: due date, modification time, stock guard and lifecycle timestamps.
                db.execSQL("ALTER TABLE quotations ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE quotations SET updatedAt = createdAt")
                db.execSQL("ALTER TABLE quotations ADD COLUMN dueDate INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN stockDeducted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE quotations ADD COLUMN sentAt INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN viewedAt INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN acceptedAt INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN rejectedAt INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN productionStartedAt INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN deliveredAt INTEGER")

                // Materials: stock tracking. Backfill remaining stock to the full spool.
                db.execSQL("ALTER TABLE materials ADD COLUMN currentWeightG REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE materials SET currentWeightG = spoolWeightG")
                db.execSQL("ALTER TABLE materials ADD COLUMN minStockG REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE materials ADD COLUMN purchaseDate INTEGER")

                // Clients: optional tax id.
                db.execSQL("ALTER TABLE clients ADD COLUMN rut TEXT NOT NULL DEFAULT ''")

                // Stock ledger.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS material_movements (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "materialId INTEGER NOT NULL, delta REAL NOT NULL, reason TEXT NOT NULL, " +
                        "previousWeightG REAL NOT NULL, newWeightG REAL NOT NULL, " +
                        "timestamp INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', quotationId INTEGER)"
                )

                // Quote status audit trail.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS quote_events (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "quotationId INTEGER NOT NULL, status TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')"
                )
            }
        }
    }
}
