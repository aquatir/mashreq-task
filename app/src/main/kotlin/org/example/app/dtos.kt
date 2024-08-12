package org.example.app

import java.time.LocalDate
import java.time.LocalTime

data class TimeSlot(
    val from: LocalTime,
    val to: LocalTime,
)

data class CreateBookingRequest(
    val date: LocalDate,
    val booking: TimeSlot
)

enum class RoomName(private val maxRoomSize: Int) {
    AMAZE(3),
    BEAUTY(7),
    INSPIRE(12),
    STRIVE(20);


    companion object {
        fun sufficientRooms(numberOfPeople: Int): List<RoomName> =
            RoomName.entries.filter { it.maxRoomSize >= numberOfPeople }
    }
}

