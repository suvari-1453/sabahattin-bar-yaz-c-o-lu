package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var rootLayout: FrameLayout? = null
    private var avatarImageView: ImageView? = null
    private var speechLayout: LinearLayout? = null
    private var speechTextView: TextView? = null
    private var closeImageView: ImageView? = null

    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var textBubbleHideJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Start Foreground to ensure robustness on Android 8.0+
        createNotificationChannelAndStartForeground()

        // Inflate or construct the view programmatically to avoid complex XML layouts
        setupFloatingView()
        
        // Collect current speech to update balloon
        serviceScope.launch {
            BubbleStateManager.currentSpeech.collectLatest { speech ->
                if (!speech.isNullOrBlank()) {
                    showSpeechBalloon(speech)
                } else {
                    hideSpeechBalloon()
                }
            }
        }

        BubbleStateManager.setBubbleActive(true)
    }

    private fun createNotificationChannelAndStartForeground() {
        val channelId = "gundi_floating_bubble_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Gündi Yüzen Balon Servisi"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Gündi Arka Planda Aktif")
            .setContentText("Yüzen balon diğer uygulamaların üzerinde açık.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(9981, notification)
    }

    private fun setupFloatingView() {
        val context = this
        rootLayout = FrameLayout(context)

        // Custom Layout Dimensions
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Avatar Layout Frame
        val avatarContainer = FrameLayout(context).apply {
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Circular Avatar Background layout using programmatic GradientDrawable
        val avatarBg = FrameLayout(context).apply {
            val size = (72 * resources.displayMetrics.density).toInt()
            val lp = FrameLayout.LayoutParams(size, size)
            setLayoutParams(lp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFF1744.toInt()) // Crimson Red outer border glow
            }
        }

        // Inner layout to hold circular avatar image
        val innerFrame = FrameLayout(context).apply {
            val size = (66 * resources.displayMetrics.density).toInt()
            val lp = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
            setLayoutParams(lp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF1E1B24.toInt()) // Cyber Dark body
            }
            clipToOutline = true
        }

        // ImageView
        avatarImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.bymix_avatar_1783590005005)
        }
        innerFrame.addView(avatarImageView)
        avatarBg.addView(innerFrame)
        avatarContainer.addView(avatarBg)

        // Close/Exit Small Button
        closeImageView = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCC000000.toInt())
            }
            val size = (24 * resources.displayMetrics.density).toInt()
            val lp = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.TOP or Gravity.END
            }
            setLayoutParams(lp)
            visibility = View.GONE
        }
        avatarContainer.addView(closeImageView)

        // Speech Balloon Layout using programmatic Rounded Rectangle
        speechLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * resources.displayMetrics.density
                setColor(0xFF1E1B24.toInt()) // Cyber dark theme
                setStroke((1.5 * resources.displayMetrics.density).toInt(), 0xFFFFD54F.toInt()) // Gold border
            }
            val padH = (14 * resources.displayMetrics.density).toInt()
            val padV = (10 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, padV)
            
            val lp = FrameLayout.LayoutParams(
                (180 * resources.displayMetrics.density).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START or Gravity.TOP
                leftMargin = (84 * resources.displayMetrics.density).toInt()
                topMargin = (12 * resources.displayMetrics.density).toInt()
            }
            setLayoutParams(lp)
            visibility = View.GONE
        }

        // Speech Balloon Text View
        speechTextView = TextView(context).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            text = ""
        }
        speechLayout?.addView(speechTextView)

        // Add views to main container
        rootLayout?.addView(speechLayout)
        rootLayout?.addView(avatarContainer)

        // Touch Listener for dragging
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        avatarBg.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    closeImageView?.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isMoving = true
                    }
                    layoutParams.x = initialX + deltaX.toInt()
                    layoutParams.y = initialY + deltaY.toInt()
                    windowManager.updateViewLayout(rootLayout, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    closeImageView?.visibility = View.GONE
                    if (!isMoving) {
                        // Click detected: open MainActivity
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        context.startActivity(intent)
                    } else {
                        // Snapping feature to nearest left or right edge of the screen
                        val displayMetrics = android.util.DisplayMetrics()
                        windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val screenWidth = displayMetrics.widthPixels
                        val bubbleWidth = if (avatarBg.width > 0) avatarBg.width else (72 * resources.displayMetrics.density).toInt()
                        val bubbleCenterX = layoutParams.x + bubbleWidth / 2
                        val snapToLeft = bubbleCenterX < screenWidth / 2
                        val targetX = if (snapToLeft) 0 else screenWidth - bubbleWidth

                        val animator = android.animation.ValueAnimator.ofInt(layoutParams.x, targetX)
                        animator.addUpdateListener { valueAnimator ->
                            layoutParams.x = valueAnimator.animatedValue as Int
                            try {
                                windowManager.updateViewLayout(rootLayout, layoutParams)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        animator.duration = 250
                        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                // Update speech layout params depending on which side it snapped to
                                val lp = speechLayout?.layoutParams as? FrameLayout.LayoutParams
                                if (lp != null) {
                                    if (snapToLeft) {
                                        lp.gravity = Gravity.START or Gravity.TOP
                                        lp.leftMargin = (84 * resources.displayMetrics.density).toInt()
                                        lp.rightMargin = 0
                                    } else {
                                        lp.gravity = Gravity.END or Gravity.TOP
                                        lp.rightMargin = (84 * resources.displayMetrics.density).toInt()
                                        lp.leftMargin = 0
                                    }
                                    speechLayout?.layoutParams = lp
                                    try {
                                        windowManager.updateViewLayout(rootLayout, layoutParams)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        })
                        animator.start()
                    }
                    true
                }
                else -> false
            }
        }

        // Close button action
        closeImageView?.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(rootLayout, layoutParams)
    }

    private fun showSpeechBalloon(text: String) {
        textBubbleHideJob?.cancel()
        speechTextView?.text = text
        speechLayout?.visibility = View.VISIBLE
        
        // Auto-hide balloon after 5 seconds of silence
        textBubbleHideJob = serviceScope.launch {
            delay(5000)
            speechLayout?.visibility = View.GONE
        }
    }

    private fun hideSpeechBalloon() {
        speechLayout?.visibility = View.GONE
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        rootLayout?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        BubbleStateManager.setBubbleActive(false)
    }
}
