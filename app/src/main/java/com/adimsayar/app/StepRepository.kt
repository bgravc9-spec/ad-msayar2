package com.adimsayar.app

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Basit, çevrimdışı veri deposu. Adım verileri cihazdan dışarı gönderilmez.
 * İlk sayaç okuması referans (baseline) kabul edilir; geçmişin tamamı sayılmaz.
 * Adımlar yalnızca başlatılmış takip sırasında sayılır.
 */
class StepRepository(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("steps_local_v1", Context.MODE_PRIVATE)

    fun todayKey(): String = LocalDate.now().toString()
    fun stepsOn(date: LocalDate): Long = prefs.getLong("steps_${date}", 0L)
    fun stepsToday(): Long = stepsOn(LocalDate.now())
    fun goal(): Int = prefs.getInt("goal", 10_000).coerceAtLeast(100)
    fun strideCm(): Int = prefs.getInt("stride_cm", 70).coerceIn(35, 120)
    fun weightKg(): Int = prefs.getInt("weight_kg", 70).coerceIn(20, 350)
    fun trackingEnabled(): Boolean = prefs.getBoolean("tracking_enabled", false)
    fun lastSensorType(): String = prefs.getString("sensor_type", "") ?: ""

    fun setGoal(value: Int) = prefs.edit().putInt("goal", value.coerceIn(100, 100_000)).apply()
    fun setStrideCm(value: Int) = prefs.edit().putInt("stride_cm", value.coerceIn(35, 120)).apply()
    fun setWeightKg(value: Int) = prefs.edit().putInt("weight_kg", value.coerceIn(20, 350)).apply()

    @Synchronized
    fun beginTracking() {
        prefs.edit()
            .putBoolean("tracking_enabled", true)
            .putLong("counter_raw", -1L) // Duraklatılmış dönemin adımlarını ekleme.
            .apply()
    }

    fun stopTracking() = prefs.edit().putBoolean("tracking_enabled", false).apply()

    @Synchronized
    fun onCounterValue(rawValue: Long) {
        if (!trackingEnabled() || rawValue < 0L) return
        val today = todayKey()
        val previousRaw = prefs.getLong("counter_raw", -1L)
        val previousDay = prefs.getString("counter_day", null)
        // Sayaç ilk kez okunmuş, cihaz yeniden başlamış veya tarih değişmiş olabilir.
        // Birikmiş fakat hangi güne ait olduğu bilinmeyen adımlar tahminen eklenmez.
        val increment = if (previousRaw >= 0 && rawValue >= previousRaw && previousDay == today) {
            (rawValue - previousRaw).coerceAtMost(100_000L)
        } else 0L
        val total = (prefs.getLong("steps_$today", 0L) + increment).coerceAtLeast(0L)
        prefs.edit()
            .putLong("steps_$today", total)
            .putLong("counter_raw", rawValue)
            .putString("counter_day", today)
            .putString("sensor_type", "COUNTER")
            .apply()
    }

    @Synchronized
    fun onDetectedStep() {
        if (!trackingEnabled()) return
        val today = todayKey()
        prefs.edit()
            .putLong("steps_$today", prefs.getLong("steps_$today", 0L) + 1L)
            .putString("sensor_type", "DETECTOR")
            .apply()
    }

    fun distanceKm(steps: Long): Double = steps.toDouble() * strideCm() / 100_000.0

    /** Basit tahmin: kilometre × kilogram × 0.7. Tıbbi ölçüm değildir. */
    fun estimatedCalories(steps: Long): Int =
        (distanceKm(steps) * weightKg() * 0.7).toInt().coerceAtLeast(0)

    fun weekSteps(): Long = (0..6).sumOf { stepsOn(LocalDate.now().minusDays(it.toLong())) }
}
