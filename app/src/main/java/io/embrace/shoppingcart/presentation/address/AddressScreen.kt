package io.embrace.shoppingcart.presentation.address

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.embrace.shoppingcart.presentation.components.MessageSnackbar
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddressScreen(viewModel: AddressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Addresses") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // List
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(state.list) { addr ->
                    Text("${addr.street}, ${addr.city}, ${addr.state}, ${addr.zip}, ${addr.country}")
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Form
            OutlinedTextField(state.street, viewModel::updateStreet, label = { Text("Street") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.city, viewModel::updateCity, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.state, viewModel::updateState, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.zip, viewModel::updateZip, label = { Text("ZIP") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.country, viewModel::updateCountry, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.geocodeCurrent() }) { Text("Geocode") }
                Button(onClick = { viewModel.save() }) { Text("Save Address") }
            }

            state.geo?.let { Text("Geocode: $it") }
        }

        state.message?.let { msg ->
            MessageSnackbar(message = msg, onDismiss = { viewModel.clearMessage() })
        }
    }
}
