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

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── Security Crypto ───────────────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── Google Tink (dependência interna do Security Crypto) ──────────────────────
# Anotações do errorprone são usadas apenas em tempo de compilação — não existem
# em runtime e podem ser ignoradas com segurança pelo R8
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**

# ── Notification receivers e Workers do app ───────────────────────────────────
# BroadcastReceivers referenciados no AndroidManifest precisam ser preservados
-keep class com.cuidadopet.notification.** { *; }

# ── Logs ──────────────────────────────────────────────────────────────────────
# Remove todas as chamadas de Log.* no build de release.
# -assumenosideeffects diz ao R8 que essas chamadas não têm efeito colateral,
# então ele as elimina completamente do bytecode gerado.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static boolean isLoggable(java.lang.String, int);
}
