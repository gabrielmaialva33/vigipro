package com.vigipro.app.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vigipro.core.ui.theme.Dimens

@Composable
fun BiometricLockScreen(
    onUnlockRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconXxl),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "VigiPro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "App protegido por biometria",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingLg))
            FilledTonalButton(
                onClick = onUnlockRequest,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconMd),
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text("Desbloquear")
            }
        }
    }
}
