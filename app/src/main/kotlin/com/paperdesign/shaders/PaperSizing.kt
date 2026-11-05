package com.paperdesign.shaders

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CPU port of Paper's vertex shader (`packages/shaders/src/vertex-shader.ts`).
 *
 * Upstream computes the object / responsive / pattern / image UVs per vertex and lets the GPU
 * interpolate them. AGSL has no vertex stage, but every one of those UVs is an affine function
 * of the fragment position, so we evaluate the vertex math at three points on the CPU and send
 * each UV as two `float3` rows: `uv = (dot(rowX, (x, y, 1)), dot(rowY, (x, y, 1)))`.
 *
 * Runs only when the size or a sizing parameter changes. Allocation free.
 */
class PaperSizing {
    // Output: affine rows (x then y, 3 floats each) per UV kind.
    val objectUV = FloatArray(6)
    val responsiveUV = FloatArray(6)
    val patternUV = FloatArray(6)
    val imageUV = FloatArray(6)
    val objectBoxSize = FloatArray(2)
    val responsiveBoxGivenSize = FloatArray(2)
    val patternBoxSize = FloatArray(2)

    /** Approximate size of one device pixel in each UV space (for `fwidth` emulation). */
    var objectPx = 0f; private set
    var responsivePx = 0f; private set
    var patternPx = 0f; private set
    var imagePx = 0f; private set

    // Scratch: vertex outputs at fragCoord (0,0), (1,0), (0,1).
    private val p0 = FloatArray(8)
    private val px = FloatArray(8)
    private val py = FloatArray(8)

    // Inputs, cached for evalVertex.
    private var resX = 1f
    private var resY = 1f
    private var pixelRatio = 1f
    private var imageAspect = 1f
    private var originX = 0.5f
    private var originY = 0.5f
    private var worldW = 0f
    private var worldH = 0f
    private var fit = 0f
    private var scale = 1f
    private var rotationDeg = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    @Suppress("LongParameterList")
    fun compute(
        width: Float,
        height: Float,
        pixelRatio: Float,
        imageAspectRatio: Float,
        fit: Float,
        scale: Float,
        rotation: Float,
        offsetX: Float,
        offsetY: Float,
        originX: Float,
        originY: Float,
        worldWidth: Float,
        worldHeight: Float,
    ) {
        resX = max(width, 1f); resY = max(height, 1f)
        this.pixelRatio = pixelRatio
        imageAspect = if (imageAspectRatio > 0f) imageAspectRatio else 1f
        this.fit = fit; this.scale = scale; this.rotationDeg = rotation
        this.offsetX = offsetX; this.offsetY = offsetY
        this.originX = originX; this.originY = originY
        worldW = worldWidth; worldH = worldHeight

        // fragCoord -> GL uv: x = fx / W - 0.5, y = 0.5 - fy / H (GL's y axis points up).
        evalVertex(-0.5f, 0.5f, p0)
        evalVertex(1f / resX - 0.5f, 0.5f, px)
        evalVertex(-0.5f, 0.5f - 1f / resY, py)
        affine(0, objectUV)
        affine(2, responsiveUV)
        affine(4, patternUV)
        affine(6, imageUV)
        objectPx = pixelSize(objectUV)
        responsivePx = pixelSize(responsiveUV)
        patternPx = pixelSize(patternUV)
        imagePx = pixelSize(imageUV)
    }

    private fun affine(o: Int, out: FloatArray) {
        out[0] = px[o] - p0[o]; out[1] = py[o] - p0[o]; out[2] = p0[o]
        out[3] = px[o + 1] - p0[o + 1]; out[4] = py[o + 1] - p0[o + 1]; out[5] = p0[o + 1]
    }

    private fun pixelSize(m: FloatArray): Float = sqrt(abs(m[0] * m[4] - m[1] * m[3]))

    /** Returns box.x, box.y and (via [noFitOut]) the no-fit width, like upstream `getBoxSize`. */
    private var noFitWidth = 0f
    private var boxX = 0f
    private var boxY = 0f

    private fun boxSize(boxRatio: Float, givenX: Float, givenY: Float) {
        var bx = boxRatio * min(givenX / boxRatio, givenY)
        noFitWidth = bx
        if (fit == 1f) bx = boxRatio * min(resX / boxRatio, resY)
        else if (fit == 2f) bx = boxRatio * max(resX / boxRatio, resY)
        boxX = bx
        boxY = bx / boxRatio
    }

    /** Port of the vertex shader `main()` for one input uv. Writes 4 vec2 outputs into [out]. */
    @Suppress("LongMethod")
    private fun evalVertex(ux: Float, uy: Float, out: FloatArray) {
        val boxOriginX = 0.5f - originX
        val boxOriginY = originY - 0.5f
        val givenX = max(worldW, 1f) * pixelRatio
        val givenY = max(worldH, 1f) * pixelRatio
        val r = rotationDeg * PI_F / 180f
        val cr = cos(r); val sr = sin(r)
        // GLSL mat2(cos, sin, -sin, cos) * v == (cos*x - sin*y, sin*x + cos*y)
        val offX = -offsetX
        val offY = offsetY

        val sizeX = if (worldW == 0f) resX else givenX
        val sizeY = if (worldH == 0f) resY else givenY

        // ---- object
        boxSize(1f, sizeX, sizeY)
        objectBoxSize[0] = boxX; objectBoxSize[1] = boxY
        var sx = resX / boxX
        var sy = resY / boxY
        var x = ux * sx + boxOriginX * (sx - 1f) + offX
        var y = uy * sy + boxOriginY * (sy - 1f) + offY
        x /= scale; y /= scale
        out[0] = cr * x - sr * y
        out[1] = sr * x + cr * y

        // ---- responsive
        responsiveBoxGivenSize[0] = sizeX; responsiveBoxGivenSize[1] = sizeY
        val responsiveRatio = sizeX / sizeY
        boxSize(responsiveRatio, sizeX, sizeY)
        sx = resX / boxX
        sy = resY / boxY
        x = ux * sx + boxOriginX * (sx - 1f) + offX
        y = uy * sy + boxOriginY * (sy - 1f) + offY
        x /= scale; y /= scale
        x *= responsiveRatio
        var rx = cr * x - sr * y
        val ry = sr * x + cr * y
        rx /= responsiveRatio
        out[2] = rx
        out[3] = ry

        // ---- pattern
        val patternRatio = sizeX / sizeY
        boxSize(patternRatio, sizeX, sizeY)
        patternBoxSize[0] = boxX; patternBoxSize[1] = boxY
        val patternNoFit = noFitWidth
        sx = resX / boxX
        sy = resY / boxY
        x = ux + offX / sx + boxOriginX - boxOriginX / sx
        y = uy + offY / sy + boxOriginY - boxOriginY / sy
        x *= resX; y *= resY
        x /= pixelRatio; y /= pixelRatio
        if (fit > 0f) {
            val k = patternNoFit / boxX
            x *= k; y *= k
        }
        x /= scale; y /= scale
        var qx = cr * x - sr * y
        var qy = sr * x + cr * y
        qx += boxOriginX / sx - boxOriginX
        qy += boxOriginY / sy - boxOriginY
        out[4] = qx * 0.01f
        out[5] = qy * 0.01f

        // ---- image
        val ia = imageAspect
        val ibx = when (fit) {
            1f -> min(resX / ia, resY) * ia
            2f -> max(resX / ia, resY) * ia
            else -> min(10f, 10f / ia * ia)
        }
        val iby = ibx / ia
        sx = resX / ibx
        sy = resY / iby
        x = ux * sx + boxOriginX * (sx - 1f) + offX
        y = uy * sy + boxOriginY * (sy - 1f) + offY
        x /= scale; y /= scale
        x *= ia
        var ix = cr * x - sr * y
        var iy = sr * x + cr * y
        ix /= ia
        ix += 0.5f
        iy += 0.5f
        out[6] = ix
        out[7] = 1f - iy
    }

    private companion object {
        const val PI_F = 3.14159265358979323846f
    }
}
