package com.nibras.nova.core.prayer

import kotlin.math.*

object Qibla {
    const val KAABA_LAT = 21.4225
    const val KAABA_LNG = 39.8262

    /** Initial great-circle bearing from (lat,lng) to the Kaaba, degrees clockwise from true north [0,360). */
    fun bearing(lat: Double, lng: Double): Double {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LNG - lng)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Haversine distance in km. */
    fun distanceKm(lat: Double, lng: Double): Double {
        val r = 6371.0088
        val dp = Math.toRadians(KAABA_LAT - lat); val dl = Math.toRadians(KAABA_LNG - lng)
        val a = sin(dp / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) * sin(dl / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    /** Angle to rotate the arrow on screen given the device's true heading. */
    fun arrowRotation(qiblaBearing: Double, trueHeading: Double): Double = ((qiblaBearing - trueHeading) % 360 + 360) % 360
}
