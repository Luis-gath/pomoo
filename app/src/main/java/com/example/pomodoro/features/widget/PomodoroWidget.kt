package com.example.pomodoro.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.pomodoro.features.timer.data.SettingsDataStore
import com.example.pomodoro.features.timer.domain.PomodoroMode
import com.example.pomodoro.features.timer.service.PomodoroForegroundService
// Note: Interacting with Service from Widget can be tricky.
// Usually we use WorkManager or specific Broadcasts.
// Since we have a foreground service running, we can send broadcasts or start service with action.
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import android.content.Intent
import androidx.glance.unit.ColorProvider
import com.example.pomodoro.MainActivity
import com.example.pomodoro.R

class PomodoroWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // In a real app we might want to collect flow from DataStore or Service here
        // But Glance state handling can be specific. 
        // For simplicity, we just show a static view or current state if possible.
        // Reading DataStore here is possible.
        
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1E1E1E)))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pomodoro",
                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "--:--", // Placeholder, requires State management tricky for this quick implementation
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
             Spacer(modifier = GlanceModifier.height(8.dp))
             Row {
                 // Open App Button
                 Text(
                     text = "Abrir",
                     style = TextStyle(color = ColorProvider(Color(0xFFBB86FC))),
                     modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                 )
             }
        }
    }
}
