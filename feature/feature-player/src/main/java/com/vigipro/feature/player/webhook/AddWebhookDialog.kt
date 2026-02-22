package com.vigipro.feature.player.webhook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vigipro.core.model.HttpMethod
import com.vigipro.core.model.WebhookAction
import java.util.UUID

private data class IconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private val iconOptions = listOf(
    IconOption("power", "Energia", PhosphorIcons.Regular.Power),
    IconOption("lock", "Tranca", PhosphorIcons.Regular.LockKey),
    IconOption("door", "Porta", PhosphorIcons.Regular.Door),
    IconOption("light", "Luz", PhosphorIcons.Regular.Lightbulb),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebhookDialog(
    cameraId: String,
    onSave: (WebhookAction) -> Unit,
    onDismiss: () -> Unit,
    editingWebhook: WebhookAction? = null,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(editingWebhook?.name ?: "") }
    var url by remember { mutableStateOf(editingWebhook?.url ?: "") }
    var method by remember { mutableStateOf(editingWebhook?.method ?: HttpMethod.POST) }
    var body by remember { mutableStateOf(editingWebhook?.body ?: "") }
    var selectedIcon by remember { mutableStateOf(editingWebhook?.icon ?: "power") }
    var methodExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = if (editingWebhook != null) "Editar Webhook" else "Novo Webhook",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex: Abrir Portao") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Nome obrigatorio") }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // URL field
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                    },
                    label = { Text("URL") },
                    placeholder = { Text("http://192.168.1.100/relay/0?turn=on") },
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text("URL obrigatoria") }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Method dropdown
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it },
                ) {
                    OutlinedTextField(
                        value = method.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metodo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false },
                    ) {
                        HttpMethod.entries.forEach { httpMethod ->
                            DropdownMenuItem(
                                text = { Text(httpMethod.name) },
                                onClick = {
                                    method = httpMethod
                                    methodExpanded = false
                                },
                            )
                        }
                    }
                }

                // Body field (only for POST/PUT)
                if (method != HttpMethod.GET) {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Corpo (opcional)") },
                        placeholder = { Text("{\"action\": \"open\"}") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Icon picker
                Text(
                    text = "Icone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    iconOptions.forEach { option ->
                        val isSelected = option.key == selectedIcon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { selectedIcon = option.key }
                                .padding(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedUrl = url.trim()
                    nameError = trimmedName.isBlank()
                    urlError = trimmedUrl.isBlank()

                    if (!nameError && !urlError) {
                        onSave(
                            WebhookAction(
                                id = editingWebhook?.id ?: UUID.randomUUID().toString(),
                                cameraId = cameraId,
                                name = trimmedName,
                                url = trimmedUrl,
                                method = method,
                                headers = if (method != HttpMethod.GET && body.isNotBlank()) {
                                    mapOf("Content-Type" to "application/json")
                                } else {
                                    emptyMap()
                                },
                                body = if (method != HttpMethod.GET && body.isNotBlank()) body else null,
                                icon = selectedIcon,
                            ),
                        )
                    }
                },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
