package com.print3d.calculator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String,
    val color: String,
    val spoolWeightG: Double,
    val spoolPrice: Double,
    val diameterMm: Double,
    val densityG: Double?,
    val currentWeightG: Double = spoolWeightG,
    val minStockG: Double = 0.0,
    val purchaseDate: Long? = null
)

@Serializable
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rut: String = "",
    val phone: String,
    val email: String,
    val address: String,
    val notes: String
)

@Serializable
@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String = "",
    val model: String,
    val price: Double,
    val lifespanH: Double,
    val powerW: Double,
    val hourCost: Double,
    val notes: String
)

@Serializable
@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val createdAt: Long,
    val clientName: String,
    val projectName: String,
    val currencyCode: String,
    val total: Double,
    val isFavorite: Boolean,
    val inputJson: String,
    val resultJson: String,
    val status: String = "DRAFT",
    val updatedAt: Long = createdAt,
    val dueDate: Long? = null,
    val stockDeducted: Boolean = false,
    val sentAt: Long? = null,
    val viewedAt: Long? = null,
    val acceptedAt: Long? = null,
    val rejectedAt: Long? = null,
    val productionStartedAt: Long? = null,
    val deliveredAt: Long? = null
)

@Serializable
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inputJson: String
)

@Serializable
@Entity(tableName = "material_movements")
data class MaterialMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val delta: Double,
    val reason: String,
    val previousWeightG: Double,
    val newWeightG: Double,
    val timestamp: Long,
    val note: String = "",
    val quotationId: Long? = null
)

@Serializable
@Entity(tableName = "quote_events")
data class QuoteEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationId: Long,
    val status: String,
    val timestamp: Long,
    val note: String = ""
)
