package com.jmr.atlas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jmr.atlas.ui.theme.Atlas
import org.junit.Assert.assertTrue
import org.junit.Test

/** Palette guard: no yellow family (hue 40–95° with saturation > 25%), and key text pairs meet WCAG 4.5:1. */
class TokenTest {
    private fun hsl(c: Color): Pair<Float, Float> {
        val r = c.red; val g = c.green; val b = c.blue
        val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val l = (mx + mn) / 2; val d = mx - mn
        if (d == 0f) return 0f to 0f
        val s = d / (1 - kotlin.math.abs(2 * l - 1))
        val h = when (mx) { r -> 60 * (((g - b) / d) % 6); g -> 60 * ((b - r) / d + 2); else -> 60 * ((r - g) / d + 4) }.let { if (it < 0) it + 360 else it }
        return h to s
    }
    private fun ratio(a: Color, b: Color): Double { val la = a.luminance() + 0.05; val lb = b.luminance() + 0.05; return maxOf(la, lb) / minOf(la, lb).toDouble() }

    @Test fun noYellowFamily() = Atlas.all.forEach { (n, c) ->
        val (h, s) = hsl(c); assertTrue("$n hue=$h sat=$s is in the prohibited yellow range", !(h in 40f..95f && s > 0.25f))
    }

    @Test fun textContrast() {
        listOf(
            Atlas.DeepPlum to Atlas.Background, Atlas.SecondaryText to Atlas.Background, Atlas.SecondaryText to Atlas.SurfaceSecondary,
            Color.White to Atlas.Primary, Atlas.Primary to Atlas.Background, Atlas.DeepPlum to Atlas.PrimaryContainer, Atlas.CoralText to Atlas.Background,
            Atlas.CoralText to Atlas.Peach, Atlas.DeepTeal to Atlas.CoolMint, Atlas.BlueText to Atlas.PowderBlue, Atlas.Primary to Atlas.PrimaryContainer,
            Atlas.Error to Atlas.Background, Atlas.SecondaryText to Atlas.Surface, Atlas.Primary to Atlas.SurfaceSecondary,
        ).forEach { (f, b) -> val r = ratio(f, b); assertTrue("${f} on ${b} = $r", r >= 4.5) }
    }
}
