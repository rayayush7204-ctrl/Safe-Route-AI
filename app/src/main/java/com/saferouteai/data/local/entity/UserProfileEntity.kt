package com.saferouteai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.saferouteai.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val email: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        displayName = displayName,
        phoneNumber = phoneNumber,
        email = email,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity = UserProfileEntity(
            id = profile.id,
            displayName = profile.displayName,
            phoneNumber = profile.phoneNumber,
            email = profile.email,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )
    }
}
