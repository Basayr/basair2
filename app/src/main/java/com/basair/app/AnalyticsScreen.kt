package com.basair.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer

private data class QuranStats(
    val verses: Int, val words: Int, val letters: Int,
    val dottedLetters: Int, val undottedLetters: Int, val dots: Int,
    val daggerAlifs: Int, val repeatedLetters: Int, val repeatedWords: Int,
    val unclassifiedDotLetters: Int
)
private data class PhraseStat(val phrase: String, val count: Int, val places: List<String>)

private fun isArabicLetter(ch: Char): Boolean {
    val type = Character.getType(ch)
    return Character.UnicodeBlock.of(ch) in setOf(
        Character.UnicodeBlock.ARABIC,
        Character.UnicodeBlock.ARABIC_SUPPLEMENT,
        Character.UnicodeBlock.ARABIC_EXTENDED_A,
        Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
        Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
    ) && type in intArrayOf(
        Character.UPPERCASE_LETTER.toInt(), Character.LOWERCASE_LETTER.toInt(),
        Character.OTHER_LETTER.toInt(), Character.TITLECASE_LETTER.toInt(),
        Character.MODIFIER_LETTER.toInt()
    )
}

/* Count written Arabic letter code points as displayed; do not decompose hamza forms. */
private fun arabicBaseLetters(text: String): List<Char> =
    text.filter(::isArabicLetter).toList()

// U+0670 is a combining mark, so it is counted separately from base letters.
private fun daggerAlifCount(text: String): Int = text.count { it == '\u0670' }

/*
 * Dot counts for the standard Hafs/Madinah Arabic letter shapes.
 * null means an unclassified glyph; it must not be silently counted as dotless.
 */
private fun dotsForLetter(letter: Char): Int? = when (letter) {
    'ب', 'ج', 'خ', 'ذ', 'ز', 'ض', 'ظ', 'غ', 'ف', 'ن' -> 1
    'ت', 'ق', 'ي', 'ة', 'ئ' -> 2
    'ث', 'ش' -> 3
    'ء', 'ا', 'أ', 'إ', 'آ', 'ٱ', 'ؤ', 'د', 'ر', 'س', 'ص',
    'ط', 'ع', 'ح', 'م', 'ل', 'ه', 'و', 'ى', 'ك' -> 0
    else -> null
}

private fun wordKey(word: String): String {
    val noMarks = Normalizer.normalize(word, Normalizer.Form.NFD)
        .filter { Character.getType(it) !in intArrayOf(6, 7, 8) }
    return noMarks.replace(Regex("[۞۩ـ،؛؟!:.()\\[\\]{}«»\\u06DD]"), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ٱ', 'ا').replace('ى', 'ي')
}

private fun wordsOf(ayah: Ayah): List<String> =
    ayah.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

private fun statsFor(verses: List<Ayah>): QuranStats {
    val letters = verses.flatMap { arabicBaseLetters(it.text) }
    val classified = letters.mapNotNull { letter -> dotsForLetter(letter)?.let { letter to it } }
    val dotted = classified.count { it.second > 0 }
    val undotted = classified.count { it.second == 0 }
    val totalDots = classified.sumOf { it.second }
    val wordCounts = verses.flatMap { wordsOf(it) }.map { wordKey(it) }
        .filter { it.isNotBlank() }.groupingBy { it }.eachCount()
    val letterCounts = letters.groupingBy { it }.eachCount()
    return QuranStats(
        verses.size, verses.sumOf { wordsOf(it).size }, letters.size,
        dotted, undotted, totalDots,
        verses.sumOf { daggerAlifCount(it.text) },
        letterCounts.count { it.value > 1 },
        wordCounts.count { it.value > 1 },
        letters.size - classified.size
    )
}

private fun repeatedPhrases(verses: List<Ayah>): List<PhraseStat> {
    val found = linkedMapOf<String, Pair<String, MutableList<String>>>()
    verses.forEach { ayah ->
        val words = wordsOf(ayah)
        if (words.size < 3) return@forEach
        val seenInAyah = mutableSetOf<String>()
        for (i in 0..words.size - 3) {
            val original = words.subList(i, i + 3).joinToString(" ")
            val key = words.subList(i, i + 3).joinToString(" ") { wordKey(it) }
            if (key.length < 5 || !seenInAyah.add(key)) continue
            found.getOrPut(key) { original to mutableListOf() }.second
                .add("${surahNames[ayah.surah - 1]}، الآية ${ayah.number}")
        }
    }
    return found.values.filter { it.second.size > 1 }
        .map { PhraseStat(it.first, it.second.size, it.second.toList()) }
        .sortedByDescending { it.count }.take(30)
}

@Composable
fun AnalyticsScreen(ayat: List<Ayah>) {
    var scope by rememberSaveable { mutableIntStateOf(0) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(1) }
    var menuExpanded by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val activeAyat = remember(ayat, scope, selectedSurah) {
        if (scope == 0) ayat else ayat.filter { it.surah == selectedSurah }
    }
    val stats = remember(activeAyat) { statsFor(activeAyat) }
    val letterFrequency = remember(activeAyat) {
        activeAyat.flatMap { arabicBaseLetters(it.text) }.groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
    }
    val wordFrequency = remember(activeAyat) {
        activeAyat.flatMap { wordsOf(it) }.map { wordKey(it) }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount()
            .entries.filter { it.value > 1 }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(30)
    }
    val phrases = remember(activeAyat) { repeatedPhrases(activeAyat) }
    val occurrences = remember(activeAyat, search) {
        if (search.isBlank()) emptyList() else activeAyat.filter { it.text.contains(search, false) }
    }
    val green = MaterialTheme.colorScheme.primary
    val softSurface = MaterialTheme.colorScheme.surfaceVariant

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(
            "لوحة التدبر والإحصاء",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = scope == 0, onClick = { scope = 0 }, label = { Text("القرآن كاملاً") })
            FilterChip(selected = scope == 1, onClick = { scope = 1 }, label = { Text("سورة") })
            if (scope == 1) {
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text("${surahNames[selectedSurah - 1]} ▾")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        surahNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text("${index + 1}. $name") },
                                onClick = { selectedSurah = index + 1; menuExpanded = false }
                            )
                        }
                    }
                }
            }
        }
        if (ayat.isEmpty()) {
            Text("بيانات القرآن غير متاحة؛ تعذر حساب الإحصاءات.")
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = softSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                if (scope == 0) "الإحصاء العام للقرآن الكريم"
                                else "إحصاءات سورة ${surahNames[selectedSurah - 1]}",
                                style = MaterialTheme.typography.titleMedium,
                                color = green,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("عدد الآيات", stats.verses.toString(), Modifier.weight(1f))
                                StatCard("عدد الكلمات", stats.words.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("الحروف المكتوبة", stats.letters.toString(), Modifier.weight(1f))
                                StatCard("مجموع النقاط", stats.dots.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("حروف منقوطة", stats.dottedLetters.toString(), Modifier.weight(1f))
                                StatCard("حروف بلا نقاط", stats.undottedLetters.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("الألفات الخنجرية", stats.daggerAlifs.toString(), Modifier.weight(1f))
                                StatCard("أنواع حروف تكررت", stats.repeatedLetters.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("أنواع كلمات تكررت", stats.repeatedWords.toString(), Modifier.weight(1f))
                                StatCard("حروف غير مصنفة", stats.unclassifiedDotLetters.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    SectionTitle("منهجية العدّ")
                    Text(
                        "يُحسب الحرف بحسب رمزه المكتوب في النص، وتُستبعد حركات التشكيل من عدد الحروف. " +
                            "تُحسب الألف الخنجرية (ٰ) منفصلة. عدد الكلمات المتكررة هنا هو عدد الكلمات المختلفة " +
                            "التي تكررت بعد إزالة التشكيل وتوحيد بعض صور الألف والياء. وعدد الحروف المتكررة " +
                            "هو عدد أنواع الحروف التي ظهر كل منها أكثر من مرة. عدّ النقاط يعتمد جدول نقاط " +
                            "الحروف العربية الشائعة، والحروف غير المصنفة لا تُحسب ضمن الحروف بلا نقاط. عدد غير المصنف: ${stats.unclassifiedDotLetters}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    SectionTitle("توزيع الحروف")
                    Text("الحرف وعدد مرات ظهوره وعدد نقاطه وفق جدول العدّ أعلاه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(letterFrequency) { entry ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry.key.toString(), fontSize = 23.sp, color = green, fontWeight = FontWeight.Bold)
                            Text("${entry.value} مرة")
                            Text("${entry.value * dotsForLetter(entry.key)} نقطة")
                        }
                    }
                }
                item {
                    SectionTitle("الكلمات الأكثر تكراراً")
                    Text("تظهر هنا الكلمات التي تكررت مرتين فأكثر بعد التطبيع الإحصائي، ولا تُدمج هذه النتائج مع البحث الحرفي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(wordFrequency) { entry ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.key, fontSize = 18.sp)
                            Text("${entry.value} مرات", color = green, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    SectionTitle("عبارات ثلاثية متكررة")
                    Text("تُقارن العبارات بعد التطبيع الإحصائي للكلمات، وتُعرض مواضع كل تطابق.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(phrases) { phrase ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(phrase.phrase, fontSize = 17.sp, textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth())
                            Text("عدد مرات الظهور: ${phrase.count}", color = green,
                                fontWeight = FontWeight.SemiBold)
                            Text(phrase.places.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    SectionTitle("البحث الحرفي في النص")
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("اكتب الكلمة أو العبارة كما تظهر في المصحف") },
                        singleLine = false,
                        textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Right)
                    )
                    Text("عدد الآيات التي تحتوي النص حرفياً: ${occurrences.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = green)
                }
                items(occurrences.take(200)) { verse ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(verse.text, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Text("${surahNames[verse.surah - 1]} • الآية ${verse.number}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    SectionTitle("إحصاء كل آية")
                    Text("كل آية مع اسم سورتها ورقمها، لتفادي اختلاط ترقيم الآيات عند الانتقال بين السور.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(activeAyat) { verse ->
                    val verseLetters = remember(verse.text) { arabicBaseLetters(verse.text) }
                    val verseWords = remember(verse.text) { wordsOf(verse).size }
                    val verseDots = remember(verse.text) { verseLetters.sumOf(::dotsForLetter) }
                    val verseDaggerAlifs = remember(verse.text) { daggerAlifCount(verse.text) }
                    Card {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "${surahNames[verse.surah - 1]}  •  الآية ${verse.number}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right,
                                color = green,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniStat("كلمات", verseWords.toString(), Modifier.weight(1f))
                                MiniStat("حروف", verseLetters.size.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniStat("نقاط", verseDots.toString(), Modifier.weight(1f))
                                MiniStat("ألفات خنجرية", verseDaggerAlifs.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
