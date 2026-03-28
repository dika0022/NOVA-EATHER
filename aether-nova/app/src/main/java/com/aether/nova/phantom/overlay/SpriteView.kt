package com.aether.nova.phantom.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

open class SpriteView(
    context: Context,
    protected val bodyColor: Int,
    protected val bodyColorDark: Int,
    protected val bodyColorLight: Int,
    protected val robotName: String
) : View(context) {

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val dp: Float = resources.displayMetrics.density

    // Sprite state
    var currentAnimation = SpriteAnimation.IDLE
        protected set

    // Position
    var spriteX = 0f
    var spriteY = 0f

    // Animation state values
    protected var floatOffset = 0f
    protected var blinkProgress = 0f
    protected var isBlinking = false
    protected var antennaWiggle = 0f
    protected var walkCycle = 0f
    protected var jumpBounce = 0f
    var armRaise = 0f
    protected var breathePhase = 0f
    protected var zzzPhase = 0f
    protected var steamPhase = 0f
    protected var tearProgress = 0f
    protected var questionPhase = 0f
    protected var heartPhase = 0f
    protected var spinAngle = 0f
    protected var turnAngle = 0f
    protected var bodyScale = 1f
    protected var bodyTilt = 0f
    protected var eyeMode = 0 // 0=normal, 1=happy, 2=closed, 3=sad, 4=angry, 5=wide, 6=half
    protected var legPhase = 0f
    protected var armCrossed = 0f
    protected var rocketPhase = 0f
    protected var gearPhase = 0f
    protected var thermometerPhase = 0f
    protected var facingBack = false
    protected var confusedHeadTilt = 0f
    protected var scratchHand = 0f

    // Size
    protected val spriteSize = 90f * dp
    protected val halfSize = spriteSize / 2f

    // Animators
    private val animators = mutableListOf<ValueAnimator>()

    // Idle animation
    private var idleAnimator: ValueAnimator? = null
    private var blinkAnimator: ValueAnimator? = null
    private var antennaAnimator: ValueAnimator? = null

    // Gesture detection
    private var onDragListener: ((Float, Float) -> Unit)? = null
    private var onFlingListener: ((Float, Float) -> Unit)? = null
    private var onTapListener: (() -> Unit)? = null
    private var onDoubleTapListener: (() -> Unit)? = null
    private var onLongPressListener: (() -> Unit)? = null

    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var viewStartX = 0f
    private var viewStartY = 0f

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        startIdleAnimation()
        startBlinkAnimation()
    }

    fun setOnDragListener(listener: (Float, Float) -> Unit) {
        onDragListener = listener
    }

    fun setOnFlingListener(listener: (Float, Float) -> Unit) {
        onFlingListener = listener
    }

    fun setOnTapListener(listener: () -> Unit) {
        onTapListener = listener
    }

    fun setOnDoubleTapListener(listener: () -> Unit) {
        onDoubleTapListener = listener
    }

    fun setOnLongPressListener(listener: () -> Unit) {
        onLongPressListener = listener
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragStartY = event.rawY
                viewStartX = spriteX
                viewStartY = spriteY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - dragStartX
                val dy = event.rawY - dragStartY
                if (abs(dx) > 10 * dp || abs(dy) > 10 * dp) {
                    isDragging = true
                }
                if (isDragging) {
                    spriteX = viewStartX + dx
                    spriteY = viewStartY + dy
                    onDragListener?.invoke(spriteX, spriteY)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    // Bounce effect on release
                    playBounceRelease()
                } else {
                    onTapListener?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun playBounceRelease() {
        val animator = ValueAnimator.ofFloat(1.15f, 1f)
        animator.duration = 300
        animator.interpolator = BounceInterpolator()
        animator.addUpdateListener {
            bodyScale = it.animatedValue as Float
            invalidate()
        }
        safeStartAnimator(animator)
    }

    fun handleDoubleTap() {
        onDoubleTapListener?.invoke()
    }

    fun handleLongPress() {
        onLongPressListener?.invoke()
    }

    fun handleFling(velocityX: Float, velocityY: Float) {
        onFlingListener?.invoke(velocityX, velocityY)
    }

    // ========== ANIMATION CONTROL ==========

    fun setAnimation(anim: SpriteAnimation) {
        if (currentAnimation == anim) return
        cancelAllAnimations()
        currentAnimation = anim
        resetAnimationState()

        when (anim) {
            SpriteAnimation.IDLE -> startIdleAnimation()
            SpriteAnimation.HAPPY -> startHappyAnimation()
            SpriteAnimation.WALKING -> startWalkingAnimation()
            SpriteAnimation.FLYING -> startFlyingAnimation()
            SpriteAnimation.SLEEPING -> startSleepAnimation()
            SpriteAnimation.ANGRY -> startAngryAnimation()
            SpriteAnimation.SAD -> startSadAnimation()
            SpriteAnimation.THINKING -> startThinkingAnimation()
            SpriteAnimation.CONFUSED -> startConfusedAnimation()
            SpriteAnimation.TIRED -> startTiredAnimation()
            SpriteAnimation.TOUCHED -> startTouchedAnimation()
            SpriteAnimation.TURNING_BACK -> startTurningAnimation()
            SpriteAnimation.SICK -> startSickAnimation()
            SpriteAnimation.SPIN -> startSpinAnimation()
            SpriteAnimation.SHAKE -> startShakeAnimation()
            SpriteAnimation.WAKE_UP -> startWakeUpAnimation()
        }
    }

    private fun resetAnimationState() {
        floatOffset = 0f
        jumpBounce = 0f
        armRaise = 0f
        walkCycle = 0f
        bodyScale = 1f
        bodyTilt = 0f
        eyeMode = 0
        zzzPhase = 0f
        steamPhase = 0f
        tearProgress = 0f
        questionPhase = 0f
        heartPhase = 0f
        spinAngle = 0f
        turnAngle = 0f
        legPhase = 0f
        armCrossed = 0f
        rocketPhase = 0f
        gearPhase = 0f
        thermometerPhase = 0f
        facingBack = false
        confusedHeadTilt = 0f
        scratchHand = 0f
    }

    // ========== IDLE ==========

    private fun startIdleAnimation() {
        eyeMode = 0
        idleAnimator = createLoopAnimator(0f, 1f, 2500L) { value ->
            floatOffset = sin(value * Math.PI * 2).toFloat() * 5f * dp
            breathePhase = value
            invalidate()
        }
        antennaAnimator = createLoopAnimator(0f, 1f, 3000L) { value ->
            antennaWiggle = sin(value * Math.PI * 2).toFloat() * 3f
            invalidate()
        }
        startBlinkAnimation()
    }

    private fun startBlinkAnimation() {
        blinkAnimator?.cancel()
        val delay = (3000L..5000L).random()
        blinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            startDelay = delay
            addUpdateListener {
                blinkProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    blinkProgress = 0f
                    invalidate()
                    startBlinkAnimation()
                }
            })
            safeStartAnimator(this)
        }
    }

    // ========== HAPPY ==========

    private fun startHappyAnimation() {
        eyeMode = 1
        val jumpAnim = ValueAnimator.ofFloat(0f, 1f, 0f, 1f, 0f)
        jumpAnim.duration = 1200
        jumpAnim.interpolator = AccelerateDecelerateInterpolator()
        jumpAnim.addUpdateListener {
            val v = it.animatedValue as Float
            jumpBounce = v * 20f * dp
            armRaise = v
            bodyScale = 1f + v * 0.05f
            invalidate()
        }
        jumpAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                setAnimation(SpriteAnimation.IDLE)
            }
        })
        safeStartAnimator(jumpAnim)
    }

    // ========== WALKING ==========

    private fun startWalkingAnimation() {
        val walkAnim = createLoopAnimator(0f, 1f, 600L) { value ->
            walkCycle = value
            floatOffset = abs(sin(value * Math.PI * 2)).toFloat() * 4f * dp
            invalidate()
        }
    }

    // ========== FLYING ==========

    private fun startFlyingAnimation() {
        val flyAnim = createLoopAnimator(0f, 1f, 400L) { value ->
            rocketPhase = value
            floatOffset = sin(value * Math.PI * 4).toFloat() * 3f * dp
            invalidate()
        }
    }

    // ========== SLEEPING ==========

    private fun startSleepAnimation() {
        eyeMode = 2
        val sleepAnim = createLoopAnimator(0f, 1f, 4000L) { value ->
            breathePhase = value
            floatOffset = sin(value * Math.PI * 2).toFloat() * 2f * dp
            bodyTilt = 15f
            zzzPhase = value
            invalidate()
        }
    }

    // ========== ANGRY ==========

    private fun startAngryAnimation() {
        eyeMode = 4
        armCrossed = 1f
        val angryAnim = createLoopAnimator(0f, 1f, 400L) { value ->
            steamPhase = value
            val shake = sin(value * Math.PI * 6).toFloat() * 3f * dp
            floatOffset = shake
            invalidate()
        }
    }

    // ========== SAD ==========

    private fun startSadAnimation() {
        eyeMode = 3
        bodyScale = 0.95f
        val sadAnim = createLoopAnimator(0f, 1f, 2000L) { value ->
            tearProgress = value
            floatOffset = sin(value * Math.PI * 2).toFloat() * 2f * dp
            invalidate()
        }
    }

    // ========== THINKING ==========

    private fun startThinkingAnimation() {
        val thinkAnim = createLoopAnimator(0f, 1f, 3000L) { value ->
            gearPhase = value
            questionPhase = value
            bodyTilt = sin(value * Math.PI * 2).toFloat() * 5f
            invalidate()
        }
    }

    // ========== CONFUSED ==========

    private fun startConfusedAnimation() {
        val confAnim = createLoopAnimator(0f, 1f, 2000L) { value ->
            questionPhase = value
            confusedHeadTilt = sin(value * Math.PI * 2).toFloat() * 10f
            scratchHand = if (value > 0.3f && value < 0.7f) 1f else 0f
            invalidate()
        }
    }

    // ========== TIRED ==========

    private fun startTiredAnimation() {
        eyeMode = 6
        val tiredAnim = createLoopAnimator(0f, 1f, 3000L) { value ->
            breathePhase = value
            floatOffset = sin(value * Math.PI * 2).toFloat() * 3f * dp
            bodyScale = 0.95f + sin(value * Math.PI).toFloat() * 0.02f
            zzzPhase = value
            invalidate()
        }
    }

    // ========== TOUCHED (TERHARU) ==========

    private fun startTouchedAnimation() {
        eyeMode = 5
        val touchedAnim = ValueAnimator.ofFloat(0f, 1f)
        touchedAnim.duration = 2000
        touchedAnim.repeatCount = 1
        touchedAnim.addUpdateListener {
            val v = it.animatedValue as Float
            heartPhase = v
            bodyScale = 1f + sin(v * Math.PI * 3).toFloat() * 0.03f
            invalidate()
        }
        touchedAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                setAnimation(SpriteAnimation.IDLE)
            }
        })
        safeStartAnimator(touchedAnim)
    }

    // ========== TURNING BACK ==========

    private fun startTurningAnimation() {
        val turnAnim = ValueAnimator.ofFloat(0f, 1f, 2f, 3f)
        turnAnim.duration = 2000
        turnAnim.addUpdateListener {
            val v = it.animatedValue as Float
            turnAngle = when {
                v < 1f -> v * 180f
                v < 2f -> 180f
                else -> 180f + (v - 2f) * 180f
            }
            facingBack = v in 1f..2f
            invalidate()
        }
        turnAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                facingBack = false
                turnAngle = 0f
                setAnimation(SpriteAnimation.IDLE)
            }
        })
        safeStartAnimator(turnAnim)
    }

    // ========== SICK ==========

    private fun startSickAnimation() {
        eyeMode = 3
        val sickAnim = createLoopAnimator(0f, 1f, 3000L) { value ->
            thermometerPhase = value
            gearPhase = value
            bodyScale = 0.92f + sin(value * Math.PI).toFloat() * 0.03f
            bodyTilt = sin(value * Math.PI * 4).toFloat() * 5f
            invalidate()
        }
    }

    // ========== SPIN ==========

    private fun startSpinAnimation() {
        val spinAnim = ValueAnimator.ofFloat(0f, 360f)
        spinAnim.duration = 800
        spinAnim.interpolator = AccelerateDecelerateInterpolator()
        spinAnim.addUpdateListener {
            spinAngle = it.animatedValue as Float
            invalidate()
        }
        spinAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                spinAngle = 0f
                setAnimation(SpriteAnimation.IDLE)
            }
        })
        safeStartAnimator(spinAnim)
    }

    // ========== SHAKE ==========

    private fun startShakeAnimation() {
        val shakeAnim = ValueAnimator.ofFloat(0f, 1f)
        shakeAnim.duration = 600
        shakeAnim.repeatCount = 5
        shakeAnim.addUpdateListener {
            val v = it.animatedValue as Float
            floatOffset = sin(v * Math.PI * 6).toFloat() * 5f * dp
            bodyTilt = sin(v * Math.PI * 4).toFloat() * 8f
            invalidate()
        }
        shakeAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                bodyTilt = 0f
                setAnimation(SpriteAnimation.IDLE)
            }
        })
        safeStartAnimator(shakeAnim)
    }

    // ========== WAKE UP ==========

    private fun startWakeUpAnimation() {
        val wakeAnim = ValueAnimator.ofFloat(0f, 1f)
        wakeAnim.duration = 1500
        wakeAnim.interpolator = OvershootInterpolator()
        wakeAnim.addUpdateListener {
            val v = it.animatedValue as Float
            bodyScale = 0.9f + v * 0.15f
            eyeMode = if (v < 0.5f) 2 else if (v < 0.8f) 6 else 1
            jumpBounce = sin(v * Math.PI).toFloat() * 10f * dp
            invalidate()
        }
        wakeAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                setAnimation(SpriteAnimation.HAPPY)
            }
        })
        safeStartAnimator(wakeAnim)
    }

    // ========== DRAWING ==========

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(spriteX + halfSize, spriteY + halfSize)

        // Apply transforms
        canvas.translate(0f, -floatOffset)
        canvas.rotate(bodyTilt)
        canvas.rotate(spinAngle)
        canvas.scale(bodyScale, bodyScale)

        if (turnAngle > 0f) {
            canvas.scale(
                cos(Math.toRadians(turnAngle.toDouble())).toFloat(),
                1f
            )
        }

        drawRobot(canvas)

        canvas.restore()

        // Draw effects outside transform
        drawEffects(canvas)
    }

    protected open fun drawRobot(canvas: Canvas) {
        val s = spriteSize
        val hs = halfSize

        // Body (oval/tube shape)
        paint.color = bodyColor
        paint.style = Paint.Style.FILL
        val bodyRect = RectF(-s * 0.3f, -s * 0.1f, s * 0.3f, s * 0.4f)
        canvas.drawRoundRect(bodyRect, s * 0.15f, s * 0.15f, paint)

        // Legs
        val legY = s * 0.35f
        val legOffset = sin(legPhase * Math.PI * 2).toFloat() * s * 0.05f
        paint.color = bodyColor
        canvas.drawRoundRect(
            RectF(-s * 0.22f, legY, -s * 0.08f, legY + s * 0.18f - legOffset),
            s * 0.05f, s * 0.05f, paint
        )
        canvas.drawRoundRect(
            RectF(s * 0.08f, legY, s * 0.22f, legY + s * 0.18f + legOffset),
            s * 0.05f, s * 0.05f, paint
        )

        // Arms
        val armY = -s * 0.05f
        val armAngle = armRaise * -60f
        paint.color = bodyColor

        // Left arm
        canvas.save()
        canvas.translate(-s * 0.32f, armY)
        canvas.rotate(armAngle)
        canvas.drawRoundRect(
            RectF(-s * 0.06f, 0f, s * 0.06f, s * 0.22f),
            s * 0.05f, s * 0.05f, paint
        )
        canvas.restore()

        // Right arm
        canvas.save()
        canvas.translate(s * 0.32f, armY)
        canvas.rotate(-armAngle)
        canvas.drawRoundRect(
            RectF(-s * 0.06f, 0f, s * 0.06f, s * 0.22f),
            s * 0.05f, s * 0.05f, paint
        )
        canvas.restore()

        // Crossed arms (ANGRY)
        if (armCrossed > 0f) {
            paint.color = bodyColor
            canvas.save()
            canvas.translate(-s * 0.15f, s * 0.1f)
            canvas.rotate(45f * armCrossed)
            canvas.drawRoundRect(
                RectF(-s * 0.05f, 0f, s * 0.05f, s * 0.2f),
                s * 0.04f, s * 0.04f, paint
            )
            canvas.restore()
            canvas.save()
            canvas.translate(s * 0.15f, s * 0.1f)
            canvas.rotate(-45f * armCrossed)
            canvas.drawRoundRect(
                RectF(-s * 0.05f, 0f, s * 0.05f, s * 0.2f),
                s * 0.04f, s * 0.04f, paint
            )
            canvas.restore()
        }

        // Head (half circle dome)
        paint.color = bodyColor
        val headPath = Path()
        headPath.addArc(
            RectF(-s * 0.28f, -s * 0.42f, s * 0.28f, -s * 0.08f),
            180f, 180f
        )
        headPath.lineTo(s * 0.28f, -s * 0.1f)
        headPath.lineTo(-s * 0.28f, -s * 0.1f)
        headPath.close()
        canvas.drawPath(headPath, paint)

        // Eyes
        drawEyes(canvas, s)

        // Antennae
        drawAntennae(canvas, s)

        // Mouth
        drawMouth(canvas, s)

        // Label (optional "Android" text)
        paint.color = Color.argb(80, 255, 255, 255)
        paint.textSize = s * 0.08f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(robotName, 0f, s * 0.22f, paint)
    }

    protected open fun drawEyes(canvas: Canvas, s: Float) {
        val eyeSize = s * 0.06f
        val eyeY = -s * 0.28f
        val eyeSpacing = s * 0.1f

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL

        when (eyeMode) {
            0 -> { // Normal
                val blinkH = if (isBlinking) eyeSize * 0.1f else eyeSize
                canvas.drawOval(
                    RectF(-eyeSpacing - eyeSize, eyeY - blinkH, -eyeSpacing + eyeSize, eyeY + blinkH),
                    paint
                )
                canvas.drawOval(
                    RectF(eyeSpacing - eyeSize, eyeY - blinkH, eyeSpacing + eyeSize, eyeY + blinkH),
                    paint
                )
            }
            1 -> { // Happy (arc/smile eyes)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s * 0.03f
                val eyePath = Path()
                eyePath.addArc(
                    RectF(-eyeSpacing - eyeSize, eyeY - eyeSize, -eyeSpacing + eyeSize, eyeY + eyeSize * 0.5f),
                    200f, 140f
                )
                canvas.drawPath(eyePath, paint)
                eyePath.reset()
                eyePath.addArc(
                    RectF(eyeSpacing - eyeSize, eyeY - eyeSize, eyeSpacing + eyeSize, eyeY + eyeSize * 0.5f),
                    200f, 140f
                )
                canvas.drawPath(eyePath, paint)
            }
            2 -> { // Closed
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s * 0.02f
                canvas.drawLine(-eyeSpacing - eyeSize, eyeY, -eyeSpacing + eyeSize, eyeY, paint)
                canvas.drawLine(eyeSpacing - eyeSize, eyeY, eyeSpacing + eyeSize, eyeY, paint)
            }
            3 -> { // Sad
                canvas.drawOval(
                    RectF(-eyeSpacing - eyeSize, eyeY - eyeSize * 0.7f, -eyeSpacing + eyeSize, eyeY + eyeSize * 0.7f),
                    paint
                )
                canvas.drawOval(
                    RectF(eyeSpacing - eyeSize, eyeY - eyeSize * 0.7f, eyeSpacing + eyeSize, eyeY + eyeSize * 0.7f),
                    paint
                )
                // Sad eyebrows
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s * 0.02f
                canvas.drawLine(-eyeSpacing - eyeSize, eyeY - eyeSize * 1.5f, -eyeSpacing + eyeSize, eyeY - eyeSize * 1.2f, paint)
                canvas.drawLine(eyeSpacing - eyeSize, eyeY - eyeSize * 1.2f, eyeSpacing + eyeSize, eyeY - eyeSize * 1.5f, paint)
            }
            4 -> { // Angry
                canvas.drawOval(
                    RectF(-eyeSpacing - eyeSize, eyeY - eyeSize, -eyeSpacing + eyeSize, eyeY + eyeSize),
                    paint
                )
                canvas.drawOval(
                    RectF(eyeSpacing - eyeSize, eyeY - eyeSize, eyeSpacing + eyeSize, eyeY + eyeSize),
                    paint
                )
                // Angry eyebrows
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s * 0.03f
                canvas.drawLine(-eyeSpacing - eyeSize * 1.3f, eyeY - eyeSize * 1.5f, -eyeSpacing + eyeSize * 0.5f, eyeY - eyeSize * 0.8f, paint)
                canvas.drawLine(eyeSpacing - eyeSize * 0.5f, eyeY - eyeSize * 0.8f, eyeSpacing + eyeSize * 1.3f, eyeY - eyeSize * 1.5f, paint)
            }
            5 -> { // Wide (TERHARU)
                val bigEye = eyeSize * 1.5f
                canvas.drawOval(
                    RectF(-eyeSpacing - bigEye, eyeY - bigEye, -eyeSpacing + bigEye, eyeY + bigEye),
                    paint
                )
                canvas.drawOval(
                    RectF(eyeSpacing - bigEye, eyeY - bigEye, eyeSpacing + bigEye, eyeY + bigEye),
                    paint
                )
                // Sparkle
                paint.color = bodyColor
                canvas.drawCircle(-eyeSpacing - bigEye * 0.3f, eyeY - bigEye * 0.3f, bigEye * 0.3f, paint)
                canvas.drawCircle(eyeSpacing - bigEye * 0.3f, eyeY - bigEye * 0.3f, bigEye * 0.3f, paint)
            }
            6 -> { // Half open (TIRED)
                canvas.drawOval(
                    RectF(-eyeSpacing - eyeSize, eyeY, -eyeSpacing + eyeSize, eyeY + eyeSize),
                    paint
                )
                canvas.drawOval(
                    RectF(eyeSpacing - eyeSize, eyeY, eyeSpacing + eyeSize, eyeY + eyeSize),
                    paint
                )
            }
        }

        // Apply blink
        if (isBlinking && eyeMode == 0) {
            paint.color = bodyColor
            paint.style = Paint.Style.FILL
            val blinkH = eyeSize * blinkProgress
            canvas.drawOval(
                RectF(-eyeSpacing - eyeSize, eyeY - eyeSize, -eyeSpacing + eyeSize, eyeY - eyeSize + blinkH * 2),
                paint
            )
            canvas.drawOval(
                RectF(eyeSpacing - eyeSize, eyeY - eyeSize, eyeSpacing + eyeSize, eyeY - eyeSize + blinkH * 2),
                paint
            )
        }
    }

    protected open fun drawAntennae(canvas: Canvas, s: Float) {
        paint.color = bodyColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.03f
        paint.strokeCap = Paint.Cap.ROUND

        val wiggleRad = Math.toRadians(antennaWiggle.toDouble())

        // Left antenna
        val lx = -s * 0.12f
        val ly = -s * 0.42f
        val leX = lx + sin(wiggleRad - 0.3).toFloat() * s * 0.08f
        val leY = ly - s * 0.12f
        canvas.drawLine(lx, ly, leX, leY, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(leX, leY, s * 0.025f, paint)

        // Right antenna
        paint.style = Paint.Style.STROKE
        val rx = s * 0.12f
        val ry = -s * 0.42f
        val reX = rx + sin(wiggleRad + 0.3).toFloat() * s * 0.08f
        val reY = ry - s * 0.12f
        canvas.drawLine(rx, ry, reX, reY, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(reX, reY, s * 0.025f, paint)
    }

    protected open fun drawMouth(canvas: Canvas, s: Float) {
        val mouthY = -s * 0.16f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.015f
        paint.color = Color.argb(150, 0, 0, 0)

        when (eyeMode) {
            1 -> { // Happy smile
                val smilePath = Path()
                smilePath.addArc(
                    RectF(-s * 0.08f, mouthY - s * 0.03f, s * 0.08f, mouthY + s * 0.06f),
                    0f, 180f
                )
                canvas.drawPath(smilePath, paint)
            }
            4 -> { // Angry grimace
                canvas.drawLine(-s * 0.06f, mouthY, s * 0.06f, mouthY, paint)
            }
            3 -> { // Sad frown
                val frownPath = Path()
                frownPath.addArc(
                    RectF(-s * 0.08f, mouthY, s * 0.08f, mouthY + s * 0.08f),
                    180f, 180f
                )
                canvas.drawPath(frownPath, paint)
            }
            else -> { // Neutral
                canvas.drawLine(-s * 0.04f, mouthY, s * 0.04f, mouthY, paint)
            }
        }

        // Thermometer (SICK)
        if (thermometerPhase > 0f) {
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            val tx = s * 0.04f
            val ty = -s * 0.14f
            canvas.drawRoundRect(
                RectF(tx - s * 0.01f, ty, tx + s * 0.01f, ty + s * 0.12f),
                s * 0.005f, s * 0.005f, paint
            )
            canvas.drawCircle(tx, ty + s * 0.12f, s * 0.015f, paint)
        }
    }

    protected open fun drawEffects(canvas: Canvas) {
        val hs = halfSize
        val cx = spriteX + hs
        val cy = spriteY + hs

        // ZZZ (SLEEPING/TIRED)
        if (currentAnimation == SpriteAnimation.SLEEPING || currentAnimation == SpriteAnimation.TIRED) {
            paint.color = Color.YELLOW
            paint.textSize = 12f * dp
            paint.style = Paint.Style.FILL
            val zOffset = zzzPhase * 30f * dp
            for (i in 0..2) {
                val zx = cx + s * 0.25f + i * 10f * dp
                val zy = cy - halfSize - i * 12f * dp - zOffset
                paint.alpha = (255 - (zzzPhase * 255).toInt()).coerceIn(0, 255)
                val size = (10f + i * 2f) * dp
                paint.textSize = size
                canvas.drawText("z", zx, zy, paint)
            }
            paint.alpha = 255
        }

        // Steam (ANGRY)
        if (currentAnimation == SpriteAnimation.ANGRY) {
            paint.color = Color.argb(150, 200, 200, 200)
            paint.style = Paint.Style.FILL
            for (i in 0..1) {
                val sx = cx + (if (i == 0) -1 else 1) * s * 0.15f
                val sy = cy - halfSize - s * 0.35f - sin((steamPhase + i * 0.5f) * Math.PI * 2).toFloat() * s * 0.1f
                val sr = s * 0.04f + sin((steamPhase + i * 0.3f) * Math.PI).toFloat() * s * 0.01f
                canvas.drawCircle(sx, sy, sr, paint)
                canvas.drawCircle(sx + (if (i == 0) -1 else 1) * s * 0.05f, sy - s * 0.06f, sr * 0.7f, paint)
            }
        }

        // Tears (SAD)
        if (currentAnimation == SpriteAnimation.SAD) {
            paint.color = Color.argb(180, 66, 165, 245)
            paint.style = Paint.Style.FILL
            for (i in 0..1) {
                val tx = cx + (if (i == 0) -1 else 1) * halfSize * 0.35f
                val ty = cy - halfSize * 0.1f + (tearProgress * s * 0.5f)
                if (tearProgress < 0.7f) {
                    val tearSize = s * 0.03f
                    canvas.drawOval(
                        RectF(tx - tearSize, ty - tearSize * 1.5f, tx + tearSize, ty + tearSize),
                        paint
                    )
                }
            }
        }

        // Hearts (TOUCHED)
        if (currentAnimation == SpriteAnimation.TOUCHED) {
            paint.color = Color.argb(200, 233, 30, 99)
            paint.style = Paint.Style.FILL
            for (i in 0..2) {
                val hx = cx + sin((heartPhase * 2 + i * 0.3f) * Math.PI * 2).toFloat() * s * 0.3f
                val hy = cy - halfSize - s * 0.3f - heartPhase * s * 0.5f - i * s * 0.15f
                val heartSize = s * 0.04f * (1f - heartPhase * 0.5f)
                if (heartSize > 0) {
                    drawHeart(canvas, hx, hy, heartSize, paint)
                }
            }
        }

        // Question mark (CONFUSED/THINKING)
        if (currentAnimation == SpriteAnimation.CONFUSED || currentAnimation == SpriteAnimation.THINKING) {
            paint.color = if (currentAnimation == SpriteAnimation.THINKING) Color.GRAY else Color.rgb(255, 152, 0)
            paint.textSize = s * 0.12f
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            val qx = cx + s * 0.2f
            val qy = cy - halfSize - s * 0.15f - sin(questionPhase * Math.PI * 2).toFloat() * s * 0.05f
            canvas.drawText("?", qx, qy, paint)
            if (currentAnimation == SpriteAnimation.THINKING) {
                // Gear icon
                val gx = cx - s * 0.2f
                val gy = cy - halfSize - s * 0.2f
                drawGear(canvas, gx, gy, s * 0.06f, paint)
            }
        }

        // Rocket boost (FLYING)
        if (currentAnimation == SpriteAnimation.FLYING) {
            val boostX = cx
            val boostY = cy + halfSize * 0.9f
            val flameSize = s * 0.15f + sin(rocketPhase * Math.PI * 6).toFloat() * s * 0.05f
            paint.color = Color.rgb(255, 87, 34)
            paint.style = Paint.Style.FILL
            val flamePath = Path()
            flamePath.moveTo(boostX - s * 0.08f, boostY)
            flamePath.lineTo(boostX, boostY + flameSize)
            flamePath.lineTo(boostX + s * 0.08f, boostY)
            flamePath.close()
            canvas.drawPath(flamePath, paint)
            paint.color = Color.rgb(255, 193, 7)
            val innerFlame = flamePath
            innerFlame.reset()
            innerFlame.moveTo(boostX - s * 0.04f, boostY)
            innerFlame.lineTo(boostX, boostY + flameSize * 0.6f)
            innerFlame.lineTo(boostX + s * 0.04f, boostY)
            innerFlame.close()
            canvas.drawPath(innerFlame, paint)
        }
    }

    protected fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val path = Path()
        path.moveTo(x, y + size * 0.3f)
        path.cubicTo(x - size, y - size * 0.5f, x - size * 0.5f, y - size, x, y - size * 0.3f)
        path.cubicTo(x + size * 0.5f, y - size, x + size, y - size * 0.5f, x, y + size * 0.3f)
        canvas.drawPath(path, paint)
    }

    protected fun drawGear(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.color = Color.GRAY
        val teeth = 6
        val path = Path()
        for (i in 0 until teeth * 2) {
            val angle = (gearPhase * 360f + i * 180f / teeth) * Math.PI / 180
            val r = if (i % 2 == 0) size else size * 0.65f
            val px = x + cos(angle).toFloat() * r
            val py = y + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
        paint.color = bodyColor
        canvas.drawCircle(x, y, size * 0.35f, paint)
    }

    // ========== ANIMATOR HELPERS ==========

    protected fun createLoopAnimator(
        from: Float, to: Float, duration: Long,
        onUpdate: (Float) -> Unit
    ): ValueAnimator {
        val animator = ValueAnimator.ofFloat(from, to)
        animator.duration = duration
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.RESTART
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { onUpdate(it.animatedValue as Float) }
        safeStartAnimator(animator)
        return animator
    }

    protected fun safeStartAnimator(animator: ValueAnimator) {
        animators.add(animator)
        animator.start()
    }

    fun cancelAllAnimations() {
        animators.forEach {
            if (it.isRunning) it.cancel()
        }
        animators.clear()
        idleAnimator?.cancel()
        idleAnimator = null
        blinkAnimator?.cancel()
        blinkAnimator = null
        antennaAnimator?.cancel()
        antennaAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllAnimations()
    }

    // ========== ACCESSORS ==========

    fun getAnimators(): List<ValueAnimator> = animators.toList()

    val spriteWidth: Float get() = spriteSize
    val spriteHeight: Float get() = spriteSize

    companion object {
        val s: Float get() = 90f // fallback, actual use instance field
    }
}
