package com.kien.movies.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Which Home element had focus last, and which card each row had focus on last, kept in Home's
 * saved state, with a requester per composed element so focus can go back to it.
 *
 * Why Home needs it: the shell restores focus through `saveFocusedChild`, which records only its
 * direct child. Home's element sits three focus groups deeper (Home, the vertical list, the row),
 * and those are new nodes when Home comes back, so the shell's restore would land on the first
 * card. Home's root sends focus on entry to the remembered element instead (AC N4), or to the
 * hero button on launch (AC N1). Each row sends focus on entry to its remembered card (AC H2).
 * Both run from `onEnter`, which Compose also calls for every group a direct request passes
 * through; they agree because both come from the same focus writes, and the outermost one wins.
 *
 * Writing [lastKey] on a focus move is a plain field write: it recomposes nothing.
 */
@Stable
class HomeFocusMemory(lastKey: String? = null, rowLastKeys: Map<String, String> = emptyMap()) {
    var lastKey: String? = lastKey
        private set

    private val rowLastKeys = rowLastKeys.toMutableMap()
    private val requesters = mutableMapOf<String, FocusRequester>()

    /** Focuses the element registered under [key]; false when it is not composed. */
    fun focus(key: String): Boolean = requesters[key]?.requestFocus(FocusDirection.Enter) == true

    /** Focuses the remembered element, else the hero button; false when neither is composed. */
    fun restore(): Boolean = lastKey?.let(::focus) == true || focus(HERO_FOCUS_KEY)

    /** Focuses the card the row [rowKey] had focus on last; false when there is none composed. */
    fun restoreRow(rowKey: String): Boolean = rowLastKeys[rowKey]?.let(::focus) == true

    internal fun register(key: String, requester: FocusRequester) {
        requesters[key] = requester
    }

    internal fun unregister(key: String, requester: FocusRequester) {
        if (requesters[key] === requester) requesters.remove(key)
    }

    internal fun focused(key: String, rowKey: String?) {
        lastKey = key
        if (rowKey != null) rowLastKeys[rowKey] = key
    }

    companion object {
        /** Saved as `[lastKey or "", row, card, row, card, ...]`, a list a Bundle can hold. */
        val Saver: Saver<HomeFocusMemory, ArrayList<String>> = Saver(
            save = { memory ->
                arrayListOf(memory.lastKey.orEmpty()).apply {
                    memory.rowLastKeys.forEach { (row, card) ->
                        add(row)
                        add(card)
                    }
                }
            },
            restore = { saved ->
                val rows = saved.drop(1).chunked(2).filter { pair -> pair.size == 2 }.associate { (row, card) -> row to card }
                HomeFocusMemory(saved.firstOrNull()?.takeIf(String::isNotEmpty), rows)
            }
        )
    }
}

/** Registers this element under [key] in [memory] and records when it gains focus, in [rowKey] too. */
@Composable
fun Modifier.rememberedFocus(memory: HomeFocusMemory, key: String, rowKey: String? = null): Modifier {
    val requester = remember { FocusRequester() }
    DisposableEffect(memory, key) {
        memory.register(key, requester)
        onDispose { memory.unregister(key, requester) }
    }
    return focusRequester(requester).onFocusChanged { state -> if (state.isFocused) memory.focused(key, rowKey) }
}

/** The key of a card: row and item, so one title in two rows has two keys. */
fun cardFocusKey(rowKey: String, itemKey: String): String = "$rowKey/$itemKey"

const val HERO_FOCUS_KEY = "hero"
