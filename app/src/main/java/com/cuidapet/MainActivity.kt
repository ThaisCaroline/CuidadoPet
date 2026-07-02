package com.cuidadopet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cuidadopet.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.ui.navigation.AppNavigation
import com.cuidadopet.ui.theme.CuidadoPetTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import com.cuidadopet.notification.CareReminderWorker
import java.util.Calendar
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cuidadopet.notification.ReEngagementWorker
import com.cuidadopet.widget.TodayWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateDownloaded = MutableStateFlow(false)

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            updateDownloaded.value = true
        }
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* flexible update: o resultado não precisa de tratamento especial */ }

    override fun attachBaseContext(newBase: Context) {
        val tag = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("language_tag", "") ?: ""
        if (tag.isNotEmpty()) {
            val locale = java.util.Locale.forLanguageTag(tag)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent.getIntExtra("cancel_notif_id", -1).also {
            if (it != -1) NotificationManagerCompat.from(this).cancel(it)
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateListener)
        checkForUpdate()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReEngagementWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReEngagementWorker>(24, TimeUnit.HOURS).build()
        )

        // Care reminder às 19h — dispara se o tutor não registrou cuidados no dia
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CareReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CareReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(target.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .build()
        )

        setContent {
            CuidadoPetTheme {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppNavigation()

                val downloaded by updateDownloaded.collectAsStateWithLifecycle()
                if (downloaded) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.update_ready_title)) },
                        text  = { Text(stringResource(R.string.update_ready_msg)) },
                        confirmButton = {
                            Button(onClick = { appUpdateManager.completeUpdate() }) {
                                Text(stringResource(R.string.update_restart_now))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateDownloaded.value = false }) {
                                Text(stringResource(R.string.update_later))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateDownloaded.value = true
            }
        }
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val now2 = System.currentTimeMillis()
        prefs.edit()
            .putLong("last_app_open", now2)
            .putInt("reengagement_stage", 0)
            .apply()
        if (prefs.getLong("first_app_open", 0L) == 0L) {
            prefs.edit().putLong("first_app_open", now2).apply()
        }

        requestReviewIfEligible()

        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)
            val ids = manager.getGlanceIds(TodayWidget::class.java)
            ids.forEach { TodayWidget().update(this@MainActivity, it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun requestReviewIfEligible() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("review_requested", false)) return
        val firstOpen = prefs.getLong("first_app_open", 0L)
        if (firstOpen == 0L) return
        val sevenDays = 7L * 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - firstOpen < sevenDays) return

        val reviewManager = ReviewManagerFactory.create(this)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(this, task.result).addOnCompleteListener {
                    prefs.edit().putBoolean("review_requested", true).apply()
                }
            }
        }
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }
}
