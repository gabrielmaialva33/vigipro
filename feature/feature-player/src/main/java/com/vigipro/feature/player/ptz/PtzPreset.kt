package com.vigipro.feature.player.ptz

import androidx.compose.runtime.Immutable

@Immutable
data class PtzPreset(
    val token: String,
    val name: String,
)
