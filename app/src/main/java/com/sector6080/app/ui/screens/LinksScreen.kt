package com.sector6080.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sector6080.app.ui.DISCORD_URL
import com.sector6080.app.ui.RULES_URL
import com.sector6080.app.ui.openUrl

@Composable
fun LinksScreen() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Link", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = { context.openUrl(RULES_URL) }, modifier = Modifier.fillMaxWidth()) {
            Text("Regolamento")
        }
        OutlinedButton(onClick = { context.openUrl(DISCORD_URL) }, modifier = Modifier.fillMaxWidth()) {
            Text("Server Discord (facoltativo)")
        }

        Text("Regole rapide", style = MaterialTheme.typography.titleMedium)
        listOf(
            "Sector 6080 è PvE: tutti i player collaborano contro i nemici gestiti dai Game Master.",
            "Movimento e range solo orizzontale/verticale, max 1 movimento per turno.",
            "Ogni componente si attiva max 1 volta per turno.",
            "Un rifornimento: tutti gli HP oppure tutti gli utilizzi di un componente.",
            "Se l'armatura del bersaglio è maggiore della perforazione, l'attacco fallisce.",
            "Le unità trasportate non si muovono né attaccano, ma usano i rifornimenti.",
            "Si scende dai mezzi solo su caselle adiacenti libere.",
        ).forEach { Text("• $it") }
    }
}
