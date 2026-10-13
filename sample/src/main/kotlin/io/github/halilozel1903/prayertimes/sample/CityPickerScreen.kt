package io.github.halilozel1903.prayertimes.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.TurkeyProvinces

/**
 * The 81 provinces in Turkish alphabetical order with search. Search ignores case and Turkish letters
 * (`TurkeyProvinces.search`): "izmir" finds İzmir, "sanliurfa" finds Şanlıurfa and "06" finds Ankara.
 */
@Composable
fun CityPickerScreen(
    selected: Province,
    onSelect: (Province) -> Unit,
    onBack: (() -> Unit)?,
    header: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { TurkeyProvinces.search(query) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 4.dp else 20.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                val back = stringResource(R.string.back)
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = back }) {
                    BackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp))
                }
            }
            Text(
                stringResource(R.string.city_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { SearchIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        val clear = stringResource(R.string.clear_search)
                        IconButton(onClick = { query = "" }, modifier = Modifier.semantics { contentDescription = clear }) {
                            CloseIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(18.dp))
                        }
                    }
                } else {
                    null
                },
            )
            header?.invoke()
            Text(
                pluralStringResource(R.plurals.province_count, results.size, results.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    stringResource(R.string.no_results, query),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.plate }) { province ->
                    ProvinceRow(province, isSelected = province == selected, onClick = { onSelect(province) })
                }
            }
        }
    }
}

@Composable
private fun ProvinceRow(province: Province, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(if (isSelected) colors.primary else colors.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                province.plateCode,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(16.dp))
        Text(
            province.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) CheckIcon(colors.primary, Modifier.size(22.dp))
    }
}
