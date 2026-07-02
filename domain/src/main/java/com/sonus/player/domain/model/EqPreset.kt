package com.sonus.player.domain.model

data class EqPreset(
    val name: String,
    val bands: List<Float>
) {
    companion object {
        val ROCK = EqPreset("Rock", listOf(6f, 0f, 0f, 3f, 0f))
        val POP = EqPreset("Pop", listOf(4f, 0f, 0f, 0f, 4f))
        val CLASSICAL = EqPreset("Classical", listOf(0f, 0f, 0f, 0f, 0f))
        val JAZZ = EqPreset("Jazz", listOf(3f, 0f, 0f, 2f, 0f))
        val FLAT = EqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f))

        val ALL = listOf(ROCK, POP, CLASSICAL, JAZZ, FLAT)
    }
}
