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
import java.io.BufferedReader

data class Ayah(val surah: Int, val number: Int, val text: String)

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

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "بصائر",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "للبحث والتدبر في القرآن الكريم",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
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
                    if (query.isEmpty()) Text("اكتب كلمة أو عبارة كما هي في المصحف")
                    inner()
                }
            )
            Button(onClick = {
                searched = true
                results = if (query.isEmpty()) emptyList()
                else ayat.filter { it.text.contains(query, ignoreCase = false) }
            }) { Text("ابحث") }
        }

        if (ayat.isEmpty()) {
            Text(
                text = "نسخة التطوير جاهزة. يجري تجهيز ملف القرآن العثماني الموثق لإضافته دون أي تغيير.",
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        } else if (searched) {
            Text("عدد النتائج: ${results.size}")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results) { ayah ->
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
                                lineHeight = 44.sp
                            )
                            Text(
                                text = "سورة ${ayah.surah} — آية ${ayah.number}",
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
