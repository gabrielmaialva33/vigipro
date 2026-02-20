package com.vigipro.feature.accesscontrol.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.vigipro.core.ui.theme.Dimens

private const val INVITE_BASE_URL = "https://vigipro.app/invite/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteResultSheet(
    inviteCode: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val inviteUrl = INVITE_BASE_URL + inviteCode

    val qrBitmap = remember(inviteCode) {
        generateQrCode(inviteUrl, 512)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Convite Criado",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // QR Code
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code do convite",
                    modifier = Modifier.size(200.dp),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Invite code
            Text(
                text = inviteCode,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXs))

            Text(
                text = inviteUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Share button
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Use este convite para acessar o monitoramento VigiPro: $inviteUrl\n\nCodigo: $inviteCode")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar convite"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Compartilhar")
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fechar")
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))
        }
    }
}

private fun generateQrCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
