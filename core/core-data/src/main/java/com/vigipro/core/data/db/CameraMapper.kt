package com.vigipro.core.data.db

import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraStatus

fun CameraEntity.toDomain(): Camera = Camera(
    id = id,
    siteId = siteId,
    name = name,
    onvifAddress = onvifAddress,
    rtspUrl = rtspUrl,
    username = username,
    streamProfile = streamProfile,
    ptzCapable = ptzCapable,
    audioCapable = audioCapable,
    status = CameraStatus.entries.find { it.name.equals(status, ignoreCase = true) }
        ?: CameraStatus.OFFLINE,
    thumbnailUrl = thumbnailUrl,
    sortOrder = sortOrder,
    hlsUrl = hlsUrl,
    isDemo = isDemo,
)

fun Camera.toEntity(): CameraEntity = CameraEntity(
    id = id,
    siteId = siteId,
    name = name,
    onvifAddress = onvifAddress,
    rtspUrl = rtspUrl,
    username = username,
    streamProfile = streamProfile,
    ptzCapable = ptzCapable,
    audioCapable = audioCapable,
    status = status.name.lowercase(),
    thumbnailUrl = thumbnailUrl,
    sortOrder = sortOrder,
    hlsUrl = hlsUrl,
    isDemo = isDemo,
)
