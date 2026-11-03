package com.nexora.circle.language

/**
 * Character-level translator driven entirely by the loaded table.
 * Characters without a table entry are kept unchanged and reported as unmapped.
 */
class NexoraXTranslator(private val table: NexoraXTable) {

    private val toNexora: Map<String, String> =
        table.all.associate { it.latin.lowercase() to it.symbol }

    private val toLatin: Map<String, String> =
        table.all.associate { it.symbol to it.latin }

    private val longestSymbol: Int = table.all.maxOfOrNull { it.symbol.length } ?: 1

    fun englishToNexora(input: String): TranslationResult {
        val unmapped = linkedSetOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            val ch = String(Character.toChars(cp))
            i += ch.length
            val mapped = toNexora[ch.lowercase()]
            if (mapped != null) {
                sb.append(mapped)
            } else {
                sb.append(ch)
                if (!ch.isBlank() && !isPlainPunctuation(ch)) unmapped += ch
            }
        }
        return TranslationResult(sb.toString(), unmapped)
    }

    fun nexoraToEnglish(input: String): TranslationResult {
        val unmapped = linkedSetOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            var matched = false
            val maxLen = minOf(longestSymbol, input.length - i)
            // Prefer the longest matching symbol so multi-character symbols work.
            for (len in maxLen downTo 1) {
                val piece = input.substring(i, i + len)
                val latin = toLatin[piece]
                if (latin != null) {
                    sb.append(latin)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                val cp = input.codePointAt(i)
                val ch = String(Character.toChars(cp))
                sb.append(ch)
                if (!ch.isBlank() && !isPlainPunctuation(ch)) unmapped += ch
                i += ch.length
            }
        }
        return TranslationResult(sb.toString(), unmapped)
    }

    private fun isPlainPunctuation(ch: String): Boolean =
        ch.length == 1 && ch[0] in ".,;:!?'\"()-_/\\@#%&*+=<>[]{}|~`^$"
}
