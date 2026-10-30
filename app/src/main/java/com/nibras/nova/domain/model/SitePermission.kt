package com.nibras.nova.domain.model

enum class SitePermissionType(val displayName: String) {
    CAMERA("Camera"),
    MICROPHONE("Microphone"),
    LOCATION("Geolocation"),
    PROTECTED_MEDIA_DRM("Protected DRM Media")
}

enum class PermissionDecision(val label: String) {
    ALLOW("Allowed"),
    DENY("Blocked")
}

data class SitePermission(
    val id: Long = 0L,
    val origin: String,
    val permissionType: SitePermissionType,
    val decision: PermissionDecision,
    val updatedAt: Long = System.currentTimeMillis()
)
