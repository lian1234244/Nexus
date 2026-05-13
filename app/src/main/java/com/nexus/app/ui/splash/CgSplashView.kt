package com.nexus.app.ui.splash

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*
import kotlin.random.Random

class CgSplashView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val thread = RenderThread()
    @Volatile private var running = false
    private var phase = 0f; private var time = 0L; private var startTime = 0L
    private val cx get() = width / 2f; private val cy get() = height / 2f
    private val R get() = min(width, height) / 2f
    private var logoAlpha = 0f; private var textAlpha = 0f; private var arcP = 0f
    private var webP = 0f; private var glowP = 0f
    private val TAU = (Math.PI * 2).toFloat()

    private val stars3 = Array(3) { l ->
        val cfg = listOf(Triple(500, .3f, .08f), Triple(200, .6f, .3f), Triple(70, 1f, .8f))[l]
        Array(cfg.first) { Star(cfg.third) }
    }
    private val dustArr = Array(250) { Dust() }
    private val orbArr = Array(160) { Orb() }
    private val ribbonArr = Array(10) { Ribbon() }
    private val webPts = Array(16) { floatArrayOf(0f, 0f) }
    private val webEdges = mutableListOf<Pair<Int, Int>>()
    private val sparks = mutableListOf<Spark>()

    private val addP = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) }
    private val nP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }

    private fun rand(a: Float, b: Float) = a + Random.nextFloat() * (b - a)
    private fun randI(a: Int, b: Int) = Random.nextInt(a, b + 1)
    private fun cl(v: Float, a: Float, b: Float) = v.coerceIn(a, b)
    private fun lr(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun lrI(a: Int, b: Int, t: Float) = (a + (b - a) * t).toInt()
    private fun ease(t: Float): Float { val c = cl(t, 0f, 1f); return if (c < .5f) 4 * c * c * c else 1 - (-2 * c + 2).pow(3) / 2f }

    init { holder.addCallback(this) }

    private fun rebuildWeb() {
        for (i in webPts.indices) {
            val a = rand(0f, TAU); val d = rand(.1f, .5f)
            webPts[i][0] = cx + cos(a) * d * R; webPts[i][1] = cy + sin(a) * d * R
        }
        webEdges.clear()
        for (i in webPts.indices) {
            (webPts.indices.filter { it != i }).sortedBy { j -> hypot((webPts[i][0] - webPts[j][0]).toDouble(), (webPts[i][1] - webPts[j][1]).toDouble()) }.take(2).forEach { j ->
                val e = Pair(minOf(i, j), maxOf(i, j))
                if (webEdges.none { it == e }) webEdges.add(e)
            }
        }
    }

    private fun soft(c: Canvas, x: Float, y: Float, r: Float, color: Int, add: Boolean) {
        if (r <= 0 || Color.alpha(color) <= 1) return
        val p = if (add) addP else nP
        p.shader = RadialGradient(x, y, r, color, Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)), Shader.TileMode.CLAMP)
        c.drawCircle(x, y, r, p); p.shader = null
    }

    override fun surfaceCreated(h: SurfaceHolder) { startTime = System.currentTimeMillis(); running = true; if (!thread.isAlive) thread.start() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h: Int) { rebuildWeb() }
    override fun surfaceDestroyed(h: SurfaceHolder) { running = false; thread.interrupt() }

    private inner class RenderThread : Thread() {
        override fun run() {
            while (running) {
                val canvas = holder.lockCanvas() ?: continue
                try { time = System.currentTimeMillis() - startTime; phase = cl(time / 6200f, 0f, 1f); update(); render(canvas) }
                finally { holder.unlockCanvasAndPost(canvas) }
                try { sleep(16) } catch (_: InterruptedException) { break }
            }
        }
    }

    private fun update() {
        val dt = .016f
        glowP = ease(cl((phase - .05f) / .2f, 0f, 1f)); arcP = ease(cl((phase - .18f) / .25f, 0f, 1f))
        webP = ease(cl((phase - .28f) / .2f, 0f, 1f)); logoAlpha = ease(cl((phase - .36f) / .2f, 0f, 1f))
        textAlpha = ease(cl((phase - .52f) / .15f, 0f, 1f))
        stars3.forEach { it.forEach { s -> s.a = s.baseA * (.35f + .65f * (sin(time * .001f * s.tw + s.to) * .5f + .5f)) * glowP } }
        dustArr.forEach { it.update(dt, phase) }; orbArr.forEach { it.update(dt, phase, arcP) }; ribbonArr.forEach { it.update(dt, phase, arcP) }
        if (phase > .25f && phase < .55f && sparks.size < 100 && Random.nextFloat() < .35f) repeat(3) { sparks.add(Spark()) }
        sparks.forEach { it.update(dt) }; sparks.removeAll { it.life <= 0 }
    }

    private fun render(c: Canvas) {
        c.drawColor(Color.BLACK)
        drawBg(c); drawNebula(c); drawStars(c); drawDust(c); drawOrbs(c); drawRibbons(c); drawWeb(c); drawCore(c); drawSparks(c); drawLogo(c); drawFlare(c); drawText(c); drawGrain(c); drawVignette(c)
    }

    private fun drawBg(c: Canvas) {
        val bg = RadialGradient(cx, cy, 0f, cx, cy, R * 1.6f, intArrayOf(0xFF060A1A.toInt(), 0xFF030512.toInt(), 0xFF01020A.toInt()), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP)
        nP.shader = bg; c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), nP); nP.shader = null
    }

    private fun drawNebula(c: Canvas) {
        c.saveLayer(null, null)
        arrayOf(floatArrayOf(-.28f, -.22f, .38f, 10f, 5f, 40f), floatArrayOf(.22f, .18f, .32f, 18f, 3f, 28f), floatArrayOf(.05f, -.28f, .28f, 5f, 12f, 35f)).forEachIndexed { i, v ->
            val nx = cx + v[0] * R + sin(time * .00012f + i * 2f).toFloat() * R * .03f
            val ny = cy + v[1] * R + cos(time * .0001f + i * 2f).toFloat() * R * .025f
            val nr = R * v[2] * (.8f + glowP * .2f)
            val na = cl(glowP * .1f + sin(time * .0008f + i * 1.5f).toFloat() * .02f, .0f, .12f)
            soft(c, nx, ny, nr, Color.argb((na * 255).toInt(), v[3].toInt(), v[4].toInt(), v[5].toInt()), true)
        }
        c.restore()
    }

    private fun drawStars(c: Canvas) {
        c.saveLayer(null, null)
        val dX = sin(time * .00007f).toFloat() * R * .015f; val dY = cos(time * .00005f).toFloat() * R * .012f
        val pxf = floatArrayOf(.3f, .6f, 1f)
        stars3.forEachIndexed { li, layer ->
            val px = dX * pxf[li]; val py = dY * pxf[li]
            layer.forEach { s ->
                val sx = ((s.x * width + px) % width + width) % width; val sy = ((s.y * height + py) % height + height) % height
                if (s.a < .01f) return@forEach
                val ai = (s.a * 255).toInt().coerceIn(0, 255)
                val r2 = lrI(170, 255, s.temp); val g2 = lrI(180, 255, s.temp); val b2 = lrI(210, 255, s.temp)
                nP.color = Color.argb(ai, r2, g2, b2); nP.shader = null; c.drawCircle(sx, sy, s.sz, nP)
                if (s.sz > 1f && s.a > .12f) soft(c, sx, sy, s.sz * 4, Color.argb((ai * .1f).toInt().coerceIn(0, 255), r2, g2, b2), true)
            }
        }
        c.restore()
    }

    private fun drawDust(c: Canvas) {
        c.saveLayer(null, null)
        dustArr.forEach { d -> if (d.a < .01f) return@forEach; val ai = (d.a * 255).toInt().coerceIn(0, 255)
            val tLen = hypot((d.x - d.px).toDouble(), (d.y - d.py).toDouble()).toFloat()
            if (tLen > .5f) { sP.color = Color.argb((ai * .25f).toInt().coerceIn(0, 255), d.cr, d.cg, d.cb); sP.strokeWidth = d.sz * .4f; sP.strokeCap = Paint.Cap.ROUND; c.drawLine(d.px, d.py, d.x, d.y, sP) }
            soft(c, d.x, d.y, d.sz, Color.argb(ai, d.cr, d.cg, d.cb), true)
        }
        c.restore()
    }

    private fun drawOrbs(c: Canvas) {
        c.saveLayer(null, null)
        orbArr.forEach { o -> if (o.a < .02f) return@forEach; val ai = (o.a * 255).toInt().coerceIn(0, 255)
            if (hypot((o.x - o.px).toDouble(), (o.y - o.py).toDouble()).toFloat() > 1f) soft(c, o.px, o.py, o.sz * .7f, Color.argb((ai * .2f).toInt().coerceIn(0, 255), o.cr, o.cg, o.cb), true)
            soft(c, o.x, o.y, o.sz, Color.argb(ai, o.cr, o.cg, o.cb), true)
        }
        c.restore()
    }

    private fun drawRibbons(c: Canvas) {
        c.saveLayer(null, null)
        ribbonArr.forEach { rb -> if (rb.a < .01f || rb.pts.size < 2) return@forEach
            for (i in 1 until rb.pts.size) { val t = i.toFloat() / rb.pts.size; val sa = rb.a * t; if (sa < .005f) continue
                sP.color = Color.argb((sa * 255).toInt().coerceIn(0, 255), lrI(rb.c1[0], rb.c2[0], t), lrI(rb.c1[1], rb.c2[1], t), lrI(rb.c1[2], rb.c2[2], t))
                sP.strokeWidth = rb.w * (.2f + .8f * t); sP.strokeCap = Paint.Cap.ROUND; c.drawLine(rb.pts[i - 1][0], rb.pts[i - 1][1], rb.pts[i][0], rb.pts[i][1], sP) }
            if (rb.pts.size > 1) { val tip = rb.pts.last(); soft(c, tip[0], tip[1], rb.w * 4, Color.argb((rb.a * 180).toInt().coerceIn(0, 255), rb.c2[0], rb.c2[1], rb.c2[2]), true) }
        }
        c.restore()
    }

    private fun drawWeb(c: Canvas) {
        if (webP < .01f) return; c.saveLayer(null, null)
        val tot = webEdges.size.toFloat()
        webEdges.forEachIndexed { i, (a, b) -> val eS = i / tot; val eE = (i + 1) / tot; if (webP < eS) return@forEachIndexed
            val ep = cl((webP - eS) / (eE - eS), 0f, 1f); val p1 = webPts[a]; val p2 = webPts[b]
            sP.color = Color.argb((ep * 38).toInt().coerceIn(0, 255), 0, 200, 255); sP.strokeWidth = .6f; sP.strokeCap = Paint.Cap.ROUND
            c.drawLine(p1[0], p1[1], lr(p1[0], p2[0], ep), lr(p1[1], p2[1], ep), sP) }
        if (webP > .1f) { val na = min((webP - .1f) / .4f, 1f) * .3f; webPts.forEach { nP.color = Color.argb((na * 255).toInt().coerceIn(0, 255), 0, 210, 255); c.drawCircle(it[0], it[1], 1.5f, nP) } }
        c.restore()
    }

    private fun drawCore(c: Canvas) {
        if (glowP < .01f) return; c.saveLayer(null, null)
        val cR = R * .04f * glowP * (1 + sin(time * .003f).toFloat() * .1f)
        arrayOf(floatArrayOf(cR * 14, .02f), floatArrayOf(cR * 7, .05f), floatArrayOf(cR * 3.5f, .13f), floatArrayOf(cR * 1.8f, .28f), floatArrayOf(cR, .45f)).forEach { soft(c, cx, cy, it[0], Color.argb((glowP * it[1] * 255).toInt().coerceIn(0, 255), 0, 185, 255), true) }
        nP.shader = RadialGradient(cx, cy, 0f, cx, cy, cR, Color.argb((glowP * 230).toInt().coerceIn(0, 255), 230, 248, 255), Color.argb((glowP * 128).toInt().coerceIn(0, 255), 0, 215, 255), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, cR, nP); nP.shader = null; c.restore()
    }

    private fun drawSparks(c: Canvas) {
        c.saveLayer(null, null)
        sparks.forEach { soft(c, it.x, it.y, it.sz * 2, Color.argb((it.life * 115).toInt().coerceIn(0, 255), it.cr, it.cg, it.cb), true)
            sP.color = Color.argb((it.life * 38).toInt().coerceIn(0, 255), it.cr, it.cg, it.cb); sP.strokeWidth = it.sz * .3f; sP.strokeCap = Paint.Cap.ROUND; c.drawLine(it.x, it.y, it.x - it.vx * .02f, it.y - it.vy * .02f, sP) }
        c.restore()
    }

    private fun drawLogo(c: Canvas) {
        if (logoAlpha < .01f) return
        val sz = R * .13f; val sc = .3f + .7f * logoAlpha; val rot = time * .0005f
        c.save(); c.translate(cx, cy); c.scale(sc, sc)
        val drawHex: (Float, Float) -> Unit = { size, off -> val path = Path(); for (i in 0..5) { val a = Math.PI.toFloat() / 3f * i - Math.PI.toFloat() / 6f + off; val x2 = cos(a) * size; val y2 = sin(a) * size; if (i == 0) path.moveTo(x2, y2) else path.lineTo(x2, y2) }; path.close(); c.drawPath(path, sP) }
        c.saveLayer(null, null)
        c.save(); c.rotate(-rot * 6f % TAU); sP.color = Color.argb((logoAlpha * 31).toInt().coerceIn(0, 255), 0, 170, 255); sP.strokeWidth = .5f; drawHex(sz * 1.3f, 0f); c.restore()
        c.save(); c.rotate(rot * 10f % TAU); sP.color = Color.argb((logoAlpha * 38).toInt().coerceIn(0, 255), 70, 40, 180); sP.strokeWidth = .5f; drawHex(sz * 1.12f, Math.PI.toFloat() / 6f); c.restore()
        c.restore()
        sP.color = Color.argb((logoAlpha * 217).toInt().coerceIn(0, 255), 0, 220, 255); sP.strokeWidth = 1.8f; sP.strokeJoin = Paint.Join.ROUND; drawHex(sz, 0f)
        val fp = Path(); for (i in 0..5) { val a = Math.PI.toFloat() / 3f * i - Math.PI.toFloat() / 6f; if (i == 0) fp.moveTo(cos(a) * sz, sin(a) * sz) else fp.lineTo(cos(a) * sz, sin(a) * sz) }; fp.close(); nP.color = Color.argb((logoAlpha * 10).toInt().coerceIn(0, 255), 0, 180, 255); c.drawPath(fp, nP)
        c.save(); c.rotate(rot * 18f % TAU); sP.color = Color.argb((logoAlpha * 153).toInt().coerceIn(0, 255), 120, 65, 255); sP.strokeWidth = 1.3f; drawHex(sz * .48f, 0f); c.restore()
        c.saveLayer(null, null)
        val cR2 = sz * .1f; arrayOf(floatArrayOf(cR2 * 5, .08f), floatArrayOf(cR2 * 2.5f, .25f), floatArrayOf(cR2 * 1.2f, .6f)).forEach { soft(c, 0f, 0f, it[0], Color.argb((logoAlpha * it[1] * 255).toInt().coerceIn(0, 255), 0, 200, 255), true) }
        nP.shader = RadialGradient(0f, 0f, 0f, 0f, 0f, cR2, Color.argb((logoAlpha * 230).toInt().coerceIn(0, 255), 210, 245, 255), Color.argb(0, 0, 180, 255), Shader.TileMode.CLAMP); c.drawCircle(0f, 0f, cR2, nP); nP.shader = null
        c.restore(); c.restore()
    }

    private fun drawFlare(c: Canvas) {
        if (logoAlpha < .4f) return; c.saveLayer(null, null)
        val fi = (logoAlpha - .4f) / .6f; val fl = R * .5f * fi
        for (i in -2..2) { sP.color = Color.argb((fi * .05f * (1 - abs(i) / 3f) * 255).toInt().coerceIn(0, 255), 0, 190, 255); sP.strokeWidth = 1f - abs(i) * .25f; c.drawLine(cx - fl, cy + i * 3f, cx + fl, cy + i * 3f, sP) }
        c.restore()
    }

    private fun drawText(c: Canvas) {
        if (textAlpha < .01f) return
        val tsz = R * .052f; val ty = cy + R * .24f
        tP.textSize = tsz; tP.textAlign = Paint.Align.CENTER
        val title = "NEXUS"; val tw = tP.measureText(title); val sx = cx - tw / 2f; val cw = tw / title.length
        for (i in title.indices) { val cp = cl((textAlpha - i * .06f) / .25f, 0f, 1f); if (cp <= 0) continue; tP.color = Color.argb((cp * 230).toInt().coerceIn(0, 255), 0, 225, 255); c.drawText(title[i].toString(), sx + i * cw + cw / 2f, ty, tP) }
        val sp2 = cl((textAlpha - .25f) / .35f, 0f, 1f)
        if (sp2 > .01f) { nP.textSize = tsz * .28f; nP.textAlign = Paint.Align.CENTER; nP.color = Color.argb((sp2 * 153).toInt().coerceIn(0, 255), 110, 65, 255); nP.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL); c.drawText("INITIALIZING NEXUS", cx, ty + tsz * 1.15f, nP); nP.typeface = null }
        val sp3 = cl((textAlpha - .3f) / .3f, 0f, 1f)
        if (sp3 > .01f) { val pw = R * .18f; val py = ty + tsz * 2f; sP.color = Color.argb((sp3 * 38).toInt().coerceIn(0, 255), 0, 170, 255); sP.strokeWidth = .8f; sP.strokeCap = Paint.Cap.ROUND; c.drawLine(cx - pw, py, cx + pw, py, sP); sP.color = Color.argb((sp3 * 166).toInt().coerceIn(0, 255), 0, 220, 255); sP.strokeWidth = 1f; c.drawLine(cx - pw, py, cx - pw + pw * 2 * phase, py, sP); soft(c, cx - pw + pw * 2 * phase, py, 4f, Color.argb((sp3 * 128).toInt().coerceIn(0, 255), 0, 230, 255), true) }
    }

    private fun drawGrain(c: Canvas) { for (i in 0 until 40) { nP.color = Color.argb(5, Random.nextInt(110, 146), Random.nextInt(110, 146), Random.nextInt(110, 146)); c.drawPoint(Random.nextFloat() * width, Random.nextFloat() * height, nP) } }

    private fun drawVignette(c: Canvas) {
        val vig = RadialGradient(cx, cy, R * .4f, cx, cy, R * 1.4f, intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(89, 0, 0, 0), Color.argb(237, 0, 0, 0)), floatArrayOf(0f, .5f, .82f, 1f), Shader.TileMode.CLAMP)
        nP.shader = vig; c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), nP); nP.shader = null
    }

    fun isAnimationComplete(): Boolean = phase >= 1f

    inner class Star(szBase: Float) {
        var x = Random.nextFloat(); var y = Random.nextFloat(); var sz = Random.nextFloat().pow(2) * szBase + .08f
        var a = 0f; var baseA = Random.nextFloat().pow(1.5f) * .5f + .01f; var tw = rand(.4f, 5f); var to = rand(0f, TAU); var temp = Random.nextFloat()
    }

    inner class Dust {
        var x = rand(0f, 2000f); var y = rand(0f, 2000f); var px = x; var py = y; var sz = rand(1.5f, 5f); var a = 0f
        var vx = (Random.nextFloat() - .5f) * 16f; var vy = (Random.nextFloat() - .5f) * 16f; var nOff = rand(0f, 100f)
        val cols = arrayOf(intArrayOf(0, 180, 255), intArrayOf(60, 100, 255), intArrayOf(0, 255, 180), intArrayOf(180, 140, 255), intArrayOf(140, 50, 255), intArrayOf(255, 160, 80))
        val c = cols[Random.nextInt(cols.size)]; val cr = c[0]; val cg = c[1]; val cb = c[2]
        fun update(dt: Float, phase: Float) {
            px = x; py = y; val n = sin(time * .002f + nOff).toFloat() * 10f
            x += (vx + n) * dt; y += (vy + cos(time * .0017f + nOff).toFloat() * 7f) * dt
            if (phase > .1f) { val pull = (phase - .1f) * .6f; val dx = cx - x; val dy = cy - y; val d = hypot(dx.toDouble(), dy.toDouble()).toFloat(); if (d > R * .05f) { x += dx / d * pull * 20f * dt; y += dy / d * pull * 20f * dt } }
            a = if (phase < .06f) phase / .06f * .3f else .3f * (1 - phase * .2f)
        }
    }

    inner class Orb {
        var angle = rand(0f, TAU); var distF = rand(.2f, 1.5f); var x = 0f; var y = 0f; var px = 0f; var py = 0f; var sz = rand(1.5f, 6f); var a = 0f
        var spd = rand(40f, 170f); var spiral = rand(-2.5f, 2.5f); var wobble = rand(0f, 2.5f); var wOff = rand(0f, TAU)
        val cols = arrayOf(intArrayOf(0, 220, 255), intArrayOf(50, 80, 255), intArrayOf(120, 50, 255), intArrayOf(0, 255, 200), intArrayOf(200, 120, 255))
        val c = cols[Random.nextInt(cols.size)]; val cr = c[0]; val cg = c[1]; val cb = c[2]
        fun update(dt: Float, phase: Float, arcP2: Float) {
            px = x; py = y; if (phase < .05f) { a = 0f; return }
            val dR = distF * R; angle += (spd / max(dR, 1f) + spiral) * dt; distF -= spd * dt * arcP2 * .6f / R; distF += sin(time * .005f + wOff).toFloat() * wobble * .003f
            if (distF < R * .03f / R) { distF = rand(.2f, 1.4f); angle = rand(0f, TAU); a = 0f; return }
            val d2 = distF * R; x = cx + cos(angle) * d2; y = cy + sin(angle) * d2
            a = min((phase - .05f) / .1f, 1f) * (.15f + (1 - distF / 1.5f) * .7f); sz = 1 + (1 - distF / 1.5f) * 4.5f
        }
    }

    inner class Ribbon {
        var baseA2 = rand(0f, TAU); var dist = rand(.25f, .9f); var spd = rand(25f, 80f); var spiralR = rand(1.2f, 3f)
        var segs = randI(18, 28); var w = rand(.8f, 2.2f); val swap = Random.nextBoolean()
        val c1 = if (swap) intArrayOf(120, 45, 255) else intArrayOf(0, 190, 255); val c2 = if (swap) intArrayOf(0, 190, 255) else intArrayOf(120, 45, 255)
        val pts = mutableListOf<FloatArray>(); var a = 0f; val off = rand(0f, 100f)
        fun update(dt: Float, phase: Float, arcP2: Float) {
            if (phase < .12f || phase > .65f) { a = 0f; return }; a = sin((phase - .12f) / .53f * Math.PI.toFloat()) * .55f
            val d3 = dist * R * (1 - arcP2 * .35f); baseA2 += spiralR * dt; pts.clear(); var a2 = baseA2; var d = d3
            for (i in 0 until segs) { pts.add(floatArrayOf(cx + cos(a2) * d, cy + sin(a2) * d)); a2 += .2f + sin(time * .003f + i * .4f + off).toFloat() * .06f; d -= d3 * .03f }
        }
    }

    inner class Spark {
        var x = cx + rand(-15f, 15f); var y = cy + rand(-15f, 15f); var vx = rand(-300f, 300f); var vy = rand(-300f, 300f)
        var sz = rand(.3f, 2.5f); var life = 1f; var decay = rand(1f, 2.5f); var cr = randI(0, 80); var cg = randI(180, 255); var cb = randI(200, 255)
        fun update(dt: Float) { x += vx * dt; y += vy * dt; vx *= .96f; vy *= .96f; life -= decay * dt }
    }
}
