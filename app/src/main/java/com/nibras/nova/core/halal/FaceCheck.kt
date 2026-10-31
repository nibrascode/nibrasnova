package com.nibras.nova.core.halal

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.TimeUnit

/** On-device face detection (ML Kit, bundled model; images never leave the phone). */
object FaceCheck {
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.08f)
                .build()
        )
    }

    /** Number of faces, or null if detection failed (caller then blurs). Call off the main thread. */
    fun count(bmp: Bitmap): Int? = runCatching {
        Tasks.await(detector.process(InputImage.fromBitmap(bmp, 0)), 2, TimeUnit.SECONDS).size
    }.getOrNull()
}
