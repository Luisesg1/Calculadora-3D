package com.print3d.calculator.data.repo

import com.print3d.calculator.data.local.ClientEntity
import com.print3d.calculator.data.local.MachineEntity
import com.print3d.calculator.data.local.MaterialEntity
import com.print3d.calculator.data.local.MaterialMovementEntity
import com.print3d.calculator.data.local.QuotationEntity
import com.print3d.calculator.data.local.QuoteEventEntity
import com.print3d.calculator.data.local.TemplateEntity
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.QuoteInput
import com.print3d.calculator.domain.model.QuoteResult
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteTemplate
import kotlinx.serialization.json.Json

val AppJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun MaterialEntity.toDomain() = Material(
    id, name, brand, color, spoolWeightG, spoolPrice, diameterMm, densityG,
    currentWeightG, minStockG, purchaseDate
)

fun Material.toEntity() = MaterialEntity(
    id, name, brand, color, spoolWeightG, spoolPrice, diameterMm, densityG,
    currentWeightG, minStockG, purchaseDate
)

fun ClientEntity.toDomain() = Client(id, name, rut, phone, email, address, notes)

fun Client.toEntity() = ClientEntity(id, name, rut, phone, email, address, notes)

fun MachineEntity.toDomain() = Machine(
    id, name, brand, model, price, lifespanH, powerW, hourCost, notes
)

fun Machine.toEntity() = MachineEntity(
    id, name, brand, model, price, lifespanH, powerW, hourCost, notes
)

fun QuotationEntity.toDomain() = Quotation(
    id = id,
    number = number,
    createdAt = createdAt,
    input = AppJson.decodeFromString(QuoteInput.serializer(), inputJson),
    result = AppJson.decodeFromString(QuoteResult.serializer(), resultJson),
    currencyCode = currencyCode,
    isFavorite = isFavorite,
    status = com.print3d.calculator.domain.model.QuoteStatus.fromName(status),
    updatedAt = updatedAt,
    dueDate = dueDate,
    stockDeducted = stockDeducted,
    sentAt = sentAt,
    viewedAt = viewedAt,
    acceptedAt = acceptedAt,
    rejectedAt = rejectedAt,
    productionStartedAt = productionStartedAt,
    deliveredAt = deliveredAt
)

fun Quotation.toEntity() = QuotationEntity(
    id = id,
    number = number,
    createdAt = createdAt,
    clientName = input.clientName,
    projectName = input.projectName,
    currencyCode = currencyCode,
    total = result.total,
    isFavorite = isFavorite,
    inputJson = AppJson.encodeToString(QuoteInput.serializer(), input),
    resultJson = AppJson.encodeToString(QuoteResult.serializer(), result),
    status = status.name,
    updatedAt = updatedAt,
    dueDate = dueDate,
    stockDeducted = stockDeducted,
    sentAt = sentAt,
    viewedAt = viewedAt,
    acceptedAt = acceptedAt,
    rejectedAt = rejectedAt,
    productionStartedAt = productionStartedAt,
    deliveredAt = deliveredAt
)

fun MaterialMovementEntity.toDomain() = com.print3d.calculator.domain.model.MaterialMovement(
    id = id,
    materialId = materialId,
    delta = delta,
    reason = com.print3d.calculator.domain.model.MovementReason.fromName(reason),
    previousWeightG = previousWeightG,
    newWeightG = newWeightG,
    timestamp = timestamp,
    note = note,
    quotationId = quotationId
)

fun com.print3d.calculator.domain.model.MaterialMovement.toEntity() = MaterialMovementEntity(
    id = id,
    materialId = materialId,
    delta = delta,
    reason = reason.name,
    previousWeightG = previousWeightG,
    newWeightG = newWeightG,
    timestamp = timestamp,
    note = note,
    quotationId = quotationId
)

fun QuoteEventEntity.toDomain() = com.print3d.calculator.domain.model.QuoteEvent(
    id = id,
    quotationId = quotationId,
    status = com.print3d.calculator.domain.model.QuoteStatus.fromName(status),
    timestamp = timestamp,
    note = note
)

fun com.print3d.calculator.domain.model.QuoteEvent.toEntity() = QuoteEventEntity(
    id = id,
    quotationId = quotationId,
    status = status.name,
    timestamp = timestamp,
    note = note
)

fun TemplateEntity.toDomain() = QuoteTemplate(
    id = id,
    name = name,
    input = AppJson.decodeFromString(QuoteInput.serializer(), inputJson)
)

fun QuoteTemplate.toEntity() = TemplateEntity(
    id = id,
    name = name,
    inputJson = AppJson.encodeToString(QuoteInput.serializer(), input)
)
