package org.example.app.service

import org.example.app.RoomName
import org.example.app.TimeSlot
import org.springframework.stereotype.Service

@Service
class BookingService(
    private val bookingsStorage: BookingsStorage,
) {


    fun createBooking(roomName: RoomName, from: TimeSlot, to: TimeSlot) {
        if (!roomIsAvailable(roomName = roomName, from = from, to = to)) {
            throw RuntimeException("TODO");
        }
    }

    private fun roomIsAvailable(roomName: RoomName, from: TimeSlot, to: TimeSlot): Boolean {
        return true
    }
}
