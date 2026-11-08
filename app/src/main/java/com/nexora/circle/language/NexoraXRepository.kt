package com.nexora.circle.language

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the Nexora-X tables from the JSON files in the assets/nexora_x folder.
 * The language lives only in those files; no symbol is hard-coded here.
 */
object NexoraXRepository {

    private const val ALPHABET_FILE = "nexora_x/alphabet.json"
    private const val NUMBERS_FILE = "nexora_x/numbers.json"

    fun load(context: Context): NexoraXTable {
        val warnings = mutableListOf<String>()
        val glyphs = readList(context, ALPHABET_FILE, "glyphs", warnings)
        val digits = readList(context, NUMBERS_FILE, "digits", warnings)
        return NexoraXTable(glyphs, digits, warnings)
    }

    private fun readList(
        context: Context,
        file: String,
        key: String,
        warnings: MutableList<String>,
    ): List<Glyph> {
        val text = try {
            context.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            warnings += "Could not read $file: ${e.message}"
            return emptyList()
        }

        val array: JSONArray = try {
            JSONObject(text).optJSONArray(key) ?: JSONArray()
        } catch (e: Exception) {
            warnings += "$file is not valid JSON: ${e.message}"
            return emptyList()
        }

        val seen = mutableSetOf<String>()
        val result = mutableListOf<Glyph>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            if (obj == null) {
                warnings += "$file entry #$i is not an object"
                continue
            }
            val latin = obj.optString("latin", "")
            val symbol = obj.optString("symbol", "")
            if (latin.isEmpty() || symbol.isEmpty()) {
                warnings += "$file entry #$i needs both \"latin\" and \"symbol\""
                continue
            }
            if (!seen.add(latin)) {
                warnings += "$file has a duplicate entry for \"$latin\" (first one kept)"
                continue
            }
            result += Glyph(latin, symbol, obj.optString("name", ""))
        }
        return result
    }
}
