package pl.bkacala.fiszkide.data

data class VocabularyEntry(val german: String, val polish: String)

/** Reads UTF-8 CSV with a BOM, optional header, quoted fields and embedded newlines. */
object VocabularyCsv {
    fun parse(text: String): List<VocabularyEntry> {
        val input = text.removePrefix("\uFEFF")
        val firstLine = input.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (';' in firstLine) ';' else ','
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var quoteClosed = false
        var index = 0

        fun finishField() {
            row.add(field.toString().trim())
            field.clear()
            quoteClosed = false
        }
        fun finishRow() {
            finishField()
            if (row.any { it.isNotBlank() }) rows.add(row.toList())
            row = mutableListOf()
        }
        while (index < input.length) {
            val char = input[index]
            when {
                quoted && char == '"' -> {
                    if (input.getOrNull(index + 1) == '"') {
                        field.append('"')
                        index++
                    } else {
                        quoted = false
                        quoteClosed = true
                    }
                }
                quoted -> field.append(char)
                char == delimiter -> finishField()
                char == '\n' || char == '\r' -> {
                    finishRow()
                    if (char == '\r' && input.getOrNull(index + 1) == '\n') index++
                }
                char == '"' && field.isBlank() && !quoteClosed -> {
                    field.clear()
                    quoted = true
                }
                quoteClosed -> require(char.isWhitespace()) {
                    "Niepoprawny CSV: tekst po zamknięciu cudzysłowu (rekord ${rows.size + 1})."
                }
                else -> field.append(char)
            }
            index++
        }
        require(!quoted) { "Niepoprawny CSV: niedomknięty cudzysłów." }
        if (field.isNotEmpty() || row.isNotEmpty() || quoteClosed) finishRow()
        val header = rows.firstOrNull()?.map { it.lowercase() }
        val hasHeader = header?.size == 2 &&
            header[0] in setOf("deutsch", "niemiecki", "german", "de") &&
            header[1] in setOf("polski", "polish", "pl")
        val entries = rows.drop(if (hasHeader) 1 else 0).mapIndexed { rowIndex, values ->
            require(values.size == 2 && values.all { it.isNotBlank() }) {
                "Niepoprawny CSV: rekord ${rowIndex + if (hasHeader) 2 else 1} musi mieć dwie niepuste kolumny."
            }
            VocabularyEntry(german = values[0], polish = values[1])
        }
        require(entries.isNotEmpty()) { "Plik slowka.csv nie zawiera żadnych fraz." }
        return entries
    }
}
