package com.aether.nova.phantom.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.aether.nova.phantom.utils.TimeScheduler
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class DuoAnimation {
    CHATTING,
    PLAYING,
    SLEEPING_TOGETHER,
    RUNNING_TOGETHER,
    HIGH_FIVE,
    POINTING_AT_USER,
    NOVA_WAKE_AETHER,
    CHASE
}

class DuoAnimator(
    private val context: Context,
    private val aether: AetherSprite,
    private val nova: NovaSprite,
    private val timeScheduler: TimeScheduler,
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    private val handler = Handler(Looper.getMainLooper())
    private val animators = mutableListOf<ValueAnimator>()
    private var currentDuoAnimation: DuoAnimation? = null
    private var isRunning = false

    private var interactionRunnable: Runnable? = null

    fun startScheduling() {
        isRunning = true
        scheduleNextInteraction()
    }

    fun stop() {
        isRunning = false
        interactionRunnable?.let { handler.removeCallbacks(it) }
        cancelAllAnimators()
    }

    private fun scheduleNextInteraction() {
        if (!isRunning) return
        val interval = timeScheduler.getInteractionIntervalMs()
        interactionRunnable = Runnable {
            if (!isRunning) return@Runnable
            performRandomDuoAnimation()
            scheduleNextInteraction()
        }
        handler.postDelayed(interactionRunnable ?: return, interval)
    }

    fun performRandomDuoAnimation() {
        val period = timeScheduler.getCurrentPeriod()
        val animations = when (period) {
            com.aether.nova.phantom.utils.TimePeriod.MORNING -> listOf(
                DuoAnimation.NOVA_WAKE_AETHER,
                DuoAnimation.CHATTING,
                DuoAnimation.POINTING_AT_USER,
                DuoAnimation.HIGH_FIVE
            )
            com.aether.nova.phantom.utils.TimePeriod.AFTERNOON -> listOf(
                DuoAnimation.CHATTING,
                DuoAnimation.PLAYING,
                DuoAnimation.RUNNING_TOGETHER,
                DuoAnimation.CHASE,
                DuoAnimation.HIGH_FIVE
            )
            com.aether.nova.phantom.utils.TimePeriod.EVENING -> listOf(
                DuoAnimation.CHATTING,
                DuoAnimation.PLAYING,
                DuoAnimation.SLEEPING_TOGETHER
            )
            com.aether.nova.phantom.utils.TimePeriod.NIGHT -> listOf(
                DuoAnimation.SLEEPING_TOGETHER
            )
        }

        val selected = animations.random()
        performDuoAnimation(selected)
    }

    fun performDuoAnimation(animation: DuoAnimation) {
        cancelAllAnimators()
        currentDuoAnimation = animation

        when (animation) {
            DuoAnimation.CHATTING -> performChatting()
            DuoAnimation.PLAYING -> performPlaying()
            DuoAnimation.SLEEPING_TOGETHER -> performSleepingTogether()
            DuoAnimation.RUNNING_TOGETHER -> performRunningTogether()
            DuoAnimation.HIGH_FIVE -> performHighFive()
            DuoAnimation.POINTING_AT_USER -> performPointingAtUser()
            DuoAnimation.NOVA_WAKE_AETHER -> performNovaWakeAether()
            DuoAnimation.CHASE -> performChase()
        }
    }

    // ========== CHATTING ==========
    private fun performChatting() {
        // Nova walks toward Aether
        val targetX = aether.spriteX + aether.spriteWidth + 10f
        val duration = 2000L

        aether.setAnimation(SpriteAnimation.IDLE)
        nova.setAnimation(SpriteAnimation.WALKING)

        val walkAnim = ValueAnimator.ofFloat(nova.spriteX, targetX)
        walkAnim.duration = duration
        walkAnim.interpolator = LinearInterpolator()
        walkAnim.addUpdateListener {
            nova.spriteX = it.animatedValue as Float
            nova.invalidate()
        }
        walkAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                nova.setAnimation(SpriteAnimation.IDLE)
                // Nova talks, Aether nods
                handler.postDelayed({
                    aether.setAnimation(SpriteAnimation.THINKING)
                    handler.postDelayed({
                        aether.setAnimation(SpriteAnimation.HAPPY)
                        handler.postDelayed({
                            aether.setAnimation(SpriteAnimation.IDLE)
                            nova.setAnimation(SpriteAnimation.IDLE)
                        }, 2000)
                    }, 3000)
                }, 500)
            }
        })
        safeStartAnimator(walkAnim)
    }

    // ========== PLAYING ==========
    private fun performPlaying() {
        aether.setAnimation(SpriteAnimation.HAPPY)
        nova.setAnimation(SpriteAnimation.HAPPY)

        handler.postDelayed({
            aether.setAnimation(SpriteAnimation.SPIN)
            handler.postDelayed({
                nova.setAnimation(SpriteAnimation.SPIN)
                handler.postDelayed({
                    aether.setAnimation(SpriteAnimation.HAPPY)
                    nova.setAnimation(SpriteAnimation.HAPPY)
                    handler.postDelayed({
                        aether.setAnimation(SpriteAnimation.IDLE)
                        nova.setAnimation(SpriteAnimation.IDLE)
                    }, 1500)
                }, 1000)
            }, 1000)
        }, 1500)
    }

    // ========== SLEEPING TOGETHER ==========
    private fun performSleepingTogether() {
        // Move them close together
        val targetX = aether.spriteX + aether.spriteWidth + 5f

        val moveAnim = ValueAnimator.ofFloat(nova.spriteX, targetX)
        moveAnim.duration = 1500
        moveAnim.addUpdateListener {
            nova.spriteX = it.animatedValue as Float
            nova.invalidate()
        }
        moveAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                aether.setAnimation(SpriteAnimation.SLEEPING)
                nova.setAnimation(SpriteAnimation.SLEEPING)
            }
        })
        safeStartAnimator(moveAnim)
    }

    // ========== RUNNING TOGETHER ==========
    private fun performRunningTogether() {
        aether.setAnimation(SpriteAnimation.WALKING)
        nova.setAnimation(SpriteAnimation.WALKING)

        val direction = if (aether.spriteX < screenWidth / 2) 1f else -1f
        val runDuration = 4000L

        val runAether = ValueAnimator.ofFloat(aether.spriteX, aether.spriteX + direction * screenWidth * 0.6f)
        runAether.duration = runDuration
        runAether.interpolator = LinearInterpolator()
        runAether.addUpdateListener {
            aether.spriteX = it.animatedValue as Float
            aether.invalidate()
        }

        val runNova = ValueAnimator.ofFloat(nova.spriteX, nova.spriteX + direction * screenWidth * 0.6f)
        runNova.duration = runDuration
        runNova.interpolator = LinearInterpolator()
        runNova.addUpdateListener {
            nova.spriteX = it.animatedValue as Float
            nova.invalidate()
        }

        runAether.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                aether.setAnimation(SpriteAnimation.TIRED)
                nova.setAnimation(SpriteAnimation.TIRED)
                handler.postDelayed({
                    aether.setAnimation(SpriteAnimation.IDLE)
                    nova.setAnimation(SpriteAnimation.IDLE)
                }, 3000)
            }
        })

        safeStartAnimator(runAether)
        safeStartAnimator(runNova)
    }

    // ========== HIGH FIVE ==========
    private fun performHighFive() {
        val midX = (aether.spriteX + nova.spriteX) / 2f

        aether.setAnimation(SpriteAnimation.WALKING)
        nova.setAnimation(SpriteAnimation.WALKING)

        val moveAether = ValueAnimator.ofFloat(aether.spriteX, midX - aether.spriteWidth * 0.5f)
        moveAether.duration = 1500
        moveAether.addUpdateListener {
            aether.spriteX = it.animatedValue as Float
            aether.invalidate()
        }

        val moveNova = ValueAnimator.ofFloat(nova.spriteX, midX + 10f)
        moveNova.duration = 1500
        moveNova.addUpdateListener {
            nova.spriteX = it.animatedValue as Float
            nova.invalidate()
        }

        moveAether.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                aether.setAnimation(SpriteAnimation.HAPPY)
                nova.setAnimation(SpriteAnimation.HAPPY)
                handler.postDelayed({
                    aether.setAnimation(SpriteAnimation.IDLE)
                    nova.setAnimation(SpriteAnimation.IDLE)
                }, 2000)
            }
        })

        safeStartAnimator(moveAether)
        safeStartAnimator(moveNova)
    }

    // ========== POINTING AT USER ==========
    private fun performPointingAtUser() {
        aether.armRaise = 0.8f
        nova.armRaise = 0.8f
        aether.invalidate()
        nova.invalidate()

        aether.setAnimation(SpriteAnimation.HAPPY)
        nova.setAnimation(SpriteAnimation.HAPPY)

        handler.postDelayed({
            aether.setAnimation(SpriteAnimation.IDLE)
            nova.setAnimation(SpriteAnimation.IDLE)
        }, 3000)
    }

    // ========== NOVA WAKE AETHER ==========
    private fun performNovaWakeAether() {
        aether.setAnimation(SpriteAnimation.SLEEPING)

        val targetX = aether.spriteX + aether.spriteWidth + 5f

        nova.setAnimation(SpriteAnimation.WALKING)
        val walkAnim = ValueAnimator.ofFloat(nova.spriteX, targetX)
        walkAnim.duration = 1500
        walkAnim.addUpdateListener {
            nova.spriteX = it.animatedValue as Float
            nova.invalidate()
        }
        walkAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                nova.setAnimation(SpriteAnimation.SHAKE)
                handler.postDelayed({
                    aether.setAnimation(SpriteAnimation.WAKE_UP)
                    handler.postDelayed({
                        aether.setAnimation(SpriteAnimation.HAPPY)
                        nova.setAnimation(SpriteAnimation.HAPPY)
                        handler.postDelayed({
                            aether.setAnimation(SpriteAnimation.IDLE)
                            nova.setAnimation(SpriteAnimation.IDLE)
                        }, 2000)
                    }, 1500)
                }, 1200)
            }
        })
        safeStartAnimator(walkAnim)
    }

    // ========== CHASE ==========
    private fun performChase() {
        nova.setAnimation(SpriteAnimation.WALKING)
        val direction = if (Math.random() > 0.5) 1f else -1f
        val targetNovaX = if (direction > 0) screenWidth - nova.spriteWidth else 0f

        val chaseNova = ValueAnimator.ofFloat(nova.spriteX, targetNovaX)
        chaseNova.duration = 3000
        chaseNova.interpolator = LinearInterpolator()
        chaseNova.addUpdateListener {
            nova.spriteX = it.animatedValue as Float
            nova.invalidate()
        }

        aether.setAnimation(SpriteAnimation.WALKING)
        val chaseAether = ValueAnimator.ofFloat(aether.spriteX, targetNovaX - direction * aether.spriteWidth * 0.5f)
        chaseAether.duration = 3500
        chaseAether.interpolator = LinearInterpolator()
        chaseAether.addUpdateListener {
            aether.spriteX = it.animatedValue as Float
            aether.invalidate()
        }

        chaseAether.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                aether.setAnimation(SpriteAnimation.TIRED)
                nova.setAnimation(SpriteAnimation.TIRED)
                handler.postDelayed({
                    aether.setAnimation(SpriteAnimation.IDLE)
                    nova.setAnimation(SpriteAnimation.IDLE)
                }, 4000)
            }
        })

        safeStartAnimator(chaseNova)
        safeStartAnimator(chaseAether)
    }

    // ========== HELPERS ==========

    private fun safeStartAnimator(animator: ValueAnimator) {
        animators.add(animator)
        animator.start()
    }

    private fun cancelAllAnimators() {
        animators.forEach {
            if (it.isRunning) it.cancel()
        }
        animators.clear()
    }

    fun updateScreenSize(width: Float, height: Float) {
        // Screen size updated - handled via constructor
    }
}
