package org.example.app

import java.time.LocalTime

data class TimeSlot(
    val from: LocalTime,
    val to: LocalTime,
)

// More extensible design would be to save the rooms in the database table, and extract them on a startup
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

