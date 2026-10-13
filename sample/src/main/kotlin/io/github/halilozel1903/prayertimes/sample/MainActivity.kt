package io.github.halilozel1903.prayertimes.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Prayer times for Turkey's 81 provinces, in Turkish or English, built on prayer-times-kotlin. It is an
 * [AppCompatActivity] so the in-app language switch ([AppLanguage]) also works below Android 13.
 *
 * `scripts/screenshots.sh` starts it with `--es scene <scene>` (see [Scene]) to open a fixed province, language
 * and screen, loaded live from api.aladhan.com like a normal launch.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))

        if (scene != null && scene.language.tag != null) {
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (!current.startsWith(scene.language.tag)) {
                // Switch the language the same way the settings screen does. The activity is recreated in the new
                // language, so this instance shows nothing.
                Log.i(SceneMonitor.TAG, "Switching to ${scene.language.tag} for ${scene.key}")
                AppLanguage.apply(scene.language)
                return
            }
        }

        setContent {
            PrayerTimesTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    PrayerApp(scene = scene, graph = applicationContext.graph)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
