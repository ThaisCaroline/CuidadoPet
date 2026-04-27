package com.cuidadopet.data.repository

import com.cuidadopet.data.db.dao.FeedingDao
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
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
        // Desativa planos antigos para não acumular registros ativos
        feedingDao.deactivateAllMealPlans(plan.petId)

        // Insere o novo plano e obtém o ID gerado pelo banco
        val planId = feedingDao.insertMealPlan(plan)

        // Cria as refeições associadas a este plano, vinculando ao ID real
        val mealsWithPlanId = meals.map { it.copy(mealPlanId = planId) }
        feedingDao.insertMeals(mealsWithPlanId)

        // Cancela lembretes antigos e agenda os novos com os horários corretos
        mealAlarmScheduler.cancelAllForPet(plan.petId)
        mealsWithPlanId.forEach { meal ->
            // Cada refeição recebe um alarme diário recorrente no horário configurado
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
}
