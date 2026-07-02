package com.sonus.player.domain.model

sealed class CoverArtResult {
    data class Embedded(val bytes: ByteArray) : CoverArtResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Embedded) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class Remote(val url: String) : CoverArtResult()
    data class Generated(val colors: List<Int>) : CoverArtResult()
    data object NotFound : CoverArtResult()
}
