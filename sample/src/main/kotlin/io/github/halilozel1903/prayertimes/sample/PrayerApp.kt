package io.github.halilozel1903.prayertimes.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.TurkeyProvinces
import io.github.halilozel1903.prayertimes.sample.reminders.Reminders
import io.github.halilozel1903.prayertimes.sample.widget.PrayerWidget
import kotlinx.coroutines.launch

/**
 * The whole app. Phones get a bottom bar (Today, Month, Qibla, Settings) and a full screen province picker; on wide
 * screens a navigation rail, the picker and the selected tab sit side by side. Screenshot [scene]s start on a
 * fixed province and tab and report to a [SceneMonitor] when their live data has loaded.
 */
@Composable
fun PrayerApp(scene: Scene?, graph: AppGraph) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var plate by rememberSaveable { mutableIntStateOf(scene?.plate ?: graph.settings.plate) }
    var tab by rememberSaveable { mutableStateOf(scene?.tab ?: Tab.Today) }
    var pickerOpen by rememberSaveable { mutableStateOf(scene?.pickerOpen ?: false) }
    val province = TurkeyProvinces.byPlate(plate)

    val select: (Province) -> Unit = { chosen ->
        plate = chosen.plate
        pickerOpen = false
        // Scenes never touch the user's own choice.
        if (scene == null) {
            graph.settings.plate = chosen.plate
            scope.launch {
                runCatching { Reminders.refresh(context) }
                PrayerWidget.refresh(context)
            }
        }
    }

    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 840.dp) {
                WideLayout(
                    province = province,
                    tab = tab,
                    onTabChange = { tab = it },
                    onSelect = select,
                    graph = graph,
                    allowCompass = scene == null,
                )
            } else if (pickerOpen) {
                BackHandler { pickerOpen = false }
                CityPickerScreen(
                    selected = province,
                    onSelect = select,
                    onBack = { pickerOpen = false },
                    header = { SelectedSummary(province, graph.repository) },
                    modifier = Modifier.safeDrawingPadding(),
                )
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    icon = { TabIcon(item, MaterialTheme.colorScheme.onSurface) },
                                    label = { Text(stringResource(item.label)) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    TabContent(
                        tab = tab,
                        province = province,
                        graph = graph,
                        onChangeCity = { pickerOpen = true },
                        allowCompass = scene == null,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun WideLayout(
    province: Province,
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    onSelect: (Province) -> Unit,
    graph: AppGraph,
    allowCompass: Boolean,
) {
    Row(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        NavigationRail(containerColor = Color.Transparent) {
            Tab.entries.forEach { item ->
                NavigationRailItem(
                    selected = tab == item,
                    onClick = { onTabChange(item) },
                    icon = { TabIcon(item, MaterialTheme.colorScheme.onSurface) },
                    label = { Text(stringResource(item.label)) },
                )
            }
        }
        CityPickerScreen(
            selected = province,
            onSelect = onSelect,
            onBack = null,
            header = null,
            modifier = Modifier.width(360.dp),
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        TabContent(
            tab = tab,
            province = province,
            graph = graph,
            onChangeCity = null,
            allowCompass = allowCompass,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabContent(
    tab: Tab,
    province: Province,
    graph: AppGraph,
    onChangeCity: (() -> Unit)?,
    allowCompass: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (tab) {
            Tab.Today -> TodayScreen(province, graph.repository, onChangeCity)
            Tab.Month -> MonthScreen(province, graph.repository)
            Tab.Qibla -> QiblaScreen(province, graph.repository, allowCompass)
            Tab.Settings -> SettingsScreen(province, graph)
        }
    }
}

private val Tab.label: Int
    get() = when (this) {
        Tab.Today -> R.string.tab_today
        Tab.Month -> R.string.tab_month
        Tab.Qibla -> R.string.tab_qibla
        Tab.Settings -> R.string.tab_settings
    }

@Composable
private fun TabIcon(tab: Tab, color: Color) {
    val modifier = Modifier.size(24.dp)
    when (tab) {
        Tab.Today -> ClockIcon(color, modifier)
        Tab.Month -> CalendarIcon(color, modifier)
        Tab.Qibla -> CompassIcon(color, modifier)
        Tab.Settings -> SettingsIcon(color, modifier)
    }
}
