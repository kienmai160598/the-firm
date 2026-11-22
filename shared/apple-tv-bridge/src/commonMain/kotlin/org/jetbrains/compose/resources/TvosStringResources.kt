package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable

class StringResource(val key: String, val englishText: String)

class PluralStringResource(val key: String, val englishOne: String, val englishOther: String)

private val positionalArgument = Regex("""%(\d+)\$[sd]""")
private val sequentialArgument = Regex("""%[sd]""")

private fun formatEnglish(template: String, formatArgs: Array<out Any>): String {
    val positional = positionalArgument.replace(template) { match ->
        formatArgs.getOrNull(match.groupValues[1].toInt() - 1)?.toString() ?: match.value
    }
    var nextIndex = 0
    return sequentialArgument.replace(positional) { match ->
        formatArgs.getOrNull(nextIndex++)?.toString() ?: match.value
    }
}

private fun PluralStringResource.englishFor(quantity: Int): String =
    if (quantity == 1) englishOne else englishOther

suspend fun getString(resource: StringResource): String = resource.englishText

suspend fun getString(resource: StringResource, vararg formatArgs: Any): String =
    formatEnglish(resource.englishText, formatArgs)

suspend fun getPluralString(resource: PluralStringResource, quantity: Int): String =
    resource.englishFor(quantity)

suspend fun getPluralString(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String = formatEnglish(resource.englishFor(quantity), formatArgs)

@Composable
fun stringResource(resource: StringResource): String = resource.englishText

@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String =
    formatEnglish(resource.englishText, formatArgs)

@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String =
    resource.englishFor(quantity)

@Composable
fun pluralStringResource(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String = formatEnglish(resource.englishFor(quantity), formatArgs)
