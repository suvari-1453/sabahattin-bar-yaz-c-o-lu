package com.example

import java.lang.StringBuilder

/**
 * Cleans the input text by removing markdown formatting, punctuation signs/symbols, 
 * and emojis/icons before passing to TextToSpeech to prevent TTS from reading them aloud.
 */
fun cleanTextForTts(text: String?): String {
    if (text == null || text.isEmpty()) return ""
    
    // 1. Remove markdown symbols
    var cleaned = text
        .replace("\\*+".toRegex(), "")
        .replace("_+".toRegex(), "")
        .replace("#+".toRegex(), "")
        .replace("`+".toRegex(), "")
        .replace("~+".toRegex(), "")
        .replace("<+".toRegex(), "")
        .replace(">+".toRegex(), "")
        .replace("\\[".toRegex(), "")
        .replace("\\]".toRegex(), "")
        .replace("\\{".toRegex(), "")
        .replace("\\}".toRegex(), "")

    val sb = StringBuilder()
    var i = 0
    while (i < cleaned.length) {
        val codePoint = cleaned.codePointAt(i)
        val charCount = Character.charCount(codePoint)
        val type = Character.getType(codePoint)
        
        // Skip emojis, symbols, and formatting characters
        // Character types:
        // SURROGATE = 19
        // OTHER_SYMBOL = 28
        // MATH_SYMBOL = 14
        // MODIFIER_SYMBOL = 27
        // PRIVATE_USE = 18
        val shouldSkip = when (type.toByte()) {
            Character.SURROGATE,
            Character.PRIVATE_USE,
            Character.OTHER_SYMBOL,
            Character.MODIFIER_SYMBOL -> true
            else -> false
        }
        
        if (!shouldSkip) {
            val ch = cleaned[i]
            // Skip other specific non-speech punctuation symbols
            if (ch != '@' && ch != '#' && ch != '*' && ch != '^' && ch != '~' && ch != '|' && ch != '\\' && ch != '/' && ch != '<' && ch != '>' && ch != '=' && ch != '+') {
                sb.append(cleaned.substring(i, i + charCount))
            }
        }
        i += charCount
    }
    
    // Normalize spacing and return
    return sb.toString().replace("\\s+".toRegex(), " ").trim()
}
