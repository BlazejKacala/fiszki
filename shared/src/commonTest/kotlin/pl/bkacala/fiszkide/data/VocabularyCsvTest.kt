package pl.bkacala.fiszkide.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VocabularyCsvTest {
    @Test
    fun readsBomHeaderPolishCharactersAndBlankLines() {
        assertEquals(
            listOf(VocabularyEntry("die Münze /n", "moneta"), VocabularyEntry("bar bezahlen", "płacić gotówką")),
            VocabularyCsv.parse("\uFEFFdeutsch;polski\r\ndie Münze /n;moneta\r\n\r\nbar bezahlen;płacić gotówką\r\n"),
        )
    }

    @Test
    fun readsQuotedDelimitersEscapedQuotesAndNewlines() {
        assertEquals(
            listOf(VocabularyEntry("ja; klar", "tak, \"jasne\"\nnaprawdę")),
            VocabularyCsv.parse("deutsch;polski\n\"ja; klar\";\"tak, \"\"jasne\"\"\nnaprawdę\""),
        )
    }

    @Test
    fun acceptsCommaDelimiterAndNoHeader() {
        assertEquals(listOf(VocabularyEntry("Hallo", "Cześć")), VocabularyCsv.parse("Hallo,Cześć"))
    }

    @Test
    fun rejectsIncompleteAndMalformedVocabulary() {
        listOf("deutsch;polski\n", "Hallo;", "Hallo;Cześć;extra", "\"Hallo;Cześć", "\"Hallo\"x;Cześć").forEach {
            assertFailsWith<IllegalArgumentException> { VocabularyCsv.parse(it) }
        }
    }
}
