package com.nexus.app.ui.splash

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*
import kotlin.random.Random

class CgSplashView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val renderThread = RenderThread()
    @Volatile private var running = false

    private var phase = 0f
    private var time = 0L
    private var startTime = 0L
    private var frameCount = 0

    private val d get() = context.resources.displayMetrics.density
    private val cx get() = width / 2f
    private val cy get() = height / 2f
    private val R get() = min(width, height) / 2f

    private var logoAlpha = 0f
    private var textAlpha = 0f
    private var convergenceP = 0f
    private var corePulse = 0f
    private var shockP = 0f
    private var shockAlpha = 0f
    private var bloomIntensity = 0f

    private val stars = Array(400) { Star() }
    private val dustMotes = Array(200) { DustMote() }
    private val energyOrbs = Array(120) { EnergyOrb() }
    private val ribbons = Array(8) { EnergyRibbon() }
    private val sparks = mutableListOf<Spark>()
    private val nebulae = Array(5) { Nebula() }
    private val grainBuffer = IntArray(256)

    private val additivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }
    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private var offscreen: Bitmap? = null
    private var offCanvas: Canvas? = null

    init {
        holder.addCallback(this)
        repeat(256) { grainBuffer[it] = Random.nextInt(60) - 30 }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startTime = System.currentTimeMillis()
        running = true
        if (!renderThread.isAlive) renderThread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width > 0 && height > 0) {
            offscreen?.recycle()
            offscreen = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            offCanvas = Canvas(offscreen!!)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        renderThread.interrupt()
    }

    private inner class RenderThread : Thread() {
        override fun run() {
            while (running) {
                val canvas = holder.lockCanvas() ?: continue
                try {
                    time = System.currentTimeMillis() - startTime
                    phase = (time / 6000f).coerceIn(0f, 1f)
                    frameCount++
                    update()
                    render(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
                try { sleep(16) } catch (_: InterruptedException) { break }
            }
        }
    }

    private fun update() {
        val dt = 0.016f

        when {
            phase < 0.25f -> {
                convergenceP = 0f; logoAlpha = 0f; textAlpha = 0f
                bloomIntensity = phase / 0.25f * 0.3f
            }
            phase < 0.5f -> {
                val p = (phase - 0.25f) / 0.25f
                convergenceP = ease(p)
                logoAlpha = ease(p)
                bloomIntensity = 0.3f + p * 0.7f
            }
            phase < 0.7f -> {
                convergenceP = 1f; logoAlpha = 1f
                textAlpha = ease((phase - 0.5f) / 0.2f)
                bloomIntensity = 1f
            }
            else -> {
                convergenceP = 1f; logoAlpha = 1f; textAlpha = 1f
                bloomIntensity = 1f - (phase - 0.7f) / 0.3f * 0.3f
            }
        }

        corePulse = if (phase in 0.2f..0.65f) {
            ease((phase - 0.2f) / 0.45f)
        } else 0f

        if (phase in 0.35f..0.55f) {
            val sp = (phase - 0.35f) / 0.2f
            shockP = sp
            shockAlpha = (1f - sp).pow(2) * 0.9f
        } else {
            shockAlpha = 0f
        }

        stars.forEach { it.update(dt) }
        dustMotes.forEach { it.update(dt, phase) }
        energyOrbs.forEach { it.update(dt, phase, convergenceP) }
        ribbons.forEach { it.update(dt, phase) }
        nebulae.forEach { it.update(dt, phase) }

        if (phase > 0.3f && sparks.size < 100 && frameCount % 2 == 0) {
            repeat(3) { sparks.add(Spark()) }
        }
        sparks.forEach { it.update(dt) }
        sparks.removeAll { it.life <= 0 }
    }

    private fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        drawNebulae(canvas)
        drawStars(canvas)
        drawDustMotes(canvas)
        drawRibbons(canvas)
        drawEnergyOrbs(canvas)
        drawShockwave(canvas)
        drawCoreSphere(canvas)
        drawSparks(canvas)
        drawLogoGlow(canvas)
        drawLogo(canvas)
        drawLensFlare(canvas)
        drawTitle(canvas)
        applyBloom(canvas)
        applyGrain(canvas)
        applyVignette(canvas)
    }

    private fun drawSoftCircle(canvas: Canvas, x: Float, y: Float, r: Float, color: Int, addi: Boolean) {
        if (r <= 0) return
        val a = Color.alpha(color)
        if (a <= 1) return
        val paint = if (addi) additivePaint else normalPaint
        paint.shader = RadialGradient(x, y, r,
            color, Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, r, paint)
        paint.shader = null
    }

    private fun drawNebulae(canvas: Canvas) {
        canvas.saveLayer(null, null)
        nebulae.forEach { n ->
            val a = (n.alpha * 255).toInt().coerceIn(0, 255)
            drawSoftCircle(canvas, n.x, n.y, n.r,
                Color.argb(a, Color.red(n.color), Color.green(n.color), Color.blue(n.color)), true)
        }
        canvas.restore()
    }

    private fun drawStars(canvas: Canvas) {
        stars.forEach { s ->
            val a = (s.alpha * 255).toInt().coerceIn(0, 255)
            if (a <= 2) return@forEach
            normalPaint.color = Color.argb(a, 255, 255, 255)
            normalPaint.shader = null
            canvas.drawCircle(s.x, s.y, s.size, normalPaint)
            if (s.size > 1.2f && a > 80) {
                val ga = (a * 0.2f).toInt().coerceIn(0, 255)
                drawSoftCircle(canvas, s.x, s.y, s.size * 4f,
                    Color.argb(ga, 200, 220, 255), true)
            }
        }
    }

    private fun drawDustMotes(canvas: Canvas) {
        canvas.saveLayer(null, null)
        dustMotes.forEach { m ->
            val a = (m.alpha * 255).toInt().coerceIn(0, 255)
            if (a <= 2) return@forEach
            drawSoftCircle(canvas, m.x, m.y, m.size,
                Color.argb(a, Color.red(m.color), Color.green(m.color), Color.blue(m.color)), true)
            if (m.trailLen > 0.5f) {
                strokePaint.color = Color.argb((a * 0.4f).toInt().coerceIn(0, 255),
                    Color.red(m.color), Color.green(m.color), Color.blue(m.color))
                strokePaint.strokeWidth = m.size * 0.6f
                strokePaint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(m.x, m.y, m.tx, m.ty, strokePaint)
            }
        }
        canvas.restore()
    }

    private fun drawRibbons(canvas: Canvas) {
        canvas.saveLayer(null, null)
        ribbons.forEach { ribbon ->
            if (ribbon.alpha <= 0.01f) return@forEach
            val pts = ribbon.points
            if (pts.size < 2) return@forEach
            for (i in 1 until pts.size) {
                val segAlpha = ribbon.alpha * (i.toFloat() / pts.size)
                val a = (segAlpha * 255).toInt().coerceIn(0, 255)
                if (a <= 1) continue
                val t = i.toFloat() / pts.size
                val r = lerp(Color.red(ribbon.c1), Color.red(ribbon.c2), t)
                val g = lerp(Color.green(ribbon.c1), Color.green(ribbon.c2), t)
                val b = lerp(Color.blue(ribbon.c1), Color.blue(ribbon.c2), t)
                strokePaint.color = Color.argb(a, r, g, b)
                strokePaint.strokeWidth = ribbon.width * (0.3f + 0.7f * t)
                strokePaint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(pts[i - 1].first, pts[i - 1].second,
                    pts[i].first, pts[i].second, strokePaint)
            }
            val last = pts.last()
            val la = (ribbon.alpha * 255).toInt().coerceIn(0, 255)
            drawSoftCircle(canvas, last.first, last.second, ribbon.width * 3f,
                Color.argb(la, Color.red(ribbon.c2), Color.green(ribbon.c2), Color.blue(ribbon.c2)), true)
        }
        canvas.restore()
    }

    private fun drawEnergyOrbs(canvas: Canvas) {
        canvas.saveLayer(null, null)
        energyOrbs.forEach { orb ->
            val a = (orb.alpha * 255).toInt().coerceIn(0, 255)
            if (a <= 2) return@forEach
            drawSoftCircle(canvas, orb.x, orb.y, orb.size,
                Color.argb(a, Color.red(orb.color), Color.green(orb.color), Color.blue(orb.color)), true)
            if (orb.trailDist > 1f) {
                val ta = (a * 0.35f).toInt().coerceIn(0, 255)
                drawSoftCircle(canvas, orb.tx, orb.ty, orb.size * 0.7f,
                    Color.argb(ta, Color.red(orb.color), Color.green(orb.color), Color.blue(orb.color)), true)
            }
        }
        canvas.restore()
    }

    private fun drawShockwave(canvas: Canvas) {
        if (shockAlpha <= 0.01f) return
        val r1 = shockP * R * 2.2f
        val r2 = shockP * R * 1.6f
        for (i in 0..2) {
            val ri = r1 - i * 15f * d
            if (ri <= 0) continue
            val a = (shockAlpha * (1f - i * 0.3f) * 255).toInt().coerceIn(0, 255)
            strokePaint.color = Color.argb(a, 0, 230, 255)
            strokePaint.strokeWidth = (3f - i * 0.8f) * d
            strokePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawCircle(cx, cy, ri, strokePaint)
        }
        val a2 = (shockAlpha * 0.5f * 255).toInt().coerceIn(0, 255)
        strokePaint.color = Color.argb(a2, 160, 100, 255)
        strokePaint.strokeWidth = 1.5f * d
        canvas.drawCircle(cx, cy, r2, strokePaint)
    }

    private fun drawCoreSphere(canvas: Canvas) {
        if (corePulse <= 0.01f) return
        val baseR = R * 0.06f * corePulse
        val breath = 1f + sin(time * 0.004f).toFloat() * 0.15f
        val r = baseR * breath

        drawSoftCircle(canvas, cx, cy, r * 3f,
            Color.argb((corePulse * 30).toInt().coerceIn(0, 255), 0, 180, 255), true)
        drawSoftCircle(canvas, cx, cy, r * 1.8f,
            Color.argb((corePulse * 80).toInt().coerceIn(0, 255), 0, 200, 255), true)

        normalPaint.shader = RadialGradient(cx, cy, r,
            Color.argb((corePulse * 255).toInt().coerceIn(0, 255), 180, 240, 255),
            Color.argb((corePulse * 120).toInt().coerceIn(0, 255), 0, 180, 255),
            Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, normalPaint)
        normalPaint.shader = null
    }

    private fun drawSparks(canvas: Canvas) {
        canvas.saveLayer(null, null)
        sparks.forEach { sp ->
            val a = (sp.alpha * 255).toInt().coerceIn(0, 255)
            if (a <= 2) return@forEach
            drawSoftCircle(canvas, sp.x, sp.y, sp.size,
                Color.argb(a, Color.red(sp.color), Color.green(sp.color), Color.blue(sp.color)), true)
            val ta = (a * 0.5f).toInt().coerceIn(0, 255)
            strokePaint.color = Color.argb(ta, Color.red(sp.color), Color.green(sp.color), Color.blue(sp.color))
            strokePaint.strokeWidth = sp.size * 0.4f
            strokePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(sp.x, sp.y, sp.x - sp.vx * 0.03f, sp.y - sp.vy * 0.03f, strokePaint)
        }
        canvas.restore()
    }

    private fun drawLogoGlow(canvas: Canvas) {
        if (logoAlpha <= 0.01f) return
        canvas.saveLayer(null, null)
        val glowR = R * 0.2f
        for (i in 6 downTo 1) {
            val r = glowR * (1f + i * 0.5f)
            val a = logoAlpha * 0.08f / i
            drawSoftCircle(canvas, cx, cy, r,
                Color.argb((a * 255).toInt().coerceIn(0, 255), 0, 200, 255), true)
        }
        val pT = sin(time * 0.003f).toFloat() * 0.5f + 0.5f
        drawSoftCircle(canvas, cx, cy, glowR * (1.2f + pT * 0.4f),
            Color.argb((logoAlpha * 0.1f * 255).toInt().coerceIn(0, 255), 120, 80, 255), true)
        canvas.restore()
    }

    private fun drawLogo(canvas: Canvas) {
        if (logoAlpha <= 0.01f) return
        val size = R * 0.16f
        val sc = 0.5f + 0.5f * convergenceP
        val rot = time * 0.0008f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(sc, sc)

        val hex = hexPath(size)
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.strokeCap = Paint.Cap.ROUND

        strokePaint.color = Color.argb((logoAlpha * 255).toInt().coerceIn(0, 255), 0, 230, 255)
        strokePaint.strokeWidth = 2.5f * d
        canvas.drawPath(hex, strokePaint)

        normalPaint.color = Color.argb((logoAlpha * 25).toInt().coerceIn(0, 255), 0, 200, 255)
        canvas.drawPath(hex, normalPaint)

        canvas.save()
        canvas.rotate((rot * 25f) % 360f)
        val inner = hexPath(size * 0.52f)
        strokePaint.color = Color.argb((logoAlpha * 180).toInt().coerceIn(0, 255), 140, 90, 255)
        strokePaint.strokeWidth = 1.5f * d
        canvas.drawPath(inner, strokePaint)
        canvas.restore()

        canvas.save()
        canvas.rotate((-rot * 15f) % 360f)
        val outer = hexPath(size * 1.15f)
        strokePaint.color = Color.argb((logoAlpha * 50).toInt().coerceIn(0, 255), 0, 200, 255)
        strokePaint.strokeWidth = 1f * d
        canvas.drawPath(outer, strokePaint)
        canvas.restore()

        drawSoftCircle(canvas, 0f, 0f, size * 0.18f,
            Color.argb((logoAlpha * 255).toInt().coerceIn(0, 255), 200, 240, 255), false)

        canvas.restore()
    }

    private fun drawLensFlare(canvas: Canvas) {
        if (logoAlpha < 0.5f) return
        val intensity = (logoAlpha - 0.5f) * 2f * bloomIntensity
        if (intensity <= 0.01f) return

        canvas.saveLayer(null, null)
        val flareAngle = time * 0.0002f
        val streakLen = R * 0.8f * intensity
        val a = (intensity * 0.25f * 255).toInt().coerceIn(0, 255)

        for (i in -3..3) {
            val offset = i * 8f * d
            val sa = (a * (1f - abs(i) / 4f)).toInt().coerceIn(0, 255)
            strokePaint.color = Color.argb(sa, 0, 200, 255)
            strokePaint.strokeWidth = (2f - abs(i) * 0.4f) * d
            canvas.drawLine(
                cx - streakLen * cos(flareAngle) + offset * sin(flareAngle),
                cy - streakLen * sin(flareAngle) - offset * cos(flareAngle),
                cx + streakLen * cos(flareAngle) + offset * sin(flareAngle),
                cy + streakLen * sin(flareAngle) - offset * cos(flareAngle),
                strokePaint
            )
        }

        val ghosts = floatArrayOf(0.3f, -0.5f, 0.8f, -1.2f, 1.6f)
        val ghostColors = intArrayOf(
            Color.argb(255, 0, 200, 255), Color.argb(255, 140, 80, 255),
            Color.argb(255, 0, 255, 200), Color.argb(255, 255, 100, 150),
            Color.argb(255, 100, 200, 255)
        )
        ghosts.forEachIndexed { i, g ->
            val gx = cx + (cx - cx) * g
            val gy = cy + (cy - cy) * g
            val gr = R * 0.04f * abs(g) * intensity
            val ga = (intensity * 0.15f * 255 / (abs(g) + 1f)).toInt().coerceIn(0, 255)
            drawSoftCircle(canvas, gx, gy, gr,
                Color.argb(ga, Color.red(ghostColors[i]), Color.green(ghostColors[i]), Color.blue(ghostColors[i])), true)
        }
        canvas.restore()
    }

    private fun drawTitle(canvas: Canvas) {
        if (textAlpha <= 0.01f) return
        val titleSize = R * 0.07f
        textPaint.textSize = titleSize
        textPaint.letterSpacing = 0.2f
        textPaint.color = Color.argb((textAlpha * 255).toInt().coerceIn(0, 255), 0, 230, 255)
        val title = "NEXUS"
        val tw = textPaint.measureText(title)
        val ty = cy + R * 0.3f
        val tx = cx - tw / 2f
        canvas.drawText(title, tx, ty, textPaint)

        val scanX = tx + tw * ((time % 2000) / 2000f)
        val scanA = (textAlpha * 150).toInt().coerceIn(0, 255)
        canvas.saveLayer(null, null)
        additivePaint.color = Color.argb(scanA, 100, 220, 255)
        val scanRect = RectF(scanX - 15f * d, ty - titleSize, scanX + 15f * d, ty + 5f * d)
        canvas.drawRect(scanRect, additivePaint)
        additivePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        canvas.restore()

        val subSz = titleSize * 0.35f
        normalPaint.textSize = subSz
        normalPaint.letterSpacing = 0.35f
        normalPaint.color = Color.argb((textAlpha * 130).toInt().coerceIn(0, 255), 140, 90, 255)
        normalPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val sub = "INITIALIZING NEXUS"
        val sw = normalPaint.measureText(sub)
        canvas.drawText(sub, cx - sw / 2f, ty + subSz * 2f, normalPaint)
        normalPaint.typeface = null

        val pw = R * 0.25f
        val py = ty + subSz * 3.8f
        strokePaint.strokeWidth = 1.5f * d
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.color = Color.argb((textAlpha * 40).toInt().coerceIn(0, 255), 0, 200, 255)
        canvas.drawLine(cx - pw, py, cx + pw, py, strokePaint)
        strokePaint.color = Color.argb((textAlpha * 220).toInt().coerceIn(0, 255), 0, 230, 255)
        val fillEnd = cx - pw + pw * 2f * phase
        canvas.drawLine(cx - pw, py, fillEnd, py, strokePaint)
        drawSoftCircle(canvas, fillEnd, py, 6f * d,
            Color.argb((textAlpha * 200).toInt().coerceIn(0, 255), 0, 230, 255), true)
    }

    private fun applyBloom(canvas: Canvas) {
        if (bloomIntensity <= 0.01f) return
        canvas.saveLayer(null, null)
        val points = mutableListOf<Triple<Float, Float, Int>>()
        energyOrbs.filter { it.alpha > 0.3f }.forEach {
            points.add(Triple(it.x, it.y, it.color))
        }
        sparks.filter { it.alpha > 0.5f }.take(20).forEach {
            points.add(Triple(it.x, it.y, it.color))
        }
        if (corePulse > 0.1f) {
            points.add(Triple(cx, cy, Color.argb(255, 0, 200, 255)))
        }
        points.take(30).forEach { (x, y, c) ->
            val br = R * 0.08f * bloomIntensity
            val ba = (bloomIntensity * 0.06f * 255).toInt().coerceIn(0, 255)
            drawSoftCircle(canvas, x, y, br,
                Color.argb(ba, Color.red(c), Color.green(c), Color.blue(c)), true)
        }
        canvas.restore()
    }

    private fun applyGrain(canvas: Canvas) {
        val ga = 12
        for (i in 0 until 80) {
            val gx = Random.nextFloat() * width
            val gy = Random.nextFloat() * height
            val gv = grainBuffer[Random.nextInt(256)] + 128
            normalPaint.color = Color.argb(ga, gv, gv, gv)
            canvas.drawPoint(gx, gy, normalPaint)
        }
    }

    private fun applyVignette(canvas: Canvas) {
        val gradient = RadialGradient(cx, cy, R * 1.3f,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(200, 0, 0, 0)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        normalPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), normalPaint)
        normalPaint.shader = null
    }

    private fun hexPath(size: Float): Path {
        return Path().apply {
            for (i in 0..5) {
                val a = Math.toRadians(60.0 * i - 30.0)
                val x = size * cos(a).toFloat()
                val y = size * sin(a).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

    private fun ease(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return if (c < 0.5f) 4f * c * c * c else 1f - (-2f * c + 2f).pow(3) / 2f
    }

    private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).toInt()

    fun isAnimationComplete(): Boolean = phase >= 1f

    inner class Star {
        var x = Random.nextFloat() * 3000
        var y = Random.nextFloat() * 3000
        var size = Random.nextFloat().pow(2) * 2f + 0.3f
        var baseAlpha = Random.nextFloat().pow(1.5f) * 0.7f + 0.1f
        var alpha = baseAlpha
        private var twinkleSpd = Random.nextFloat() * 4f + 0.5f
        private var twinkleOff = Random.nextFloat() * 6.28f
        private var driftSpd = Random.nextFloat() * 3f + 0.5f
        private var driftAngle = Random.nextFloat() * 6.28f
        private var tintR = 200 + Random.nextInt(56)
        private var tintG = 200 + Random.nextInt(56)
        private var tintB = 220 + Random.nextInt(36)

        fun update(dt: Float) {
            x += cos(driftAngle) * driftSpd * dt
            y += sin(driftAngle) * driftSpd * dt
            if (x < -10) x += width + 20f
            if (x > width + 10) x -= width + 20f
            if (y < -10) y += height + 20f
            if (y > height + 10) y -= height + 20f
            val t = time * 0.001f * twinkleSpd + twinkleOff
            alpha = baseAlpha * (0.4f + 0.6f * (sin(t) * 0.5f + 0.5f))
        }
    }

    inner class DustMote {
        var x = Random.nextFloat() * 3000
        var y = Random.nextFloat() * 3000
        var tx = x; var ty = y
        var size = Random.nextFloat() * 5f + 2f
        var alpha = 0f
        var trailLen = 0f
        var color = when (Random.nextInt(5)) {
            0 -> Color.argb(255, 0, 200, 255)
            1 -> Color.argb(255, 80, 120, 255)
            2 -> Color.argb(255, 0, 255, 200)
            3 -> Color.argb(255, 200, 180, 255)
            else -> Color.argb(255, 140, 80, 255)
        }
        private var vx = (Random.nextFloat() - 0.5f) * 20f
        private var vy = (Random.nextFloat() - 0.5f) * 20f
        private var noiseOff = Random.nextFloat() * 100f

        fun update(dt: Float, phase: Float) {
            tx = x; ty = y
            val noise = sin(time * 0.002f + noiseOff).toFloat() * 15f
            x += (vx + noise) * dt
            y += (vy + cos(time * 0.0015f + noiseOff).toFloat() * 10f) * dt
            if (phase > 0.15f) {
                val pull = (phase - 0.15f) * 0.8f
                val dx = cx - x; val dy = cy - y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > R * 0.08f) {
                    x += dx / dist * pull * 25f * dt
                    y += dy / dist * pull * 25f * dt
                }
            }
            trailLen = sqrt((x - tx) * (x - tx) + (y - ty) * (y - ty))
            alpha = if (phase < 0.1f) phase / 0.1f * 0.4f else 0.4f
        }
    }

    inner class EnergyOrb {
        var angle = Random.nextFloat() * 6.28f
        var dist = Random.nextFloat() * R * 1.5f + R * 0.2f
        var x = cx + cos(angle) * dist
        var y = cy + sin(angle) * dist
        var tx = x; var ty = y
        var size = Random.nextFloat() * 6f + 3f
        var alpha = 0f
        var trailDist = 0f
        var color = when (Random.nextInt(4)) {
            0 -> Color.argb(255, 0, 230, 255)
            1 -> Color.argb(255, 60, 100, 255)
            2 -> Color.argb(255, 140, 80, 255)
            else -> Color.argb(255, 0, 255, 220)
        }
        private var speed = Random.nextFloat() * 150f + 50f
        private var spiral = (Random.nextFloat() - 0.5f) * 4f
        private var wobble = Random.nextFloat() * 2f
        private var wobbleOff = Random.nextFloat() * 6.28f

        fun update(dt: Float, phase: Float, convergence: Float) {
            tx = x; ty = y
            if (phase < 0.08f) { alpha = 0f; return }
            angle += (speed / max(dist, 1f) + spiral) * dt
            dist -= speed * dt * convergence * 0.7f
            val w = sin(time * 0.005f + wobbleOff).toFloat() * wobble * 5f
            dist += w * dt
            if (dist < R * 0.04f) {
                dist = Random.nextFloat() * R * 1.3f + R * 0.2f
                angle = Random.nextFloat() * 6.28f
                color = when (Random.nextInt(4)) {
                    0 -> Color.argb(255, 0, 230, 255)
                    1 -> Color.argb(255, 60, 100, 255)
                    2 -> Color.argb(255, 140, 80, 255)
                    else -> Color.argb(255, 0, 255, 220)
                }
                alpha = 0f
            }
            x = cx + cos(angle) * dist
            y = cy + sin(angle) * dist
            trailDist = sqrt((x - tx) * (x - tx) + (y - ty) * (y - ty))
            val fadeIn = min((phase - 0.08f) / 0.12f, 1f)
            val distF = 1f - (dist / (R * 1.5f)).coerceIn(0f, 1f)
            alpha = fadeIn * (0.3f + distF * 0.7f)
            size = (2f + distF * 5f) * d
        }
    }

    inner class EnergyRibbon {
        var alpha = 0f
        var width = 0f
        var c1 = Color.argb(255, 0, 200, 255)
        var c2 = Color.argb(255, 140, 80, 255)
        val points = mutableListOf<Pair<Float, Float>>()
        private var baseAngle = Random.nextFloat() * 6.28f
        private var dist = Random.nextFloat() * R * 0.8f + R * 0.3f
        private var speed = Random.nextFloat() * 80f + 40f
        private var spiralRate = Random.nextFloat() * 2f + 1f
        private var segCount = Random.nextInt(15, 25)

        init {
            if (Random.nextBoolean()) {
                c1 = Color.argb(255, 140, 80, 255)
                c2 = Color.argb(255, 0, 200, 255)
            }
            width = (Random.nextFloat() * 2f + 1f) * d
        }

        fun update(dt: Float, phase: Float) {
            if (phase < 0.12f || phase > 0.65f) { alpha = 0f; return }
            val localP = (phase - 0.12f) / 0.53f
            alpha = sin(localP * Math.PI.toFloat()) * 0.7f
            dist -= speed * dt * 0.3f
            if (dist < R * 0.05f) dist = R * 0.8f + Random.nextFloat() * R * 0.3f
            baseAngle += spiralRate * dt
            points.clear()
            var a = baseAngle
            var d = dist
            for (i in 0 until segCount) {
                val px = cx + cos(a) * d
                val py = cy + sin(a) * d
                points.add(Pair(px, py))
                a += 0.25f + sin(time * 0.003f + i * 0.5f).toFloat() * 0.1f
                d -= dist * 0.04f
            }
        }
    }

    inner class Spark {
        var x = cx + (Random.nextFloat() - 0.5f) * 30f * d
        var y = cy + (Random.nextFloat() - 0.5f) * 30f * d
        var vx = (Random.nextFloat() - 0.5f) * 500f
        var vy = (Random.nextFloat() - 0.5f) * 500f
        var size = Random.nextFloat() * 4f * d + 1f
        var alpha = 1f
        var life = 1f
        var color = when (Random.nextInt(5)) {
            0 -> Color.argb(255, 0, 230, 255)
            1 -> Color.argb(255, 255, 200, 100)
            2 -> Color.argb(255, 0, 255, 220)
            3 -> Color.argb(255, 200, 150, 255)
            else -> Color.argb(255, 100, 220, 255)
        }
        private var decay = Random.nextFloat() * 2f + 1f

        fun update(dt: Float) {
            x += vx * dt; y += vy * dt
            vx *= 0.98f; vy *= 0.98f
            life -= decay * dt
            alpha = life.coerceIn(0f, 1f)
            size *= (1f - dt * 0.8f)
        }
    }

    inner class Nebula {
        var x = cx + (Random.nextFloat() - 0.5f) * R * 1.5f
        var y = cy + (Random.nextFloat() - 0.5f) * R * 1.5f
        var r = Random.nextFloat() * R * 0.5f + R * 0.2f
        var alpha = Random.nextFloat() * 0.08f + 0.02f
        var color = when (Random.nextInt(3)) {
            0 -> Color.argb(255, 15, 8, 50)
            1 -> Color.argb(255, 30, 5, 60)
            else -> Color.argb(255, 5, 20, 55)
        }
        private var dx = (Random.nextFloat() - 0.5f) * 6f
        private var dy = (Random.nextFloat() - 0.5f) * 6f

        fun update(dt: Float, phase: Float) {
            x += dx * dt; y += dy * dt
            val breathe = sin(time * 0.0004f + x * 0.005f).toFloat()
            alpha = (0.03f + breathe * 0.04f).coerceIn(0.01f, 0.1f) * (1f - phase * 0.4f)
            r = (R * 0.3f + breathe * R * 0.05f).coerceIn(R * 0.1f, R * 0.6f)
        }
    }
}
