package com.cuidadopet.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuidadopet.data.db.AppDatabase
import com.cuidadopet.data.db.entity.WeightRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HealthDao

    // Banco em memória — sem SQLCipher, sem disco, zerado a cada teste
    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.healthDao()
    }

    @After
    fun teardown() = db.close()

    // ── getWeightRecordForDate ────────────────────────────────────────────────

    @Test
    fun getWeightRecordForDate_retornaRegistroCorreto() = runTest {
        val date = 1_000_000L
        dao.insertWeightRecord(WeightRecordEntity(petId = 1L, date = date, weightKg = 5.0))

        val result = dao.getWeightRecordForDate(petId = 1L, date = date)

        assertNotNull(result)
        assertEquals(5.0, result!!.weightKg, 0.001)
    }

    @Test
    fun getWeightRecordForDate_retornaNull_quandoNaoExiste() = runTest {
        assertNull(dao.getWeightRecordForDate(petId = 1L, date = 1_000_000L))
    }

    // Reproduz o bug original: peso do pet 2 não pode aparecer na busca do pet 1
    @Test
    fun getWeightRecordForDate_naoConfundePets() = runTest {
        val date = 1_000_000L
        dao.insertWeightRecord(WeightRecordEntity(petId = 1L, date = date, weightKg = 5.0))
        dao.insertWeightRecord(WeightRecordEntity(petId = 2L, date = date, weightKg = 10.0))

        assertEquals(5.0,  dao.getWeightRecordForDate(petId = 1L, date = date)?.weightKg ?: -1.0, 0.001)
        assertEquals(10.0, dao.getWeightRecordForDate(petId = 2L, date = date)?.weightKg ?: -1.0, 0.001)
    }

    @Test
    fun getWeightRecordForDate_naoConfundeDatas() = runTest {
        val ontem = 0L
        val hoje  = 86_400_000L
        dao.insertWeightRecord(WeightRecordEntity(petId = 1L, date = ontem, weightKg = 4.5))
        dao.insertWeightRecord(WeightRecordEntity(petId = 1L, date = hoje,  weightKg = 5.0))

        assertEquals(4.5, dao.getWeightRecordForDate(petId = 1L, date = ontem)?.weightKg ?: -1.0, 0.001)
        assertEquals(5.0, dao.getWeightRecordForDate(petId = 1L, date = hoje )?.weightKg ?: -1.0, 0.001)
    }

    // ── updateWeightRecord ────────────────────────────────────────────────────

    @Test
    fun updateWeightRecord_atualizaPesoSemCriarRegistroDuplicado() = runTest {
        val date = 1_000_000L
        dao.insertWeightRecord(WeightRecordEntity(petId = 1L, date = date, weightKg = 5.0))

        val existing = dao.getWeightRecordForDate(petId = 1L, date = date)!!
        dao.updateWeightRecord(existing.copy(weightKg = 6.0))

        val result = dao.getWeightRecordForDate(petId = 1L, date = date)
        assertNotNull(result)
        assertEquals(6.0, result!!.weightKg, 0.001)
    }

    @Test
    fun updateWeightRecord_naoAfetaOutroPet() = runTest {
        val date = 1_000_000L
        dao.insertWeightRecord(WeightRecordEntity(petId = 1L, date = date, weightKg = 5.0))
        dao.insertWeightRecord(WeightRecordEntity(petId = 2L, date = date, weightKg = 10.0))

        val existing = dao.getWeightRecordForDate(petId = 1L, date = date)!!
        dao.updateWeightRecord(existing.copy(weightKg = 6.0))

        // Pet 2 não pode ter sido alterado
        assertEquals(10.0, dao.getWeightRecordForDate(petId = 2L, date = date)?.weightKg ?: -1.0, 0.001)
    }
}
