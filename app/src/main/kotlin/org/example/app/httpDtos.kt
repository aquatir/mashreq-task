package org.example.app

import java.time.LocalDate

data class ErrorResponse(val error: String)

data class CreateBookingRequest(
    val date: LocalDate,
    val booking: TimeSlot
)
