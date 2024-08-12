package org.example.app

import java.time.LocalDate

data class ErrorResponse(val error: String)

data class CreateBookingRequest(
    val date: LocalDate,
    val numberOfPeople: Int,
    val booking: TimeSlot
)

data class CreateBookingResponse(
    val roomName: RoomName,
)

data class GetAvailableSlotsResponse(
    val slots: Map<RoomName, List<TimeSlot>>
)
