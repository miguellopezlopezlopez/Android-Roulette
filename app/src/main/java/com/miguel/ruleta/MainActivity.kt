package com.miguel.ruleta

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.miguel.ruleta.ui.theme.RuletaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuletaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RouletteApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RouletteApp(modifier: Modifier = Modifier) {
    RouletteGame(modifier = Modifier.fillMaxSize())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteGame(modifier: Modifier = Modifier) {
    var balance by remember { mutableDoubleStateOf(1000.0) } // Saldo inicial
    var bet by remember { mutableStateOf("") } // Cantidad a apostar
    var selectedNumber by remember { mutableStateOf("") } // Número seleccionado para la apuesta
    var result by remember { androidx.compose.runtime.mutableIntStateOf(0) } // Resultado de la ruleta
    var showDialog by remember { mutableStateOf(false) } // Control del popup
    var message by remember { mutableStateOf("") } // Mensaje a mostrar en el popup

    // Scroll
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF008f39)) // Fondo verde oscuro
            .verticalScroll(scrollState) // Permite desplazarse verticalmente
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Comienza desde arriba
    ) {
        Text(text = "Saldo: ${"%.2f".format(balance)}€", color = White)
        Spacer(modifier = Modifier.height(16.dp))



        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar la imagen del tablero de la ruleta
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    Glide.with(context)
                        .load(R.drawable.tablero) // Carga la imagen del tablero
                        .into(this)
                }
            },
            modifier = Modifier.height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Entrada para la cantidad a apostar
        OutlinedTextField(
            value = bet,
            onValueChange = { bet = it },
            label = { Text("Cantidad a apostar", color = White) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedTextColor = White,
                focusedTextColor = White,
                focusedBorderColor = Green,
                unfocusedBorderColor = Green),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(16.dp)
        )

        // Entrada para el número a apostar
        OutlinedTextField(
            value = selectedNumber,
            onValueChange = { selectedNumber = it },
            label = { Text("Número a apostar (0-36)", color = White) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedTextColor = White,
                focusedTextColor = White,
                focusedBorderColor = Green,
                unfocusedBorderColor = Green),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val betAmount = bet.toDoubleOrNull() ?: 0.0
            val number = selectedNumber.toIntOrNull()

            // Validaciones para apuestas y ganancias
            if (betAmount > 0 && betAmount <= balance && number != null && number in 0..36) {
                result = (0..36).random() // Generar el número aleatorio de la ruleta
                balance -= betAmount // Restar la cantidad apostada del saldo

                if (result == number) {
                    val winnings = betAmount * 36 // Multiplicar la cantidad apostada por 36
                    balance += winnings // Sumar las ganancias al saldo
                    message = "¡Ganaste! Tu número: $number, Resultado: $result. Ganaste ${"%.2f".format(winnings)}€"
                } else {
                    message = "Perdiste. Tu número: $number, Resultado: $result."
                }
                showDialog = true // Mostrar el popup con el mensaje
            } else {
                message = "Saldo insuficiente, apuesta inválida o número incorrecto."
                showDialog = true // Mostrar el popup con el error
            }
        }) {
            Text(text = "Girar Ruleta")
        }

        Spacer(modifier = Modifier.height(16.dp))


        // Popup para mostrar el mensaje
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Resultado") },
                text = { Text(text = message) },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text(text = "Aceptar")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoulettePreview() {
    RuletaTheme {
        RouletteApp()
    }
}
