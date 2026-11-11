package com.basair.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Ayah(val surah: Int, val number: Int, val text: String)

internal val surahNames = listOf(
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
            val basairGreen = Color(0xFF176B45)
            val basairDeepGreen = Color(0xFF104B34)
            val basairMint = Color(0xFFEAF4EC)
            val basairGold = Color(0xFFC6A45A)
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = basairGreen,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD5EBDD),
                    onPrimaryContainer = basairDeepGreen,
                    secondary = basairGold,
                    onSecondary = Color(0xFF241B08),
                    secondaryContainer = Color(0xFFF2E8D0),
                    onSecondaryContainer = Color(0xFF453719),
                    background = Color(0xFFF7FAF6),
                    onBackground = Color(0xFF1C2B22),
                    surface = Color(0xFFFFFEFA),
                    onSurface = Color(0xFF1C2B22),
                    surfaceVariant = basairMint,
                    onSurfaceVariant = Color(0xFF30483A),
                    outline = Color(0xFFB7CDBD)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BasairHome(ayat = ayat)
                }
            }
        }
    }

    private fun readQuranAsset(): List<Ayah> {
        return try {
            val lines = assets.open("quran-uthmani.txt").bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence()
                    .map { it.trim().removePrefix("\uFEFF") }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }

            // Accept both Tanzil formats: numbered "surah|ayah|text" and
            // the plain one-ayah-per-line Uthmani file used by this project.
            val numbered = lines.mapNotNull { line ->
                val parts = line.split("|", limit = 3)
                if (parts.size != 3) null else {
                    val surah = parts[0].toIntOrNull()
                    val number = parts[1].toIntOrNull()
                    if (surah == null || number == null || parts[2].isBlank()) null
                    else Ayah(surah, number, parts[2])
                }
            }
            if (numbered.size == lines.size && numbered.isNotEmpty()) {
                numbered
            } else {
                val counts = intArrayOf(
                    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,
                    98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,
                    75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,
                    13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,
                    29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,
                    9,5,4,7,3,6,3,5,4,5,6
                )
                check(lines.size == 6236) {
                    "Quran asset has ${lines.size} verses; expected 6236."
                }
                val parsed = ArrayList<Ayah>(6236)
                var index = 0
                for (surah in counts.indices) {
                    for (number in 1..counts[surah]) {
                        parsed.add(Ayah(surah + 1, number, lines[index++]))
                    }
                }
                parsed
            }
        } catch (error: Exception) {
            android.util.Log.e("Basair", "Unable to read Quran asset", error)
            emptyList()
        }
    }
}


@Composable
private fun BasairHome(ayat: List<Ayah>) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "بصائر",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "رفيقك في قراءة القرآن وتدبّره",
                        color = Color(0xFFE2F1E7),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("۞", color = Color(0xFFE6CC8D), fontSize = 34.sp)
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OriginalMushafScreen()
                1 -> AnalyticsScreen(ayat = ayat)
                else -> BasairScreen(ayat = ayat)
            }
        }
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Text("۞", fontSize = 19.sp) },
                label = { Text("المصحف") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Text("✧", fontSize = 19.sp) },
                label = { Text("التدبر والإحصاء") }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Text("⌕", fontSize = 21.sp) },
                label = { Text("النص والبحث") }
            )
        }
    }
}

@Composable
private fun OriginalMushafScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("basair_reading", android.content.Context.MODE_PRIVATE)
    }
    var page by rememberSaveable {
        mutableIntStateOf(preferences.getInt("mushaf_page", 1).coerceIn(1, 604))
    }
    var sliderPage by rememberSaveable { mutableIntStateOf(page) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(
                onClick = {
                    sliderPage = (sliderPage - 1).coerceAtLeast(1)
                    page = sliderPage
                    preferences.edit().putInt("mushaf_page", page).apply()
                },
                enabled = sliderPage > 1
            ) { Text("السابق") }
            Text("صفحة $sliderPage من 604", style = MaterialTheme.typography.titleMedium)
            androidx.compose.material3.TextButton(
                onClick = {
                    sliderPage = (sliderPage + 1).coerceAtMost(604)
                    page = sliderPage
                    preferences.edit().putInt("mushaf_page", page).apply()
                },
                enabled = sliderPage < 604
            ) { Text("التالي") }
        }
        Slider(
            value = sliderPage.toFloat(),
            onValueChange = { sliderPage = it.toInt().coerceIn(1, 604) },
            onValueChangeFinished = {
                page = sliderPage
                preferences.edit().putInt("mushaf_page", page).apply()
            },
            valueRange = 1f..604f,
            steps = 602
        )
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                }
            },
            update = { webView ->
                if (webView.tag != page) {
                    webView.tag = page
                    val pageFile = page.toString().padStart(3, '0') + ".svg"
                val html = """
                    <!doctype html>
                    <html lang="ar">
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=4.0">
                      <style>
                        html, body { margin:0; padding:0; background:#ffffff; width:100%; min-height:100%; }
                        svg { display:block; width:100%; height:auto; margin:0 auto; }
                        #status { padding:24px; color:#555; text-align:center; font-family:sans-serif; }
                      </style>
                    </head>
                    <body>
                      <div id="status">جارٍ تحميل صفحة المصحف…</div>
                      <script>
                        fetch("$pageFile").then(function(response) {
                          if (!response.ok) throw new Error("HTTP " + response.status);
                          return response.text();
                        }).then(function(source) {
                          document.body.innerHTML = source;
                          var root = document.querySelector("svg");
                          if (!root) throw new Error("SVG page is missing");
                          root.removeAttribute("width");
                          root.removeAttribute("height");
                          root.setAttribute("style", "width:100%;height:auto;display:block");
                          requestAnimationFrame(function() {
                            try {
                              var bounds = root.getBBox();
                              if (bounds.width > 0 && bounds.height > 0) {
                                var margin = Math.max(bounds.width, bounds.height) * 0.04;
                                root.setAttribute("viewBox",
                                  (bounds.x - margin) + " " + (bounds.y - margin) + " " +
                                  (bounds.width + margin * 2) + " " + (bounds.height + margin * 2));
                              }
                            } catch (ignored) {}
                          });
                        }).catch(function(error) {
                          document.getElementById("status").textContent =
                            "تعذر تحميل الصفحة. تحقق من اتصال الإنترنت ثم أعد المحاولة.";
                        });
                      </script>
                    </body>
                    </html>
                """.trimIndent()
                    webView.loadDataWithBaseURL(
                        "https://cdn.quran.ws/svg/pages/v1.1.1/hafs-kfqc/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
        Text(
            "صفحات مصحف المدينة النبوية – رواية حفص عن عاصم",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun BasairScreen(ayat: List<Ayah>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("basair_reading", android.content.Context.MODE_PRIVATE)
    }
    var query by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }
    var currentSurah by rememberSaveable {
        mutableIntStateOf(preferences.getInt("last_surah", 1).coerceIn(1, 114))
    }
    var fontSize by rememberSaveable { mutableIntStateOf(27) }
    var menuExpanded by remember { mutableStateOf(false) }
    var bookmarkSurah by remember { mutableIntStateOf(preferences.getInt("bookmark_surah", 1)) }
    var bookmarkAyah by remember { mutableIntStateOf(preferences.getInt("bookmark_ayah", 1)) }
    var message by remember { mutableStateOf("") }
    var pendingScrollAyah by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val ayatBySurah = remember(ayat) { ayat.groupBy { it.surah } }
    val currentSurahAyat = remember(ayatBySurah, currentSurah) {
        ayatBySurah[currentSurah].orEmpty()
    }
    val results = remember(ayat, query, searched) {
        if (!searched || query.isBlank()) emptyList()
        else ayat.filter { it.text.contains(query, ignoreCase = false) }
    }
    val visibleAyat = if (searched) results else currentSurahAyat

    LaunchedEffect(currentSurah, pendingScrollAyah, searched) {
        if (!searched && pendingScrollAyah > 0) {
            val targetIndex = currentSurahAyat.indexOfFirst { it.number == pendingScrollAyah }
            if (targetIndex >= 0) listState.animateScrollToItem(targetIndex)
            pendingScrollAyah = 0
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = "بصائر",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "القرآن الكريم بالرسم العثماني",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
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
                    query = ""
                    preferences.edit().putInt("last_surah", currentSurah).apply()
                },
                enabled = currentSurah > 1 && ayat.isNotEmpty()
            ) { Text("السابق") }

            Box {
                Button(onClick = { menuExpanded = true }, enabled = ayat.isNotEmpty()) {
                    Text("سورة ${surahNames[currentSurah - 1]} ▾")
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    surahNames.forEachIndexed { index, name ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("${index + 1}. $name") },
                            onClick = {
                                currentSurah = index + 1
                                searched = false
                                query = ""
                                menuExpanded = false
                                preferences.edit().putInt("last_surah", currentSurah).apply()
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    currentSurah = (currentSurah + 1).coerceAtMost(114)
                    searched = false
                    query = ""
                    preferences.edit().putInt("last_surah", currentSurah).apply()
                },
                enabled = currentSurah < 114 && ayat.isNotEmpty()
            ) { Text("التالي") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).padding(12.dp),
                textStyle = TextStyle(fontSize = 17.sp, textAlign = TextAlign.Right),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("بحث حرفي في القرآن")
                    inner()
                }
            )
            Button(onClick = {
                searched = query.isNotBlank()
                message = if (query.isBlank()) "اكتب كلمة أو عبارة للبحث" else ""
            }) { Text("بحث") }
            Button(onClick = {
                query = ""
                searched = false
                message = ""
            }) { Text("إلغاء") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (searched) "نتائج البحث: ${results.size}"
                else "عدد الآيات: ${currentSurahAyat.size}",
                style = MaterialTheme.typography.labelLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("حجم الخط")
                androidx.compose.material3.TextButton(
                    onClick = { fontSize = (fontSize - 2).coerceAtLeast(20) }
                ) { Text("−") }
                Text("$fontSize")
                androidx.compose.material3.TextButton(
                    onClick = { fontSize = (fontSize + 2).coerceAtMost(40) }
                ) { Text("+") }
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (ayat.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("لم يتم تحميل نص القرآن.")
                    Text("أعد بناء النسخة بعد إصلاح مصدر النص، وتأكد من وجود ملف القرآن داخل التطبيق.")
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleAyat) { ayah ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                            Text(
                                text = ayah.text,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = fontSize.sp,
                                textAlign = TextAlign.Right,
                                lineHeight = (fontSize + 19).sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${surahNames[ayah.surah - 1]} • الآية ${ayah.number}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        bookmarkSurah = ayah.surah
                                        bookmarkAyah = ayah.number
                                        preferences.edit()
                                            .putInt("bookmark_surah", ayah.surah)
                                            .putInt("bookmark_ayah", ayah.number)
                                            .apply()
                                        message = "تم حفظ موضعك: ${surahNames[ayah.surah - 1]}، الآية ${ayah.number}"
                                    }
                                ) { Text("حفظ الموضع") }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(
                onClick = {
                    currentSurah = bookmarkSurah.coerceIn(1, 114)
                    pendingScrollAyah = bookmarkAyah
                    searched = false
                    query = ""
                    message = "موضع الحفظ: ${surahNames[currentSurah - 1]}، الآية $bookmarkAyah"
                },
                enabled = ayat.isNotEmpty()
            ) { Text("آخر موضع محفوظ") }
            androidx.compose.material3.TextButton(
                onClick = {
                    currentSurah = preferences.getInt("last_surah", 1).coerceIn(1, 114)
                    searched = false
                    query = ""
                },
                enabled = ayat.isNotEmpty()
            ) { Text("متابعة القراءة") }
        }
    }
}
