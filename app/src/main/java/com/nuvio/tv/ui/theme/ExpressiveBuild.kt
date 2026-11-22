package com.nuvio.tv.ui.theme

import com.nuvio.tv.core.build.BoxBuild
import com.nuvio.tv.domain.model.AppTheme

// Fixed per build from its default theme, not read from the composition, so tokens that code
// reads outside one (radii, component sizes) can follow it too.
internal val isExpressiveBuild: Boolean = BoxBuild.appTheme == AppTheme.EXPRESSIVE
