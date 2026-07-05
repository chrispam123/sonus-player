package com.sonus.player.domain.model

data class EqPreset(
    val name: String,
    val bands: List<Float>
) {
    companion object {
        val ROCK = EqPreset("Rock", listOf(10f, 4f, -2f, 5f, 7f))
        val POP = EqPreset("Pop", listOf(4f, 2f, 3f, 5f, 8f))
        val CLASSICAL = EqPreset("Classical", listOf(-2f, 0f, 2f, 4f, 5f))
        val JAZZ = EqPreset("Jazz", listOf(5f, 2f, -2f, 3f, 2f))
        val FLAT = EqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f))

        val ALL = listOf(ROCK, POP, CLASSICAL, JAZZ, FLAT)
    }
}
