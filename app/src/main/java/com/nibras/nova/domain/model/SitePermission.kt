package com.nibras.nova.domain.model

import com.nibras.nova.R
import com.nibras.nova.core.common.L

enum class SitePermissionType(val displayName: String) {
    CAMERA(L.s(R.string.t_camera)),
    MICROPHONE(L.s(R.string.t_microphone)),
    LOCATION(L.s(R.string.t_geolocation)),
    PROTECTED_MEDIA_DRM(L.s(R.string.t_protected_drm_media))
}

enum class PermissionDecision(val label: String) {
    ALLOW(L.s(R.string.t_allowed)),
    DENY(L.s(R.string.t_blocked))
}

data class SitePermission(
    val id: Long = 0L,
    val origin: String,
    val permissionType: SitePermissionType,
    val decision: PermissionDecision,
    val updatedAt: Long = System.currentTimeMillis()
)
