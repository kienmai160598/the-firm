package com.kien.movies.shell

import androidx.lifecycle.ViewModel

/**
 * Holds the [Router] and the per entry ViewModel stores at Activity scope, so the back stack
 * survives a configuration change without a saver for routes such as [AppRoute.Streams]. A
 * process restart starts again at Home.
 */
class ShellViewModel : ViewModel() {
    val router = Router()
    val stores = RouteViewModelStores()

    override fun onCleared() {
        stores.clearAll()
    }
}
