package com.cuidadopet.data.repository

import com.cuidadopet.data.db.dao.FeedingDao
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.notification.MealAlarmScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// Repositório de alimentação — centraliza toda a lógica de acesso ao banco
// para planos alimentares, refeições individuais e logs diários.
// Também coordena o agendamento de lembretes de refeição.
@Singleton
class FeedingRepository @Inject constructor(
    private val feedingDao: FeedingDao,
    private val mealAlarmScheduler: MealAlarmScheduler
) {

    // ─── Plano alimentar ────────────────────────────────────────────────────

    // Retorna o plano ativo do pet como Flow — mantido para compatibilidade com ReportRepository
    fun getActiveMealPlan(petId: Long): Flow<MealPlanEntity?> =
        feedingDao.getActiveMealPlan(petId)

    // Retorna todos os planos ativos do pet — suporta múltiplos planos simultâneos
    fun getActiveMealPlans(petId: Long): Flow<List<MealPlanEntity>> =
        feedingDao.getActiveMealPlans(petId)

    // Busca um plano específico pelo ID — usado ao editar um plano existente
    suspend fun getMealPlanById(planId: Long): MealPlanEntity? =
        feedingDao.getMealPlanById(planId)

    // Cria um novo plano ou substitui um plano existente.
    // planIdToReplace == null → adiciona novo plano sem desativar os outros.
    // planIdToReplace != null → desativa só aquele plano, migra logs e cria o novo.
    suspend fun setMealPlan(
        plan: MealPlanEntity,
        meals: List<MealEntity>,
        petName: String,
        planIdToReplace: Long? = null
    ) {
        val oldMeals = if (planIdToReplace != null)
            feedingDao.getMealsForPlanOnce(planIdToReplace)
        else
            emptyList()

        if (planIdToReplace != null) {
            feedingDao.deactivateMealPlan(planIdToReplace)
        }

        val planId = feedingDao.insertMealPlan(plan)

        val newMeals = meals.map { meal ->
            val newId = feedingDao.insertMeal(meal.copy(mealPlanId = planId))
            meal.copy(mealPlanId = planId, id = newId)
        }

        if (planIdToReplace != null) {
            val oldMealsByTime = oldMeals.associateBy { it.timeOfDay }
            newMeals.forEach { newMeal ->
                val oldMeal = oldMealsByTime[newMeal.timeOfDay]
                if (oldMeal != null) {
                    feedingDao.reassignMealLogs(oldMeal.id, newMeal.id)
                }
            }
            oldMeals.forEach { mealAlarmScheduler.cancelMeal(it.id) }
        }

        newMeals.forEach { mealAlarmScheduler.scheduleMeal(it, petName) }
    }

    suspend fun deleteMealPlan(petId: Long, planId: Long) {
        val oldMeals = feedingDao.getMealsForPlanOnce(planId)
        oldMeals.forEach { mealAlarmScheduler.cancelMeal(it.id) }
        feedingDao.deleteMealPlan(planId)
    }

    // ─── Refeições ──────────────────────────────────────────────────────────

    // Busca refeição pelo ID apenas se o plano ainda estiver ativo — usado pelo receiver
    // para reagendar com dados atuais em vez de usar extras stale do Intent.
    suspend fun getMealByIdIfActive(mealId: Long): MealEntity? =
        feedingDao.getMealByIdIfActive(mealId)

    // Lista as refeições de um plano, ordenadas por horário (já garantido pelo DAO)
    fun getMealsForPlan(planId: Long): Flow<List<MealEntity>> =
        feedingDao.getMealsForPlan(planId)

    // Lista as refeições de todos os planos do pet — necessário no relatório para
    // resolver mealId de logs que referenciam planos anteriores já desativados
    fun getAllMealsForPet(petId: Long): Flow<List<MealEntity>> =
        feedingDao.getAllMealsForPet(petId)

    // ─── Logs de refeição ───────────────────────────────────────────────────

    // Busca o log de uma refeição específica no dia de hoje
    // :date deve ser o timestamp de meia-noite do dia desejado
    fun getLogForMealOnDate(mealId: Long, date: Long): Flow<MealLogEntity?> =
        feedingDao.getLogForMealOnDate(mealId, date)

    // Versão suspend (não-Flow) — usada em markMeal para leitura pontual
    suspend fun getLogForMealOnDateOnce(mealId: Long, date: Long): MealLogEntity? =
        feedingDao.getLogForMealOnDateOnce(mealId, date)

    // Histórico de logs de um pet em um período — usado na tela de relatório
    fun getLogsForPetInPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<MealLogEntity>> =
        feedingDao.getLogsForPetInPeriod(petId, startDate, endDate)

    // Salva ou atualiza o registro de uma refeição (o que o pet comeu)
    // O DAO usa OnConflictStrategy.REPLACE, então serve para insert e update
    suspend fun saveMealLog(log: MealLogEntity): Long =
        feedingDao.insertMealLog(log)

    suspend fun updateMealLog(log: MealLogEntity) =
        feedingDao.updateMealLog(log)

    suspend fun deleteMealLog(logId: Long) =
        feedingDao.deleteMealLog(logId)

    // ─── Refeições esporádicas ──────────────────────────────────────────────

    // Busca os registros extras do pet em um dia específico
    fun getSporadicLogsForDay(petId: Long, dayStart: Long, dayEnd: Long): Flow<List<SporadicMealLogEntity>> =
        feedingDao.getSporadicLogsForDay(petId, dayStart, dayEnd)

    // Salva uma refeição esporádica (ex: petisco fora do plano)
    suspend fun saveSporadicLog(log: SporadicMealLogEntity): Long =
        feedingDao.insertSporadicLog(log)

    suspend fun updateSporadicLog(log: SporadicMealLogEntity) =
        feedingDao.updateSporadicLog(log)

    // Remove um registro esporádico
    suspend fun deleteSporadicLog(id: Long) =
        feedingDao.deleteSporadicLog(id)

    // Busca todos os registros esporádicos do pet em um período — para o relatório
    suspend fun getSporadicLogsForPeriod(petId: Long, start: Long, end: Long): List<SporadicMealLogEntity> =
        feedingDao.getSporadicLogsForPeriodOnce(petId, start, end)
}
