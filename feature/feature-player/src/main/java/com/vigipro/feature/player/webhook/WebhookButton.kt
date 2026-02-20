package com.vigipro.feature.player.webhook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Light
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vigipro.core.model.WebhookAction

private val WebhookIdleColor = Color.Black.copy(alpha = 0.6f)
private val WebhookExecutingColor = Color(0xFF1B5E20)

fun webhookIcon(iconName: String): ImageVector = when (iconName) {
    "power" -> Icons.Default.PowerSettingsNew
    "lock" -> Icons.Default.Lock
    "door" -> Icons.Default.DoorFront
    "light" -> Icons.Default.Light
    else -> Icons.Default.Webhook
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WebhookButton(
    webhooks: List<WebhookAction>,
    onExecute: (WebhookAction) -> Unit,
    isExecuting: Boolean,
    modifier: Modifier = Modifier,
    onAddWebhook: () -> Unit = {},
    onDeleteWebhook: (String) -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = webhooks.isNotEmpty(),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        Box {
            val pulseTransition = rememberInfiniteTransition(label = "webhook_pulse")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "webhook_pulse_scale",
            )

            val primaryWebhook = webhooks.first()

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(if (isExecuting) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(if (isExecuting) WebhookExecutingColor else WebhookIdleColor)
                    .combinedClickable(
                        onClick = {
                            if (!isExecuting) {
                                if (webhooks.size == 1) {
                                    onExecute(primaryWebhook)
                                } else {
                                    showMenu = true
                                }
                            }
                        },
                        onLongClick = { showMenu = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = webhookIcon(primaryWebhook.icon),
                    contentDescription = primaryWebhook.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp),
                ),
            ) {
                webhooks.forEach { webhook ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = webhookIcon(webhook.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = webhook.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = "${webhook.method.name} ${webhook.url}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteWebhook(webhook.id)
                                        showMenu = false
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        onClick = {
                            onExecute(webhook)
                            showMenu = false
                        },
                    )
                }

                // Separator and add button
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Webhook,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adicionar webhook",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        onAddWebhook()
                        showMenu = false
                    },
                )
            }
        }
    }
}
