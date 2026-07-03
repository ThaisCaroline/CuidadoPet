package com.cuidadopet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// BootReceiver é disparado pelo Android logo após o celular reiniciar.
// Por que é necessário: o AlarmManager não persiste os alarmes após reinicializações.
// Todo alarme agendado com setExactAndAllowWhileIdle() é apagado quando o dispositivo
// desliga. Este receiver recria todos os alarmes ao religar o celular.
//
// Obs: O WorkManager (usado pelos lembretes de água) já persiste sozinho após reboot —
// o Android reagenda seus workers automaticamente. Por isso, água não precisa ser tratada aqui.
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    // Hilt injeta os repositórios automaticamente graças ao @AndroidEntryPoint acima.
    // Em BroadcastReceiver, a injeção usa lateinit var (não construtor).
    @Inject lateinit var petRepository: PetRepository
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var feedingRepository: FeedingRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var mealAlarmScheduler: MealAlarmScheduler
    @Inject lateinit var birthdayAlarmScheduler: BirthdayAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        // Filtra para garantir que só processamos o boot — o receiver pode receber
        // outros broadcasts dependendo da configuração do sistema
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        ReEngagementReceiver.schedule(context)

        // onReceive() roda na thread principal e tem limite de tempo (~10s).
        // goAsync() estende esse limite e libera a thread principal imediatamente.
        // O Android só "mata" o processo depois que pendingResult.finish() for chamado.
        val pendingResult = goAsync()

        // SupervisorJob garante que uma falha em um filho não cancela os outros.
        // Dispatchers.IO usa threads otimizadas para operações de banco de dados.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                reagendarTodosOsAlarmes()
            } finally {
                // SEMPRE chamar finish() para liberar o BroadcastReceiver,
                // mesmo se ocorrer uma exceção
                pendingResult.finish()
            }
        }
    }

    // Percorre todos os pets e reagenda medicamentos, refeições e aniversários de cada um.
    private suspend fun reagendarTodosOsAlarmes() {
        val pets = petRepository.getAllPets().first()

        pets.forEach { pet ->
            reagendarMedicamentos(pet.id, pet.name)
            reagendarRefeicoes(pet.id, pet.name)
            if (pet.birthDate != null) birthdayAlarmScheduler.scheduleBirthday(pet)
        }
    }

    private suspend fun reagendarMedicamentos(petId: Long, petName: String) {
        val medications = medicationRepository.getActiveMedications(petId).first()
        val now = System.currentTimeMillis()

        medications.forEach { med ->
            // Não reagenda tratamentos já encerrados pela data de fim
            if (med.endDate != null && med.endDate < now) return@forEach
            alarmScheduler.scheduleMedication(med, petName)
        }
    }

    private suspend fun reagendarRefeicoes(petId: Long, petName: String) {
        val plans = feedingRepository.getActiveMealPlans(petId).first()
        plans.forEach { plan ->
            val meals = feedingRepository.getMealsForPlan(plan.id).first()
            meals.forEach { meal -> mealAlarmScheduler.scheduleMeal(meal, petName) }
        }
    }
}
