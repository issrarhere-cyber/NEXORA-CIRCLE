package com.nexora.circle.language

/** One Latin character and the Nexora-X symbol that represents it. */
data class Glyph(
    val latin: String,
    val symbol: String,
    val name: String = "",
)

/** The loaded language tables (alphabet and numbers). */
data class NexoraXTable(
    val glyphs: List<Glyph>,
    val digits: List<Glyph>,
    val warnings: List<String> = emptyList(),
) {
    val all: List<Glyph> get() = glyphs + digits
    val isEmpty: Boolean get() = all.isEmpty()
}

/** Result of translating text, including anything the table could not map. */
data class TranslationResult(
    val output: String,
    val unmapped: Set<String>,
)
