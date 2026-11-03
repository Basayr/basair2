package com.basair.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Ayah(val surah: Int, val number: Int, val text: String)

private val surahNames = listOf(
    "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف",
    "الأنفال", "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر",
    "النحل", "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون",
    "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
    "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص",
    "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية",
    "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور",
    "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر",
    "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم",
    "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن", "المزمل",
    "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
    "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق",
    "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس", "الليل", "الضحى",
    "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
    "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون",
    "الكوثر", "الكافرون", "النصر", "المسد", "الإخلاص", "الفلق", "الناس"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ayat = readQuranAsset()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BasairScreen(ayat = ayat)
                }
            }
        }
    }

    private fun readQuranAsset(): List<Ayah> {
        return try {
            assets.open("quran-uthmani.txt").bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().mapNotNull { line ->
                    val parts = line.split("|", limit = 3)
                    if (parts.size != 3) null else {
                        val surah = parts[0].toIntOrNull()
                        val number = parts[1].toIntOrNull()
                        if (surah == null || number == null) null
                        else Ayah(surah, number, parts[2])
                    }
                }.toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Composable
private fun BasairScreen(ayat: List<Ayah>) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Ayah>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var currentSurah by remember { mutableIntStateOf(1) }

    val currentSurahAyat = remember(ayat, currentSurah) {
        ayat.filter { it.surah == currentSurah }
    }
    val visibleAyat = if (searched) results else currentSurahAyat

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "بصائر",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "القرآن الكريم بالرسم العثماني",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    currentSurah = (currentSurah - 1).coerceAtLeast(1)
                    searched = false
                },
                enabled = currentSurah > 1 && ayat.isNotEmpty()
            ) { Text("السابقة") }
            Text(
                text = "سورة ${surahNames[currentSurah - 1]}",
                style = MaterialTheme.typography.titleLarge
            )
            Button(
                onClick = {
                    currentSurah = (currentSurah + 1).coerceAtMost(114)
                    searched = false
                },
                enabled = currentSurah < 114 && ayat.isNotEmpty()
            ) { Text("التالية") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).padding(12.dp),
                textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Right),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("ابحث بمطابقة النص كما هو")
                    inner()
                }
            )
            Button(onClick = {
                searched = true
                results = if (query.isEmpty()) emptyList()
                else ayat.filter { it.text.contains(query, ignoreCase = false) }
            }) { Text("ابحث") }
            Button(onClick = {
                query = ""
                searched = false
            }) { Text("قراءة") }
        }

        if (ayat.isEmpty()) {
            Text(
                text = "تعذر تحميل نص القرآن. سيُضمّن النص العثماني الموثق تلقائياً عند بناء التطبيق.",
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        } else {
            if (searched) {
                Text("نتائج المطابقة الحرفية: ${results.size}")
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleAyat) { ayah ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = ayah.text,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 25.sp,
                                textAlign = TextAlign.Right,
                                lineHeight = 46.sp
                            )
                            Text(
                                text = "سورة ${surahNames[ayah.surah - 1]} — آية ${ayah.number}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}
