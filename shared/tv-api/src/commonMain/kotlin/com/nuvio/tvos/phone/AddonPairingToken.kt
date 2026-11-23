package com.nuvio.tvos.phone

/**
 * The secret in the QR code of an "Add from phone" session (docs/android-tv-rebuild.md, 5b):
 * 128 random bits as 32 hex characters, new for each session. Whoever can read the TV screen can
 * propose addons; nobody else can.
 *
 * The random bytes come from the platform (`SecureRandom` on Android), so this file stays plain
 * Kotlin for both apps.
 */
class AddonPairingToken private constructor(val value: String) {
    /**
     * True when [candidate] is this token. Constant time over the token's length, so the answer
     * time does not tell how many leading characters were right.
     */
    fun matches(candidate: String?): Boolean {
        if (candidate == null) return false
        var difference = value.length xor candidate.length
        for (index in value.indices) {
            val other = if (index < candidate.length) candidate[index].code else 0
            difference = difference or (value[index].code xor other)
        }
        return difference == 0
    }

    override fun toString(): String = "AddonPairingToken(hidden)"

    companion object {
        const val BYTE_COUNT = 16

        fun fromRandomBytes(bytes: ByteArray): AddonPairingToken {
            require(bytes.size == BYTE_COUNT) { "A pairing token needs $BYTE_COUNT random bytes, got ${bytes.size}" }
            return AddonPairingToken(bytes.joinToString(separator = "") { byte -> byte.toHex() })
        }

        private fun Byte.toHex(): String {
            val unsigned = toInt() and 0xFF
            return HEX[unsigned shr 4].toString() + HEX[unsigned and 0x0F]
        }

        private const val HEX = "0123456789abcdef"
    }
}
