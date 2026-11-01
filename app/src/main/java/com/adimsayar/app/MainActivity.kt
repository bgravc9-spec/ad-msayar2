package com.adimsayar.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Native Android/Kotlin arayüz. HTML WebView kullanılmıyor. */
class MainActivity : Activity() {
    private val bg = Color.rgb(7, 19, 34)
    private val card = Color.rgb(17, 36, 60)
    private val card2 = Color.rgb(23, 46, 75)
    private val white = Color.rgb(240, 249, 255)
    private val muted = Color.rgb(150, 177, 206)
    private val cyan = Color.rgb(68, 214, 239)
    private val green = Color.rgb(92, 231, 182)
    private lateinit var repo: StepRepository
    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout
    private var page = 0
    private var ring: StepRingView? = null
    private var chart: StepWeekChart? = null
    private var distance: TextView? = null
    private var calories: TextView? = null
    private var weekly: TextView? = null
    private var trackingButton: TextView? = null
    private var trackingCaption: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshLoop = object : Runnable {
        override fun run() {
            refreshHome()
            handler.postDelayed(this, 1700L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = StepRepository(this)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildShell()
        showPage(0)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshLoop)
        handler.post(refreshLoop)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshLoop)
        super.onPause()
    }

    private fun dp(n: Int): Int = dip(n.toFloat()).roundToInt()

    private fun shape(color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun label(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            setTextColor(color)
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            includeFontPadding = true
        }

    private fun vertical(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun paddedCard(pad: Int = 18, background: Int = card): LinearLayout = vertical().apply {
        setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
        this.background = shape(background, 22, Color.rgb(36, 64, 96))
    }

    private fun gap(h: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }

    private fun buildShell() {
        root = vertical().apply {
            setBackgroundColor(bg)
            fitsSystemWindows = false
        }
        // Android 15 edge-to-edge ve eski Android sürümleri için sistem çubuğu payları.
        root.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                top = bars.top
                bottom = bars.bottom
            } else {
                top = insets.systemWindowInsetTop
                bottom = insets.systemWindowInsetBottom
            }
            v.setPadding(0, top, 0, bottom)
            insets
        }
        setContentView(root)
        root.addView(buildHeader())
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(buildNav())
        root.requestApplyInsets()
    }

    private fun buildHeader(): View = row().apply {
        setPadding(dp(22), dp(15), dp(22), dp(12))
        val emblem = label("↟", 30f, cyan, true).apply {
            gravity = Gravity.CENTER
            background = shape(card2, 16)
        }
        addView(emblem, LinearLayout.LayoutParams(dp(48), dp(48)))
        val titleBlock = vertical().apply { setPadding(dp(12), 0, 0, 0) }
        titleBlock.addView(label("ADIM SAYAR", 20f, white, true))
        titleBlock.addView(label("Her adım yeni bir başlangıç", 11.5f, muted))
        addView(titleBlock, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(label("●", 16f, green))
    }

    private fun buildNav(): View = row().apply {
        setPadding(dp(10), dp(9), dp(10), dp(14))
        setBackgroundColor(Color.rgb(10, 26, 45))
        listOf("⌂" to "Bugün", "▥" to "Geçmiş", "⚙" to "Ayarlar").forEachIndexed { idx, pair ->
            val cell = vertical().apply {
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(7), dp(4), dp(7))
                background = if (page == idx) shape(card2, 14) else shape(Color.TRANSPARENT, 14)
            }
            cell.addView(label(pair.first, 24f, if (page == idx) cyan else muted).apply { gravity = Gravity.CENTER })
            cell.addView(label(pair.second, 11f, if (page == idx) white else muted, page == idx).apply { gravity = Gravity.CENTER })
            cell.setOnClickListener { showPage(idx) }
            addView(cell, LinearLayout.LayoutParams(0, dp(64), 1f))
        }
    }

    private fun showPage(target: Int) {
        page = target
        ring = null
        chart = null
        distance = null
        calories = null
        weekly = null
        trackingButton = null
        trackingCaption = null
        content.removeAllViews()
        when (page) {
            0 -> content.addView(homePage())
            1 -> content.addView(historyPage())
            else -> content.addView(settingsPage())
        }
        // Gezinme butonlarında seçili durum güncellenir.
        root.removeViewAt(root.childCount - 1)
        root.addView(buildNav())
        refreshHome()
    }

    private fun scrollPage(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        val inner = vertical().apply {
            setPadding(dp(18), dp(10), dp(18), dp(25))
        }
        scroll.addView(inner)
        return scroll to inner
    }

    private fun homePage(): View {
        val (scroll, main) = scrollPage()
        val dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR")))
        main.addView(label("Bugün", 26f, white, true))
        main.addView(label(dateString.replaceFirstChar { it.uppercase() }, 13f, muted))
        main.addView(gap(17))

        val hero = paddedCard(16).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(19, 53, 93), Color.rgb(12, 35, 67))).apply {
                cornerRadius = dp(24).toFloat()
            }
        }
        val ringView = StepRingView(this)
        ring = ringView
        hero.addView(ringView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)))
        hero.addView(label("Telefonun hareket sensöründen takip edilir", 11.5f, muted).apply {
            gravity = Gravity.CENTER
        })
        hero.addView(gap(16))
        val button = label("TAKİBİ BAŞLAT", 15f, Color.rgb(4, 27, 43), true).apply {
            gravity = Gravity.CENTER
            background = shape(cyan, 15)
            setOnClickListener { if (repo.trackingEnabled()) stopTracking() else beginTracking() }
        }
        trackingButton = button
        hero.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(51)))
        val status = label("", 11.5f, muted).apply { gravity = Gravity.CENTER }
        trackingCaption = status
        hero.addView(gap(10))
        hero.addView(status)
        main.addView(hero)
        main.addView(gap(15))

        val stats = row()
        val (distanceCell, distanceText) = metricCard("MESAFE", "0,00 km", "⌁")
        val (calorieCell, calorieText) = metricCard("KALORİ", "~0 kcal", "♨")
        distance = distanceText
        calories = calorieText
        stats.addView(distanceCell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        stats.addView(View(this), LinearLayout.LayoutParams(dp(10), 1))
        stats.addView(calorieCell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        main.addView(stats)
        main.addView(gap(16))

        val sevenCard = paddedCard()
        val topRow = row()
        topRow.addView(label("Son 7 gün", 18f, white, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val total = label("", 11f, cyan, true)
        weekly = total
        topRow.addView(total)
        sevenCard.addView(topRow)
        sevenCard.addView(gap(10))
        val weekGraph = StepWeekChart(this)
        chart = weekGraph
        sevenCard.addView(weekGraph, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(162)))
        main.addView(sevenCard)
        main.addView(gap(12))
        main.addView(label("Bilgi: İlk takip başlatıldığında sensörün o anki değeri başlangıç kabul edilir. Mesafe ve kalori tahminidir.", 11.5f, muted))
        return scroll
    }

    private fun metricCard(title: String, initialValue: String, symbol: String): Pair<View, TextView> {
        val body = paddedCard(15)
        body.addView(label("$symbol   $title", 11.5f, muted, true))
        body.addView(gap(9))
        val value = label(initialValue, 21f, white, true)
        body.addView(value)
        return body to value
    }

    private fun historyPage(): View {
        val (scroll, main) = scrollPage()
        main.addView(label("Adım geçmişi", 26f, white, true))
        main.addView(label("İnternetsiz, sadece cihazında kayıtlı", 13f, muted))
        main.addView(gap(17))
        val weekBox = paddedCard()
        weekBox.addView(label("Son 7 gün toplamı", 13f, muted))
        weekBox.addView(gap(6))
        weekBox.addView(label(formattedSteps(repo.weekSteps()) + " adım", 27f, cyan, true))
        val chartView = StepWeekChart(this)
        chartView.update(repo)
        weekBox.addView(gap(12))
        weekBox.addView(chartView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(155)))
        main.addView(weekBox)
        main.addView(gap(19))
        main.addView(label("Son 14 gün", 18f, white, true))
        main.addView(gap(10))
        val fmt = DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("tr", "TR"))
        for (i in 0L..13L) {
            val day = LocalDate.now().minusDays(i)
            val value = repo.stepsOn(day)
            val dayCard = paddedCard(13)
            val head = row()
            head.addView(label(if (i == 0L) "Bugün" else day.format(fmt), 13f, white),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            head.addView(label("${formattedSteps(value)} adım", 12.5f, cyan, true))
            dayCard.addView(head)
            dayCard.addView(gap(10))
            val track = FrameLayout(this).apply { background = shape(Color.rgb(34, 61, 92), 5) }
            val fill = View(this).apply { background = shape(green, 5) }
            track.addView(fill, FrameLayout.LayoutParams(0, dp(7)))
            track.post {
                val percent = (value.toFloat() / repo.goal()).coerceIn(0f, 1f)
                fill.layoutParams = FrameLayout.LayoutParams((track.width * percent).toInt(), dp(7))
            }
            dayCard.addView(track, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)))
            main.addView(dayCard)
            main.addView(gap(8))
        }
        return scroll
    }

    private fun settingsPage(): View {
        val (scroll, main) = scrollPage()
        main.addView(label("Ayarlar", 26f, white, true))
        main.addView(label("Hedeflerini ve hesaplamaları kişiselleştir", 13f, muted))
        main.addView(gap(18))
        settingCard(main, "Günlük adım hedefi", "${formattedSteps(repo.goal().toLong())} adım", "100–100.000 arası", repo.goal()) {
            repo.setGoal(it)
        }
        settingCard(main, "Adım uzunluğu", "${repo.strideCm()} cm", "Mesafe tahmini için", repo.strideCm()) {
            repo.setStrideCm(it)
        }
        settingCard(main, "Vücut ağırlığı", "${repo.weightKg()} kg", "Kalori tahmini için (cihazda kalır)", repo.weightKg()) {
            repo.setWeightKg(it)
        }
        main.addView(gap(15))
        val privacy = paddedCard()
        privacy.addView(label("Gizlilik ve sınırlar", 17f, white, true))
        privacy.addView(gap(8))
        privacy.addView(label(
            "• Adımlar ve ayarlar yalnızca telefonunda tutulur.\n" +
            "• Sunucuya, reklamlara veya üçüncü kişilere veri gönderilmez.\n" +
            "• GPS ve internet izni kullanılmaz.\n" +
            "• Sayaç bazı cihazlarda gecikebilir veya pil tasarrufu nedeniyle durabilir.\n" +
            "• Gece yarısını aşan sensör boşlukları geriye dönük doğru dağıtılamayabilir.\n" +
            "• Kalori ve mesafe hesapları tahminidir.", 12.5f, muted
        ))
        privacy.addView(gap(14))
        privacy.addView(label("ADIM SAYAR v1.0 • Android 8.0+", 11.5f, cyan))
        main.addView(privacy)
        return scroll
    }

    private fun settingCard(parent: LinearLayout, name: String, selected: String,
                            help: String, initial: Int, save: (Int) -> Unit) {
        val box = paddedCard()
        val header = row()
        val info = vertical()
        info.addView(label(name, 15f, white, true))
        info.addView(gap(4))
        info.addView(label(help, 11f, muted))
        header.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(label("$selected  ›", 14f, cyan, true))
        box.addView(header)
        box.setOnClickListener {
            val field = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(initial.toString())
                setSelectAllOnFocus(true)
                setPadding(dp(18), dp(10), dp(18), dp(10))
            }
            AlertDialog.Builder(this)
                .setTitle(name)
                .setView(field)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet") { _, _ ->
                    val entered = field.text.toString().toIntOrNull()
                    if (entered == null || entered <= 0) {
                        Toast.makeText(this, "Geçerli bir sayı gir", Toast.LENGTH_SHORT).show()
                    } else {
                        save(entered)
                        showPage(2)
                    }
                }
                .show()
        }
        parent.addView(box)
        parent.addView(gap(10))
    }

    private fun hasSensor(): Boolean {
        val manager = getSystemService(SENSOR_SERVICE) as SensorManager
        return manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }

    private fun beginTracking() {
        if (!hasSensor()) {
            AlertDialog.Builder(this)
                .setTitle("Adım sensörü bulunamadı")
                .setMessage("Bu telefonda TYPE_STEP_COUNTER veya TYPE_STEP_DETECTOR bulunmuyor. Gerçek sensörlü bir Android telefonla dene.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }
        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), REQ_ACTIVITY)
            return
        }
        startNow()
    }

    private fun startNow() {
        repo.beginTracking()
        try {
            startForegroundService(Intent(this, StepTrackingService::class.java)
                .setAction(StepTrackingService.ACTION_START))
            Toast.makeText(this, "Adım takibi başlatıldı", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            repo.stopTracking()
            Toast.makeText(this, "Takip başlatılamadı: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        refreshHome()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
        }
    }

    private fun stopTracking() {
        repo.stopTracking()
        try {
            startService(Intent(this, StepTrackingService::class.java)
                .setAction(StepTrackingService.ACTION_STOP))
        } catch (_: Exception) {
            // Repo zaten durduruldu; servis başlamamış da olabilir.
        }
        refreshHome()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_ACTIVITY) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startNow()
            else Toast.makeText(this, "Adım takibi için hareket izni gerekli", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshHome() {
        if (page != 0) return
        val steps = repo.stepsToday()
        ring?.update(steps, repo.goal())
        distance?.text = String.format(Locale("tr", "TR"), "%.2f km", repo.distanceKm(steps))
        calories?.text = "~${repo.estimatedCalories(steps)} kcal"
        weekly?.text = "${formattedSteps(repo.weekSteps())} adım"
        chart?.update(repo)
        trackingButton?.text = if (repo.trackingEnabled()) "TAKİBİ DURDUR" else "TAKİBİ BAŞLAT"
        trackingButton?.background = shape(if (repo.trackingEnabled()) Color.rgb(255, 184, 114) else cyan, 15)
        trackingCaption?.text = when {
            !hasSensor() -> "Bu cihazda adım sayma sensörü bulunamadı"
            repo.trackingEnabled() -> "● Takip açık • Bildirim üzerinden durdurabilirsin"
            else -> "Takip kapalı • Başlatmak için düğmeye dokun"
        }
        trackingCaption?.setTextColor(if (repo.trackingEnabled()) green else muted)
    }

    companion object {
        private const val REQ_ACTIVITY = 42
        private const val REQ_NOTIFICATION = 43
    }
}
