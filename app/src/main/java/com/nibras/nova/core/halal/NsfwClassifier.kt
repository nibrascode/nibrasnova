package com.nibras.nova.core.halal

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** Bundled open_nsfw TFLite model (224x224 BGR, mean-subtracted). Output [sfw, nsfw]. */
class NsfwClassifier(context: Context) {
    private val interpreter: Interpreter
    private val input = ByteBuffer.allocateDirect(4 * SIZE * SIZE * 3).order(ByteOrder.nativeOrder())
    private val output = Array(1) { FloatArray(2) }
    private val px = IntArray(SIZE * SIZE)

    init {
        val fd = context.assets.openFd(MODEL)
        val buf = FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        interpreter = Interpreter(buf, Interpreter.Options().setNumThreads(2))
    }

    /** Returns (nsfwScore, skinRatio). Thread-safe. */
    @Synchronized
    fun score(src: Bitmap): Pair<Float, Float> {
        val b = Bitmap.createScaledBitmap(src, SIZE, SIZE, true)
        b.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        if (b !== src) b.recycle()
        input.rewind()
        for (c in px) {
            input.putFloat(((c and 0xFF) - 104f))
            input.putFloat((((c shr 8) and 0xFF) - 117f))
            input.putFloat((((c shr 16) and 0xFF) - 123f))
        }
        input.rewind()
        interpreter.run(input, output)
        return output[0][1] to HalalLogic.skinRatio(px)
    }

    companion object { const val SIZE = 224; const val MODEL = "nsfw_open.tflite" }
}
