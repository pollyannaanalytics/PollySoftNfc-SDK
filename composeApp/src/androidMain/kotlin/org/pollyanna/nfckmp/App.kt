package org.pollyanna.nfckmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pollyanna.nfckmp.model.PaymentState

@Composable
fun App(viewModel: PaymentViewModel = viewModel()) {
    val state by viewModel.paymentState.collectAsStateWithLifecycle()
    var amountText by remember { mutableStateOf("") }
    val isIdle = state == PaymentState.Idle

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "PollySoft NFC Payment",
                style = MaterialTheme.typography.headlineMedium,
            )

            StateCard(state)

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = isIdle,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.initialize() },
                enabled = isIdle,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Initialize Device")
            }

            OutlinedButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@OutlinedButton
                    viewModel.startTransaction(amount)
                },
                enabled = isIdle && amountText.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pay Now")
            }
        }
    }
}

@Composable
private fun StateCard(state: PaymentState) {
    val (text, containerColor) = when (state) {
        PaymentState.Idle -> "Ready" to MaterialTheme.colorScheme.surfaceVariant
        PaymentState.Initializing -> "Initializing device..." to MaterialTheme.colorScheme.primaryContainer
        PaymentState.WaitingForCard -> "Tap your NFC card" to MaterialTheme.colorScheme.secondaryContainer
        PaymentState.Communicating -> "Processing payment..." to MaterialTheme.colorScheme.tertiaryContainer
        PaymentState.Success -> "Payment successful!" to MaterialTheme.colorScheme.primaryContainer
        PaymentState.Failed.NotInitialized -> "Device not initialized" to MaterialTheme.colorScheme.errorContainer
        PaymentState.Failed.LocalSecurityFailed -> "Security check failed" to MaterialTheme.colorScheme.errorContainer
        is PaymentState.Failed.BackendError -> "Error: ${state.message}" to MaterialTheme.colorScheme.errorContainer
    }
    val isLoading = state == PaymentState.Initializing || state == PaymentState.Communicating

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
