package com.saferouteai.presentation.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.ContactRelationship
import com.saferouteai.domain.model.TrustedContact
import java.util.UUID

@Composable
fun AddEditContactDialog(
    contact: TrustedContact?,
    onDismiss: () -> Unit,
    onSave: (TrustedContact) -> Unit
) {
    var name by remember(contact) { mutableStateOf(contact?.name ?: "") }
    var relationship by remember(contact) { mutableStateOf(contact?.relationship ?: ContactRelationship.FRIEND) }
    var phoneNumber by remember(contact) { mutableStateOf(contact?.phoneNumber ?: "") }
    var email by remember(contact) { mutableStateOf(contact?.email ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (contact == null) "Add Trusted Contact" else "Edit Trusted Contact",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; localError = null },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Relationship",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ContactRelationship.entries.forEach { rel ->
                        FilterChip(
                            selected = relationship == rel,
                            onClick = { relationship = rel },
                            label = { Text(rel.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; localError = null },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; localError = null },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "* Contact must have at least a phone number or email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        localError = "Name cannot be blank"
                        return@Button
                    }
                    if (phoneNumber.trim().isBlank() && email.trim().isBlank()) {
                        localError = "Provide at least a phone number or email"
                        return@Button
                    }
                    val newContact = TrustedContact(
                        id = contact?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        relationship = relationship,
                        phoneNumber = phoneNumber.trim(),
                        email = email.trim(),
                        isEnabled = contact?.isEnabled ?: true,
                        createdAt = contact?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(newContact)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
