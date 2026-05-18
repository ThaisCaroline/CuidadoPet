package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow
import com.cuidadopet.data.db.entity.SporadicMealLogEntity

// DAO unificado para tudo relacionado a alimentação:
// plano alimentar, refeições individuais e registros diários
@Dao
interface FeedingDao {

    // ─── Plano alimentar ────────────────────────────────────────────

    // Busca o plano alimentar ativo do pet
    // LIMIT 1 garante que retornamos no máximo um resultado
    @Query("SELECT * FROM meal_plans WHERE petId = :petId AND isActive = 1 LIMIT 1")
    fun getActiveMealPlan(petId: Long): Flow<MealPlanEntity?>

    // Busca todos os planos ativos do pet — suporta múltiplos planos simultâneos
    @Query("SELECT * FROM meal_plans WHERE petId = :petId AND isActive = 1 ORDER BY createdAt ASC")
    fun getActiveMealPlans(petId: Long): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE petId = :petId AND isActive = 1 ORDER BY createdAt ASC")
    suspend fun getActiveMealPlansOnce(petId: Long): List<MealPlanEntity>

    @Query("SELECT * FROM meal_plans WHERE id = :planId LIMIT 1")
    suspend fun getMealPlanById(planId: Long): MealPlanEntity?

    @Query("SELECT * FROM meal_plans")
    suspend fun getAllMealPlansForBackup(): List<MealPlanEntity>

    @Query("SELECT * FROM meals")
    suspend fun getAllMealsForBackup(): List<MealEntity>

    @Query("SELECT * FROM meal_logs")
    suspend fun getAllMealLogsForBackup(): List<MealLogEntity>

    @Query("SELECT * FROM sporadic_meal_logs")
    suspend fun getAllSporadicLogsForBackup(): List<SporadicMealLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(plan: MealPlanEntity): Long

    @Update
    suspend fun updateMealPlan(plan: MealPlanEntity)

    // Desativa todos os planos do pet de uma vez — usado no fluxo de exclusão
    @Query("UPDATE meal_plans SET isActive = 0 WHERE petId = :petId")
    suspend fun deactivateAllMealPlans(petId: Long)

    // Desativa um plano específico — usado ao editar um dos planos ativos
    @Query("UPDATE meal_plans SET isActive = 0 WHERE id = :planId")
    suspend fun deactivateMealPlan(planId: Long)

    @Query("DELETE FROM meal_plans WHERE id = :planId")
    suspend fun deleteMealPlan(planId: Long)

    // ─── Refeições ──────────────────────────────────────────────────

    // Versão suspend (não-Flow) — usada em operações de escrita como setMealPlan
    @Query("SELECT * FROM meal_plans WHERE petId = :petId AND isActive = 1 LIMIT 1")
    suspend fun getActiveMealPlanOnce(petId: Long): MealPlanEntity?

    // Busca uma refeição pelo ID, mas só se o plano dela ainda estiver ativo.
    // Retorna null se o plano foi substituído — evita reagendar refeições de planos antigos.
    @Query("""
        SELECT meals.* FROM meals
        INNER JOIN meal_plans ON meals.mealPlanId = meal_plans.id
        WHERE meals.id = :mealId AND meal_plans.isActive = 1
        LIMIT 1
    """)
    suspend fun getMealByIdIfActive(mealId: Long): MealEntity?

    // Busca todas as refeições do plano, ordenadas por horário
    @Query("SELECT * FROM meals WHERE mealPlanId = :planId ORDER BY timeOfDay ASC")
    fun getMealsForPlan(planId: Long): Flow<List<MealEntity>>

    // Versão suspend — usada para ler refeições antes de substituir o plano
    @Query("SELECT * FROM meals WHERE mealPlanId = :planId ORDER BY timeOfDay ASC")
    suspend fun getMealsForPlanOnce(planId: Long): List<MealEntity>

    // Migra logs de um mealId antigo para um novo quando o plano é reconfigurado
    @Query("UPDATE meal_logs SET mealId = :newMealId WHERE mealId = :oldMealId")
    suspend fun reassignMealLogs(oldMealId: Long, newMealId: Long)

    // Busca todas as refeições de todos os planos do pet — usado no relatório para
    // resolver mealId de logs que pertencem a planos anteriores (já desativados)
    @Query("""
        SELECT meals.* FROM meals
        INNER JOIN meal_plans ON meals.mealPlanId = meal_plans.id
        WHERE meal_plans.petId = :petId
        ORDER BY meals.timeOfDay ASC
    """)
    fun getAllMealsForPet(petId: Long): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    // Remove todas as refeições de um plano — usado quando o tutor reconfigura o horário
    @Query("DELETE FROM meals WHERE mealPlanId = :planId")
    suspend fun deleteMealsForPlan(planId: Long)

    // ─── Logs de refeição ───────────────────────────────────────────

    // Busca o log de uma refeição específica em um dia específico
    // :date é o timestamp do início do dia (meia-noite)
    @Query("SELECT * FROM meal_logs WHERE mealId = :mealId AND date = :date LIMIT 1")
    fun getLogForMealOnDate(mealId: Long, date: Long): Flow<MealLogEntity?>

    // Versão suspend (não-Flow) — usada em markMeal para leitura pontual
    @Query("SELECT * FROM meal_logs WHERE mealId = :mealId AND date = :date LIMIT 1")
    suspend fun getLogForMealOnDateOnce(mealId: Long, date: Long): MealLogEntity?

    // Busca todos os logs de um pet em um período — para o relatório
    @Query("""
        SELECT ml.* FROM meal_logs ml
        INNER JOIN meals m ON ml.mealId = m.id
        INNER JOIN meal_plans mp ON m.mealPlanId = mp.id
        WHERE mp.petId = :petId
        AND ml.date BETWEEN :startDate AND :endDate
        ORDER BY ml.date ASC
    """)
    fun getLogsForPetInPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(log: MealLogEntity): Long

    @Update
    suspend fun updateMealLog(log: MealLogEntity)

    @Query("DELETE FROM meal_logs WHERE id = :logId")
    suspend fun deleteMealLog(logId: Long)

    // ─── Refeições esporádicas ──────────────────────────────────────

    // Busca os registros extras do pet em um dia específico
    @Query("SELECT * FROM sporadic_meal_logs WHERE petId = :petId AND registeredAt BETWEEN :dayStart AND :dayEnd ORDER BY registeredAt ASC")
    fun getSporadicLogsForDay(petId: Long, dayStart: Long, dayEnd: Long): Flow<List<SporadicMealLogEntity>>

    // Insere um registro de refeição esporádica
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSporadicLog(log: SporadicMealLogEntity): Long

    @Update
    suspend fun updateSporadicLog(log: SporadicMealLogEntity)

    // Remove um registro esporádico pelo id
    @Query("DELETE FROM sporadic_meal_logs WHERE id = :id")
    suspend fun deleteSporadicLog(id: Long)

    // Busca todos os registros esporádicos do pet em um período — para o relatório
    @Query("SELECT * FROM sporadic_meal_logs WHERE petId = :petId AND registeredAt BETWEEN :start AND :end ORDER BY registeredAt ASC")
    suspend fun getSporadicLogsForPeriodOnce(petId: Long, start: Long, end: Long): List<SporadicMealLogEntity>
}
