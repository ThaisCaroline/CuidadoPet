package com.cuidadopet.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.cuidadopet.ui.screens.onboarding.OnboardingScreen
import com.cuidadopet.ui.screens.pet.PetFormScreen
import com.cuidadopet.ui.screens.health.HealthEntryFormScreen
import com.cuidadopet.ui.screens.settings.PrivacyPolicyScreen
import com.cuidadopet.ui.screens.settings.SettingsScreen
import com.cuidadopet.ui.screens.health.WeightHistoryScreen
import com.cuidadopet.ui.screens.report.ReportScreen
import com.cuidadopet.ui.screens.vaccine.VaccineFormScreen
import com.cuidadopet.ui.screens.vaccine.VaccineListScreen
import com.cuidadopet.ui.screens.paywall.PaywallScreen
import com.cuidadopet.ui.screens.water.WaterConfigFormScreen

private const val PREFS_NAME       = "app_prefs"
private const val KEY_ONBOARDING   = "onboarding_done"

object Routes {
    const val ONBOARDING    = "onboarding"
    const val HOME          = "home"
    const val PET_FORM      = "pet_form?petId={petId}"
    const val DASHBOARD     = "dashboard/{petId}"
    const val MED_FORM      = "med_form/{petId}?medicationId={medicationId}"
    const val MEAL_PLAN     = "meal_plan/{petId}?planId={planId}"    // formulário de plano alimentar
    const val WATER_CONFIG   = "water_config/{petId}"   // configuração de hidratação
    const val HEALTH_ENTRY   = "health_entry/{petId}?entryId={entryId}" // diário saúde
    const val WEIGHT_HISTORY = "weight_history/{petId}" // histórico de peso
    const val REPORT         = "report/{petId}"          // relatório do pet
    const val PRIVACY_POLICY = "privacy_policy"           // política de privacidade
    const val SETTINGS       = "settings"                  // configurações / backup
    const val PAYWALL        = "paywall"                   // assinatura premium
    const val VACCINE_LIST   = "vaccine_list/{petId}"
    const val VACCINE_FORM   = "vaccine_form/{petId}?vaccineId={vaccineId}"

    // Funções auxiliares para montar rotas com parâmetros reais
    fun petForm(petId: Long? = null)  = if (petId != null) "pet_form?petId=$petId" else "pet_form"
    fun dashboard(petId: Long)        = "dashboard/$petId"
    fun medForm(petId: Long, medicationId: Long? = null) =
        if (medicationId != null) "med_form/$petId?medicationId=$medicationId"
        else "med_form/$petId"
    fun mealPlan(petId: Long, planId: Long? = null) =
        if (planId != null) "meal_plan/$petId?planId=$planId" else "meal_plan/$petId"
    fun waterConfig(petId: Long)      = "water_config/$petId"
    fun healthEntry(petId: Long, entryId: Long? = null) =
        if (entryId != null) "health_entry/$petId?entryId=$entryId"
        else "health_entry/$petId"
    fun weightHistory(petId: Long)    = "weight_history/$petId"
    fun report(petId: Long)           = "report/$petId"
    fun vaccineList(petId: Long)      = "vaccine_list/$petId"
    fun vaccineForm(petId: Long, vaccineId: Long? = null) =
        if (vaccineId != null) "vaccine_form/$petId?vaccineId=$vaccineId"
        else "vaccine_form/$petId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context        = LocalContext.current
    val prefs          = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val startDestination = remember {
        if (prefs.getBoolean(KEY_ONBOARDING, false)) Routes.HOME else Routes.ONBOARDING
    }

    val lifecycleOwner   = LocalLifecycleOwner.current
    val resumeTrigger    = remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTrigger.intValue++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeTrigger.intValue) {
        val petId = prefs.getLong("open_today_pet_id", -1L)
        if (petId != -1L) {
            prefs.edit().remove("open_today_pet_id").apply()
            navController.navigate(Routes.dashboard(petId)) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Onboarding — exibido apenas no primeiro acesso ───────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ── Home — lista de pets ─────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onAddPetClick   = { navController.navigate(Routes.petForm()) },
                onPetClick      = { petId -> navController.navigate(Routes.dashboard(petId)) },
                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onSettings      = { navController.navigate(Routes.SETTINGS) },
                onOpenPaywall   = { navController.navigate(Routes.PAYWALL) }
            )
        }

        // ── Política de privacidade ───────────────────────────────────────
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Configurações / Backup ────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenPaywall  = { navController.navigate(Routes.PAYWALL) }
            )
        }

        // ── Paywall — planos premium ──────────────────────────────────────
        composable(Routes.PAYWALL) {
            PaywallScreen(onNavigateBack = { navController.popBackStack() })
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
                onConfigureMealPlan  = { planId -> navController.navigate(Routes.mealPlan(petId, planId)) },
                onConfigureWater     = { navController.navigate(Routes.waterConfig(petId)) },
                onNewHealthEntry     = { navController.navigate(Routes.healthEntry(petId)) },
                onEditHealthEntry    = { navController.navigate(Routes.healthEntry(petId, it)) },
                onWeightHistory      = { navController.navigate(Routes.weightHistory(petId)) },
                onReport             = { navController.navigate(Routes.report(petId)) },
                onVaccines           = { navController.navigate(Routes.vaccineList(petId)) },
                onOpenPaywall        = { navController.navigate(Routes.PAYWALL) }
            )
        }

        // ── Lista de vacinas e vermífugos ─────────────────────────────────────
        composable(
            route = Routes.VACCINE_LIST,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { entry ->
            val petId = entry.arguments!!.getLong("petId")
            VaccineListScreen(
                petId          = petId,
                onNavigateBack = { navController.popBackStack() },
                onAddVaccine   = { navController.navigate(Routes.vaccineForm(petId)) },
                onEditVaccine  = { vaccineId -> navController.navigate(Routes.vaccineForm(petId, vaccineId)) }
            )
        }

        // ── Formulário de vacina / vermífugo ──────────────────────────────────
        composable(
            route = Routes.VACCINE_FORM,
            arguments = listOf(
                navArgument("petId")    { type = NavType.LongType },
                navArgument("vaccineId"){ type = NavType.LongType; defaultValue = -1L }
            )
        ) { entry ->
            val petId     = entry.arguments!!.getLong("petId")
            val vaccineId = entry.arguments!!.getLong("vaccineId").takeIf { it != -1L }
            VaccineFormScreen(
                petId          = petId,
                vaccineId      = vaccineId,
                onNavigateBack = { navController.popBackStack() }
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
            arguments = listOf(
                navArgument("petId")  { type = NavType.LongType },
                navArgument("planId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { entry ->
            val petId  = entry.arguments!!.getLong("petId")
            val planId = entry.arguments!!.getLong("planId").takeIf { it != -1L }
            MealPlanFormScreen(
                petId          = petId,
                planId         = planId,
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
