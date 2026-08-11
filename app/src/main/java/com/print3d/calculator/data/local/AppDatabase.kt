package com.print3d.calculator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MaterialEntity::class, MachineEntity::class, QuotationEntity::class, ClientEntity::class, TemplateEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun machineDao(): MachineDao
    abstract fun quotationDao(): QuotationDao
    abstract fun clientDao(): ClientDao
    abstract fun templateDao(): TemplateDao

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
    }
}
