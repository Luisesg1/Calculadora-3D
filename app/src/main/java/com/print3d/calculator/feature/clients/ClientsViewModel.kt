package com.print3d.calculator.feature.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.ClientRepository
import com.print3d.calculator.domain.model.Client
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val repo: ClientRepository
) : ViewModel() {

    val clients: StateFlow<List<Client>> = repo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(client: Client) = viewModelScope.launch { repo.save(client) }
    fun delete(client: Client) = viewModelScope.launch { repo.delete(client) }
    fun duplicate(client: Client) = viewModelScope.launch {
        repo.save(client.copy(id = 0, name = client.name + " (copia)"))
    }
}
