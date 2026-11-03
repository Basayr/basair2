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
import java.util.Locale

private data class QuranStats(
    val verses: Int,
    val words: Int,
    val letters: Int,
    val dottedLetters: Int,
    val undottedLetters: Int,
    val dots: Int
)

private fun arabicBaseLetters(text: String): List<Char> {
    val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
    return normalized.filter { ch ->
        Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.ARABIC &&
            Character.getType(ch) in intArrayOf(
                Character.UPPERCASE_LETTER.toInt(),
                Character.LOWERCASE_LETTER.toInt(),
                Character.OTHER_LETTER.toInt(),
                Character.TITLECASE_LETTER.toInt(),
                Character.MODIFIER_LETTER.toInt()
            )
    }.map { ch ->
        when (ch) {
            'أ', 'إ', 'آ', 'ٱ' -> 'ا'
            'ؤ' -> 'و'
            'ئ', 'ى', 'ے' -> 'ي'
            'ة' -> 'ه'
            else -> ch
        }
    }
}

private fun dotsForLetter(letter: Char): Int = when (letter) {
    'ب', 'ج', 'خ', 'ذ', 'ز', 'ض', 'ظ', 'غ', 'ف', 'ن' -> 1
    'ت', 'ق', 'ي' -> 2
    'ث', 'ش' -> 3
    else -> 0
}

private fun wordKey(word: String): String {
    val noMarks = Normalizer.normalize(word, Normalizer.Form.NFD)
        .filter { Character.getType(it) !in intArrayOf(6, 7, 8) }
    return noMarks.replace(Regex("[۞۩ـ،؛؟!:.()\\[\\]{}]"), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ٱ', 'ا').replace('ى', 'ي')
}

private fun statsFor(verses: List<Ayah>): QuranStats {
    val letters = verses.flatMap { arabicBaseLetters(it.text) }
    val dotted = letters.count { dotsForLetter(it) > 0 }
    return QuranStats(
        verses = verses.size,
        words = verses.sumOf { it.text.trim().split(Regex("\\s+")).count { token -> token.isNotBlank() } },
        letters = letters.size,
        dottedLetters = dotted,
        undottedLetters = letters.size - dotted,
        dots = letters.sumOf { dotsForLetter(it) }
    )
}

@Composable
fun AnalyticsScreen(ayat: List<Ayah>) {
    var scope by rememberSaveable { mutableIntStateOf(0) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(1) }
    var menuExpanded by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val surahAyat = remember(ayat, selectedSurah) { ayat.filter { it.surah == selectedSurah } }
    val activeAyat = if (scope == 0) ayat else surahAyat
    val stats = remember(activeAyat) { statsFor(activeAyat) }
    val frequency = remember(activeAyat) {
        activeAyat.flatMap { it.text.trim().split(Regex("\\s+")) }
            .filter { it.isNotBlank() }
            .groupingBy { wordKey(it) }.eachCount()
            .filterKeys { it.isNotBlank() }
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(20)
    }
    val occurrences = remember(ayat, search) {
        if (search.isBlank()) emptyList()
        else ayat.filter { it.text.contains(search, ignoreCase = false) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("لوحة التدبر والإحصاء", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = scope == 0, onClick = { scope = 0 }, label = { Text("القرآن كاملاً") })
            FilterChip(selected = scope == 1, onClick = { scope = 1 }, label = { Text("سورة محددة") })
            if (scope == 1) {
                Box {
                    TextButton(onClick = { menuExpanded = true }) { Text(surahNames[selectedSurah - 1] + " ▾") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        surahNames.forEachIndexed { index, name ->
                            DropdownMenuItem(text = { Text("${index + 1}. $name") }, onClick = {
                                selectedSurah = index + 1
                                menuExpanded = false
                            })
                        }
                    }
                }
            }
        }
        if (ayat.isEmpty()) {
            Text("بيانات القرآن غير متاحة. لا يمكن حساب الإحصاءات قبل تحميل النص.")
        } else {
            Text(if (scope == 0) "نتائج القرآن الكريم" else "نتائج سورة ${surahNames[selectedSurah - 1]}",
                style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("الآيات", stats.verses.toString(), Modifier.weight(1f))
                StatCard("الكلمات", stats.words.toString(), Modifier.weight(1f))
                StatCard("الحروف", stats.letters.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("حروف منقوطة", stats.dottedLetters.toString(), Modifier.weight(1f))
                StatCard("غير منقوطة", stats.undottedLetters.toString(), Modifier.weight(1f))
                StatCard("مجموع النقاط", stats.dots.toString(), Modifier.weight(1f))
            }
            Text("تنبيه منهجي: العدّ هنا يحصي الحروف العربية بعد فصل التشكيل، ويجمع أشكال الألف والياء والتاء المربوطة وفق قواعد مبينة في التطبيق. النقاط هي نقاط جسم الحرف، ولا تشمل علامات التشكيل أو علامات الوقف.")
            Divider()
            Text("أكثر 20 كلمة تكراراً (بعد إزالة التشكيل وتوحيد بعض صور الحروف)", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items(frequency) { entry ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.key, fontSize = 18.sp)
                        Text(entry.value.toString())
                    }
                }
            }
            Divider()
            Text("البحث عن مواضع كلمة أو عبارة", style = MaterialTheme.typography.titleMedium)
            BasicTextField(
                value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                textStyle = TextStyle(fontSize = 17.sp, textAlign = TextAlign.Right),
                decorationBox = { inner -> if (search.isEmpty()) Text("اكتب الكلمة أو العبارة كما تظهر في المصحف") ; inner() }
            )
            Text("عدد الآيات التي تحتوي النص حرفياً: ${occurrences.size}")
            if (search.isNotBlank()) {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(occurrences.take(100)) { verse ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(verse.text, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                Text("${surahNames[verse.surah - 1]} • الآية ${verse.number}",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
