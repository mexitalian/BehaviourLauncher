package app.olauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import app.olauncher.R

/**
 * Subtle dot-grid ("dot notebook") background for the home screen.
 * The grid step follows the typography rhythm (~one text line) so the
 * dots sit on the same vertical beat as the clock, date and intention rows.
 */
class DotGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val density = resources.displayMetrics.density
    private val step = 28f * density
    private val radius = 1f * density

    init {
        val typedArray = context.obtainStyledAttributes(intArrayOf(R.attr.primaryColor))
        val color = typedArray.getColor(0, android.graphics.Color.WHITE)
        typedArray.recycle()
        paint.color = color
        paint.alpha = (255 * 0.10f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val w = width.toFloat()
        var y = step
        while (y < h) {
            var x = step
            while (x < w) {
                canvas.drawCircle(x, y, radius, paint)
                x += step
            }
            y += step
        }
    }
}
