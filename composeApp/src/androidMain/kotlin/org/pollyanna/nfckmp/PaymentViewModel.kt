package org.pollyanna.nfckmp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.pollyanna.nfckmp.network.MockBackendService
import org.pollyanna.nfckmp.nfc_provider.PlatformProviderFactory
import org.pollyanna.nfckmp.nfc_provider.createEngine

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = PlatformProviderFactory(application)
        .createEngine(backendService = MockBackendService())

    val paymentState = engine.paymentState

    fun initialize() {
        viewModelScope.launch { engine.initialize() }
    }

    fun startTransaction(amount: Double) {
        viewModelScope.launch { engine.startTransaction(amount) }
    }
}
