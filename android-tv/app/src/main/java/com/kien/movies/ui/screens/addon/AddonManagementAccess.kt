package com.kien.movies.ui.screens.addon

import com.kien.movies.core.server.AddonWebConfigMode
import com.kien.movies.domain.model.ExperienceMode
import com.kien.movies.domain.model.UserProfile

internal object AddonManagementAccess {

    fun isReadOnly(profile: UserProfile?): Boolean {
        return profile?.let { !it.isPrimary && it.usesPrimaryAddons } == true
    }

    fun webConfigMode(
        profile: UserProfile?,
        experienceMode: ExperienceMode = ExperienceMode.ADVANCED
    ): AddonWebConfigMode {
        return when {
            isReadOnly(profile) -> AddonWebConfigMode.COLLECTIONS_ONLY
            experienceMode == ExperienceMode.ESSENTIAL -> AddonWebConfigMode.ADDONS_ONLY
            else -> AddonWebConfigMode.FULL
        }
    }
}
