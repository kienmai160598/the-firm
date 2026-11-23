package com.kien.movies.addons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.CardSize
import com.kien.movies.design.Focus
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles

/**
 * The left column of Addons (tvOS AddonInstallPanel.swift): what addons are, a manifest URL field,
 * Install with its result, and "Add from phone" (Android only, docs/android-tv-rebuild.md, 5b).
 * Scrolls with focus when the result needs more room than the column has.
 */
@Composable
fun AddonInstallPanel(
    state: AddonsUiState,
    onInstall: (String) -> Unit,
    onAddFromPhone: () -> Unit,
    fieldFocus: FocusRequester,
    phoneButtonFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    var manifestUrl by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.installStatus) {
        if (state.installStatus is AddonInstallStatus.Installed) manifestUrl = ""
    }
    val install = { onInstall(manifestUrl) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(PANEL_SPACING)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_addons),
            contentDescription = null,
            tint = Palette.textSecondary,
            modifier = Modifier.size(SYMBOL_SIZE)
        )
        Text(text = "Addons", style = MaterialTheme.typography.headlineSmall)
        Text(text = EXPLANATION, style = CALLOUT, color = Palette.textSecondary)
        Text(text = "Add an addon", style = HEADLINE, modifier = Modifier.padding(top = HEADER_TOP))
        ManifestUrlField(value = manifestUrl, onValueChange = { manifestUrl = it }, onSubmit = install, focusRequester = fieldFocus)
        Button(
            onClick = install,
            enabled = !state.isInstalling && manifestUrl.isNotBlank(),
            colors = GlassStyle.prominentButtonColors(),
            scale = GlassStyle.buttonScale(),
            shape = GlassStyle.buttonShape(),
            modifier = Modifier.testTag(INSTALL_BUTTON_TAG)
        ) {
            if (state.isInstalling) {
                Text(text = "Installing", style = TextStyles.button)
            } else {
                Icon(painter = painterResource(R.drawable.ic_addons_plus), contentDescription = null, modifier = Modifier.size(BUTTON_ICON))
                Spacer(Modifier.size(Spacing.textGap))
                Text(text = "Install", style = TextStyles.button)
            }
        }
        Button(
            onClick = onAddFromPhone,
            colors = GlassStyle.buttonColors(),
            scale = GlassStyle.buttonScale(),
            shape = GlassStyle.buttonShape(),
            modifier = Modifier.focusRequester(phoneButtonFocus).testTag(PHONE_BUTTON_TAG)
        ) {
            Icon(painter = painterResource(R.drawable.ic_addons_phone), contentDescription = null, modifier = Modifier.size(BUTTON_ICON))
            Spacer(Modifier.size(Spacing.textGap))
            Text(text = "Add from phone", style = TextStyles.button)
        }
        StatusLine(state.installStatus)
    }
}

/**
 * The manifest URL field. A text field that takes D-pad focus opens the keyboard at once and
 * keeps the arrows, so the field rests as a focusable surface; Select starts editing (system
 * keyboard), Done installs, Back leaves editing. Either way focus returns to the surface.
 */
@Composable
private fun ManifestUrlField(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit, focusRequester: FocusRequester) {
    var isEditing by remember { mutableStateOf(false) }
    var refocusTick by remember { mutableIntStateOf(0) }
    var textHadFocus by remember { mutableStateOf(false) }
    val textFocus = remember { FocusRequester() }
    val stopEditing: () -> Unit = {
        isEditing = false
        refocusTick++
    }

    Box(modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).focusGroup()) {
        if (isEditing) {
            BackHandler(onBack = stopEditing)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyles.body.copy(color = Palette.textPrimary),
                cursorBrush = SolidColor(Palette.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onDone = {
                    stopEditing()
                    onSubmit()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FIELD_HEIGHT)
                    .focusRequester(textFocus)
                    .onFocusChanged { focus ->
                        // Called once unfocused on attach: only a real focus loss ends editing.
                        if (focus.hasFocus) {
                            textHadFocus = true
                        } else if (textHadFocus) {
                            textHadFocus = false
                            isEditing = false
                        }
                    }
                    .testTag(URL_FIELD_TAG),
                decorationBox = { field -> FieldFrame(isPlaceholder = value.isEmpty()) { field() } }
            )
            LaunchedEffect(Unit) {
                withFrameNanos {}
                textFocus.requestFocus()
            }
        } else {
            Surface(
                onClick = { isEditing = true },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Palette.frostRest,
                    contentColor = Palette.textPrimary,
                    focusedContainerColor = Palette.focusedContainer,
                    focusedContentColor = Palette.focusedContent
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = Focus.buttonScale),
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(CardSize.cornerRadius)),
                modifier = Modifier.fillMaxWidth().height(FIELD_HEIGHT).testTag(URL_SURFACE_TAG)
            ) {
                Text(
                    text = value.ifEmpty { PLACEHOLDER },
                    style = TextStyles.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = LocalContentColor.current.copy(alpha = if (value.isEmpty()) PLACEHOLDER_ALPHA else 1f),
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = FIELD_PADDING)
                )
            }
        }
    }
    LaunchedEffect(refocusTick) {
        if (refocusTick == 0) return@LaunchedEffect
        withFrameNanos {}
        focusRequester.requestFocus()
    }
}

@Composable
private fun FieldFrame(isPlaceholder: Boolean, field: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FIELD_HEIGHT)
            .background(Palette.frostRaised, RoundedCornerShape(CardSize.cornerRadius))
            .padding(horizontal = FIELD_PADDING),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isPlaceholder) Text(text = PLACEHOLDER, style = TextStyles.body, color = Palette.textTertiary)
        field()
    }
}

@Composable
private fun StatusLine(status: AddonInstallStatus?) {
    when (status) {
        is AddonInstallStatus.Installed -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.textGap), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_addons_check), contentDescription = null, tint = Palette.textSecondary, modifier = Modifier.size(STATUS_ICON))
            Text(text = "Installed ${status.name}", style = CALLOUT, color = Palette.textSecondary)
        }

        is AddonInstallStatus.Failed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.textGapSmall)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.textGap), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ic_addons_warning), contentDescription = null, tint = ERROR_RED, modifier = Modifier.size(STATUS_ICON))
                Text(text = INSTALL_FAILED, style = CALLOUT, color = ERROR_RED)
            }
            Text(text = status.message, style = TextStyles.small, color = Palette.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        AddonInstallStatus.Installing, null -> Unit
    }
}

const val URL_SURFACE_TAG = "addon-url"
const val URL_FIELD_TAG = "addon-url-field"
const val INSTALL_BUTTON_TAG = "addon-install"
const val PHONE_BUTTON_TAG = "addon-add-from-phone"
const val INSTALL_FAILED = "Couldn't add this addon"

private const val EXPLANATION = "Addons bring the catalogs, streams and subtitles you see in the app."
private const val PLACEHOLDER = "Manifest URL"
private const val PLACEHOLDER_ALPHA = 0.45f
private val PANEL_SPACING = 14.dp
private val HEADER_TOP = 10.dp
private val SYMBOL_SIZE = 48.dp
private val BUTTON_ICON = 18.dp
private val STATUS_ICON = 16.dp
private val FIELD_HEIGHT = 40.dp
private val FIELD_PADDING = 14.dp
