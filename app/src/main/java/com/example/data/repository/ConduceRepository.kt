package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CompanyProfile
import com.example.data.model.Conduce
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ConduceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun conducesRef(userId: String) =
        db.collection("users").document(userId).collection("conduces")

    private fun settingsRef(userId: String) =
        db.collection("users").document(userId).collection("settings")

    fun observeConduces(): Flow<List<Conduce>> = callbackFlow {
        val uid = requireUserId()
        val path = "users/$uid/conduces"
        val registration = conducesRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(Conduce::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getConduceById(conduceId: String): Result<Conduce?> {
        val uid = requireUserId()
        val path = "users/$uid/conduces/$conduceId"
        return try {
            val doc = conducesRef(uid).document(conduceId).get().await()
            Result.success(doc.toObject(Conduce::class.java))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveConduce(conduce: Conduce): Result<String> {
        val uid = requireUserId()
        val docId = if (conduce.id.isNotBlank()) conduce.id else UUID.randomUUID().toString()
        val docRef = conducesRef(uid).document(docId)
        val isNew = conduce.id.isBlank() || conduce.createdAt == null

        return try {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "conduceNumber" to conduce.conduceNumber,
                "type" to conduce.type,
                "status" to conduce.status,
                "originProject" to conduce.originProject,
                "destinationProject" to conduce.destinationProject,
                "items" to conduce.items
            )

            if (conduce.originResponsible.isNotBlank()) payload["originResponsible"] = conduce.originResponsible
            if (conduce.destinationResponsible.isNotBlank()) payload["destinationResponsible"] = conduce.destinationResponsible
            if (conduce.carrierName.isNotBlank()) payload["carrierName"] = conduce.carrierName
            if (conduce.carrierIdNumber.isNotBlank()) payload["carrierIdNumber"] = conduce.carrierIdNumber
            if (conduce.carrierPhone.isNotBlank()) payload["carrierPhone"] = conduce.carrierPhone
            if (conduce.vehiclePlate.isNotBlank()) payload["vehiclePlate"] = conduce.vehiclePlate
            if (conduce.vehicleDescription.isNotBlank()) payload["vehicleDescription"] = conduce.vehicleDescription
            if (conduce.dispatcherName.isNotBlank()) payload["dispatcherName"] = conduce.dispatcherName
            if (conduce.dispatcherSignature.isNotBlank()) payload["dispatcherSignature"] = conduce.dispatcherSignature
            if (conduce.carrierSignature.isNotBlank()) payload["carrierSignature"] = conduce.carrierSignature
            if (conduce.receiverName.isNotBlank()) payload["receiverName"] = conduce.receiverName
            if (conduce.receiverSignature.isNotBlank()) payload["receiverSignature"] = conduce.receiverSignature
            if (conduce.receiverNotes.isNotBlank()) payload["receiverNotes"] = conduce.receiverNotes
            if (conduce.evidenceCount > 0) payload["evidenceCount"] = conduce.evidenceCount
            if (conduce.notes.isNotBlank()) payload["notes"] = conduce.notes

            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                payload["updatedAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                payload["createdAt"] = conduce.createdAt!!
                payload["updatedAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStatus(conduceId: String, newStatus: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = conducesRef(uid).document(conduceId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteConduce(conduceId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = conducesRef(uid).document(conduceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCompanyProfile(): Flow<CompanyProfile?> = callbackFlow {
        val uid = requireUserId()
        val path = "users/$uid/settings/company"
        val registration = settingsRef(uid).document("company")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    close(error)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(CompanyProfile::class.java)
                trySend(profile)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveCompanyProfile(profile: CompanyProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = settingsRef(uid).document("company")
        return try {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "companyName" to profile.companyName
            )
            if (profile.taxId.isNotBlank()) payload["taxId"] = profile.taxId
            if (profile.phone.isNotBlank()) payload["phone"] = profile.phone
            if (profile.email.isNotBlank()) payload["email"] = profile.email
            if (profile.address.isNotBlank()) payload["address"] = profile.address
            if (profile.logoBase64.isNotBlank()) payload["logoBase64"] = profile.logoBase64

            val isNew = profile.createdAt == null
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                payload["updatedAt"] = FieldValue.serverTimestamp()
            } else {
                payload["createdAt"] = profile.createdAt!!
                payload["updatedAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
