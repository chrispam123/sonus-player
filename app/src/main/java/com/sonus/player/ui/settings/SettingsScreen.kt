package com.sonus.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonus.player.domain.model.EqPreset
import com.sonus.player.ui.theme.CyberLime
import com.sonus.player.ui.theme.DeepGray
import com.sonus.player.ui.theme.GhostWhite
import com.sonus.player.ui.theme.InkBlack
import com.sonus.player.ui.theme.MediumGray
import com.sonus.player.ui.theme.SoftGray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section: PROCESAMIENTO DE SEÑAL
        Text(
            text = "PROCESAMIENTO DE SEÑAL",
            style = MaterialTheme.typography.labelLarge,
            color = CyberLime
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ecualizador de 5 Bandas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // EQ Enable toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVAR EQ",
                style = MaterialTheme.typography.labelMedium,
                color = SoftGray
            )
            Switch(
                checked = uiState.eqEnabled,
                onCheckedChange = { viewModel.setEqEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = InkBlack,
                    checkedTrackColor = CyberLime,
                    uncheckedThumbColor = SoftGray,
                    uncheckedTrackColor = MediumGray
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // EQ Presets as chips
        Text(
            text = "AJUSTES PREESTABLECIDOS",
            style = MaterialTheme.typography.labelSmall,
            color = SoftGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqPreset.ALL.forEach { preset ->
                EqChip(
                    label = preset.name.uppercase(),
                    selected = uiState.eqPreset.name == preset.name,
                    enabled = uiState.eqEnabled,
                    onClick = { viewModel.setEqPreset(preset) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MediumGray, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(32.dp))

        // Section: SLEEP TIMER
        Text(
            text = "TEMPORIZADOR",
            style = MaterialTheme.typography.labelLarge,
            color = CyberLime
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sleep Timer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.sleepTimerActive) {
            val remaining = uiState.sleepTimerRemainingMs ?: 0
            val minutes = remaining / 60_000
            val seconds = (remaining % 60_000) / 1000
            Text(
                text = "SE DETENDRÁ EN: %02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.labelLarge,
                color = CyberLime
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { viewModel.cancelSleepTimer() }) {
                Text("CANCELAR", style = MaterialTheme.typography.labelMedium, color = SoftGray)
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 60, 90).forEach { minutes ->
                    EqChip(
                        label = "${minutes}MIN",
                        selected = false,
                        enabled = true,
                        onClick = { viewModel.startSleepTimer(minutes) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun EqChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) CyberLime else InkBlack
    val textColor = if (selected) InkBlack else if (enabled) MaterialTheme.colorScheme.onSurface else SoftGray
    val borderColor = if (selected) CyberLime else GhostWhite

    Box(
        modifier = Modifier
            .border(1.dp, borderColor)
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
