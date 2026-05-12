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

    private val thread = SplashThread()
    @Volatile private var running = false

    private var phase = 0f
    private var time = 0L
    private var startTime = 0L

    private val starParticles = mutableListOf<StarParticle>()
    private val energyParticles = mutableListOf<EnergyParticle>()
    private val nebulaBlobs = mutableListOf<NebulaBlob>()
    private val streakLines = mutableListOf<StreakLine>()
    private val sparkParticles = mutableListOf<SparkParticle>()

    private val density get() = context.resources.displayMetrics.density
    private val centerX get() = width / 2f
    private val centerY get() = height / 2f
    private val radius get() = min(width, height) / 2f

    private val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val bgPaint = Paint()

    private var logoAlpha = 0f
    private var textAlpha = 0f
    private var pulseRadius = 0f
    private var pulseAlpha = 0f
    private var shockwaveRadius = 0f
    private var shockwaveAlpha = 0f
    private var convergenceProgress = 0f

    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    init {
        holder.addCallback(this)
        initParticles()
    }

    private fun initParticles() {
        for (i in 0..300) {
            starParticles.add(StarParticle())
        }
        for (i in 0..150) {
            energyParticles.add(EnergyParticle())
        }
        for (i in 0..6) {
            nebulaBlobs.add(NebulaBlob())
        }
        for (i in 0..12) {
            streakLines.add(StreakLine())
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startTime = System.currentTimeMillis()
        running = true
        if (!thread.isAlive) thread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread.interrupt()
    }

    private inner class SplashThread : Thread() {
        override fun run() {
            while (running) {
                val canvas = holder.lockCanvas() ?: continue
                try {
                    time = System.currentTimeMillis() - startTime
                    phase = min(time / 5500f, 1f)
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
            phase < 0.3f -> {
                convergenceProgress = 0f
                logoAlpha = 0f
                textAlpha = 0f
            }
            phase < 0.55f -> {
                val p = (phase - 0.3f) / 0.25f
                convergenceProgress = easeInOutCubic(p)
                logoAlpha = easeInOutCubic(p)
            }
            phase < 0.75f -> {
                convergenceProgress = 1f
                logoAlpha = 1f
                textAlpha = easeInOutCubic((phase - 0.55f) / 0.2f)
            }
            else -> {
                convergenceProgress = 1f
                logoAlpha = 1f
                textAlpha = 1f
            }
        }

        if (phase in 0.28f..0.58f) {
            val sp = (phase - 0.28f) / 0.3f
            pulseRadius = sp * radius * 1.5f
            pulseAlpha = (1f - sp) * 0.8f
        } else {
            pulseAlpha = 0f
        }

        if (phase in 0.32f..0.52f) {
            val sp = (phase - 0.32f) / 0.2f
            shockwaveRadius = sp * radius * 2f
            shockwaveAlpha = (1f - sp) * 0.6f
        } else {
            shockwaveAlpha = 0f
        }

        starParticles.forEach { it.update(dt, phase) }
        energyParticles.forEach { it.update(dt, phase, convergenceProgress) }

        if (phase > 0.25f && sparkParticles.size < 80) {
            for (i in 0..2) sparkParticles.add(SparkParticle())
        }
        sparkParticles.forEach { it.update(dt) }
        sparkParticles.removeAll { it.life <= 0 }

        streakLines.forEach { it.update(dt, phase) }
        nebulaBlobs.forEach { it.update(dt, phase) }
    }

    private fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        renderNebula(canvas)
        renderStars(canvas)
        renderStreaks(canvas)
        renderEnergyParticles(canvas)
        renderShockwave(canvas)
        renderEnergyPulse(canvas)
        renderSparks(canvas)
        renderLogoGlow(canvas)
        renderLogo(canvas)
        renderTitle(canvas)
        renderVignette(canvas)
    }

    private fun renderNebula(canvas: Canvas) {
        nebulaBlobs.forEach { blob ->
            val alpha = (blob.alpha * (1f - phase * 0.5f) * 255).toInt().coerceIn(0, 255)
            if (alpha <= 0) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = blob.color
                this.alpha = alpha
            }
            canvas.save()
            canvas.scale(blob.scale, blob.scale, blob.x, blob.y)
            canvas.drawCircle(blob.x, blob.y, blob.r, paint)
            canvas.restore()
        }
    }

    private fun renderStars(canvas: Canvas) {
        starParticles.forEach { star ->
            val alpha = (star.alpha * 255).toInt().coerceIn(0, 255)
            if (alpha <= 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                this.alpha = alpha
            }
            canvas.drawCircle(star.x, star.y, star.size, paint)
        }
    }

    private fun renderStreaks(canvas: Canvas) {
        streakLines.forEach { streak ->
            val alpha = (streak.alpha * 255).toInt().coerceIn(0, 255)
            if (alpha <= 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = streak.color
                this.alpha = alpha
                strokeWidth = streak.width
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(streak.x1, streak.y1, streak.x2, streak.y2, paint)
        }
    }

    private fun renderEnergyParticles(canvas: Canvas) {
        energyParticles.forEach { p ->
            val alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            if (alpha <= 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = p.color
                this.alpha = alpha
            }
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }

    private fun renderShockwave(canvas: Canvas) {
        if (shockwaveAlpha <= 0) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
            color = Color.argb(
                (shockwaveAlpha * 255).toInt().coerceIn(0, 255),
                0, 245, 255
            )
        }
        canvas.drawCircle(centerX, centerY, shockwaveRadius, paint)

        val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            color = Color.argb(
                (shockwaveAlpha * 0.5f * 255).toInt().coerceIn(0, 255),
                139, 92, 246
            )
        }
        canvas.drawCircle(centerX, centerY, shockwaveRadius * 0.8f, paint2)
    }

    private fun renderEnergyPulse(canvas: Canvas) {
        if (pulseAlpha <= 0) return
        val steps = 4
        for (i in 0 until steps) {
            val r = pulseRadius - i * 20f * density
            if (r <= 0) continue
            val a = pulseAlpha * (1f - i.toFloat() / steps)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (3f - i * 0.5f) * density
                color = Color.argb((a * 200).toInt().coerceIn(0, 255), 0, 212, 255)
            }
            canvas.drawCircle(centerX, centerY, r, paint)
        }
    }

    private fun renderSparks(canvas: Canvas) {
        sparkParticles.forEach { spark ->
            val alpha = (spark.alpha * 255).toInt().coerceIn(0, 255)
            if (alpha <= 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spark.color
                this.alpha = alpha
            }
            canvas.drawCircle(spark.x, spark.y, spark.size, paint)
        }
    }

    private fun renderLogoGlow(canvas: Canvas) {
        if (logoAlpha <= 0) return
        val glowR = radius * 0.22f
        val layers = 5
        for (i in layers downTo 1) {
            val r = glowR * (1f + i * 0.4f)
            val a = logoAlpha * 0.12f / i
            glowPaint.color = Color.argb((a * 255).toInt().coerceIn(0, 255), 0, 212, 255)
            canvas.drawCircle(centerX, centerY, r, glowPaint)
        }

        val pulseT = sin(time * 0.003f).toFloat() * 0.5f + 0.5f
        val pulseR = glowR * (1.1f + pulseT * 0.3f)
        glowPaint.color = Color.argb(
            (logoAlpha * 0.08f * 255).toInt().coerceIn(0, 255), 139, 92, 246
        )
        canvas.drawCircle(centerX, centerY, pulseR, glowPaint)
    }

    private fun renderLogo(canvas: Canvas) {
        if (logoAlpha <= 0) return
        val size = radius * 0.18f
        val scale = 0.6f + 0.4f * convergenceProgress

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.scale(scale, scale)

        val hexPath = createHexagonPath(size)
        logoPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            strokeJoin = Paint.Join.ROUND
            color = Color.argb((logoAlpha * 255).toInt().coerceIn(0, 255), 0, 245, 255)
        }
        canvas.drawPath(hexPath, logoPaint)

        logoPaint.style = Paint.Style.FILL
        logoPaint.color = Color.argb((logoAlpha * 40).toInt().coerceIn(0, 255), 0, 245, 255)
        canvas.drawPath(hexPath, logoPaint)

        val innerSize = size * 0.55f
        val innerPath = createHexagonPath(innerSize)
        val rotation = time * 0.001f
        canvas.rotate((rotation * 30 % 360))
        logoPaint.style = Paint.Style.STROKE
        logoPaint.strokeWidth = 2f * density
        logoPaint.color = Color.argb((logoAlpha * 200).toInt().coerceIn(0, 255), 139, 92, 246)
        canvas.drawPath(innerPath, logoPaint)

        val coreSize = size * 0.2f
        logoPaint.style = Paint.Style.FILL
        logoPaint.color = Color.argb((logoAlpha * 255).toInt().coerceIn(0, 255), 0, 212, 255)
        canvas.drawCircle(0f, 0f, coreSize, logoPaint)

        val coreGlowSize = coreSize * (1.5f + sin(time * 0.005f).toFloat() * 0.3f)
        logoPaint.color = Color.argb((logoAlpha * 80).toInt().coerceIn(0, 255), 0, 212, 255)
        canvas.drawCircle(0f, 0f, coreGlowSize, logoPaint)

        canvas.restore()
    }

    private fun renderTitle(canvas: Canvas) {
        if (textAlpha <= 0) return
        val titleSize = radius * 0.08f
        textPaint.textSize = titleSize
        textPaint.color = Color.argb((textAlpha * 255).toInt().coerceIn(0, 255), 0, 245, 255)
        textPaint.letterSpacing = 0.15f
        val title = "NEXUS"
        val titleWidth = textPaint.measureText(title)
        val titleY = centerY + radius * 0.32f
        canvas.drawText(title, centerX - titleWidth / 2, titleY, textPaint)

        val subSize = titleSize * 0.4f
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = subSize
            color = Color.argb((textAlpha * 150).toInt().coerceIn(0, 255), 139, 92, 246)
            letterSpacing = 0.3f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val subtitle = "INITIALIZING NEXUS"
        val subWidth = subPaint.measureText(subtitle)
        canvas.drawText(subtitle, centerX - subWidth / 2, titleY + subSize * 1.8f, subPaint)

        val progressWidth = radius * 0.3f
        val progressY = titleY + subSize * 3.5f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((textAlpha * 40).toInt().coerceIn(0, 255), 0, 245, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((textAlpha * 200).toInt().coerceIn(0, 255), 0, 245, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            strokeCap = Paint.Cap.ROUND
        }
        val rect = RectF(
            centerX - progressWidth, progressY,
            centerX + progressWidth, progressY
        )
        canvas.drawLine(rect.left, rect.top, rect.right, rect.top, bgPaint)

        val fillEnd = rect.left + (rect.right - rect.left) * phase
        canvas.drawLine(rect.left, rect.top, fillEnd, rect.top, fillPaint)
    }

    private fun renderVignette(canvas: Canvas) {
        val gradient = RadialGradient(
            centerX, centerY, radius * 1.2f,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun createHexagonPath(size: Float): Path {
        return Path().apply {
            for (i in 0..5) {
                val angle = Math.toRadians(60.0 * i - 30.0)
                val x = size * cos(angle).toFloat()
                val y = size * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

    private fun easeInOutCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return if (c < 0.5f) 4f * c * c * c else 1f - (-2f * c + 2f).pow(3) / 2f
    }

    inner class StarParticle {
        var x = Random.nextFloat() * 2000
        var y = Random.nextFloat() * 2000
        var size = Random.nextFloat() * 2.5f + 0.5f
        var baseAlpha = Random.nextFloat() * 0.6f + 0.2f
        var alpha = baseAlpha
        private var twinkleSpeed = Random.nextFloat() * 3f + 1f
        private var twinkleOffset = Random.nextFloat() * 6.28f
        private var driftX = (Random.nextFloat() - 0.5f) * 8f
        private var driftY = (Random.nextFloat() - 0.5f) * 8f

        fun update(dt: Float, phase: Float) {
            x += driftX * dt
            y += driftY * dt
            if (x < 0) x += width.toFloat()
            if (x > width) x -= width.toFloat()
            if (y < 0) y += height.toFloat()
            if (y > height) y -= height.toFloat()
            val t = time * 0.001f * twinkleSpeed + twinkleOffset
            alpha = baseAlpha * (0.5f + 0.5f * sin(t))
            if (phase > 0.25f) {
                val pull = (phase - 0.25f) * 0.5f
                val dx = centerX - x
                val dy = centerY - y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > radius * 0.15f) {
                    x += dx / dist * pull * 15f * dt
                    y += dy / dist * pull * 15f * dt
                }
            }
        }
    }

    inner class EnergyParticle {
        var angle = Random.nextFloat() * 6.28f
        var dist = Random.nextFloat() * radius * 1.5f + radius * 0.3f
        var x = centerX + cos(angle) * dist
        var y = centerY + sin(angle) * dist
        var size = Random.nextFloat() * 4f + 2f
        var alpha = 0f
        var color = randomNeonColor()
        private var speed = Random.nextFloat() * 120f + 60f
        private var spiralRate = (Random.nextFloat() - 0.5f) * 3f
        private var trailAngle = angle
        private var activated = false

        fun update(dt: Float, phase: Float, convergence: Float) {
            if (phase < 0.1f) {
                alpha = 0f
                return
            }
            if (!activated && phase > 0.1f) activated = true
            if (!activated) return

            angle += (speed / max(dist, 1f) + spiralRate) * dt
            dist -= speed * dt * convergence * 0.8f

            if (dist < radius * 0.08f) {
                dist = Random.nextFloat() * radius * 1.2f + radius * 0.3f
                angle = Random.nextFloat() * 6.28f
                color = randomNeonColor()
                alpha = 0f
            }

            x = centerX + cos(angle) * dist
            y = centerY + sin(angle) * dist
            trailAngle = angle

            val fadeIn = min((phase - 0.1f) / 0.15f, 1f)
            val distFactor = 1f - (dist / (radius * 1.5f)).coerceIn(0f, 1f)
            alpha = fadeIn * distFactor * 0.9f
            size = (2f + distFactor * 3f) * density
        }

        private fun randomNeonColor(): Int {
            return when (Random.nextInt(4)) {
                0 -> Color.argb(255, 0, 245, 255)
                1 -> Color.argb(255, 0, 102, 255)
                2 -> Color.argb(255, 139, 92, 246)
                else -> Color.argb(255, 255, 0, 255)
            }
        }
    }

    inner class NebulaBlob {
        var x = centerX + (Random.nextFloat() - 0.5f) * radius
        var y = centerY + (Random.nextFloat() - 0.5f) * radius
        var r = Random.nextFloat() * radius * 0.4f + radius * 0.15f
        var alpha = Random.nextFloat() * 0.12f + 0.03f
        var scale = 1f
        var color = randomNebulaColor()
        private var driftX = (Random.nextFloat() - 0.5f) * 10f
        private var driftY = (Random.nextFloat() - 0.5f) * 10f

        fun update(dt: Float, phase: Float) {
            x += driftX * dt
            y += driftY * dt
            scale = 1f + sin(time * 0.0005f + x * 0.01f) * 0.15f
            alpha = (0.03f + sin(time * 0.001f + y * 0.005f) * 0.06f).coerceIn(0.01f, 0.12f)
        }

        private fun randomNebulaColor(): Int {
            return when (Random.nextInt(3)) {
                0 -> Color.argb(255, 20, 10, 60)
                1 -> Color.argb(255, 40, 0, 80)
                else -> Color.argb(255, 0, 30, 70)
            }
        }
    }

    inner class StreakLine {
        var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f
        var alpha = 0f
        var width = 1f
        var color = Color.argb(255, 0, 245, 255)
        private var baseAngle = Random.nextFloat() * 6.28f
        private var speed = Random.nextFloat() * 500f + 200f
        private var length = Random.nextFloat() * 200f + 80f
        private var dist = Random.nextFloat() * radius * 2f

        fun update(dt: Float, phase: Float) {
            if (phase < 0.15f || phase > 0.6f) {
                alpha = 0f
                return
            }
            val localPhase = (phase - 0.15f) / 0.45f
            dist += speed * dt
            if (dist > radius * 2.5f) {
                dist = -length
                baseAngle = Random.nextFloat() * 6.28f
                color = randomStreakColor()
            }
            val cx = centerX + cos(baseAngle) * dist
            val cy = centerY + sin(baseAngle) * dist
            x1 = cx - cos(baseAngle) * length * 0.5f
            y1 = cy - sin(baseAngle) * length * 0.5f
            x2 = cx + cos(baseAngle) * length * 0.5f
            y2 = cy + sin(baseAngle) * length * 0.5f
            alpha = sin(localPhase * Math.PI.toFloat()) * 0.6f * density
            width = (Random.nextFloat() * 1.5f + 0.5f) * density
        }

        private fun randomStreakColor(): Int {
            return when (Random.nextInt(3)) {
                0 -> Color.argb(255, 0, 245, 255)
                1 -> Color.argb(255, 0, 102, 255)
                else -> Color.argb(255, 139, 92, 246)
            }
        }
    }

    inner class SparkParticle {
        var x = centerX
        var y = centerY
        var size = Random.nextFloat() * 3f + 1f
        var alpha = 1f
        var life = 1f
        var color = randomSparkColor()
        private var vx = (Random.nextFloat() - 0.5f) * 400f
        private var vy = (Random.nextFloat() - 0.5f) * 400f
        private var decay = Random.nextFloat() * 1.5f + 0.8f

        fun update(dt: Float) {
            x += vx * dt
            y += vy * dt
            life -= decay * dt
            alpha = life.coerceIn(0f, 1f)
            size *= (1f - dt * 0.5f)
        }

        private fun randomSparkColor(): Int {
            return when (Random.nextInt(4)) {
                0 -> Color.argb(255, 0, 245, 255)
                1 -> Color.argb(255, 255, 215, 0)
                2 -> Color.argb(255, 255, 0, 255)
                else -> Color.argb(255, 0, 212, 255)
            }
        }
    }

    fun isAnimationComplete(): Boolean = phase >= 1f
}
