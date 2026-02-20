package com.vigipro.feature.accesscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigipro.core.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemInviteSection(
    code: String,
    isRedeeming: Boolean,
    onCodeChange: (String) -> Unit,
    onRedeem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.SpacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Resgatar Convite",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        Text(
            text = "Insira o codigo do convite recebido",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Codigo do convite") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isRedeeming,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        Button(
            onClick = onRedeem,
            modifier = Modifier.fillMaxWidth(),
            enabled = code.isNotBlank() && !isRedeeming,
        ) {
            if (isRedeeming) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Resgatar")
            }
        }
    }
}
