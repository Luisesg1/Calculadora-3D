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
    val densityG: Double?
)

@Serializable
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
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
    val status: String = "DRAFT"
)

@Serializable
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inputJson: String
)
