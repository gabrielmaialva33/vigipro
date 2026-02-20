package com.vigipro.feature.accesscontrol.ui

import android.Manifest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.feature.accesscontrol.InviteCodeParser
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemInviteSection(
    code: String,
    isRedeeming: Boolean,
    onCodeChange: (String) -> Unit,
    onRedeem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var scannedCode by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) showScanner = true
    }

    // Auto-fill scanned code
    LaunchedEffect(scannedCode) {
        scannedCode?.let { detected ->
            val inviteCode = InviteCodeParser.extractCode(detected)
            onCodeChange(inviteCode)
            scannedCode = null
            showScanner = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.SpacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Resgatar Convite",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        Text(
            text = "Escaneie o QR code ou digite o codigo manualmente",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        // QR Scanner section
        AnimatedVisibility(
            visible = showScanner && cameraPermissionGranted,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    QrCameraPreview(
                        onBarcodeDetected = { barcode ->
                            if (scannedCode == null) {
                                scannedCode = barcode
                            }
                        },
                    )

                    // Viewfinder overlay
                    ViewfinderOverlay()
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                TextButton(onClick = { showScanner = false }) {
                    Text("Fechar scanner")
                }
            }
        }

        // Open scanner button
        AnimatedVisibility(
            visible = !showScanner,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Button(
                onClick = {
                    if (cameraPermissionGranted) {
                        showScanner = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRedeeming,
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(Dimens.SpacingSm))
                Text("Escanear QR Code")
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        // Divider "ou"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "ou",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        // Manual code entry
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Codigo do convite") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isRedeeming,
            leadingIcon = {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        Button(
            onClick = onRedeem,
            modifier = Modifier.fillMaxWidth(),
            enabled = code.isNotBlank() && !isRedeeming,
        ) {
            if (isRedeeming) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Resgatar")
            }
        }
    }
}

@Composable
private fun QrCameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImage(imageProxy, barcodeScanner, onBarcodeDetected)
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (_: Exception) {
                    // Camera not available
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxSize()) {
        val overlayColor = Color.Black.copy(alpha = 0.5f)
        val cutoutSize = size.minDimension * 0.7f
        val cutoutLeft = (size.width - cutoutSize) / 2
        val cutoutTop = (size.height - cutoutSize) / 2

        // Dark overlay
        drawRect(color = overlayColor)

        // Transparent cutout
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(cutoutLeft, cutoutTop),
            size = androidx.compose.ui.geometry.Size(cutoutSize, cutoutSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = BlendMode.Clear,
        )

        // Border around cutout
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(cutoutLeft, cutoutTop),
            size = androidx.compose.ui.geometry.Size(cutoutSize, cutoutSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f)),
            ),
        )
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeDetected: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    if (barcode.valueType == Barcode.TYPE_URL ||
                        barcode.valueType == Barcode.TYPE_TEXT ||
                        rawValue.isNotBlank()
                    ) {
                        onBarcodeDetected(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

