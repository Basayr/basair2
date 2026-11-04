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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer

private data class QuranStats(
    val verses: Int, val words: Int, val letters: Int,
    val dottedLetters: Int, val undottedLetters: Int, val dots: Int
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

/* Count the written Arabic letter code points as they appear; do not decompose
   hamza-bearing letters or merge distinct written forms before counting. */
private fun arabicBaseLetters(text: String): List<Char> =
    text.filter(::isArabicLetter).toList()

private fun dotsForLetter(letter: Char): Int = when (letter) {
    'ب', 'ج', 'خ', 'ذ', 'ز', 'ض', 'ظ', 'غ', 'ف', 'ن' -> 1
    'ت', 'ق', 'ي', 'ة' -> 2
    'ث', 'ش' -> 3
    else -> 0
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
    val dotted = letters.count { dotsForLetter(it) > 0 }
    return QuranStats(
        verses.size, verses.sumOf { wordsOf(it).size }, letters.size,
        dotted, letters.size - dotted, letters.sumOf(::dotsForLetter)
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
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(30)
    }
    val phrases = remember(activeAyat) { repeatedPhrases(activeAyat) }
    val occurrences = remember(activeAyat, search) {
        if (search.isBlank()) emptyList() else activeAyat.filter { it.text.contains(search, false) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text("لوحة التدبر والإحصاء", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = scope == 0, onClick = { scope = 0 }, label = { Text("القرآن كاملاً") })
            FilterChip(selected = scope == 1, onClick = { scope = 1 }, label = { Text("سورة") })
            if (scope == 1) {
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(surahNames[selectedSurah - 1] + " ▾")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        surahNames.forEachIndexed { index, name ->
                            DropdownMenuItem(text = { Text("${index + 1}. $name") },
                                onClick = { selectedSurah = index + 1; menuExpanded = false })
                        }
                    }
                }
            }
        }
        if (ayat.isEmpty()) {
            Text("بيانات القرآن غير متاحة؛ تعذر حساب الإحصاءات.")
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    Text(if (scope == 0) "الإحصاء العام للقرآن الكريم"
                        else "إحصاءات سورة ${surahNames[selectedSurah - 1]}",
                        style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        StatCard("الآيات", stats.verses.toString(), Modifier.weight(1f))
                        StatCard("الكلمات", stats.words.toString(), Modifier.weight(1f))
                        StatCard("الحروف", stats.letters.toString(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        StatCard("منقوطة", stats.dottedLetters.toString(), Modifier.weight(1f))
                        StatCard("غير منقوطة", stats.undottedLetters.toString(), Modifier.weight(1f))
                        StatCard("النقاط", stats.dots.toString(), Modifier.weight(1f))
                    }
                    Text("منهج العدّ: تُحصى الحروف العربية المكتوبة كما تظهر دون تفكيك الهمزات أو دمج صور الحروف، وتُستبعد علامات التشكيل من عدد الحروف. الألف المقصورة (ى) بلا نقاط، والتاء المربوطة (ة) بنقطتين. حساب النقاط يعتمد جدول الحروف العربية الشائعة؛ وتبقى العلامات والرسم العثماني الخاص بحاجة إلى تدقيق متخصص.")
                    Divider()
                    Text("تكرار الحروف", style = MaterialTheme.typography.titleMedium)
                    Text("الحرف — مرات الورود — مجموع نقاطه", style = MaterialTheme.typography.labelSmall)
                }
                items(letterFrequency) { entry ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.key.toString(), fontSize = 21.sp)
                        Text("${entry.value} مرة")
                        Text("${entry.value * dotsForLetter(entry.key)} نقطة")
                    }
                }
                item {
                    Divider()
                    Text("أكثر 30 كلمة تكراراً", style = MaterialTheme.typography.titleMedium)
                    Text("توحيد بعض صور الألف والياء وإزالة التشكيل لأغراض الإحصاء فقط.")
                }
                items(wordFrequency) { entry ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.key, fontSize = 17.sp)
                        Text("${entry.value} مرة")
                    }
                }
                item {
                    Divider()
                    Text("عبارات ثلاثية متكررة", style = MaterialTheme.typography.titleMedium)
                    Text("العبارات المتجاورة المتكررة ومواضعها بعد تطبيع إحصائي للكلمات.")
                }
                items(phrases) { phrase ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(9.dp)) {
                            Text(phrase.phrase, fontSize = 17.sp, textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth())
                            Text("تكررت ${phrase.count} مرات")
                            Text(phrase.places.take(8).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                item {
                    Divider()
                    Text("البحث عن مواضع كلمة أو عبارة", style = MaterialTheme.typography.titleMedium)
                    BasicTextField(value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        textStyle = TextStyle(fontSize = 17.sp, textAlign = TextAlign.Right),
                        decorationBox = { inner ->
                            if (search.isEmpty()) Text("اكتب النص كما يظهر في المصحف")
                            inner()
                        })
                    Text("عدد الآيات المطابقة حرفياً: ${occurrences.size}")
                }
                items(occurrences.take(200)) { verse ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            Text(verse.text, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Text("${surahNames[verse.surah - 1]} • الآية ${verse.number}",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                item {
                    Divider()
                    Text("إحصاء كل آية", style = MaterialTheme.typography.titleMedium)
                    Text("عدد الكلمات والحروف والنقاط في كل آية ضمن النطاق المختار.")
                }
                items(activeAyat) { verse ->
                    val verseLetters = remember(verse.text) { arabicBaseLetters(verse.text) }
                    val verseWords = remember(verse.text) { wordsOf(verse).size }
                    val verseDots = remember(verse.text) { verseLetters.sumOf(::dotsForLetter) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${verse.number}.")
                        Text("${verseWords} كلمة")
                        Text("${verseLetters.size} حرف")
                        Text("${verseDots} نقطة")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
