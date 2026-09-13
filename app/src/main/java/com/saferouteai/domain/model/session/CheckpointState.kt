package com.saferouteai.domain.model.session

/**
 * Status of the periodic safety checkpoint.
 */
enum class CheckpointState {
    /**
     * Next checkpoint is scheduled for a future timestamp.
     */
    SCHEDULED,

    /**
     * Checkpoint timer reached; awaiting user acknowledgement in foreground UI.
     */
    DUE,

    /**
     * User acknowledged the safety prompt ("I'm Okay").
     */
    ACKNOWLEDGED,

    /**
     * Acknowledgement window expired without user interaction.
     * Note: Recorded as a local safety indicator only; does NOT trigger emergency calls/SOS.
     */
    MISSED,

    /**
     * Checkpoint system is inactive (e.g. session idle, completed, or disabled).
     */
    DISARMED
}
