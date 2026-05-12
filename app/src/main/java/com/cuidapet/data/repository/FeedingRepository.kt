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

    // Retorna o plano ativo do pet como Flow — a tela reage automaticamente a mudanças
    fun getActiveMealPlan(petId: Long): Flow<MealPlanEntity?> =
        feedingDao.getActiveMealPlan(petId)

    // Cria um novo plano alimentar para o pet.
    // Desativa qualquer plano anterior antes de inserir o novo —
    // um pet só tem um plano ativo por vez.
    suspend fun setMealPlan(plan: MealPlanEntity, meals: List<MealEntity>, petName: String) {
        // Lê as refeições do plano ATUAL antes de desativá-lo para poder migrar
        // os logs existentes: se o tutor só mudou a porção de um horário já marcado,
        // o registro do dia não pode sumir da aba Hoje nem duplicar no relatório.
        val currentPlan = feedingDao.getActiveMealPlanOnce(plan.petId)
        val oldMeals = if (currentPlan != null)
            feedingDao.getMealsForPlanOnce(currentPlan.id)
        else
            emptyList()

        // Desativa planos antigos para não acumular registros ativos
        feedingDao.deactivateAllMealPlans(plan.petId)

        // Insere o novo plano e obtém o ID gerado pelo banco
        val planId = feedingDao.insertMealPlan(plan)

        // Insere as refeições uma a uma para capturar o ID real gerado pelo banco —
        // necessário para reassociar os logs na etapa seguinte
        val newMeals = meals.map { meal ->
            val newId = feedingDao.insertMeal(meal.copy(mealPlanId = planId))
            meal.copy(mealPlanId = planId, id = newId)
        }

        // Para cada novo horário que existia no plano anterior, migra os logs:
        // atualiza mealId nos registros antigos para apontar para o novo meal do mesmo horário
        val oldMealsByTime = oldMeals.associateBy { it.timeOfDay }
        newMeals.forEach { newMeal ->
            val oldMeal = oldMealsByTime[newMeal.timeOfDay]
            if (oldMeal != null) {
                feedingDao.reassignMealLogs(oldMeal.id, newMeal.id)
            }
        }

        // Cancela lembretes antigos e agenda os novos com os horários corretos
        mealAlarmScheduler.cancelAllForPet(plan.petId)
        newMeals.forEach { meal ->
            mealAlarmScheduler.scheduleMeal(meal, petName)
        }
    }

    suspend fun deleteMealPlan(petId: Long, planId: Long) {
        mealAlarmScheduler.cancelAllForPet(petId)
        feedingDao.deleteMealPlan(planId)
    }

    // ─── Refeições ──────────────────────────────────────────────────────────

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
