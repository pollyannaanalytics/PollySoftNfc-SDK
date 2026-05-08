package org.pollyanna.nfckmp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.pollyanna.nfckmp.network.MockBackendService

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = PollyNfc(application)
        .createEngine(backendService = MockBackendService())

    val paymentState = engine.paymentState

    fun initialize() {
        viewModelScope.launch { engine.initialize() }
    }

    fun startTransaction(amount: Double) {
        viewModelScope.launch { engine.startTransaction(amount) }
    }
}
