package com.saferouteai.domain.model

/**
 * Neutral, inclusive relationship taxonomy for trusted contacts.
 */
enum class ContactRelationship(val label: String) {
    PARENT("Parent"),
    SIBLING("Sibling"),
    PARTNER("Partner"),
    FRIEND("Friend"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String): ContactRelationship {
            return entries.find { it.label.equals(label, ignoreCase = true) } ?: OTHER
        }
    }
}
