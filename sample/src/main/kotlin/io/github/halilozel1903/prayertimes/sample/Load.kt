package io.github.halilozel1903.prayertimes.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.prayertimes.PrayerTimesException
import kotlinx.coroutines.CancellationException

/** The state of something loading from Aladhan. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>

    /** [apiReason] is Aladhan's own message for an API error, `null` for a network error. */
    data class Failed(val message: String, val apiReason: String?) : Load<Nothing>
}

/** Runs [block] and wraps its result or the error. */
suspend fun <T> load(block: suspend () -> T): Load<T> = try {
    Load.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: PrayerTimesException.Api) {
    Load.Failed(e.message ?: "API error", e.reason)
} catch (e: PrayerTimesException) {
    Load.Failed(e.message ?: "Network error", null)
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(state: Load.Failed, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            if (state.apiReason != null) stringResource(R.string.error_api, state.apiReason) else stringResource(R.string.error_network),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
