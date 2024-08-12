package org.example.app.service

import AllRoomsAreBookedException
import BookingFallsForMaintenance
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.app.RoomName
import org.example.app.TimeSlot
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalTime

class BookingServiceTest {

    private val bookingService = BookingService()

    companion object {
        @JvmStatic
        fun timeSlotsFallingAroundMaintenanceWindow() = listOf(
            arrayOf(TimeSlot(from = LocalTime.parse("08:30"), to = LocalTime.parse("09:15"))),
            arrayOf(TimeSlot(from = LocalTime.parse("09:00"), to = LocalTime.parse("09:15"))),
            arrayOf(TimeSlot(from = LocalTime.parse("09:00"), to = LocalTime.parse("09:30"))),

            arrayOf(TimeSlot(from = LocalTime.parse("12:30"), to = LocalTime.parse("13:15"))),
            arrayOf(TimeSlot(from = LocalTime.parse("13:00"), to = LocalTime.parse("13:15"))),
            arrayOf(TimeSlot(from = LocalTime.parse("13:00"), to = LocalTime.parse("13:30"))),

            arrayOf(TimeSlot(from = LocalTime.parse("16:30"), to = LocalTime.parse("17:15"))),
            arrayOf(TimeSlot(from = LocalTime.parse("17:00"), to = LocalTime.parse("17:15"))),
            arrayOf(TimeSlot(from = LocalTime.parse("17:00"), to = LocalTime.parse("17:30"))),
        )
    }

    @ParameterizedTest
    @EnumSource(RoomName::class)
    fun `test initialization removes maintenance windows`(roomName: RoomName) {
        val expectedInitialValues = listOf(
            TimeSlot(from = LocalTime.parse("00:00"), to = LocalTime.parse("09:00")),
            TimeSlot(from = LocalTime.parse("09:15"), to = LocalTime.parse("13:00")),
            TimeSlot(from = LocalTime.parse("13:15"), to = LocalTime.parse("17:00")),
            TimeSlot(from = LocalTime.parse("17:15"), to = LocalTime.parse("23:59"))
        )

        assertThat(bookingService.availableSlots(roomName)).isEqualTo(expectedInitialValues)
    }

    @Test
    fun `test blockSlot returns correct times after booking on success`() {
        bookingService.blockSlot(TimeSlot(from = LocalTime.parse("00:00"), to = LocalTime.parse("04:30")), 2)

        val afterBooking = bookingService.availableSlots(RoomName.AMAZE)

        assertThat(afterBooking).isEqualTo(
            listOf(
                TimeSlot(from = LocalTime.parse("04:30"), to = LocalTime.parse("09:00")),
                TimeSlot(from = LocalTime.parse("09:15"), to = LocalTime.parse("13:00")),
                TimeSlot(from = LocalTime.parse("13:15"), to = LocalTime.parse("17:00")),
                TimeSlot(from = LocalTime.parse("17:15"), to = LocalTime.parse("23:59"))
            )
        )
    }

    @Test
    fun `test blockSlot blocks next big enough room if previous is blocked`() {
        val timeSlot = TimeSlot(from = LocalTime.parse("00:15"), to = LocalTime.parse("04:30"))
        bookingService.blockSlot(timeSlot, 2)
        bookingService.blockSlot(timeSlot, 2)
        bookingService.blockSlot(timeSlot, 2)
        bookingService.blockSlot(timeSlot, 2)

        val expectedBookings = listOf(
            TimeSlot(from = LocalTime.parse("00:00"), to = LocalTime.parse("00:15")),
            TimeSlot(from = LocalTime.parse("04:30"), to = LocalTime.parse("09:00")),
            TimeSlot(from = LocalTime.parse("09:15"), to = LocalTime.parse("13:00")),
            TimeSlot(from = LocalTime.parse("13:15"), to = LocalTime.parse("17:00")),
            TimeSlot(from = LocalTime.parse("17:15"), to = LocalTime.parse("23:59"))
        )

        assertThat(bookingService.availableSlots(RoomName.AMAZE)).isEqualTo(expectedBookings)
        assertThat(bookingService.availableSlots(RoomName.BEAUTY)).isEqualTo(expectedBookings)
        assertThat(bookingService.availableSlots(RoomName.INSPIRE)).isEqualTo(expectedBookings)
        assertThat(bookingService.availableSlots(RoomName.STRIVE)).isEqualTo(expectedBookings)
    }

    @Test
    fun `test blockSlot throws AllRoomsAreBookedException if all rooms are booked`() {
        val timeSlot = TimeSlot(from = LocalTime.parse("00:15"), to = LocalTime.parse("04:30"))
        bookingService.blockSlot(timeSlot, 2)
        bookingService.blockSlot(timeSlot, 2)
        bookingService.blockSlot(timeSlot, 2)
        bookingService.blockSlot(timeSlot, 2)

        assertThatThrownBy {
            bookingService.blockSlot(timeSlot, 2)
        }.isInstanceOf(AllRoomsAreBookedException::class.java)
            .hasMessageMatching("No rooms ara available for 2 between 00:15 and 04:30.")
    }

    @ParameterizedTest
    @MethodSource("timeSlotsFallingAroundMaintenanceWindow")
    fun `test blockSlot throws BookingFallsForMaintenance if booking around maintenance window`(timeSlot: TimeSlot) {
        assertThatThrownBy {
            bookingService.blockSlot(timeSlot, 2)
        }.isInstanceOf(BookingFallsForMaintenance::class.java)
            .hasMessageMatching("Failed to block a room between ${timeSlot.from} and ${timeSlot.to} due to maintenance.")
    }
}
