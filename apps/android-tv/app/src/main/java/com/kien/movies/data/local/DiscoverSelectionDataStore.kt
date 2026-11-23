package com.kien.movies.data.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kien.movies.core.profile.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DiscoverSelectionDataStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    private companion object {
        const val FEATURE = "discover_selection"
        val selectedCatalogKey = stringPreferencesKey("selected_catalog_key")
    }

    private fun store() = factory.get(profileManager.activeProfileId.value, FEATURE)

    suspend fun getSelectedCatalogKey(): String? = store().data.first()[selectedCatalogKey]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    suspend fun setSelectedCatalogKey(catalogKey: String) {
        val normalized = catalogKey.trim()
        if (normalized.isEmpty()) return
        store().edit { preferences ->
            preferences[selectedCatalogKey] = normalized
        }
    }
}
