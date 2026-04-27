package com.cuidadopet

import android.app.Application
import com.cuidadopet.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp inicializa o Hilt no app inteiro.
// Esta classe é o ponto de entrada — executada antes de qualquer Activity.
@HiltAndroidApp
class CuidadoPetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Cria os canais de notificação assim que o app inicia.
        // Obrigatório no Android 8+ — sem o canal, notificações não aparecem.
        NotificationChannels.createAll(this)
    }
}
