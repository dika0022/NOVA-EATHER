package com.aether.nova.phantom.overlay

import android.content.Context
import android.graphics.Color

class AetherSprite(context: Context) : SpriteView(
    context,
    bodyColor = Color.parseColor("#4CAF50"),
    bodyColorDark = Color.parseColor("#388E3C"),
    bodyColorLight = Color.parseColor("#81C784"),
    robotName = "Aether"
) {
    init {
        // Aether-specific: slightly wider body, more serious proportions
    }
}
