package org.amnezia.awg.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.model.ObservableTunnel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Premium power control drawn entirely with Canvas.
 * No font glyphs, PNG buttons, Lottie files or third-party dependencies are used.
 */
class CifVpnPowerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class VisualState { OFF, CONNECTING, ON }

    private val density = resources.displayMetrics.density
    private val ringRect = RectF()
    private val shaderMatrix = Matrix()

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val depthPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val runnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val powerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var ringRadius = 0f
    private var coreRadius = 0f

    private var tunnelState: Tunnel.State = Tunnel.State.DOWN
    private var connectionStatus = ObservableTunnel.ConnectionStatus.DISCONNECTED
    private var visualState = VisualState.OFF
    private var runnerAngle = 0f
    private var runnerShader: SweepGradient? = null
    private var ringShader: SweepGradient? = null
    private var animator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateAccessibilityText()
    }

    fun setVpnState(state: Tunnel.State?) {
        val newState = state ?: Tunnel.State.DOWN
        if (tunnelState == newState) return
        tunnelState = newState
        updateVisualState()
    }

    fun setConnectionStatus(status: ObservableTunnel.ConnectionStatus?) {
        val newStatus = status ?: ObservableTunnel.ConnectionStatus.DISCONNECTED
        if (connectionStatus == newStatus) return
        connectionStatus = newStatus
        updateVisualState()
    }

    private fun updateVisualState() {
        val next = when {
            connectionStatus == ObservableTunnel.ConnectionStatus.CONNECTING -> VisualState.CONNECTING
            connectionStatus == ObservableTunnel.ConnectionStatus.CONNECTED || tunnelState == Tunnel.State.UP -> VisualState.ON
            else -> VisualState.OFF
        }
        if (next == visualState) {
            invalidate()
            return
        }
        visualState = next
        rebuildShaders()
        restartAnimatorIfNeeded()
        updateAccessibilityText()
        invalidate()
    }

    private fun updateAccessibilityText() {
        contentDescription = when (visualState) {
            VisualState.OFF -> "Cif VPN. VPN выключен. Нажмите, чтобы подключить"
            VisualState.CONNECTING -> "Cif VPN. Выполняется подключение"
            VisualState.ON -> "Cif VPN. Соединение защищено. Нажмите, чтобы отключить"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val preferred = dp(236f).toInt()
        val width = resolveSize(preferred, widthMeasureSpec)
        val height = resolveSize(preferred, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f - dp(1f)
        outerRadius = min(w, h) * 0.46f
        ringRadius = outerRadius * 0.84f
        coreRadius = outerRadius * 0.67f
        ringRect.set(
            centerX - ringRadius,
            centerY - ringRadius,
            centerX + ringRadius,
            centerY + ringRadius
        )
        ringPaint.strokeWidth = dp(8f)
        runnerPaint.strokeWidth = dp(10f)
        highlightPaint.strokeWidth = dp(2f)
        powerPaint.strokeWidth = dp(7f)
        rebuildShaders()
    }

    private fun rebuildShaders() {
        if (width == 0 || height == 0) return

        val ringColors = when (visualState) {
            VisualState.OFF -> intArrayOf(
                Color.rgb(53, 67, 91),
                Color.rgb(23, 34, 54),
                Color.rgb(76, 89, 113),
                Color.rgb(35, 46, 67),
                Color.rgb(53, 67, 91)
            )
            VisualState.CONNECTING -> intArrayOf(
                Color.rgb(34, 74, 109),
                Color.rgb(49, 214, 255),
                Color.rgb(115, 91, 255),
                Color.rgb(34, 74, 109),
                Color.rgb(34, 74, 109)
            )
            VisualState.ON -> intArrayOf(
                Color.rgb(31, 112, 151),
                Color.rgb(55, 226, 196),
                Color.rgb(54, 157, 255),
                Color.rgb(31, 112, 151),
                Color.rgb(31, 112, 151)
            )
        }
        ringShader = SweepGradient(centerX, centerY, ringColors, null)

        val transparent = Color.TRANSPARENT
        val runnerColors = when (visualState) {
            VisualState.OFF -> intArrayOf(transparent, transparent, transparent, transparent, transparent)
            VisualState.CONNECTING -> intArrayOf(
                transparent,
                transparent,
                Color.argb(40, 65, 220, 255),
                Color.WHITE,
                transparent
            )
            VisualState.ON -> intArrayOf(
                transparent,
                transparent,
                Color.argb(35, 68, 234, 190),
                Color.rgb(214, 255, 246),
                transparent
            )
        }
        runnerShader = SweepGradient(
            centerX,
            centerY,
            runnerColors,
            floatArrayOf(0f, 0.68f, 0.79f, 0.88f, 1f)
        )
    }

    private fun restartAnimatorIfNeeded() {
        animator?.cancel()
        animator = null
        if (visualState == VisualState.OFF || !isAttachedToWindow) {
            runnerAngle = 0f
            return
        }
        val duration = if (visualState == VisualState.CONNECTING) 1250L else 4200L
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                runnerAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        restartAnimatorIfNeeded()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (outerRadius <= 0f) return

        val pressedScale = if (isPressed) 0.965f else 1f
        canvas.save()
        canvas.scale(pressedScale, pressedScale, centerX, centerY)

        val pulse = if (visualState == VisualState.OFF) 0f
        else (0.5f + 0.5f * sin(runnerAngle * PI.toFloat() / 180f))

        drawAura(canvas, pulse)
        drawThreeDimensionalBody(canvas)
        drawAnimatedRing(canvas)
        drawPowerSymbol(canvas)

        canvas.restore()
    }

    private fun drawAura(canvas: Canvas, pulse: Float) {
        val auraColor = when (visualState) {
            VisualState.OFF -> Color.rgb(53, 75, 112)
            VisualState.CONNECTING -> Color.rgb(56, 197, 255)
            VisualState.ON -> Color.rgb(56, 229, 192)
        }
        val alpha = when (visualState) {
            VisualState.OFF -> 22
            VisualState.CONNECTING -> (38 + pulse * 32).toInt()
            VisualState.ON -> (30 + pulse * 26).toInt()
        }
        auraPaint.shader = RadialGradient(
            centerX,
            centerY,
            outerRadius * 1.28f,
            intArrayOf(Color.argb(alpha, Color.red(auraColor), Color.green(auraColor), Color.blue(auraColor)), Color.TRANSPARENT),
            floatArrayOf(0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, outerRadius * 1.28f, auraPaint)
        auraPaint.shader = null
    }

    private fun drawThreeDimensionalBody(canvas: Canvas) {
        depthPaint.color = Color.argb(205, 0, 3, 10)
        canvas.drawCircle(centerX, centerY + dp(10f), outerRadius, depthPaint)

        bodyPaint.shader = LinearGradient(
            centerX - outerRadius,
            centerY - outerRadius,
            centerX + outerRadius,
            centerY + outerRadius,
            intArrayOf(
                Color.rgb(35, 49, 74),
                Color.rgb(12, 20, 35),
                Color.rgb(4, 9, 18)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, outerRadius, bodyPaint)
        bodyPaint.shader = null

        innerPaint.shader = RadialGradient(
            centerX - coreRadius * 0.28f,
            centerY - coreRadius * 0.36f,
            coreRadius * 1.5f,
            when (visualState) {
                VisualState.OFF -> intArrayOf(Color.rgb(35, 48, 70), Color.rgb(9, 16, 29), Color.rgb(4, 8, 16))
                VisualState.CONNECTING -> intArrayOf(Color.rgb(35, 71, 104), Color.rgb(10, 24, 42), Color.rgb(4, 9, 18))
                VisualState.ON -> intArrayOf(Color.rgb(26, 90, 86), Color.rgb(9, 31, 42), Color.rgb(4, 10, 18))
            },
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, coreRadius, innerPaint)
        innerPaint.shader = null

        highlightPaint.color = Color.argb(95, 255, 255, 255)
        canvas.drawArc(
            centerX - coreRadius + dp(5f),
            centerY - coreRadius + dp(5f),
            centerX + coreRadius - dp(5f),
            centerY + coreRadius - dp(5f),
            202f,
            116f,
            false,
            highlightPaint
        )
    }

    private fun drawAnimatedRing(canvas: Canvas) {
        ringPaint.shader = ringShader
        canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)
        ringPaint.shader = null

        if (visualState == VisualState.OFF) return

        runnerShader?.let { shader ->
            shaderMatrix.setRotate(runnerAngle, centerX, centerY)
            shader.setLocalMatrix(shaderMatrix)
            runnerPaint.shader = shader
            canvas.drawCircle(centerX, centerY, ringRadius, runnerPaint)
            runnerPaint.shader = null
        }

        val angle = Math.toRadians((runnerAngle + 45f).toDouble())
        val dotX = centerX + cos(angle).toFloat() * ringRadius
        val dotY = centerY + sin(angle).toFloat() * ringRadius
        runnerPaint.shader = null
        runnerPaint.style = Paint.Style.FILL
        runnerPaint.color = if (visualState == VisualState.ON) Color.rgb(222, 255, 247) else Color.WHITE
        canvas.drawCircle(dotX, dotY, dp(4.6f), runnerPaint)
        runnerPaint.style = Paint.Style.STROKE
    }

    private fun drawPowerSymbol(canvas: Canvas) {
        powerPaint.color = when (visualState) {
            VisualState.OFF -> Color.rgb(166, 179, 202)
            VisualState.CONNECTING -> Color.rgb(229, 247, 255)
            VisualState.ON -> Color.rgb(218, 255, 245)
        }
        val iconRadius = coreRadius * 0.37f
        val iconRect = RectF(
            centerX - iconRadius,
            centerY - iconRadius,
            centerX + iconRadius,
            centerY + iconRadius
        )
        canvas.drawArc(iconRect, -42f, 264f, false, powerPaint)
        canvas.drawLine(
            centerX,
            centerY - iconRadius * 1.28f,
            centerX,
            centerY - iconRadius * 0.13f,
            powerPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val inside = event.x in 0f..width.toFloat() && event.y in 0f..height.toFloat()
                if (isPressed != inside) {
                    isPressed = inside
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val shouldClick = isPressed
                isPressed = false
                invalidate()
                if (shouldClick) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dp(value: Float): Float = value * density
}
