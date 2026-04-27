package com.cuidadopet.ui.screens.feeding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.domain.FeedingStatus
import com.cuidadopet.domain.toDisplayText
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

// Aba "Alimentação" do dashboard — mostra o resumo do dia e as refeições programadas
@Composable
fun FeedingTabContent(
    petId: Long,
    onConfigurePlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedingViewModel = hiltViewModel()
) {
    // Carrega os dados ao entrar na aba
    LaunchedEffect(petId) {
        viewModel.loadFeedingData(petId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        // Exibe spinner enquanto carrega
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.plan == null) {
        // Nenhum plano configurado — convida o tutor a criar um
        NoPlanContent(
            onConfigurePlan = onConfigurePlan,
            modifier = modifier
        )
        return
    }

    // Lista de refeições do dia com seus logs
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        // Card com o resumo do dia (status geral de alimentação)
        item {
            DailySummaryCard(
                plan = state.plan!!,
                status = state.dailyStatus,
                onConfigurePlan = onConfigurePlan
            )
        }

        item { HorizontalDivider() }

        item {
            Text(
                "Refeições de hoje",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Um card por refeição programada
        items(state.meals, key = { it.id }) { meal ->
            MealCard(
                meal = meal,
                log = state.logs[meal.id],
                onLogMeal = { percentage, appetite, notes ->
                    viewModel.logMeal(meal.id, percentage, appetite, notes)
                }
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// Conteúdo exibido quando o pet não tem plano alimentar configurado ainda
@Composable
private fun NoPlanContent(
    onConfigurePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nenhum plano alimentar configurado",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Configure o plano para registrar as refeições e acompanhar a alimentação do seu pet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfigurePlan) {
            Text("Configurar plano alimentar")
        }
    }
}

// Card com o resumo do dia: tipo de alimento, meta de kcal, status geral
@Composable
private fun DailySummaryCard(
    plan: MealPlanEntity,
    status: FeedingStatus?,
    onConfigurePlan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Resumo do dia", style = MaterialTheme.typography.titleSmall)
                // Botão para editar o plano alimentar
                FilledTonalButton(
                    onClick = onConfigurePlan,
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(" Editar plano", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Tipo de alimento em texto legível
            Text(
                foodTypeLabel(plan.foodType),
                style = MaterialTheme.typography.bodyMedium
            )

            // Meta calórica, se definida
            plan.dailyKcalTarget?.let {
                Text(
                    "Meta: ${it.toInt()} kcal/dia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantidade total em gramas, se definida
            plan.dailyQuantityGrams?.let {
                Text(
                    "Quantidade: ${it.toInt()} g/dia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status do dia
            status?.let {
                Text(
                    it.toDisplayText(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Restrições, se houver
            if (!plan.restrictions.isNullOrBlank()) {
                Text(
                    "Restrições: ${plan.restrictions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Card de uma refeição individual: mostra o horário, quantidade e o log do dia
@Composable
private fun MealCard(
    meal: MealEntity,
    log: MealLogEntity?,
    onLogMeal: (eatenPercentage: Int, appetiteStatus: String, notes: String) -> Unit
) {
    // Controla se o painel de registro está expandido
    var expanded by remember { mutableStateOf(log == null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log != null)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Cabeçalho: horário e quantidade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(meal.timeOfDay, style = MaterialTheme.typography.titleMedium)
                    if (meal.quantityGrams > 0) {
                        Text(
                            "${meal.quantityGrams.toInt()} g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mostra status se já registrado, ou botão para registrar
                if (log != null && !expanded) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            appetiteLabel(log.appetiteStatus),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${log.eatenPercentage}% comido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Permite editar o registro tocando no botão
                        OutlinedButton(
                            onClick = { expanded = true },
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Editar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else if (log == null) {
                    OutlinedButton(
                        onClick = { expanded = true }
                    ) {
                        Text("Registrar")
                    }
                }
            }

            // Painel de registro expandido
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                MealLogInput(
                    initialPercentage = log?.eatenPercentage ?: 100,
                    initialAppetite = log?.appetiteStatus ?: "ALL",
                    initialNotes = log?.notes ?: "",
                    onSave = { pct, appetite, notes ->
                        onLogMeal(pct, appetite, notes)
                        expanded = false
                    },
                    onCancel = { expanded = false },
                    showCancel = log != null  // só mostra cancelar se já houver registro salvo
                )
            }
        }
    }
}

// Formulário inline para registrar o que o pet comeu em uma refeição
@Composable
private fun MealLogInput(
    initialPercentage: Int,
    initialAppetite: String,
    initialNotes: String,
    onSave: (Int, String, String) -> Unit,
    onCancel: () -> Unit,
    showCancel: Boolean
) {
    // Estado local — não precisa ir para o ViewModel até o usuário tocar em Salvar
    var selectedPct by remember { mutableIntStateOf(initialPercentage) }
    var selectedAppetite by remember { mutableStateOf(initialAppetite) }
    var notes by remember { mutableStateOf(initialNotes) }

    // Opções de porcentagem comida — valores discretos mais práticos para o tutor
    val percentageOptions = listOf(0, 25, 50, 75, 100)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quanto o pet comeu?", style = MaterialTheme.typography.labelMedium)

        // Botões de porcentagem lado a lado
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            percentageOptions.forEach { pct ->
                FilterChipCompact(
                    selected = selectedPct == pct,
                    onClick = {
                        selectedPct = pct
                        // Atualiza automaticamente o status de apetite ao tocar
                        selectedAppetite = when (pct) {
                            100  -> "ALL"
                            0    -> "REFUSED"
                            else -> "PARTIAL"
                        }
                    },
                    label = "$pct%"
                )
            }
        }

        // Linha de botões de apetite
        Text("Apetite:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipCompact(
                selected = selectedAppetite == "ALL",
                onClick = { selectedAppetite = "ALL" },
                label = "Comeu tudo"
            )
            FilterChipCompact(
                selected = selectedAppetite == "PARTIAL",
                onClick = { selectedAppetite = "PARTIAL" },
                label = "Parcial"
            )
            FilterChipCompact(
                selected = selectedAppetite == "REFUSED",
                onClick = { selectedAppetite = "REFUSED" },
                label = "Recusou"
            )
        }

        // Campo de observações livre
        androidx.compose.material3.OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Observações (opcional)") },
            placeholder = { Text("Ex: comeu devagar, misturou com patê...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        // Botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCancel) {
                OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            }
            Button(onClick = { onSave(selectedPct, selectedAppetite, notes) }) {
                Text("Salvar registro")
            }
        }
    }
}

// Chip compacto reutilizável dentro desta tela
@Composable
private fun FilterChipCompact(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

// Converte código interno do tipo de alimento para rótulo legível
private fun foodTypeLabel(code: String): String = when (code) {
    "DRY_KIBBLE"  -> "Ração seca"
    "WET_FOOD"    -> "Ração úmida"
    "NATURAL"     -> "Alimentação natural/caseira"
    "THERAPEUTIC" -> "Dieta terapêutica"
    else          -> "Outro"
}

// Converte código de apetite para emoji + texto
private fun appetiteLabel(code: String): String = when (code) {
    "ALL"     -> "Comeu tudo"
    "PARTIAL" -> "Comeu parcialmente"
    "REFUSED" -> "Recusou"
    else      -> code
}
