package org.amnezia.awg.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

/**
 * Full-screen Cif VPN dashboard drawn with Canvas.
 * No text glyphs are used for the power / action icons, so they cannot turn into square boxes.
 */
class CifVpnDashboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class UiState { OFF, CONNECTING, ON }

    var onPowerClick: (() -> Unit)? = null
    var onMenuClick: (() -> Unit)? = null
    var onHeaderSettingsClick: (() -> Unit)? = null
    var onWhitelistClick: (() -> Unit)? = null
    var onImportClick: (() -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private fun dp(v: Float) = v * density
    private fun sp(v: Float) = v * scaledDensity

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var state = UiState.OFF
    private var profileName = "Cif VPN"
    private var speedMbps = 0.0
    private var sessionSeconds = 0L
    private var whitelistCount = 0
    private var accentColor = Color.rgb(33, 229, 197)

    private val headerMenuRect = RectF()
    private val headerSettingsRect = RectF()
    private val powerRect = RectF()
    private val whitelistRect = RectF()
    private val importRect = RectF()
    private val settingsRect = RectF()

    private var ringAngle = 0f
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            ringAngle = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        isClickable = true
        isFocusable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun update(
        uiState: UiState,
        profile: String,
        speed: Double,
        elapsedSeconds: Long,
        bypassCount: Int,
        accent: Int
    ) {
        state = uiState
        profileName = profile
        speedMbps = speed.coerceAtLeast(0.0)
        sessionSeconds = elapsedSeconds.coerceAtLeast(0)
        whitelistCount = bypassCount.coerceAtLeast(0)
        accentColor = accent

        if (state == UiState.OFF) {
            animator.cancel()
            ringAngle = 0f
        } else if (!animator.isStarted) {
            animator.duration = if (state == UiState.CONNECTING) 1000L else 4200L
            animator.start()
        } else {
            animator.duration = if (state == UiState.CONNECTING) 1000L else 4200L
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec).takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val circleSize = min(measuredWidth.toFloat() - dp(76f), dp(310f)).coerceAtLeast(dp(220f))
        val desiredHeight = (dp(20f + 58f + 105f + 20f + 94f + 126f + 80f + 30f) + circleSize).toInt()
        val width = resolveSize(measuredWidth, widthMeasureSpec)
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> maxOf(MeasureSpec.getSize(heightMeasureSpec), desiredHeight)
            else -> desiredHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        drawBackground(canvas, w, h)

        val side = dp(20f)
        var y = dp(20f)

        drawHeader(canvas, side, y, w - side)
        y += dp(58f)

        drawStatusCard(canvas, side, y, w - side, y + dp(84f))
        y += dp(105f)

        val circleSize = min(w - dp(76f), dp(310f))
        val cx = w / 2f
        val cy = y + circleSize / 2f
        drawPower(canvas, cx, cy, circleSize / 2f)
        powerRect.set(cx - circleSize / 2f, cy - circleSize / 2f, cx + circleSize / 2f, cy + circleSize / 2f)
        y += circleSize + dp(20f)

        drawServerCard(canvas, side, y, w - side, y + dp(76f))
        y += dp(94f)

        val gap = dp(10f)
        val cardWidth = (w - side * 2 - gap * 2) / 3f
        whitelistRect.set(side, y, side + cardWidth, y + dp(108f))
        importRect.set(side + cardWidth + gap, y, side + cardWidth * 2 + gap, y + dp(108f))
        settingsRect.set(side + cardWidth * 2 + gap * 2, y, w - side, y + dp(108f))
        drawActionCard(canvas, whitelistRect, ActionIcon.SHIELD, "Белый список", "$whitelistCount приложений")
        drawActionCard(canvas, importRect, ActionIcon.IMPORT, "Импорт", "Конфигурация")
        drawActionCard(canvas, settingsRect, ActionIcon.SLIDERS, "Настройки", "Цвет интерфейса")
        y += dp(126f)

        drawSpeedCard(canvas, side, y, w - side, min(y + dp(80f), h - dp(12f)))
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        paint.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(4, 17, 31), Color.rgb(2, 10, 20), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = RadialGradient(w * .5f, h * .25f, w * .72f, withAlpha(accentColor, 40), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun drawHeader(canvas: Canvas, left: Float, top: Float, right: Float) {
        headerMenuRect.set(left - dp(10f), top - dp(8f), left + dp(46f), top + dp(50f))
        headerSettingsRect.set(right - dp(58f), top - dp(8f), right + dp(8f), top + dp(52f))

        paint.color = Color.rgb(139, 159, 185)
        paint.strokeWidth = dp(2.3f)
        for (i in 0..2) canvas.drawLine(left, top + dp(10f + i * 8f), left + dp(25f), top + dp(10f + i * 8f), paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = sp(29f)
        paint.color = Color.WHITE
        canvas.drawText("Cif ", width / 2f - dp(18f), top + dp(31f), paint)
        paint.color = accentColor
        canvas.drawText("VPN", width / 2f + dp(34f), top + dp(31f), paint)

        drawSlidersIcon(canvas, right - dp(20f), top + dp(22f), dp(22f), Color.rgb(137, 158, 187))
    }

    private fun drawStatusCard(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val rect = RectF(left, top, right, bottom)
        drawGlassCard(canvas, rect, dp(23f))
        drawShieldIcon(canvas, left + dp(40f), (top + bottom) / 2f, dp(22f), if (state == UiState.OFF) Color.rgb(100, 119, 145) else accentColor)

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = sp(17f)
        paint.color = Color.WHITE
        val title = when (state) {
            UiState.ON -> "Защита включена"
            UiState.CONNECTING -> "Подключение…"
            UiState.OFF -> "Защита выключена"
        }
        canvas.drawText(title, left + dp(74f), top + dp(35f), paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = sp(13.5f)
        paint.color = Color.rgb(154, 171, 195)
        val subtitle = when (state) {
            UiState.ON -> "Ваше соединение защищено"
            UiState.CONNECTING -> "Создаём защищённый туннель"
            UiState.OFF -> "Нажмите кнопку для подключения"
        }
        canvas.drawText(subtitle, left + dp(74f), top + dp(59f), paint)
    }

    private fun drawPower(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val active = state != UiState.OFF
        val glowColor = if (active) accentColor else Color.rgb(65, 85, 110)

        if (active) {
            paint.color = withAlpha(glowColor, if (state == UiState.CONNECTING) 80 else 58)
            paint.setShadowLayer(dp(34f), 0f, 0f, withAlpha(glowColor, 170))
            canvas.drawCircle(cx, cy, radius * .84f, paint)
            paint.clearShadowLayer()
        }

        paint.shader = RadialGradient(
            cx - radius * .28f, cy - radius * .35f, radius * 1.25f,
            intArrayOf(Color.rgb(36, 69, 91), Color.rgb(10, 31, 48), Color.rgb(3, 15, 27)),
            floatArrayOf(0f, .58f, 1f), Shader.TileMode.CLAMP
        )
        paint.setShadowLayer(dp(16f), 0f, dp(11f), Color.BLACK)
        canvas.drawCircle(cx, cy, radius * .82f, paint)
        paint.clearShadowLayer()
        paint.shader = null

        stroke.strokeWidth = dp(2f)
        stroke.color = Color.rgb(56, 91, 115)
        canvas.drawCircle(cx, cy, radius * .72f, stroke)

        stroke.strokeWidth = dp(13f)
        stroke.color = Color.rgb(18, 43, 60)
        canvas.drawCircle(cx, cy, radius * .88f, stroke)

        if (active) {
            val colors = intArrayOf(
                Color.TRANSPARENT, Color.TRANSPARENT,
                withAlpha(accentColor, 70), accentColor, Color.WHITE,
                accentColor, withAlpha(accentColor, 40), Color.TRANSPARENT
            )
            val positions = floatArrayOf(0f, .56f, .68f, .78f, .815f, .85f, .92f, 1f)
            val gradient = SweepGradient(cx, cy, colors, positions)
            val matrix = Matrix().apply { setRotate(ringAngle - 90f, cx, cy) }
            gradient.setLocalMatrix(matrix)
            stroke.shader = gradient
            stroke.strokeWidth = dp(12f)
            stroke.setShadowLayer(dp(10f), 0f, 0f, withAlpha(accentColor, 190))
            canvas.drawCircle(cx, cy, radius * .88f, stroke)
            stroke.clearShadowLayer()
            stroke.shader = null
        }

        drawPowerIcon(canvas, cx, cy - dp(18f), radius * .25f, if (active) Color.rgb(196, 231, 236) else Color.rgb(124, 144, 166))

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = sp(15f)
        paint.color = if (active) accentColor else Color.rgb(142, 158, 180)
        canvas.drawText(
            when (state) {
                UiState.ON -> "ПОДКЛЮЧЕНО"
                UiState.CONNECTING -> "ПОДКЛЮЧЕНИЕ"
                UiState.OFF -> "ОТКЛЮЧЕНО"
            },
            cx, cy + radius * .34f, paint
        )
        paint.typeface = Typeface.DEFAULT
        paint.textSize = sp(14f)
        paint.color = Color.rgb(205, 215, 229)
        canvas.drawText(formatDuration(sessionSeconds), cx, cy + radius * .48f, paint)
    }

    private fun drawServerCard(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val rect = RectF(left, top, right, bottom)
        drawGlassCard(canvas, rect, dp(20f))
        paint.color = withAlpha(accentColor, 35)
        canvas.drawRoundRect(RectF(left + dp(15f), top + dp(15f), left + dp(59f), bottom - dp(15f)), dp(13f), dp(13f), paint)
        drawSwirlIcon(canvas, left + dp(37f), (top + bottom) / 2f, dp(15f), accentColor)

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT
        paint.textSize = sp(12.5f)
        paint.color = Color.rgb(135, 153, 178)
        canvas.drawText("Текущее подключение", left + dp(75f), top + dp(29f), paint)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = sp(16f)
        paint.color = Color.WHITE
        canvas.drawText(profileName, left + dp(75f), top + dp(53f), paint)

        drawSignalIcon(canvas, right - dp(32f), (top + bottom) / 2f, accentColor)
    }

    private fun drawActionCard(canvas: Canvas, rect: RectF, icon: ActionIcon, title: String, subtitle: String) {
        drawGlassCard(canvas, rect, dp(19f))
        val cx = rect.centerX()
        val iconY = rect.top + dp(31f)
        when (icon) {
            ActionIcon.SHIELD -> drawShieldIcon(canvas, cx, iconY, dp(16f), accentColor)
            ActionIcon.IMPORT -> drawImportIcon(canvas, cx, iconY, dp(17f), Color.rgb(150, 170, 198))
            ActionIcon.SLIDERS -> drawSlidersIcon(canvas, cx, iconY, dp(18f), Color.rgb(150, 170, 198))
        }
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = sp(12.5f)
        paint.color = Color.WHITE
        canvas.drawText(title, cx, rect.top + dp(69f), paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = sp(10.5f)
        paint.color = Color.rgb(126, 144, 170)
        canvas.drawText(subtitle, cx, rect.top + dp(90f), paint)
    }

    private fun drawSpeedCard(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        if (bottom <= top) return
        val rect = RectF(left, top, right, bottom)
        drawGlassCard(canvas, rect, dp(19f))
        drawRocketIcon(canvas, left + dp(30f), top + dp(29f), Color.rgb(142, 164, 195))

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT
        paint.textSize = sp(13f)
        paint.color = Color.rgb(145, 162, 187)
        canvas.drawText("Скорость соединения", left + dp(54f), top + dp(31f), paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = sp(15f)
        paint.color = accentColor
        canvas.drawText(String.format("%.1f Mbps", speedMbps), right - dp(18f), top + dp(31f), paint)

        val barLeft = left + dp(54f)
        val barRight = right - dp(18f)
        val barY = bottom - dp(17f)
        stroke.strokeWidth = dp(3f)
        stroke.color = Color.rgb(28, 48, 66)
        canvas.drawLine(barLeft, barY, barRight, barY, stroke)
        stroke.color = accentColor
        val progress = (speedMbps / 100.0).coerceIn(0.04, 1.0).toFloat()
        canvas.drawLine(barLeft, barY, barLeft + (barRight - barLeft) * progress, barY, stroke)
    }

    private fun drawGlassCard(canvas: Canvas, rect: RectF, radius: Float) {
        paint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
            Color.rgb(12, 31, 48), Color.rgb(7, 22, 36), Shader.TileMode.CLAMP)
        paint.color = Color.WHITE
        paint.setShadowLayer(dp(9f), 0f, dp(4f), Color.argb(100, 0, 0, 0))
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.clearShadowLayer()
        paint.shader = null
        stroke.strokeWidth = dp(1f)
        stroke.color = Color.rgb(28, 55, 75)
        canvas.drawRoundRect(rect, radius, radius, stroke)
    }

    private fun drawPowerIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = dp(6f)
        stroke.strokeCap = Paint.Cap.ROUND
        val oval = RectF(cx - r, cy - r * .72f, cx + r, cy + r * 1.25f)
        canvas.drawArc(oval, -48f, 276f, false, stroke)
        canvas.drawLine(cx, cy - r * 1.25f, cx, cy - r * .28f, stroke)
    }

    private fun drawShieldIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = Path()
        p.moveTo(cx, cy - r)
        p.lineTo(cx + r * .78f, cy - r * .62f)
        p.lineTo(cx + r * .64f, cy + r * .38f)
        p.quadTo(cx, cy + r, cx - r * .64f, cy + r * .38f)
        p.lineTo(cx - r * .78f, cy - r * .62f)
        p.close()
        paint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, lighten(color, 38), darken(color, 25), Shader.TileMode.CLAMP)
        canvas.drawPath(p, paint)
        paint.shader = null
        stroke.color = withAlpha(Color.WHITE, 150)
        stroke.strokeWidth = dp(1.2f)
        canvas.drawPath(p, stroke)
        if (state != UiState.OFF) {
            stroke.color = Color.rgb(3, 70, 69)
            stroke.strokeWidth = dp(2.2f)
            canvas.drawLine(cx - r * .25f, cy, cx - r * .02f, cy + r * .22f, stroke)
            canvas.drawLine(cx - r * .02f, cy + r * .22f, cx + r * .35f, cy - r * .25f, stroke)
        }
    }

    private fun drawImportIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = dp(2.6f)
        canvas.drawLine(cx, cy - r, cx, cy + r * .35f, stroke)
        canvas.drawLine(cx, cy + r * .35f, cx - r * .36f, cy - r * .05f, stroke)
        canvas.drawLine(cx, cy + r * .35f, cx + r * .36f, cy - r * .05f, stroke)
        val box = RectF(cx - r * .8f, cy + r * .28f, cx + r * .8f, cy + r)
        canvas.drawArc(box, 0f, 180f, false, stroke)
    }

    private fun drawSlidersIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = dp(2.1f)
        for (i in -1..1) {
            val yy = cy + i * r * .55f
            canvas.drawLine(cx - r, yy, cx + r, yy, stroke)
            val knobX = when (i) { -1 -> cx - r * .28f; 0 -> cx + r * .38f; else -> cx - r * .08f }
            paint.color = color
            canvas.drawCircle(knobX, yy, dp(3.2f), paint)
        }
    }

    private fun drawSignalIcon(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        paint.color = color
        for (i in 0..3) {
            val height = dp(6f + i * 5f)
            canvas.drawRoundRect(RectF(cx + i * dp(7f), cy + dp(13f) - height, cx + i * dp(7f) + dp(3f), cy + dp(13f)), dp(2f), dp(2f), paint)
        }
    }

    private fun drawRocketIcon(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        paint.color = color
        val p = Path()
        p.moveTo(cx - dp(7f), cy + dp(7f))
        p.quadTo(cx - dp(3f), cy - dp(10f), cx + dp(10f), cy - dp(12f))
        p.quadTo(cx + dp(10f), cy + dp(2f), cx - dp(7f), cy + dp(7f))
        p.close()
        canvas.drawPath(p, paint)
        paint.color = Color.rgb(8, 25, 39)
        canvas.drawCircle(cx + dp(3f), cy - dp(5f), dp(2.7f), paint)
    }

    private fun drawSwirlIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        stroke.strokeWidth = dp(4f)
        stroke.color = color
        canvas.drawArc(RectF(cx-r, cy-r, cx+r, cy+r), -40f, 285f, false, stroke)
        stroke.color = lighten(color, 40)
        canvas.drawArc(RectF(cx-r*.68f, cy-r*.68f, cx+r*.68f, cy+r*.68f), 130f, 250f, false, stroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(.985f).scaleY(.985f).setDuration(70L).start()
                return true
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                val x = event.x
                val y = event.y
                when {
                    headerMenuRect.contains(x, y) -> onMenuClick?.invoke()
                    headerSettingsRect.contains(x, y) -> onHeaderSettingsClick?.invoke()
                    powerRect.contains(x, y) -> onPowerClick?.invoke()
                    whitelistRect.contains(x, y) -> onWhitelistClick?.invoke()
                    importRect.contains(x, y) -> onImportClick?.invoke()
                    settingsRect.contains(x, y) -> onSettingsClick?.invoke()
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha.coerceIn(0,255), Color.red(color), Color.green(color), Color.blue(color))
    private fun lighten(color: Int, amount: Int) = Color.rgb((Color.red(color)+amount).coerceAtMost(255), (Color.green(color)+amount).coerceAtMost(255), (Color.blue(color)+amount).coerceAtMost(255))
    private fun darken(color: Int, amount: Int) = Color.rgb((Color.red(color)-amount).coerceAtLeast(0), (Color.green(color)-amount).coerceAtLeast(0), (Color.blue(color)-amount).coerceAtLeast(0))

    private enum class ActionIcon { SHIELD, IMPORT, SLIDERS }
}
