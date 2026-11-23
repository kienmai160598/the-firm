package com.kien.movies.domain.model

data class ServerItemRef(val connectionId: String, val itemId: String) {
    fun encode(): String = "$PREFIX$connectionId:${itemId.escapeSegment()}"

    companion object {
        private const val PREFIX = "srv1:"

        fun isServerId(id: String?): Boolean = id?.startsWith(PREFIX) == true

        fun parse(id: String?): ServerItemRef? {
            if (id == null || !id.startsWith(PREFIX)) return null
            val body = id.removePrefix(PREFIX)
            val separator = body.indexOf(':')
            if (separator <= 0 || separator == body.lastIndex) return null
            val itemId = body.substring(separator + 1).unescapeSegment() ?: return null
            return ServerItemRef(connectionId = body.substring(0, separator), itemId = itemId)
        }
    }
}

private fun String.escapeSegment(): String = replace("%", "%25").replace(":", "%3A")

private fun String.unescapeSegment(): String? {
    if (':' in this) return null
    return replace("%3A", ":", ignoreCase = true).replace("%25", "%")
}

data class ServerPlaybackTarget(val item: ServerItemRef, val mediaSourceId: String?) {
    fun key(): String = "${item.encode()}:${mediaSourceId.orEmpty()}"
}
