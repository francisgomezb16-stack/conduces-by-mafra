package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ConduceItem(
    val id: String = "",
    val description: String = "",
    val category: String = "Material", // Material, Herramienta, Equipo, Otro
    val quantity: Double = 1.0,
    val unit: String = "Uds", // Uds, Sacos, m³, kg, Piezas, Cajas, Metros, Galones
    val serialOrCode: String = "",
    val condition: String = "Bueno", // Nuevo, Bueno, Usado, Regular
    val notes: String = ""
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "description" to description,
            "category" to category,
            "quantity" to quantity,
            "unit" to unit,
            "condition" to condition
        )
        if (serialOrCode.isNotBlank()) map["serialOrCode"] = serialOrCode
        if (notes.isNotBlank()) map["notes"] = notes
        return map
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ConduceItem {
            return ConduceItem(
                id = (map["id"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                category = (map["category"] as? String) ?: "Material",
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0,
                unit = (map["unit"] as? String) ?: "Uds",
                serialOrCode = (map["serialOrCode"] as? String) ?: "",
                condition = (map["condition"] as? String) ?: "Bueno",
                notes = (map["notes"] as? String) ?: ""
            )
        }
    }
}

@IgnoreExtraProperties
data class Conduce(
    @DocumentId val id: String = "",
    val conduceNumber: String = "",
    val type: String = "Traslado", // Entrega, Traslado, Devolución
    val status: String = "Borrador", // Borrador, En Tránsito, Entregado, Anulado
    val originProject: String = "",
    val originResponsible: String = "",
    val destinationProject: String = "",
    val destinationResponsible: String = "",
    val carrierName: String = "",
    val carrierIdNumber: String = "",
    val carrierPhone: String = "",
    val vehiclePlate: String = "",
    val vehicleDescription: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val dispatcherName: String = "",
    val dispatcherSignature: String = "",
    val carrierSignature: String = "",
    val receiverName: String = "",
    val receiverSignature: String = "",
    val receiverNotes: String = "",
    val evidenceCount: Long = 0L,
    val notes: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun getParsedItems(): List<ConduceItem> {
        return items.map { ConduceItem.fromMap(it) }
    }
}

@IgnoreExtraProperties
data class CompanyProfile(
    @DocumentId val id: String = "company",
    val companyName: String = "Pedralbes",
    val taxId: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val logoBase64: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val defaultResponsible: String = "",
    val phone: String = "",
    val thumbnailBase64: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "info",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "Administrador", // Administrador, Supervisor / Ing. Residente, Operador / Despachador
    val securityLevel: Int = 1, // 1 = Admin, 2 = Supervisor, 3 = Operador
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun canDeleteConduce(): Boolean = securityLevel <= 2
}

@IgnoreExtraProperties
data class DeletionAuditLog(
    @DocumentId val id: String = "",
    val conduceId: String = "",
    val conduceNumber: String = "",
    val originProject: String = "",
    val destinationProject: String = "",
    val carrierName: String = "",
    val itemsSummary: String = "",
    val totalItems: Int = 0,
    val deletedByUid: String = "",
    val deletedByName: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "Administrador",
    val securityLevel: Int = 1,
    val deletionReason: String = "",
    val pdfDeleted: Boolean = true,
    val userId: String = "",
    @ServerTimestamp val deletedAt: Timestamp? = null
)

