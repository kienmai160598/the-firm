package com.kien.movies

import android.app.Application
import android.util.Log
import com.nuvio.tvos.TvosHome
import com.nuvio.tvos.android.TvRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Whether the bridge storages are ready; nothing may call a Tvos* API before [Ready]. */
sealed interface RuntimeState {
    data object Starting : RuntimeState

    data object Ready : RuntimeState

    data class Failed(val message: String) : RuntimeState
}

class MoviesApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow<RuntimeState>(RuntimeState.Starting)

    val runtimeState: StateFlow<RuntimeState> = state.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        scope.launch { start() }
    }

    // SharedPreferences loads read disk, so they run off the main thread; TvosHome.start
    // launches on the main scope and must only run once every storage has its Context.
    private suspend fun start() {
        val initResult = withContext(Dispatchers.IO) {
            runCatching { TvRuntime.init(this@MoviesApp) }
        }
        state.value = initResult.fold(
            onSuccess = {
                TvosHome.start()
                RuntimeState.Ready
            },
            onFailure = { error ->
                Log.e(TAG, "TvRuntime.init failed", error)
                RuntimeState.Failed(error.message ?: error.javaClass.simpleName)
            },
        )
    }

    private companion object {
        const val TAG = "MoviesApp"
    }
}
