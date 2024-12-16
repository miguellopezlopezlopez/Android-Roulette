package com.miguel.ruleta

import android.media.AudioManager
import android.media.MediaPlayer
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
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el MediaPlayer con el archivo de música
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer.isLooping = true // Configurar para que la música se repita
        mediaPlayer.start() // Iniciar la reproducción

        // Configurar controles de volumen
        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent {
            RuletaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar recursos del MediaPlayer
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }
}

@Composable
fun MainApp(modifier: Modifier = Modifier) {
    var initialBalance by remember { mutableDoubleStateOf(0.0) }
    var showGameScreen by remember { mutableStateOf(false) }

    if (!showGameScreen) {
        WelcomeScreen(
            modifier = Modifier.fillMaxSize(),
            onStartGame = { balance ->
                initialBalance = balance
                showGameScreen = true
            }
        )
    } else {
        RouletteGame(
            modifier = Modifier.fillMaxSize(),
            initialBalance = initialBalance,
            onBalanceDepleted = {
                showGameScreen = false // Volver a la pantalla de inicio
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, onStartGame: (Double) -> Unit) {
    var balanceInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF008f39)) // Fondo verde oscuro
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    Glide.with(context)
                        .load(R.drawable.ruleta) // Carga el GIF de la ruleta
                        .into(this)
                }
            },
            modifier = Modifier.height(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "¡Bienvenido a la Ruleta!", color = White)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = balanceInput,
            onValueChange = { balanceInput = it },
            label = { Text("Saldo inicial (€)", color = White) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedTextColor = White,
                focusedTextColor = White,
                focusedBorderColor = Green,
                unfocusedBorderColor = Green
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(16.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = {
            val balance = balanceInput.toDoubleOrNull()
            if (balance != null && balance > 0) {
                onStartGame(balance)
            } else {
                errorMessage = "Por favor, introduce un saldo inicial válido."
            }
        }) {
            Text(text = "Comenzar Juego")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteGame(
    modifier: Modifier = Modifier,
    initialBalance: Double,
    onBalanceDepleted: () -> Unit
) {
    var balance by remember { mutableDoubleStateOf(initialBalance) } // Saldo inicial
    var bet by remember { mutableStateOf("") } // Cantidad a apostar
    var selectedNumber by remember { mutableStateOf("") } // Número seleccionado para la apuesta
    var result by remember { androidx.compose.runtime.mutableIntStateOf(0) } // Resultado de la ruleta
    var showDialog by remember { mutableStateOf(false) } // Control del popup
    var message by remember { mutableStateOf("") } // Mensaje a mostrar en el popup
    var isBalanceDepleted by remember { mutableStateOf(false) } // Estado de saldo agotado

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
                unfocusedBorderColor = Green
            ),
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
                unfocusedBorderColor = Green
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val betAmount = bet.toDoubleOrNull() ?: 0.0
            val number = selectedNumber.toIntOrNull()

            if (betAmount > 0 && betAmount <= balance && number != null && number in 0..36) {
                result = (0..1).random() // Generar el número aleatorio de la ruleta
                balance -= betAmount // Restar la cantidad apostada del saldo

                if (result == number) {
                    val winnings = betAmount * 36 // Multiplicar la cantidad apostada por 36
                    balance += winnings // Sumar las ganancias al saldo
                    message = "¡Ganaste! Tu número: $number, Resultado: $result. Ganaste ${"%.2f".format(winnings)}€"
                    showDialog = true
                } else {
                    if (balance <= 0) {
                        message = "Perdiste. Tu número: $number, Resultado: $result. ¡Te has quedado sin fondos!"
                        showDialog = true
                        isBalanceDepleted = true
                    } else {
                        message = "Perdiste. Tu número: $number, Resultado: $result."
                        showDialog = true
                    }
                }
            } else {
                message = "Saldo insuficiente, apuesta inválida o número incorrecto."
                showDialog = true
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
                    Button(onClick = {
                        showDialog = false
                        if (isBalanceDepleted) onBalanceDepleted()
                    }) {
                        Text(text = "Aceptar")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    RuletaTheme {
        WelcomeScreen(onStartGame = {})
    }
}

@Preview(showBackground = true)
@Composable
fun RouletteGamePreview() {
    RuletaTheme {
        RouletteGame(
            initialBalance = 1000.0,
            onBalanceDepleted = {}
        )
    }
}
