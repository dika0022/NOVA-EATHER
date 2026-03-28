package com.aether.nova.phantom.overlay

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.aether.nova.phantom.AetherNovaApp
import com.aether.nova.phantom.MainActivity
import com.aether.nova.phantom.R
import com.aether.nova.phantom.utils.SpritePrefs
import com.aether.nova.phantom.utils.TimeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var aetherView: AetherSprite
    private lateinit var novaView: NovaSprite
    private lateinit var duoAnimator: DuoAnimator
    private lateinit var spritePrefs: SpritePrefs
    private lateinit var timeScheduler: TimeScheduler

    private var aetherParams: WindowManager.LayoutParams? = null
    private var novaParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null
    private var timeScheduleRunnable: Runnable? = null

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var isRunning = false

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return android.app.Service.START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        try {
            initService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initService() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        spritePrefs = SpritePrefs(this)
        timeScheduler = TimeScheduler()

        getScreenSize()
        startForegroundNotification()
        createSprites()
        setupDuoAnimator()
        startWatchdog()
        startTimeScheduler()
        isRunning = true
    }

    private fun getScreenSize() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels.toFloat()
        screenHeight = metrics.heightPixels.toFloat()
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, AetherNovaApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(AetherNovaApp.NOTIFICATION_ID, notification)
    }

    private fun createSprites() {
        aetherView = AetherSprite(this)
        novaView = NovaSprite(this)

        val spriteSize = 90 * resources.displayMetrics.density

        // Create overlay params for Aether
        aetherParams = createOverlayParams()
        novaParams = createOverlayParams()

        // Set initial positions
        serviceScope.launch {
            val aetherPos = spritePrefs.getAetherPosition()
            val novaPos = spritePrefs.getNovaPosition()

            aetherView.spriteX = if (aetherPos.x < 0) 20f else aetherPos.x.coerceIn(0f, screenWidth - spriteSize)
            aetherView.spriteY = if (aetherPos.y < 0) screenHeight - spriteSize - 100f else aetherPos.y.coerceIn(0f, screenHeight - spriteSize)

            novaView.spriteX = if (novaPos.x < 0) screenWidth - spriteSize - 20f else novaPos.x.coerceIn(0f, screenWidth - spriteSize)
            novaView.spriteY = if (novaPos.y < 0) screenHeight - spriteSize - 100f else novaPos.y.coerceIn(0f, screenHeight - spriteSize)

            aetherParams?.x = aetherView.spriteX.toInt()
            aetherParams?.y = aetherView.spriteY.toInt()
            novaParams?.x = novaView.spriteX.toInt()
            novaParams?.y = novaView.spriteY.toInt()

            try {
                windowManager.addView(aetherView, aetherParams)
                windowManager.addView(novaView, novaParams)
            } catch (_: Exception) {
                // Views might already be added
            }

            setupSpriteListeners()
            applyTimeBasedBehavior()
        }
    }

    private fun createOverlayParams(): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun setupSpriteListeners() {
        val spriteSize = 90 * resources.displayMetrics.density

        // Aether listeners
        aetherView.setOnDragListener { x, y ->
            val clampedX = x.coerceIn(0f, screenWidth - spriteSize)
            val clampedY = y.coerceIn(0f, screenHeight - spriteSize)
            aetherView.spriteX = clampedX
            aetherView.spriteY = clampedY
            aetherParams?.x = clampedX.toInt()
            aetherParams?.y = clampedY.toInt()
            try {
                windowManager.updateViewLayout(aetherView, aetherParams)
            } catch (_: Exception) {}
            savePositions()
        }

        aetherView.setOnTapListener {
            if (aetherView.currentAnimation == SpriteAnimation.HAPPY) {
                aetherView.setAnimation(SpriteAnimation.ANGRY)
                handler.postDelayed({
                    aetherView.setAnimation(SpriteAnimation.HAPPY)
                    handler.postDelayed({ aetherView.setAnimation(SpriteAnimation.IDLE) }, 1500)
                }, 1500)
            } else {
                aetherView.setAnimation(SpriteAnimation.HAPPY)
                handler.postDelayed({ aetherView.setAnimation(SpriteAnimation.IDLE) }, 2000)
            }
        }

        aetherView.setOnLongPressListener {
            aetherView.setAnimation(SpriteAnimation.TOUCHED)
        }

        aetherView.setOnFlingListener { vx, vy ->
            animateFling(aetherView, aetherParams, vx, vy)
        }

        // Nova listeners
        novaView.setOnDragListener { x, y ->
            val clampedX = x.coerceIn(0f, screenWidth - spriteSize)
            val clampedY = y.coerceIn(0f, screenHeight - spriteSize)
            novaView.spriteX = clampedX
            novaView.spriteY = clampedY
            novaParams?.x = clampedX.toInt()
            novaParams?.y = clampedY.toInt()
            try {
                windowManager.updateViewLayout(novaView, novaParams)
            } catch (_: Exception) {}
            savePositions()
        }

        novaView.setOnTapListener {
            if (novaView.currentAnimation == SpriteAnimation.HAPPY) {
                novaView.setAnimation(SpriteAnimation.SPIN)
            } else {
                novaView.setAnimation(SpriteAnimation.HAPPY)
                handler.postDelayed({ novaView.setAnimation(SpriteAnimation.IDLE) }, 2000)
            }
        }

        novaView.setOnLongPressListener {
            novaView.setAnimation(SpriteAnimation.TOUCHED)
        }

        novaView.setOnFlingListener { vx, vy ->
            animateFling(novaView, novaParams, vx, vy)
        }
    }

    private fun animateFling(
        sprite: SpriteView,
        params: WindowManager.LayoutParams?,
        velocityX: Float,
        velocityY: Float
    ) {
        val spriteSize = 90 * resources.displayMetrics.density
        val speed = sqrt(velocityX * velocityX + velocityY * velocityY)
        val factor = speed.coerceAtMost(2000f) / 2000f
        val dx = velocityX * factor * 0.3f
        val dy = velocityY * factor * 0.3f

        val targetX = (sprite.spriteX + dx).coerceIn(0f, screenWidth - spriteSize)
        val targetY = (sprite.spriteY + dy).coerceIn(0f, screenHeight - spriteSize)

        val animX = android.animation.ValueAnimator.ofFloat(sprite.spriteX, targetX)
        animX.duration = 800
        animX.interpolator = BounceInterpolator()
        animX.addUpdateListener {
            sprite.spriteX = it.animatedValue as Float
            params?.x = sprite.spriteX.toInt()
            try {
                windowManager.updateViewLayout(sprite, params)
            } catch (_: Exception) {}
            sprite.invalidate()
        }

        val animY = android.animation.ValueAnimator.ofFloat(sprite.spriteY, targetY)
        animY.duration = 800
        animY.interpolator = BounceInterpolator()
        animY.addUpdateListener {
            sprite.spriteY = it.animatedValue as Float
            params?.y = sprite.spriteY.toInt()
            try {
                windowManager.updateViewLayout(sprite, params)
            } catch (_: Exception) {}
            sprite.invalidate()
        }

        animX.start()
        animY.start()
    }

    private fun setupDuoAnimator() {
        duoAnimator = DuoAnimator(
            context = this,
            aether = aetherView,
            nova = novaView,
            timeScheduler = timeScheduler,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        duoAnimator.startScheduling()
    }

    private fun applyTimeBasedBehavior() {
        val period = timeScheduler.getCurrentPeriod()
        when (period) {
            com.aether.nova.phantom.utils.TimePeriod.MORNING -> {
                // Nova wakes up excited, then wakes Aether
                novaView.setAnimation(SpriteAnimation.HAPPY)
                handler.postDelayed({
                    duoAnimator.performDuoAnimation(DuoAnimation.NOVA_WAKE_AETHER)
                }, 3000)
            }
            com.aether.nova.phantom.utils.TimePeriod.AFTERNOON -> {
                aetherView.setAnimation(SpriteAnimation.IDLE)
                novaView.setAnimation(SpriteAnimation.IDLE)
            }
            com.aether.nova.phantom.utils.TimePeriod.EVENING -> {
                aetherView.setAnimation(SpriteAnimation.TIRED)
                novaView.setAnimation(SpriteAnimation.TIRED)
                handler.postDelayed({
                    aetherView.setAnimation(SpriteAnimation.IDLE)
                    novaView.setAnimation(SpriteAnimation.IDLE)
                }, 5000)
            }
            com.aether.nova.phantom.utils.TimePeriod.NIGHT -> {
                // Nova sleeps first
                novaView.setAnimation(SpriteAnimation.SLEEPING)
                // Aether stays idle for a bit then sleeps
                handler.postDelayed({
                    aetherView.setAnimation(SpriteAnimation.SLEEPING)
                }, 15 * 60 * 1000L) // 15 min
            }
        }
    }

    private fun startTimeScheduler() {
        timeScheduleRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                applyTimeBasedBehavior()
                handler.postDelayed(this, 60_000L) // Check every minute
            }
        }
        handler.postDelayed(timeScheduleRunnable ?: return, 60_000L)
    }

    private fun startWatchdog() {
        watchdogRunnable = Runnable {
            if (!isRunning) return@Runnable
            try {
                // Check if views are still attached
                if (!aetherView.isAttachedToWindow) {
                    try {
                        windowManager.addView(aetherView, aetherParams)
                    } catch (_: Exception) {}
                }
                if (!novaView.isAttachedToWindow) {
                    try {
                        windowManager.addView(novaView, novaParams)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            handler.postDelayed(this::startWatchdog, 30_000L)
        }
        handler.postDelayed(watchdogRunnable ?: return, 30_000L)
    }

    private fun savePositions() {
        serviceScope.launch {
            try {
                spritePrefs.savePositions(
                    aetherView.spriteX, aetherView.spriteY,
                    novaView.spriteX, novaView.spriteY
                )
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        isRunning = false
        savePositions()

        watchdogRunnable?.let { handler.removeCallbacks(it) }
        timeScheduleRunnable?.let { handler.removeCallbacks(it) }

        duoAnimator.stop()
        aetherView.cancelAllAnimations()
        novaView.cancelAllAnimations()

        try {
            if (aetherView.isAttachedToWindow) {
                windowManager.removeView(aetherView)
            }
        } catch (_: Exception) {}

        try {
            if (novaView.isAttachedToWindow) {
                windowManager.removeView(novaView)
            }
        } catch (_: Exception) {}

        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        getScreenSize()
        val spriteSize = 90 * resources.displayMetrics.density
        aetherView.spriteX = aetherView.spriteX.coerceIn(0f, screenWidth - spriteSize)
        aetherView.spriteY = aetherView.spriteY.coerceIn(0f, screenHeight - spriteSize)
        novaView.spriteX = novaView.spriteX.coerceIn(0f, screenWidth - spriteSize)
        novaView.spriteY = novaView.spriteY.coerceIn(0f, screenHeight - spriteSize)

        aetherParams?.x = aetherView.spriteX.toInt()
        aetherParams?.y = aetherView.spriteY.toInt()
        novaParams?.x = novaView.spriteX.toInt()
        novaParams?.y = novaView.spriteY.toInt()

        try {
            windowManager.updateViewLayout(aetherView, aetherParams)
            windowManager.updateViewLayout(novaView, novaParams)
        } catch (_: Exception) {}
    }
}
