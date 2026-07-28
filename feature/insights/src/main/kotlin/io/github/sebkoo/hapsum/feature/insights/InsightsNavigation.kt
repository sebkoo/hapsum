package io.github.sebkoo.hapsum.feature.insights

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The insights feature's exported back-stack key — saveable from day one (ADR-0004). */
@Serializable
data object InsightsNavKey : NavKey

/**
 * The feature's exported Nav3 entry. `:app` owns the back stack and assembles the graph from
 * entries like this one — features never depend on each other (ADR-0004). `InsightsViewModel`
 * resolves its own dependencies through Hilt (ADR-0005). No effects to collect: insights
 * originates no navigation, and back is system back.
 */
fun EntryProviderScope<NavKey>.insightsEntry() {
    entry<InsightsNavKey> {
        val viewModel = hiltViewModel<InsightsViewModel>()
        InsightsScreen(viewModel = viewModel)
    }
}
