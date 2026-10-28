package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CompanyProfile
import com.example.data.model.Conduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.UUID

class ConduceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveConduce_authenticatedUser_createsDocument() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConduceRepository(firestore, auth)

    val conduce = Conduce(
      conduceNumber = "CND-2026-TEST",
      type = "Traslado",
      status = "Borrador",
      originProject = "Obra Norte",
      destinationProject = "Obra Sur",
      userId = uid
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveConduce(conduce)
    }

    assertTrue(result.isSuccess)
    val docId = result.getOrThrow()
    assertTrue(docId.isNotEmpty())

    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getConduceById(docId)
    }
    assertTrue(fetched.isSuccess)
    assertEquals("CND-2026-TEST", fetched.getOrNull()?.conduceNumber)
  }

  @Test
  fun observeConduces_authenticatedUser_emitsList() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConduceRepository(firestore, auth)

    val randomSuffix = UUID.randomUUID().toString().take(6)
    val conduce = Conduce(
      conduceNumber = "CND-$randomSuffix",
      type = "Entrega",
      status = "En Tránsito",
      originProject = "Almacén Central",
      destinationProject = "Torre Titanium",
      userId = uid
    )

    val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveConduce(conduce).getOrThrow()
    }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeConduces().first { list -> list.any { it.id == docId } }
    }
    assertTrue(emitted.map { it.id }.contains(docId))
  }

  @Test
  fun observeConduces_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = ConduceRepository(firestore, auth)

    var threwExpected = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeConduces().first()
      }
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      threwExpected = true
    } catch (e: Exception) {
      threwExpected = e is IllegalStateException
    }
    assertTrue(threwExpected)
  }

  @Test
  fun saveCompanyProfile_authenticatedUser_savesAndObserves() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ConduceRepository(firestore, auth)

    val profile = CompanyProfile(
      companyName = "Constructora del Caribe SRL",
      taxId = "1-31-00293-1",
      phone = "809-555-4321",
      userId = uid
    )

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveCompanyProfile(profile)
    }
    assertTrue(saveResult.isSuccess)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeCompanyProfile().first { it != null && it.companyName == "Constructora del Caribe SRL" }
    }
    assertEquals("Constructora del Caribe SRL", observed?.companyName)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
