package com.print3d.calculator.feature.machines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.MachineRepository
import com.print3d.calculator.domain.model.Machine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MachinesViewModel @Inject constructor(
    private val repo: MachineRepository
) : ViewModel() {

    val machines: StateFlow<List<Machine>> = repo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(machine: Machine) = viewModelScope.launch { repo.save(machine) }
    fun delete(machine: Machine) = viewModelScope.launch { repo.delete(machine) }
    fun duplicate(machine: Machine) = viewModelScope.launch {
        repo.save(machine.copy(id = 0, name = machine.name + " (copia)"))
    }
}
