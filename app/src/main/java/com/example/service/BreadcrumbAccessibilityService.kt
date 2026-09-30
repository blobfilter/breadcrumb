package com.example.service

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.CaptureActivity
import com.example.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class BreadcrumbAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var overlayContainer: LinearLayout? = null
    private var mascotBadgeView: MascotBubbleView? = null
    private var saveSelectionPill: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isDockedLeft = false

    private val hideSelectionPillRunnable = Runnable {
        updateDetectedTextPill(null)
    }

    companion object {
        const val EXTRA_LAUNCH_SNIP_TOOL = "extra_launch_snip_tool"
        const val EXTRA_ACCESSIBILITY_SELECTED_TEXT = "extra_accessibility_selected_text"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _detectedSelectedText = MutableStateFlow<String?>(null)
        val detectedSelectedText: StateFlow<String?> = _detectedSelectedText.asStateFlow()

        var latestCapturedBitmap: Bitmap? = null

        // Optional fallback provider when MainActivity is active and Accessibility takeScreenshot is rate-limited
        var inAppWindowCaptureProvider: ((onCaptured: (Bitmap?) -> Unit) -> Unit)? = null

        fun updateDetectedSelectionFromApp(text: String?) {
            val cleaned = text?.trim()?.takeIf { it.length >= 2 }
            _detectedSelectedText.value = cleaned
            instance?.mainHandler?.post {
                instance?.updateDetectedTextPill(cleaned)
            }
        }

        fun clearDetectedSelection() {
            _detectedSelectedText.value = null
            instance?.mainHandler?.post {
                instance?.updateDetectedTextPill(null)
            }
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (_isServiceConnected.value) return true
            return try {
                val expectedComponent = ComponentName(context, BreadcrumbAccessibilityService::class.java)
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(enabledServices)
                while (splitter.hasNext()) {
                    val componentString = splitter.next()
                    val enabledComponent = ComponentName.unflattenFromString(componentString)
                    if (enabledComponent != null && enabledComponent == expectedComponent) {
                        return true
                    }
                }
                false
            } catch (_: Exception) {
                false
            }
        }

        @Volatile
        private var instance: BreadcrumbAccessibilityService? = null

        fun requestScreenshotSnipFromService(): Boolean {
            val svc = instance ?: return false
            svc.triggerScreenshotAndLaunchSnipTool()
            return true
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val settings = SettingsManager.getInstance(applicationContext)
        serviceScope.launch {
            combine(settings.floatingOverlayEnabled, settings.textSelectionEnabled) { floating, textSel ->
                floating to textSel
            }.collect { (floatingEnabled, textSelEnabled) ->
                syncOverlayConfiguration(floatingEnabled, textSelEnabled)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val settings = SettingsManager.getInstance(applicationContext)

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            if (!settings.textSelectionEnabled.value) return

            // Ignore selection events inside our own CaptureActivity save sheet
            if (event.packageName?.toString() == packageName &&
                event.className?.toString()?.contains("EditText") == true &&
                CaptureActivity.isCaptureOverlayVisible
            ) {
                return
            }

            val selected = extractSelectedText(event)
            if (!selected.isNullOrBlank() && selected.length >= 2) {
                _detectedSelectedText.value = selected
                updateDetectedTextPill(selected)
                mainHandler.removeCallbacks(hideSelectionPillRunnable)
                mainHandler.postDelayed(hideSelectionPillRunnable, 8000L)
            } else if (selected != null && selected.isEmpty()) {
                mainHandler.removeCallbacks(hideSelectionPillRunnable)
                mainHandler.postDelayed(hideSelectionPillRunnable, 600L)
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Dismiss selection pill when user switches windows or apps
            if (_detectedSelectedText.value != null && !CaptureActivity.isCaptureOverlayVisible) {
                mainHandler.removeCallbacks(hideSelectionPillRunnable)
                mainHandler.postDelayed(hideSelectionPillRunnable, 1000L)
            }
        }
    }

    private fun extractSelectedText(event: AccessibilityEvent): String? {
        try {
            val source: AccessibilityNodeInfo? = event.source
            if (source != null) {
                val nodeText = source.text?.toString()
                val selStart = source.textSelectionStart
                val selEnd = source.textSelectionEnd
                if (!nodeText.isNullOrEmpty() && selStart >= 0 && selEnd >= 0) {
                    if (selStart == selEnd) return ""
                    val minIdx = min(selStart, selEnd).coerceIn(0, nodeText.length)
                    val maxIdx = max(selStart, selEnd).coerceIn(0, nodeText.length)
                    if (maxIdx > minIdx) {
                        return nodeText.substring(minIdx, maxIdx).trim()
                    }
                }
            }

            val from = event.fromIndex
            val to = event.toIndex
            val rawEventText = source?.text?.toString()?.takeIf { it.isNotBlank() }
                ?: event.text?.filterNotNull()?.joinToString(" ")?.takeIf { it.isNotBlank() }
                ?: ""
            if (rawEventText.isNotEmpty() && from >= 0 && to >= 0) {
                if (from == to) return ""
                val minIdx = min(from, to).coerceIn(0, rawEventText.length)
                val maxIdx = max(from, to).coerceIn(0, rawEventText.length)
                if (maxIdx > minIdx) {
                    return rawEventText.substring(minIdx, maxIdx).trim()
                }
            }

            // In some applications, event.text directly carries the selected text snippet
            if (event.text?.isNotEmpty() == true) {
                val directText = event.text.filterNotNull().joinToString(" ").trim()
                if (directText.length >= 2 && from < 0 && to < 0) {
                    return directText
                }
            }

            val root = rootInActiveWindow
            val focused = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null) {
                val fText = focused.text?.toString()
                val fStart = focused.textSelectionStart
                val fEnd = focused.textSelectionEnd
                if (!fText.isNullOrEmpty() && fStart >= 0 && fEnd >= 0 && fStart != fEnd) {
                    val minIdx = min(fStart, fEnd).coerceIn(0, fText.length)
                    val maxIdx = max(fStart, fEnd).coerceIn(0, fText.length)
                    if (maxIdx > minIdx) {
                        return fText.substring(minIdx, maxIdx).trim()
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun updateDetectedTextPill(selectedText: String?) {
        val settings = SettingsManager.getInstance(applicationContext)
        val textSelEnabled = settings.textSelectionEnabled.value
        val floatingEnabled = settings.floatingOverlayEnabled.value

        if (!textSelEnabled) {
            saveSelectionPill?.visibility = View.GONE
            if (!floatingEnabled) {
                overlayContainer?.visibility = View.GONE
            }
            return
        }

        if (overlayContainer == null) {
            createFloatingOverlay()
        }

        val pill = saveSelectionPill ?: return
        val container = overlayContainer ?: return
        val mascotView = mascotBadgeView
        val currentParams = layoutParams ?: return
        val wm = windowManager ?: return

        if (selectedText.isNullOrBlank()) {
            pill.visibility = View.GONE
            if (floatingEnabled) {
                mascotView?.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                container.alpha = 1f
                currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            } else {
                mascotView?.visibility = View.GONE
                container.visibility = View.GONE
                container.alpha = 0f
                currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            try {
                wm.updateViewLayout(container, currentParams)
            } catch (_: Exception) {}
        } else {
            val preview = if (selectedText.length > 20) "${selectedText.take(20)}…" else selectedText
            pill.text = if (floatingEnabled) "Save \"$preview\"" else "🍞 Save \"$preview\""
            pill.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            container.alpha = 1f

            if (floatingEnabled) {
                mascotView?.visibility = View.VISIBLE
            } else {
                mascotView?.visibility = View.GONE
            }

            val density = resources.displayMetrics.density
            val edgeMarginPx = (6 * density).toInt()
            currentParams.gravity = if (isDockedLeft) (Gravity.TOP or Gravity.START) else (Gravity.TOP or Gravity.END)
            currentParams.x = edgeMarginPx
            currentParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            val onSaveClick = View.OnClickListener {
                val textToSave = _detectedSelectedText.value ?: selectedText
                updateDetectedTextPill(null)
                _detectedSelectedText.value = null
                launchCaptureWithSelectedText(textToSave)
            }
            pill.setOnClickListener(onSaveClick)
            if (!floatingEnabled) {
                container.setOnClickListener(onSaveClick)
            } else {
                container.setOnClickListener(null)
            }

            container.requestLayout()
            try {
                wm.updateViewLayout(container, currentParams)
            } catch (_: Exception) {}
        }
    }

    private fun launchCaptureWithSelectedText(selectedText: String) {
        val intent = Intent(this, CaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACCESSIBILITY_SELECTED_TEXT, selectedText)
        }
        startActivity(intent)
    }

    private fun restoreOverlayVisibility() {
        val container = overlayContainer ?: return
        val settings = SettingsManager.getInstance(applicationContext)
        if (settings.floatingOverlayEnabled.value) {
            container.alpha = 1f
            container.visibility = View.VISIBLE
        } else if (saveSelectionPill?.visibility == View.VISIBLE) {
            container.alpha = 1f
            container.visibility = View.VISIBLE
        } else {
            container.visibility = View.GONE
        }
    }

    fun triggerScreenshotAndLaunchSnipTool() {
        val container = overlayContainer
        // Completely hide the floating mascot & text pill before taking screenshot
        container?.alpha = 0f
        container?.visibility = View.GONE

        mainHandler.postDelayed({
            performAccessibilityScreenshot(allowRetry = true)
        }, 110L)
    }

    private fun performAccessibilityScreenshot(allowRetry: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            try {
                                val hwBitmap = Bitmap.wrapHardwareBuffer(
                                    screenshot.hardwareBuffer,
                                    screenshot.colorSpace
                                )
                                val swBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                                screenshot.hardwareBuffer.close()
                                hwBitmap?.recycle()
                                if (swBitmap != null) {
                                    latestCapturedBitmap = swBitmap
                                }
                            } catch (_: Exception) {
                                try { screenshot.hardwareBuffer.close() } catch (_: Exception) {}
                            }
                            restoreOverlayVisibility()
                            openSnipToolActivity()
                        }

                        override fun onFailure(errorCode: Int) {
                            // Error code 3 = ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT (rate limit on rapid snips)
                            if (allowRetry && errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT) {
                                mainHandler.postDelayed({
                                    performAccessibilityScreenshot(allowRetry = false)
                                }, 420L)
                                return
                            }
                            val fallbackProvider = inAppWindowCaptureProvider
                            if (fallbackProvider != null) {
                                fallbackProvider.invoke { fallbackBmp ->
                                    if (fallbackBmp != null) {
                                        latestCapturedBitmap = fallbackBmp
                                    }
                                    restoreOverlayVisibility()
                                    openSnipToolActivity()
                                }
                            } else {
                                restoreOverlayVisibility()
                                openSnipToolActivity()
                            }
                        }
                    }
                )
            } catch (_: Exception) {
                restoreOverlayVisibility()
                openSnipToolActivity()
            }
        } else {
            val fallbackProvider = inAppWindowCaptureProvider
            if (fallbackProvider != null) {
                fallbackProvider.invoke { fallbackBmp ->
                    if (fallbackBmp != null) {
                        latestCapturedBitmap = fallbackBmp
                    }
                    restoreOverlayVisibility()
                    openSnipToolActivity()
                }
            } else {
                restoreOverlayVisibility()
                openSnipToolActivity()
            }
        }
    }

    private fun openSnipToolActivity() {
        val intent = Intent(this, CaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LAUNCH_SNIP_TOOL, true)
        }
        startActivity(intent)
    }

    private fun syncOverlayConfiguration(floatingEnabled: Boolean, textSelEnabled: Boolean) {
        if (!floatingEnabled && !textSelEnabled) {
            removeFloatingOverlay()
            return
        }

        if (overlayContainer == null) {
            createFloatingOverlay()
        }

        val container = overlayContainer ?: return
        val mascotView = mascotBadgeView
        val pill = saveSelectionPill

        val currentParams = layoutParams
        val wm = windowManager

        if (floatingEnabled) {
            mascotView?.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            container.alpha = 1f
            currentParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            mascotView?.visibility = View.GONE
            if (pill?.visibility == View.VISIBLE) {
                container.visibility = View.VISIBLE
                container.alpha = 1f
                currentParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            } else {
                container.visibility = View.GONE
                container.alpha = 0f
                currentParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
        }
        if (currentParams != null && wm != null) {
            try {
                wm.updateViewLayout(container, currentParams)
            } catch (_: Exception) {}
        }
    }

    private fun createFloatingOverlay() {
        val wm = windowManager ?: return
        if (overlayContainer != null) {
            return
        }

        val density = resources.displayMetrics.density
        val mascotSizePx = (56 * density).toInt()
        val edgeMarginPx = (6 * density).toInt()
        val screenSize = getScreenDimensions()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val pill = TextView(this).apply {
            visibility = View.GONE
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            paint.isFakeBoldText = true
            val padH = (14 * density).toInt()
            val padV = (8 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#45779E"))
                cornerRadius = 20 * density
                setStroke((2 * density).toInt(), Color.parseColor("#8D675A"))
            }
        }
        saveSelectionPill = pill

        val mascotView = MascotBubbleView(this)
        mascotBadgeView = mascotView

        val pillParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = (6 * density).toInt()
            marginStart = (6 * density).toInt()
        }

        // Default docked on right edge: pill on left of mascot
        container.addView(pill, pillParams)
        container.addView(mascotView, LinearLayout.LayoutParams(mascotSizePx, mascotSizePx))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isDockedLeft) (Gravity.TOP or Gravity.START) else (Gravity.TOP or Gravity.END)
            x = edgeMarginPx
            y = (screenSize.second * 0.36f).toInt()
        }
        layoutParams = params

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false

        mascotView.setOnTouchListener { _, event ->
            val currentParams = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val (screenW, _) = getScreenDimensions()
                    currentParams.gravity = Gravity.TOP or Gravity.START
                    currentParams.x = if (isDockedLeft) edgeMarginPx else (screenW - container.width - edgeMarginPx)
                    startX = currentParams.x
                    startY = currentParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!isDragging && hypot(dx, dy) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val (screenW, screenH) = getScreenDimensions()
                        val minY = (48 * density).toInt()
                        val maxY = (screenH - mascotSizePx - 64 * density).toInt().coerceAtLeast(minY)
                        currentParams.x = (startX + dx.toInt()).coerceIn(0, screenW - mascotSizePx)
                        currentParams.y = (startY + dy.toInt()).coerceIn(minY, maxY)
                        try {
                            wm.updateViewLayout(container, currentParams)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasDragging = isDragging
                    isDragging = false
                    if (!wasDragging && event.actionMasked == MotionEvent.ACTION_UP) {
                        // Clicked on the floating mascot -> Launch Image Snip Tool!
                        triggerScreenshotAndLaunchSnipTool()
                    } else if (wasDragging) {
                        val (screenW, _) = getScreenDimensions()
                        val currentWidth = container.width.takeIf { it > 0 } ?: mascotSizePx
                        val centerX = currentParams.x + currentWidth / 2
                        val snapLeft = centerX < screenW / 2
                        if (snapLeft != isDockedLeft) {
                            isDockedLeft = snapLeft
                            if (pill.parent === container) {
                                container.removeView(pill)
                            }
                            if (snapLeft) {
                                container.addView(pill, pillParams)
                            } else {
                                container.addView(pill, 0, pillParams)
                            }
                        }
                        val targetX = if (snapLeft) {
                            edgeMarginPx
                        } else {
                            (screenW - currentWidth - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                        }

                        ValueAnimator.ofInt(currentParams.x, targetX).apply {
                            duration = 220L
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { anim ->
                                currentParams.x = anim.animatedValue as Int
                                try {
                                    wm.updateViewLayout(container, currentParams)
                                } catch (_: Exception) {}
                            }
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    currentParams.gravity = if (snapLeft) (Gravity.TOP or Gravity.START) else (Gravity.TOP or Gravity.END)
                                    currentParams.x = edgeMarginPx
                                    try {
                                        wm.updateViewLayout(container, currentParams)
                                    } catch (_: Exception) {}
                                }
                            })
                            start()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(container, params)
            overlayContainer = container
            val settings = SettingsManager.getInstance(applicationContext)
            val floatingEnabled = settings.floatingOverlayEnabled.value
            if (!floatingEnabled) {
                mascotView.visibility = View.GONE
                container.visibility = View.GONE
            }
        } catch (_: Exception) {}
    }

    private fun removeFloatingOverlay() {
        val wm = windowManager
        val container = overlayContainer
        overlayContainer = null
        mascotBadgeView = null
        saveSelectionPill = null
        if (wm != null && container != null) {
            try {
                wm.removeView(container)
            } catch (_: Exception) {}
        }
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        val wm = windowManager ?: return 1080 to 2400
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels.coerceAtLeast(320) to metrics.heightPixels.coerceAtLeast(480)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        _isServiceConnected.value = false
        instance = null
        removeFloatingOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _isServiceConnected.value = false
        instance = null
        removeFloatingOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Custom View that renders the exact Breadcrumb Mascot SVG with hand-drawn squiggly crust outline
     * for the system-wide Accessibility floating overlay.
     */
    private class MascotBubbleView(context: Context) : View(context) {
        private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#F7F3E8")
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#8D675A")
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#1E2742")
        }
        private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#F6B1B1")
        }
        private val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#55555F")
            strokeCap = Paint.Cap.ROUND
        }
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(42, 30, 39, 66)
        }

        private val rectF = RectF()
        private val smilePath = Path()
        private val squigglePath = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            val scale = min(w, h) / 128f
            val left = 14f * scale
            val top = 14f * scale
            val boxW = 100f * scale
            val boxH = 100f * scale
            val rx = 24f * scale

            // Subtle drop shadow
            rectF.set(left + 1.5f * scale, top + 3.5f * scale, left + boxW + 1.5f * scale, top + boxH + 3.5f * scale)
            canvas.drawRoundRect(rectF, rx, rx, shadowPaint)

            // Face fill
            rectF.set(left, top, left + boxW, top + boxH)
            canvas.drawRoundRect(rectF, rx, rx, facePaint)

            // Polished hand-drawn squiggly border with corner damping & secondary sketch pass
            borderPaint.color = Color.parseColor("#8D675A")
            borderPaint.alpha = 255
            borderPaint.strokeWidth = 7.8f * scale
            buildNativeSquigglePath(left, top, boxW, boxH, rx, 0.85f * scale, 1.2f)
            canvas.drawPath(squigglePath, borderPaint)

            borderPaint.alpha = 82
            borderPaint.strokeWidth = 3.5f * scale
            buildNativeSquigglePath(left, top, boxW, boxH, rx, 0.55f * scale, 3.6f)
            canvas.drawPath(squigglePath, borderPaint)

            // Eyes: cx=48, cy=58, rx=5, ry=8 & cx=80, cy=58, rx=5, ry=8
            val eyeRx = 5f * scale
            val eyeRy = 8f * scale
            rectF.set(48f * scale - eyeRx, 58f * scale - eyeRy, 48f * scale + eyeRx, 58f * scale + eyeRy)
            canvas.drawOval(rectF, eyePaint)
            rectF.set(80f * scale - eyeRx, 58f * scale - eyeRy, 80f * scale + eyeRx, 58f * scale + eyeRy)
            canvas.drawOval(rectF, eyePaint)

            // Blush: cx=36, cy=78, rx=10, ry=6 & cx=92, cy=78, rx=10, ry=6
            val blushRx = 10f * scale
            val blushRy = 6f * scale
            rectF.set(36f * scale - blushRx, 78f * scale - blushRy, 36f * scale + blushRx, 78f * scale + blushRy)
            canvas.drawOval(rectF, blushPaint)
            rectF.set(92f * scale - blushRx, 78f * scale - blushRy, 92f * scale + blushRx, 78f * scale + blushRy)
            canvas.drawOval(rectF, blushPaint)

            // Smile: M48 84 Q64 98 80 84
            smilePaint.strokeWidth = 6f * scale
            smilePath.reset()
            smilePath.moveTo(48f * scale, 84f * scale)
            smilePath.quadTo(64f * scale, 98f * scale, 80f * scale, 84f * scale)
            canvas.drawPath(smilePath, smilePaint)
        }

        private fun buildNativeSquigglePath(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            r: Float,
            amp: Float,
            seed: Float
        ) {
            squigglePath.reset()
            val right = left + width
            val bottom = top + height
            val topStraight = max(0f, width - 2f * r)
            val sideStraight = max(0f, height - 2f * r)
            val cornerArcLen = 0.5f * PI.toFloat() * r
            val totalPerimeter = 2f * topStraight + 2f * sideStraight + 4f * cornerArcLen
            if (totalPerimeter <= 1f) return

            val steps = 64
            val xs = FloatArray(steps)
            val ys = FloatArray(steps)

            for (i in 0 until steps) {
                val dist = (i.toFloat() / steps) * totalPerimeter
                var d = dist
                var bx = left + r
                var by = top
                var nx = 0f
                var ny = -1f
                var damping = 1f

                if (d <= topStraight) {
                    bx = left + r + d; by = top; nx = 0f; ny = -1f
                } else {
                    d -= topStraight
                    if (d <= cornerArcLen && r > 0f) {
                        val u = (d / cornerArcLen).coerceIn(0f, 1f)
                        val angle = (-0.5f * PI.toFloat()) + u * (0.5f * PI.toFloat())
                        nx = cos(angle); ny = sin(angle)
                        bx = right - r + r * nx; by = top + r + r * ny
                        val dc = kotlin.math.abs(u - 0.5f) * 2f
                        damping = 0.22f + 0.78f * (dc * dc * (3f - 2f * dc))
                    } else {
                        d -= cornerArcLen
                        if (d <= sideStraight) {
                            bx = right; by = top + r + d; nx = 1f; ny = 0f
                        } else {
                            d -= sideStraight
                            if (d <= cornerArcLen && r > 0f) {
                                val u = (d / cornerArcLen).coerceIn(0f, 1f)
                                val angle = u * (0.5f * PI.toFloat())
                                nx = cos(angle); ny = sin(angle)
                                bx = right - r + r * nx; by = bottom - r + r * ny
                                val dc = kotlin.math.abs(u - 0.5f) * 2f
                                damping = 0.22f + 0.78f * (dc * dc * (3f - 2f * dc))
                            } else {
                                d -= cornerArcLen
                                if (d <= topStraight) {
                                    bx = right - r - d; by = bottom; nx = 0f; ny = 1f
                                } else {
                                    d -= topStraight
                                    if (d <= cornerArcLen && r > 0f) {
                                        val u = (d / cornerArcLen).coerceIn(0f, 1f)
                                        val angle = (0.5f * PI.toFloat()) + u * (0.5f * PI.toFloat())
                                        nx = cos(angle); ny = sin(angle)
                                        bx = left + r + r * nx; by = bottom - r + r * ny
                                        val dc = kotlin.math.abs(u - 0.5f) * 2f
                                        damping = 0.22f + 0.78f * (dc * dc * (3f - 2f * dc))
                                    } else {
                                        d -= cornerArcLen
                                        if (d <= sideStraight) {
                                            bx = left; by = bottom - r - d; nx = -1f; ny = 0f
                                        } else {
                                            d -= sideStraight
                                            val u = if (cornerArcLen > 0f) (d / cornerArcLen).coerceIn(0f, 1f) else 0f
                                            val angle = PI.toFloat() + u * (0.5f * PI.toFloat())
                                            nx = cos(angle); ny = sin(angle)
                                            bx = left + r + r * nx; by = top + r + r * ny
                                            val dc = kotlin.math.abs(u - 0.5f) * 2f
                                            damping = 0.22f + 0.78f * (dc * dc * (3f - 2f * dc))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val phase = (i.toFloat() / steps) * 4f * 2f * PI.toFloat()
                val rawWave = 0.55f * sin(phase + seed) +
                    0.30f * cos(phase * 2f + seed * 1.73f + 0.6f) +
                    0.15f * sin(phase * 3f - seed * 1.31f + 1.4f)
                val wave = amp * damping * rawWave

                xs[i] = bx + nx * wave
                ys[i] = by + ny * wave
            }

            val firstMidX = (xs[steps - 1] + xs[0]) * 0.5f
            val firstMidY = (ys[steps - 1] + ys[0]) * 0.5f
            squigglePath.moveTo(firstMidX, firstMidY)
            for (i in 0 until steps) {
                val next = (i + 1) % steps
                val midX = (xs[i] + xs[next]) * 0.5f
                val midY = (ys[i] + ys[next]) * 0.5f
                squigglePath.quadTo(xs[i], ys[i], midX, midY)
            }
            squigglePath.close()
        }
    }
}
