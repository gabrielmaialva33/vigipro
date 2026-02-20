package com.vigipro.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.data.preferences.ThemeMode
import com.vigipro.core.data.preferences.VideoQuality
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.feature.settings.components.SettingsActionItem
import com.vigipro.feature.settings.components.SettingsDropdownItem
import com.vigipro.feature.settings.components.SettingsInfoItem
import com.vigipro.feature.settings.components.SettingsSection
import com.vigipro.feature.settings.components.SettingsToggleItem
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SettingsSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(sideEffect.message)
            SettingsSideEffect.NavigateBack -> onBack()
            SettingsSideEffect.NavigateToLogin -> onLogout()
        }
    }

    // Confirmation dialog for clear cache
    if (state.showClearCacheConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::onClearCacheDismiss,
            title = { Text("Limpar cache") },
            text = { Text("Todas as preferencias serao redefinidas para os valores padrao. Deseja continuar?") },
            confirmButton = {
                TextButton(onClick = viewModel::onClearCacheConfirm) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onClearCacheDismiss) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = "Configuracoes",
                onBackClick = viewModel::onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Conta
                SettingsSection(title = "CONTA") {
                    SettingsInfoItem(
                        title = "Email",
                        value = state.userEmail ?: "Nao conectado",
                    )
                    SettingsActionItem(
                        title = "Sair da conta",
                        subtitle = "Encerrar sessao e voltar para tela de login",
                        onClick = viewModel::onLogout,
                        destructive = true,
                    )
                }

                HorizontalDivider()

                // Video
                SettingsSection(title = "VIDEO") {
                    SettingsDropdownItem(
                        title = "Qualidade padrao",
                        currentValue = state.preferences.videoQuality,
                        options = VideoQuality.entries,
                        onValueChange = viewModel::onVideoQualityChange,
                        displayMapper = { quality ->
                            when (quality) {
                                VideoQuality.AUTO -> "Auto"
                                VideoQuality.HD -> "HD"
                                VideoQuality.SD -> "SD"
                            }
                        },
                    )
                    SettingsToggleItem(
                        title = "Audio habilitado por padrao",
                        subtitle = "Ativar audio automaticamente ao abrir o player",
                        checked = state.preferences.audioEnabledByDefault,
                        onCheckedChange = viewModel::onAudioEnabledChange,
                    )
                }

                HorizontalDivider()

                // Interface
                SettingsSection(title = "INTERFACE") {
                    SettingsDropdownItem(
                        title = "Layout padrao da grade",
                        currentValue = state.preferences.defaultGridColumns,
                        options = listOf(1, 2, 3, 4),
                        onValueChange = viewModel::onDefaultGridColumnsChange,
                        displayMapper = { columns ->
                            when (columns) {
                                1 -> "1x1"
                                2 -> "2x2"
                                3 -> "3x3"
                                4 -> "4x4"
                                else -> "${columns}x${columns}"
                            }
                        },
                    )
                    SettingsDropdownItem(
                        title = "Tema",
                        currentValue = state.preferences.themeMode,
                        options = ThemeMode.entries,
                        onValueChange = viewModel::onThemeModeChange,
                        displayMapper = { mode ->
                            when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Escuro"
                            }
                        },
                    )
                }

                HorizontalDivider()

                // Monitoramento
                SettingsSection(title = "MONITORAMENTO") {
                    SettingsDropdownItem(
                        title = "Intervalo de verificacao",
                        subtitle = "Frequencia da checagem de status das cameras",
                        currentValue = state.preferences.statusMonitorIntervalMs,
                        options = listOf(30_000L, 60_000L, 300_000L, 600_000L),
                        onValueChange = viewModel::onMonitorIntervalChange,
                        displayMapper = { ms ->
                            when (ms) {
                                30_000L -> "30 segundos"
                                60_000L -> "1 minuto"
                                300_000L -> "5 minutos"
                                600_000L -> "10 minutos"
                                else -> "${ms / 1000}s"
                            }
                        },
                    )
                    SettingsToggleItem(
                        title = "Notificar camera offline",
                        subtitle = "Receber alerta quando uma camera perder conexao",
                        checked = state.preferences.notifyOffline,
                        onCheckedChange = viewModel::onNotifyOfflineChange,
                    )
                    SettingsToggleItem(
                        title = "Notificar camera restaurada",
                        subtitle = "Receber alerta quando uma camera voltar a funcionar",
                        checked = state.preferences.notifyOnline,
                        onCheckedChange = viewModel::onNotifyOnlineChange,
                    )
                }

                HorizontalDivider()

                // Deteccao Inteligente
                SettingsSection(title = "DETECCAO INTELIGENTE") {
                    SettingsToggleItem(
                        title = "Deteccao de objetos",
                        subtitle = "Ativar deteccao por IA no player de video",
                        checked = state.preferences.detectionEnabled,
                        onCheckedChange = viewModel::onDetectionEnabledChange,
                    )
                    if (state.preferences.detectionEnabled) {
                        SettingsDropdownItem(
                            title = "Sensibilidade",
                            subtitle = "Nivel de confianca minimo para deteccao",
                            currentValue = state.preferences.detectionConfidenceThreshold,
                            options = listOf(0.3f, 0.5f, 0.7f, 0.85f),
                            onValueChange = viewModel::onDetectionConfidenceChange,
                            displayMapper = { threshold ->
                                "${(threshold * 100).toInt()}%"
                            },
                        )
                        SettingsDropdownItem(
                            title = "Frequencia de analise",
                            subtitle = "Intervalo entre cada analise de frame",
                            currentValue = state.preferences.detectionIntervalMs,
                            options = listOf(500L, 750L, 1000L, 1500L),
                            onValueChange = viewModel::onDetectionIntervalChange,
                            displayMapper = { ms ->
                                when (ms) {
                                    500L -> "500ms (rapido)"
                                    750L -> "750ms (padrao)"
                                    1000L -> "1 segundo"
                                    1500L -> "1.5 segundos"
                                    else -> "${ms}ms"
                                }
                            },
                        )
                        SettingsToggleItem(
                            title = "Detectar pessoas",
                            checked = state.preferences.detectPersons,
                            onCheckedChange = viewModel::onDetectPersonsChange,
                        )
                        SettingsToggleItem(
                            title = "Detectar veiculos",
                            checked = state.preferences.detectVehicles,
                            onCheckedChange = viewModel::onDetectVehiclesChange,
                        )
                        SettingsToggleItem(
                            title = "Detectar animais",
                            checked = state.preferences.detectAnimals,
                            onCheckedChange = viewModel::onDetectAnimalsChange,
                        )
                        SettingsToggleItem(
                            title = "Notificar pessoa detectada",
                            subtitle = "Receber alerta quando uma pessoa for detectada",
                            checked = state.preferences.notifyPersonDetected,
                            onCheckedChange = viewModel::onNotifyPersonDetectedChange,
                        )
                    }
                }

                HorizontalDivider()

                // Capturas
                SettingsSection(title = "CAPTURAS") {
                    SettingsToggleItem(
                        title = "Marca d'agua em capturas",
                        subtitle = "Adicionar nome da camera e horario nas capturas",
                        checked = state.preferences.watermarkEnabled,
                        onCheckedChange = viewModel::onWatermarkEnabledChange,
                    )
                }

                HorizontalDivider()

                // Armazenamento
                SettingsSection(title = "ARMAZENAMENTO") {
                    SettingsInfoItem(
                        title = "Local de capturas",
                        value = "Pictures/VigiPro",
                    )
                    SettingsActionItem(
                        title = "Limpar cache",
                        subtitle = "Remove dados temporarios e preferencias",
                        onClick = viewModel::onClearCacheRequest,
                        destructive = true,
                    )
                }

                HorizontalDivider()

                // Sobre
                SettingsSection(title = "SOBRE") {
                    SettingsInfoItem(
                        title = "Versao do app",
                        value = "1.0.0",
                    )
                }
            }
        }
    }
}
