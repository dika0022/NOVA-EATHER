package com.aether.nova.phantom.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color

class NovaSprite(context: Context) : SpriteView(
    context,
    bodyColor = Color.parseColor("#1976D2"),
    bodyColorDark = Color.parseColor("#1565C0"),
    bodyColorLight = Color.parseColor("#64B5F6"),
    robotName = "Nova"
) {
    init {
        // Nova-specific: slightly leaner, more dynamic posture
    }

    override fun drawRobot(canvas: Canvas) {
        // Nova has slightly different proportions (leaner)
        val origScale = bodyScale
        // Slightly narrower
        canvas.save()
        canvas.scale(0.95f, 1.02f)
        super.drawRobot(canvas)
        canvas.restore()
    }
}
