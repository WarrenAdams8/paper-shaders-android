package com.paperdesign.shaders

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Kotlin ports of the CPU image preprocessing some Paper shaders run before upload:
 * `toProcessedHeatmap` (heatmap.ts) and `toProcessedLiquidMetal` / `toProcessedGemSmoke`
 * (a Poisson-solved "roundness" map). Run these off the main thread.
 */
object ImageProcessing {

    /** heatmap.ts `toProcessedHeatmap`: R = contour blur, G = big blur, B = inner blur of the luma. */
    fun heatmap(source: Bitmap): Bitmap {
        val canvasSize = 1000
        val ratio = source.width.toFloat() / source.height
        val maxBlur = floor(canvasSize * 0.15).toInt()
        val padding = ceil(maxBlur * 2.5).toInt()
        var imgW = canvasSize
        var imgH = canvasSize
        if (ratio > 1) imgH = floor(canvasSize / ratio).toInt() else imgW = floor(canvasSize * ratio).toInt()
        val width = imgW + 2 * padding
        val height = imgH + 2 * padding

        val canvasBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(canvasBmp).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, null, Rect(padding, padding, padding + imgW, padding + imgH), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val total = width * height
        val src = IntArray(total)
        canvasBmp.getPixels(src, 0, width, 0, 0, width, height)
        canvasBmp.recycle()

        val gray = IntArray(total)
        for (i in 0 until total) {
            val c = src[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        val big = multiPassBlur(gray, width, height, maxBlur, 3)
        val inner = multiPassBlur(gray, width, height, max(1, (0.12 * maxBlur).roundToInt()), 3)
        val contour = multiPassBlur(gray, width, height, 5, 1)

        val out = IntArray(total)
        for (i in 0 until total) {
            out[i] = (0xFF shl 24) or (contour[i] shl 16) or (big[i] shl 8) or inner[i]
        }
        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }

    /** Integral-image box blur, as upstream `blurGray`. */
    private fun blurGray(gray: IntArray, width: Int, height: Int, radius: Int): IntArray {
        if (radius <= 0) return gray.copyOf()
        val integral = LongArray(width * height)
        for (y in 0 until height) {
            var rowSum = 0L
            for (x in 0 until width) {
                val idx = y * width + x
                rowSum += gray[idx]
                integral[idx] = rowSum + if (y > 0) integral[idx - width] else 0L
            }
        }
        val out = IntArray(width * height)
        for (y in 0 until height) {
            val y1 = max(0, y - radius)
            val y2 = min(height - 1, y + radius)
            for (x in 0 until width) {
                val x1 = max(0, x - radius)
                val x2 = min(width - 1, x + radius)
                val a = integral[y2 * width + x2]
                val b = if (x1 > 0) integral[y2 * width + x1 - 1] else 0L
                val c = if (y1 > 0) integral[(y1 - 1) * width + x2] else 0L
                val d = if (x1 > 0 && y1 > 0) integral[(y1 - 1) * width + x1 - 1] else 0L
                val area = (x2 - x1 + 1) * (y2 - y1 + 1)
                out[y * width + x] = ((a - b - c + d).toDouble() / area).roundToInt().coerceIn(0, 255)
            }
        }
        return out
    }

    private fun multiPassBlur(gray: IntArray, w: Int, h: Int, radius: Int, passes: Int): IntArray {
        if (radius <= 0 || passes <= 1) return blurGray(gray, w, h, radius)
        var input = gray
        repeat(passes) { input = blurGray(input, w, h, radius) }
        return input
    }

    /**
     * liquid-metal.ts / gem-smoke.ts `toProcessed*`: solves a Poisson equation inside the shape
     * (alpha > 0) to get a smooth "roundness" falloff. Output: R = roundness (0 at the center,
     * 255 at the edges), G = original alpha, B = A = 255.
     *
     * @param padding fraction of padding around the shape (gem smoke: 0.025, liquid metal: 0).
     * @param iterations SOR iterations (gem smoke: 32, liquid metal: 40).
     * @param smoothingPasses Jacobi passes after SOR (gem smoke: 3, liquid metal: 0).
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun poissonRoundness(source: Bitmap, padding: Float, iterations: Int, smoothingPasses: Int): Bitmap {
        val originalWidth = source.width
        val originalHeight = source.height
        val scaleFactor = 512f / min(originalWidth, originalHeight)
        val width = (originalWidth * scaleFactor).roundToInt()
        val height = (originalHeight * scaleFactor).roundToInt()
        val padX = ceil(width * padding).toInt()
        val padY = ceil(height * padding).toInt()

        // 1. Shape mask at working resolution.
        val shapeBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(shapeBmp).drawBitmap(source, null, Rect(padX, padY, width - padX, height - padY), Paint(Paint.FILTER_BITMAP_FLAG))
        val n = width * height
        val px = IntArray(n)
        shapeBmp.getPixels(px, 0, width, 0, 0, width, height)
        shapeBmp.recycle()
        val shape = BooleanArray(n) { (px[it] ushr 24) != 0 }

        // 2. Boundary / interior split (8-connected boundary).
        val interior = IntArray(n)
        var interiorCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (!shape[idx]) continue
                val boundary = x == 0 || x == width - 1 || y == 0 || y == height - 1 ||
                    !shape[idx - 1] || !shape[idx + 1] || !shape[idx - width] || !shape[idx + width] ||
                    !shape[idx - width - 1] || !shape[idx - width + 1] || !shape[idx + width - 1] || !shape[idx + width + 1]
                if (!boundary) interior[interiorCount++] = idx
            }
        }
        // 3. Neighbour table (-1 = out of shape).
        val nb = IntArray(interiorCount * 4)
        for (i in 0 until interiorCount) {
            val idx = interior[i]
            val x = idx % width
            val y = idx / width
            nb[i * 4] = if (x < width - 1 && shape[idx + 1]) idx + 1 else -1
            nb[i * 4 + 1] = if (x > 0 && shape[idx - 1]) idx - 1 else -1
            nb[i * 4 + 2] = if (y > 0 && shape[idx - width]) idx - width else -1
            nb[i * 4 + 3] = if (y < height - 1 && shape[idx + width]) idx + width else -1
        }

        // 4. Red-black SOR, as upstream (C = 0.01, omega = 1.9).
        val u = FloatArray(n)
        val c = 0.01f
        val omega = 1.9f
        fun relax(parity: Int) {
            for (i in 0 until interiorCount) {
                val idx = interior[i]
                if (((idx % width) + (idx / width)) % 2 != parity) continue
                var sum = 0f
                for (k in 0 until 4) {
                    val j = nb[i * 4 + k]
                    if (j >= 0) sum += u[j]
                }
                u[idx] = omega * ((c + sum) / 4f) + (1f - omega) * u[idx]
            }
        }
        repeat(iterations) {
            relax(0)
            relax(1)
        }
        if (smoothingPasses > 0) {
            val tmp = FloatArray(n)
            repeat(smoothingPasses) {
                u.copyInto(tmp)
                for (i in 0 until interiorCount) {
                    val idx = interior[i]
                    var sum = 0f
                    var count = 0
                    for (k in 0 until 4) {
                        val j = nb[i * 4 + k]
                        if (j >= 0) { sum += tmp[j]; count++ }
                    }
                    u[idx] = if (count > 0) (tmp[idx] + sum / count) * 0.5f else tmp[idx]
                }
            }
        }
        var maxVal = 0f
        for (i in 0 until interiorCount) maxVal = max(maxVal, u[interior[i]])
        if (maxVal == 0f) maxVal = 1f

        // 5. Roundness image (transparent outside the shape), upscaled with bilinear filtering.
        val temp = IntArray(n)
        for (idx in 0 until n) {
            temp[idx] = if (!shape[idx]) {
                0x00FFFFFF
            } else {
                val gray = (255f * (1f - u[idx] / maxVal)).toInt().coerceIn(0, 255)
                (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }
        val tempBmp = Bitmap.createBitmap(temp, width, height, Bitmap.Config.ARGB_8888)
        val upscaled = Bitmap.createScaledBitmap(tempBmp, originalWidth, originalHeight, true)
        tempBmp.recycle()
        val up = IntArray(originalWidth * originalHeight)
        upscaled.getPixels(up, 0, originalWidth, 0, 0, originalWidth, originalHeight)
        upscaled.recycle()

        // 6. Re-apply the original (padded) alpha at full resolution.
        val origPadX = ceil(originalWidth * padding).toInt()
        val origPadY = ceil(originalHeight * padding).toInt()
        val origBmp = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
        Canvas(origBmp).drawBitmap(
            source,
            null,
            Rect(origPadX, origPadY, originalWidth - origPadX, originalHeight - origPadY),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        val orig = IntArray(originalWidth * originalHeight)
        origBmp.getPixels(orig, 0, originalWidth, 0, 0, originalWidth, originalHeight)
        origBmp.recycle()

        for (i in up.indices) {
            val a = orig[i] ushr 24
            val r: Int
            val g: Int
            if (a == 0) {
                r = 255
                g = 0
            } else {
                r = if ((up[i] ushr 24) == 0) 0 else (up[i] shr 16) and 0xFF
                g = a
            }
            up[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or 0xFF
        }
        return Bitmap.createBitmap(up, originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
    }
}
