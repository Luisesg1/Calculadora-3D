package com.print3d.calculator.di

import android.content.Context
import androidx.room.Room
import com.print3d.calculator.data.local.AppDatabase
import com.print3d.calculator.data.local.ClientDao
import com.print3d.calculator.data.local.MachineDao
import com.print3d.calculator.data.local.MaterialDao
import com.print3d.calculator.data.local.QuotationDao
import com.print3d.calculator.data.local.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMaterialDao(db: AppDatabase): MaterialDao = db.materialDao()

    @Provides
    fun provideMachineDao(db: AppDatabase): MachineDao = db.machineDao()

    @Provides
    fun provideQuotationDao(db: AppDatabase): QuotationDao = db.quotationDao()

    @Provides
    fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()
}
