package com.cuidadopet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cuidadopet.ui.screens.dashboard.PetDashboardScreen
import com.cuidadopet.ui.screens.feeding.MealPlanFormScreen
import com.cuidadopet.ui.screens.home.HomeScreen
import com.cuidadopet.ui.screens.medication.MedicationFormScreen
import com.cuidadopet.ui.screens.pet.PetFormScreen
import com.cuidadopet.ui.screens.health.HealthEntryFormScreen
import com.cuidadopet.ui.screens.settings.PrivacyPolicyScreen
import com.cuidadopet.ui.screens.health.WeightHistoryScreen
import com.cuidadopet.ui.screens.report.ReportScreen
import com.cuidadopet.ui.screens.water.WaterConfigFormScreen

// Todas as rotas do app centralizadas aqui
object Routes {
    const val HOME          = "home"
    const val PET_FORM      = "pet_form?petId={petId}"
    const val DASHBOARD     = "dashboard/{petId}"
    const val MED_FORM      = "med_form/{petId}?medicationId={medicationId}"
    const val MEAL_PLAN     = "meal_plan/{petId}"    // formulário de plano alimentar
    const val WATER_CONFIG   = "water_config/{petId}"   // configuração de hidratação
    const val HEALTH_ENTRY   = "health_entry/{petId}?entryId={entryId}" // diário saúde
    const val WEIGHT_HISTORY = "weight_history/{petId}" // histórico de peso
    const val REPORT         = "report/{petId}"          // relatório do pet
    const val PRIVACY_POLICY = "privacy_policy"           // política de privacidade

    // Funções auxiliares para montar rotas com parâmetros reais
    fun petForm(petId: Long? = null)  = if (petId != null) "pet_form?petId=$petId" else "pet_form"
    fun dashboard(petId: Long)        = "dashboard/$petId"
    fun medForm(petId: Long, medicationId: Long? = null) =
        if (medicationId != null) "med_form/$petId?medicationId=$medicationId"
        else "med_form/$petId"
    fun mealPlan(petId: Long)         = "meal_plan/$petId"
    fun waterConfig(petId: Long)      = "water_config/$petId"
    fun healthEntry(petId: Long, entryId: Long? = null) =
        if (entryId != null) "health_entry/$petId?entryId=$entryId"
        else "health_entry/$petId"
    fun weightHistory(petId: Long)    = "weight_history/$petId"
    fun report(petId: Long)           = "report/$petId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {

        // ── Home — lista de pets ─────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onAddPetClick      = { navController.navigate(Routes.petForm()) },
                onPetClick         = { petId -> navController.navigate(Routes.dashboard(petId)) },
                onPrivacyPolicy    = { navController.navigate(Routes.PRIVACY_POLICY) }
            )
        }

        // ── Política de privacidade ───────────────────────────────────────
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Cadastro / Edição de pet ─────────────────────────────────────
        composable(
            route = Routes.PET_FORM,
            arguments = listOf(navArgument("petId") {
                type = NavType.LongType; defaultValue = -1L
            })
        ) { entry ->
            val petId = entry.arguments?.getLong("petId")?.takeIf { it != -1L }
            PetFormScreen(
                petId          = petId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Dashboard do pet (abas: Hoje, Medicamentos, Alimentação, Saúde...) ─
        composable(
            route = Routes.DASHBOARD,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { entry ->
            val petId = entry.arguments!!.getLong("petId")
            PetDashboardScreen(
                petId                = petId,
                onNavigateBack       = { navController.popBackStack() },
                onEditPet            = { navController.navigate(Routes.petForm(it)) },
                onAddMedication      = { navController.navigate(Routes.medForm(petId)) },
                onEditMedication     = { medId -> navController.navigate(Routes.medForm(petId, medId)) },
                onConfigureMealPlan  = { navController.navigate(Routes.mealPlan(petId)) },
                onConfigureWater     = { navController.navigate(Routes.waterConfig(petId)) },
                onNewHealthEntry     = { navController.navigate(Routes.healthEntry(petId)) },
                onEditHealthEntry    = { navController.navigate(Routes.healthEntry(petId, it)) },
                onWeightHistory      = { navController.navigate(Routes.weightHistory(petId)) },
                onReport             = { navController.navigate(Routes.report(petId)) }
            )
        }

        // ── Cadastro / Edição de medicamento ─────────────────────────────
        composable(
            route = Routes.MED_FORM,
            arguments = listOf(
                navArgument("petId")       { type = NavType.LongType },
                navArgument("medicationId"){ type = NavType.LongType; defaultValue = -1L }
            )
        ) { entry ->
            val petId        = entry.arguments!!.getLong("petId")
            val medicationId = entry.arguments!!.getLong("medicationId").takeIf { it != -1L }
            MedicationFormScreen(
                petId          = petId,
                medicationId   = medicationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Plano alimentar ───────────────────────────────────────────────
        composable(
            route = Routes.MEAL_PLAN,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { entry ->
            val petId = entry.arguments!!.getLong("petId")
            MealPlanFormScreen(
                petId          = petId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Configuração de água ──────────────────────────────────────────
        composable(
            route = Routes.WATER_CONFIG,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { entry ->
            val petId = entry.arguments!!.getLong("petId")
            WaterConfigFormScreen(
                petId          = petId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Formulário de entrada do diário de saúde ──────────────────────
        composable(
            route = Routes.HEALTH_ENTRY,
            arguments = listOf(
                navArgument("petId")   { type = NavType.LongType },
                navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { entry ->
            val petId   = entry.arguments!!.getLong("petId")
            val entryId = entry.arguments!!.getLong("entryId").takeIf { it != -1L }
            HealthEntryFormScreen(
                petId          = petId,
                entryId        = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Histórico de peso com gráfico ─────────────────────────────────
        composable(
            route = Routes.WEIGHT_HISTORY,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { entry ->
            val petId = entry.arguments!!.getLong("petId")
            WeightHistoryScreen(
                petId          = petId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Relatório do pet ──────────────────────────────────────────────────
        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { entry ->
            val petId = entry.arguments!!.getLong("petId")
            ReportScreen(
                petId          = petId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
