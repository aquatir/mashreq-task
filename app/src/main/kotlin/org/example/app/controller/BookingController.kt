package org.example.app.controller

import ClientRequestException
import org.example.app.CreateBookingRequest
import org.example.app.CreateBookingResponse
import org.example.app.GetAvailableSlotsResponse
import org.example.app.RoomName
import org.example.app.service.BookingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/booking")
class BookingController(
    private val bookingService: BookingService,
) {

    @PostMapping("/")
    fun postBooking(
        @RequestBody request: CreateBookingRequest
    ): CreateBookingResponse {
        validateCreateBookingRequest(request)
        val bookedRoom = bookingService.blockSlot(
            timeSlot = request.booking,
            blockedFor = request.numberOfPeople,
        )

        return CreateBookingResponse(roomName = bookedRoom)
    }

    @GetMapping("/")
    fun getAvailableSlots(
    ): GetAvailableSlotsResponse {
        val roomToSlots = RoomName.entries.associateWith { bookingService.availableSlots(it) }
        return GetAvailableSlotsResponse(slots = roomToSlots)
    }


    private fun validateCreateBookingRequest(request: CreateBookingRequest) {
        val today = LocalDate.now()
        if (request.date != today) {
            throw ClientRequestException("Can only book rooms for today '$today', received '${request.date}' in a request")
        }

        if (request.numberOfPeople < 1) {
            throw ClientRequestException("Can only book a room if numberOfPeople is more than 1, received '${request.numberOfPeople}'")
        }


    }

}
