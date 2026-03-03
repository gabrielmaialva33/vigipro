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
            text = { Text("Todas as preferências serão redefinidas para os valores padrão. Deseja continuar?") },
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
                title = "Configurações",
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
                        value = state.userEmail ?: "Não conectado",
                    )
                    SettingsActionItem(
                        title = "Sair da conta",
                        subtitle = "Encerrar sessão e voltar para tela de login",
                        onClick = viewModel::onLogout,
                        destructive = true,
                    )
                }

                HorizontalDivider()

                // Video
                SettingsSection(title = "VIDEO") {
                    SettingsDropdownItem(
                        title = "Qualidade padrão",
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
                        title = "Áudio habilitado por padrão",
                        subtitle = "Ativar áudio automaticamente ao abrir o player",
                        checked = state.preferences.audioEnabledByDefault,
                        onCheckedChange = viewModel::onAudioEnabledChange,
                    )
                    SettingsToggleItem(
                        title = "Máscaras de privacidade",
                        subtitle = "Exibir zonas de privacidade sobre o vídeo",
                        checked = state.preferences.privacyMaskingEnabled,
                        onCheckedChange = viewModel::onPrivacyMaskingEnabledChange,
                    )
                }

                HorizontalDivider()

                // Interface
                SettingsSection(title = "INTERFACE") {
                    SettingsDropdownItem(
                        title = "Layout padrão da grade",
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
                        title = "Intervalo de verificação",
                        subtitle = "Frequência da checagem de status das câmeras",
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
                        title = "Notificar câmera offline",
                        subtitle = "Receber alerta quando uma câmera perder conexão",
                        checked = state.preferences.notifyOffline,
                        onCheckedChange = viewModel::onNotifyOfflineChange,
                    )
                    SettingsToggleItem(
                        title = "Notificar câmera restaurada",
                        subtitle = "Receber alerta quando uma câmera voltar a funcionar",
                        checked = state.preferences.notifyOnline,
                        onCheckedChange = viewModel::onNotifyOnlineChange,
                    )
                }

                HorizontalDivider()

                // Resumo de Alertas
                SettingsSection(title = "RESUMO DE ALERTAS") {
                    SettingsToggleItem(
                        title = "Resumo periódico",
                        subtitle = "Agrupar notificações em resumos periódicos",
                        checked = state.preferences.alertDigestEnabled,
                        onCheckedChange = viewModel::onAlertDigestEnabledChange,
                    )
                    if (state.preferences.alertDigestEnabled) {
                        SettingsDropdownItem(
                            title = "Intervalo do resumo",
                            currentValue = state.preferences.alertDigestIntervalMinutes,
                            options = listOf(5, 15, 30, 60),
                            onValueChange = viewModel::onAlertDigestIntervalChange,
                            displayMapper = { minutes ->
                                when (minutes) {
                                    5 -> "5 minutos"
                                    15 -> "15 minutos"
                                    30 -> "30 minutos"
                                    60 -> "1 hora"
                                    else -> "$minutes min"
                                }
                            },
                        )
                        SettingsDropdownItem(
                            title = "Horário silencioso - inicio",
                            subtitle = "Não enviar resumos a partir deste horário",
                            currentValue = state.preferences.alertDigestQuietHoursStart,
                            options = listOf(20, 21, 22, 23),
                            onValueChange = viewModel::onAlertDigestQuietStartChange,
                            displayMapper = { hour -> "${hour}:00" },
                        )
                        SettingsDropdownItem(
                            title = "Horário silencioso - fim",
                            currentValue = state.preferences.alertDigestQuietHoursEnd,
                            options = listOf(5, 6, 7, 8),
                            onValueChange = viewModel::onAlertDigestQuietEndChange,
                            displayMapper = { hour -> "${hour}:00" },
                        )
                    }
                }

                HorizontalDivider()

                // Geofencing
                SettingsSection(title = "GEOFENCING") {
                    SettingsToggleItem(
                        title = "Auto-armar por localização",
                        subtitle = "Ativar/desativar detecção automaticamente ao sair/chegar em um site",
                        checked = state.preferences.geofencingEnabled,
                        onCheckedChange = viewModel::onGeofencingEnabledChange,
                    )
                    if (state.preferences.geofencingEnabled) {
                        SettingsDropdownItem(
                            title = "Raio do geofence",
                            subtitle = "Distância do site para acionar a detecção",
                            currentValue = state.preferences.defaultGeofenceRadius,
                            options = listOf(100f, 200f, 500f, 1000f),
                            onValueChange = viewModel::onGeofenceRadiusChange,
                            displayMapper = { radius ->
                                when (radius) {
                                    100f -> "100 metros"
                                    200f -> "200 metros"
                                    500f -> "500 metros"
                                    1000f -> "1 km"
                                    else -> "${radius.toInt()}m"
                                }
                            },
                        )
                    }
                }

                HorizontalDivider()

                // Detecção Inteligente
                SettingsSection(title = "DETECÇÃO INTELIGENTE") {
                    SettingsToggleItem(
                        title = "Detecção de objetos",
                        subtitle = "Ativar detecção por IA no player de vídeo",
                        checked = state.preferences.detectionEnabled,
                        onCheckedChange = viewModel::onDetectionEnabledChange,
                    )
                    if (state.preferences.detectionEnabled) {
                        SettingsDropdownItem(
                            title = "Sensibilidade",
                            subtitle = "Nível de confiança mínimo para detecção",
                            currentValue = state.preferences.detectionConfidenceThreshold,
                            options = listOf(0.3f, 0.5f, 0.7f, 0.85f),
                            onValueChange = viewModel::onDetectionConfidenceChange,
                            displayMapper = { threshold ->
                                "${(threshold * 100).toInt()}%"
                            },
                        )
                        SettingsDropdownItem(
                            title = "Frequência de análise",
                            subtitle = "Intervalo entre cada analise de frame",
                            currentValue = state.preferences.detectionIntervalMs,
                            options = listOf(500L, 750L, 1000L, 1500L),
                            onValueChange = viewModel::onDetectionIntervalChange,
                            displayMapper = { ms ->
                                when (ms) {
                                    500L -> "500ms (rapido)"
                                    750L -> "750ms (padrão)"
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

                // Audio bidirecional
                SettingsSection(title = "ÁUDIO BIDIRECIONAL") {
                    SettingsToggleItem(
                        title = "Talkback (push-to-talk)",
                        subtitle = "Habilitar botão de áudio bidirecional no player",
                        checked = state.preferences.talkbackEnabled,
                        onCheckedChange = viewModel::onTalkbackEnabledChange,
                    )
                }

                HorizontalDivider()

                // Seguranca
                SettingsSection(title = "SEGURANÇA") {
                    SettingsToggleItem(
                        title = "Bloqueio biométrico",
                        subtitle = "Exigir autenticação ao abrir o app",
                        checked = state.preferences.biometricLockEnabled,
                        onCheckedChange = viewModel::onBiometricLockEnabledChange,
                    )
                }

                HorizontalDivider()

                // Capturas
                SettingsSection(title = "CAPTURAS") {
                    SettingsToggleItem(
                        title = "Marca d'agua em capturas",
                        subtitle = "Adicionar nome da câmera e horário nas capturas",
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
                        subtitle = "Remove dados temporários e preferências",
                        onClick = viewModel::onClearCacheRequest,
                        destructive = true,
                    )
                }

                HorizontalDivider()

                // Sobre
                SettingsSection(title = "SOBRE") {
                    SettingsInfoItem(
                        title = "Versão do app",
                        value = "1.0.0",
                    )
                }
            }
        }
    }
}
