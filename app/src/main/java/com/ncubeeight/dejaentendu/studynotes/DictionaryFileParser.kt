package com.ncubeeight.dejaentendu.studynotes

/**
 * Understands two source layouts for a dictionary file the user connects
 * from Files (see ConnectLocalDictionaryScreen), mirroring iOS's
 * ConnectLocalDictionaryView.parse:
 * - A plain two-column word list (term/definition per line, separated by
 *   a tab, comma, or dash) — typical of a simple word-list export.
 * - A prose dictionary PDF or text file laid out as one entry per line —
 *   "headword (variant) [etymology] : definition." — where the
 *   definition may wrap across several lines until the next headword.
 *   This is the layout of real compiled dictionaries (verified against a
 *   Mauritian-English Dictionary PDF export on iOS).
 */
object DictionaryFileParser {
    data class ParsedEntry(val term: String, val definition: String)

    /** Decides which layout the file is in by sampling its first non-empty lines, then parses accordingly. */
    fun parse(text: String): List<ParsedEntry> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val sample = lines.take(20)
        val tabularHits = sample.count { splitColumns(it) != null }
        if (tabularHits >= maxOf(1, sample.size / 2)) {
            return lines.mapNotNull { line -> splitColumns(line)?.let { (term, definition) -> ParsedEntry(term, definition) } }
        }

        return parseHeadwordEntries(text)
    }

    /**
     * Parses a running-text dictionary where each entry starts with a
     * headword at the beginning of a line — optionally followed by a
     * parenthetical variant and/or a bracketed etymology — then a colon
     * introducing the definition, which may wrap across several lines
     * until the next headword.
     */
    private val headwordPattern = Regex(
        """^([\p{L}][\p{L}'’\-]{0,39})(?:\s*\([^)\n]{0,80}\))?(?:\s*\[[^]\n]{0,120}])?\s*:\s*""",
        setOf(RegexOption.MULTILINE),
    )

    private fun parseHeadwordEntries(text: String): List<ParsedEntry> {
        val matches = headwordPattern.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        val entries = mutableListOf<ParsedEntry>()
        for ((index, match) in matches.withIndex()) {
            val termGroup = match.groups[1] ?: continue
            val term = termGroup.value.trim()

            val definitionStart = match.range.last + 1
            val definitionEnd = if (index + 1 < matches.size) matches[index + 1].range.first else text.length
            if (definitionEnd <= definitionStart) continue

            val definition = text.substring(definitionStart, definitionEnd)
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" ")
                .trim()

            if (term.isEmpty() || definition.isEmpty()) continue
            entries.add(ParsedEntry(term, definition))
        }
        return entries
    }

    private fun splitColumns(line: String): Pair<String, String>? {
        twoColumns(line, "\t")?.let { return it }
        csvColumns(line)?.let { return it }
        twoColumns(line, " - ")?.let { return it }
        twoColumns(line, " | ")?.let { return it }
        twoColumns(line, "|")?.let { return it }
        return null
    }

    private fun twoColumns(line: String, delimiter: String): Pair<String, String>? {
        val parts = line.split(delimiter)
        if (parts.size != 2) return null
        val term = parts[0].trim()
        val definition = parts[1].trim()
        if (term.isEmpty() || definition.isEmpty()) return null
        return term to definition
    }

    /**
     * Minimal CSV split: honors double-quoted fields (so a definition
     * containing a comma doesn't get split apart) but doesn't attempt
     * escaped-quote ("") handling — good enough for the simple two-column
     * exports this layout targets.
     */
    private fun csvColumns(line: String): Pair<String, String>? {
        if (!line.contains(",")) return null
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())
        if (fields.size != 2) return null
        val term = fields[0].trim()
        val definition = fields[1].trim()
        if (term.isEmpty() || definition.isEmpty()) return null
        return term to definition
    }
}
