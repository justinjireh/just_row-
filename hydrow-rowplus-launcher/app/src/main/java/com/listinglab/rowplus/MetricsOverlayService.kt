package com.listinglab.rowplus

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class MetricsOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var overlayDistance: TextView? = null
    private var overlayDuration: TextView? = null
    private var overlaySpm: TextView? = null
    private var overlaySplit: TextView? = null

    private val binder = OverlayBinder()

    inner class OverlayBinder : Binder() {
        fun getService(): MetricsOverlayService = this@MetricsOverlayService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun createOverlay() {
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_metrics, null)

        overlayDistance = overlayView?.findViewById(R.id.overlayDistance)
        overlayDuration = overlayView?.findViewById(R.id.overlayDuration)
        overlaySpm = overlayView?.findViewById(R.id.overlaySpm)
        overlaySplit = overlayView?.findViewById(R.id.overlaySplit)

        @Suppress("DEPRECATION")
        val windowType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 50
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (_: Exception) {
            stopSelf()
            return
        }

        setupDrag()

        overlayView?.findViewById<View>(R.id.overlayExpandBtn)?.setOnClickListener {
            val intent = Intent(this, SessionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        }
    }

    private fun setupDrag() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        overlayView = null
    }

    fun updateMetrics(
        distance: String,
        duration: String,
        spm: String,
        split: String,
    ) {
        overlayDistance?.text = distance
        overlayDuration?.text = duration
        overlaySpm?.text = spm
        overlaySplit?.text = split
    }

    fun show() {
        overlayView?.visibility = View.VISIBLE
    }

    fun hide() {
        overlayView?.visibility = View.GONE
    }
}
