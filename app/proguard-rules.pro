# Preserva informações de linha para stack traces legíveis nos crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room ──────────────────────────────────────────────────────────────────────
# Mantém todas as entidades (tabelas) e DAOs — o Room usa reflexão para acessá-los
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
# Hilt gera código em tempo de compilação (KSP) — as regras abaixo protegem
# os módulos e componentes gerados contra o shrinker
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Kotlin Serialization / Reflection ────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Vico (gráficos) ───────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Compose ───────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
