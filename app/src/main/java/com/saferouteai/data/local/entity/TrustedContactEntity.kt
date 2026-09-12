package com.saferouteai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.saferouteai.domain.model.ContactRelationship
import com.saferouteai.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val email: String,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): TrustedContact = TrustedContact(
        id = id,
        name = name,
        relationship = ContactRelationship.fromLabel(relationship),
        phoneNumber = phoneNumber,
        email = email,
        isEnabled = isEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(contact: TrustedContact): TrustedContactEntity = TrustedContactEntity(
            id = contact.id,
            name = contact.name,
            relationship = contact.relationship.label,
            phoneNumber = contact.phoneNumber,
            email = contact.email,
            isEnabled = contact.isEnabled,
            createdAt = contact.createdAt,
            updatedAt = contact.updatedAt
        )
    }
}
