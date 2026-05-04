package com.cuidadopet.data.repository

import com.cuidadopet.domain.PetReport
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// Agrega dados de todos os repositórios para montar o PetReport.
// É um repositório "orquestrador" — não tem DAO próprio, apenas combina os outros.
// O Hilt injeta todos os repositórios automaticamente porque todos têm @Inject constructor.
@Singleton
class ReportRepository @Inject constructor(
    private val petRepository: PetRepository,
    private val medicationRepository: MedicationRepository,
    private val feedingRepository: FeedingRepository,
    private val waterRepository: WaterRepository,
    private val healthRepository: HealthRepository,
    private val photoRepository: HealthPhotoRepository
) {

    // Coleta todos os dados do pet para o período informado e retorna o PetReport.
    // suspend = deve ser chamado dentro de uma coroutine (o ViewModel faz isso).
    // .first() pega o valor atual do Flow e cancela a coleta — é uma consulta pontual,
    // não um listener reativo. Perfeito para gerar um relatório snapshot.
    suspend fun buildReport(petId: Long, periodDays: Int = 7): PetReport? {
        // Define o intervalo: de meia-noite de `periodDays` dias atrás até agora
        val endDate   = System.currentTimeMillis()
        val startDate = startOfDayMinus(periodDays)

        val pet = petRepository.getPetById(petId).first() ?: return null

        // Medicamentos ativos (independente de período — são os que estão em uso agora)
        val meds = medicationRepository.getActiveMedications(petId).first()
        val medLogs = medicationRepository.getLogsForPetInPeriod(petId, startDate, endDate).first()

        // Plano alimentar ativo
        val plan = feedingRepository.getActiveMealPlan(petId).first()
        // Carrega refeições de TODOS os planos do pet — os logs podem referenciar
        // refeições de planos anteriores (desativados ao editar o plano)
        val meals = feedingRepository.getAllMealsForPet(petId).first()

        // Logs de refeição no período
        val mealLogs = feedingRepository.getLogsForPetInPeriod(petId, startDate, endDate).first()

        // Extras esporádicos no período
        val sporadicLogs = feedingRepository.getSporadicLogsForPeriod(petId, startDate, endDate)

        // Logs de água no período
        val waterLogs = waterRepository.getLogsForPeriod(petId, startDate, endDate).first()

        // Entradas do diário de saúde no período
        val healthEntries = healthRepository.getEntriesForPeriod(petId, startDate, endDate).first()

        // Todo o histórico de peso — útil para ver tendência além do período selecionado
        val weightRecords = healthRepository.getAllWeightRecords(petId).first()

        val photos = photoRepository.getForPeriod(petId, startDate, endDate)

        return PetReport(
            pet               = pet,
            periodStart       = startDate,
            periodEnd         = endDate,
            activeMedications = meds,
            medicationLogs    = medLogs,
            mealPlan          = plan,
            meals             = meals,
            mealLogs          = mealLogs,
            sporadicLogs      = sporadicLogs,
            waterLogs         = waterLogs,
            healthEntries     = healthEntries,
            weightRecords     = weightRecords,
            photos            = photos
        )
    }

    // Retorna o timestamp de meia-noite de X dias atrás
    // Ex: startOfDayMinus(7) = meia-noite de uma semana atrás
    private fun startOfDayMinus(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
