package com.jmr.atlas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jmr.atlas.MainActivity
import com.jmr.atlas.R

/**
 * The one quick-capture widget (design C01–C06): tap to type, or voice, scan, or paste the copied link.
 * Every button just opens Atlas at the right step; the widget itself reads nothing and stores nothing.
 */
class CaptureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content(context) }
    }

    @androidx.compose.runtime.Composable
    private fun Content(ctx: Context) {
        fun open(mode: String) = actionStartActivity(Intent(ctx, MainActivity::class.java).setAction(MainActivity.ACTION_QUICK).putExtra(MainActivity.EXTRA_MODE, mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        val plum = ColorProvider(Color(0xFF352044)); val lilac = ColorProvider(Color(0xFFF4EDF8)); val white = ColorProvider(Color.White)
        Row(GlanceModifier.fillMaxSize().background(white).cornerRadius(28.dp).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.defaultWeight().height(48.dp).background(lilac).cornerRadius(24.dp).padding(horizontal = 6.dp).clickable(open(MODE_TEXT))
                .semantics { contentDescription = "Capture by typing" }, verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(36.dp).background(ColorProvider(Color(0xFF7443B1))).cornerRadius(18.dp), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(R.drawable.w_add), null, GlanceModifier.size(20.dp))
                }
                Spacer(GlanceModifier.width(10.dp))
                Text("Capture…", style = TextStyle(color = plum, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
            Spacer(GlanceModifier.width(6.dp))
            Button(R.drawable.w_mic, "Voice note", open(MODE_VOICE))
            Button(R.drawable.w_camera, "Scan or photo", open(MODE_SCAN))
            Button(R.drawable.w_link, "Save copied link", open(MODE_LINK))
        }
    }

    @androidx.compose.runtime.Composable
    private fun Button(icon: Int, desc: String, action: androidx.glance.action.Action) {
        Box(GlanceModifier.size(48.dp).cornerRadius(24.dp).clickable(action).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
            Image(ImageProvider(icon), null, GlanceModifier.size(24.dp))
        }
    }

    companion object {
        const val MODE_TEXT = "text"; const val MODE_VOICE = "voice"; const val MODE_SCAN = "scan"; const val MODE_LINK = "link"
    }
}

class CaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaptureWidget()
}
