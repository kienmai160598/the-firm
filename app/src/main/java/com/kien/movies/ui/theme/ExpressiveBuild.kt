package com.kien.movies.ui.theme

import com.kien.movies.core.build.BoxBuild
import com.kien.movies.domain.model.AppTheme

// Fixed per build from its default theme, not read from the composition, so tokens that code
// reads outside one (radii, component sizes) can follow it too.
internal val isExpressiveBuild: Boolean = BoxBuild.appTheme == AppTheme.EXPRESSIVE
