package com.adimsayar.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Matrix
import android.view.View
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val tr = Locale("tr", "TR")

fun Context.dip(v: Float): Float = v * resources.displayMetrics.density
fun formattedSteps(n: Long): String = NumberFormat.getIntegerInstance(tr).format(n)

/** Tek bir Canvas içinde çizilen animasyonsuz fakat hızlı, gerçek veri gösterimi. */
class StepRingView(context: Context) : View(context) {
    private var steps = 0L
    private var goal = 10_000
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }
    private val smaller = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(167, 195, 222)
        textAlign = Paint.Align.CENTER
    }

    fun update(steps: Long, goal: Int) {
        this.steps = steps
        this.goal = goal.coerceAtLeast(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.38f
        val stroke = context.dip(15f)
        val r = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.shader = null
        paint.color = Color.rgb(37, 65, 100)
        canvas.drawArc(r, -90f, 360f, false, paint)
        val gradient = SweepGradient(cx, cy, intArrayOf(
            Color.rgb(71, 210, 245), Color.rgb(69, 244, 189), Color.rgb(71, 210, 245)
        ), null)
        val matrix = Matrix().apply { setRotate(-90f, cx, cy) }
        gradient.setLocalMatrix(matrix)
        paint.shader = gradient
        val fraction = (steps.toFloat() / goal).coerceIn(0f, 1f)
        if (fraction > 0f) canvas.drawArc(r, -90f, fraction * 360f, false, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
        smaller.textSize = context.dip(11.5f)
        canvas.drawText("BUGÜNKÜ ADIM", cx, cy - context.dip(35f), smaller)
        number.textSize = context.dip(37f)
        canvas.drawText(formattedSteps(steps), cx, cy + context.dip(7f), number)
        smaller.textSize = context.dip(12f)
        canvas.drawText("/ ${formattedSteps(goal.toLong())} hedef", cx, cy + context.dip(30f), smaller)
        smaller.color = Color.rgb(92, 235, 191)
        canvas.drawText("%${(steps * 100 / goal).coerceAtMost(999L)} tamamlandı", cx, cy + context.dip(52f), smaller)
        smaller.color = Color.rgb(167, 195, 222)
    }
}

/** Son 7 gün adımlarını gösteren çevrimdışı sütun grafiği. */
class StepWeekChart(context: Context) : View(context) {
    private var values = LongArray(7)
    private var goal = 10_000
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(163, 186, 210)
        textAlign = Paint.Align.CENTER
        textSize = context.dip(11f)
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE", tr)
    fun update(repo: StepRepository) {
        values = LongArray(7) { repo.stepsOn(LocalDate.now().minusDays((6 - it).toLong())) }
        goal = repo.goal()
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = context.dip(18f)
        val right = width - context.dip(18f)
        val top = context.dip(12f)
        val bottom = height - context.dip(35f)
        val area = (bottom - top).coerceAtLeast(1f)
        val maxValue = max(goal.toLong(), values.maxOrNull() ?: 1L).coerceAtLeast(1L)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = context.dip(1f)
        paint.color = Color.rgb(42, 70, 99)
        for (i in 0..3) {
            val y = top + i * area / 3
            canvas.drawLine(left, y, right, y, paint)
        }
        val slot = (right - left) / 7f
        val barWidth = min(slot * 0.47f, context.dip(28f))
        for (i in 0 until 7) {
            val barH = ((values[i].toDouble() / maxValue).coerceIn(0.0, 1.0) * area).toFloat()
            val x = left + slot * (i + 0.5f)
            paint.style = Paint.Style.FILL
            paint.color = if (i == 6) Color.rgb(61, 220, 234) else Color.rgb(63, 113, 174)
            canvas.drawRoundRect(x - barWidth / 2, bottom - barH,
                x + barWidth / 2, bottom, context.dip(6f), context.dip(6f), paint)
            val date = LocalDate.now().minusDays((6 - i).toLong())
            val label = date.format(dateFormatter).replaceFirstChar { it.uppercase(tr) }
            canvas.drawText(label, x, height - context.dip(12f), caption)
        }
    }
}
