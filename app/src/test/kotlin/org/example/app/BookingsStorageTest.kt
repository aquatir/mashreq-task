package org.example.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalTime

class BookingsStorageTest {

    private val bookingsStorage = BookingsStorage()

    @ParameterizedTest
    @EnumSource(RoomName::class)
    fun `test initialization removes maintenance windows`(roomName: RoomName) {
        val expectedInitialValues = listOf(
            TimeSlot(from = LocalTime.parse("00:00"), to = LocalTime.parse("09:00")),
            TimeSlot(from = LocalTime.parse("09:15"), to = LocalTime.parse("13:00")),
            TimeSlot(from = LocalTime.parse("13:15"), to = LocalTime.parse("17:00")),
            TimeSlot(from = LocalTime.parse("17:15"), to = LocalTime.parse("23:59"))
        )

        assertThat(bookingsStorage.availableSlots(roomName)).isEqualTo(expectedInitialValues)
    }

    @Test
    fun `test blockSlot returns correct times after booking on success`() {
        bookingsStorage.blockSlot(TimeSlot(from = LocalTime.parse("00:00"), to = LocalTime.parse("04:30")), 2)

        val afterBooking = bookingsStorage.availableSlots(RoomName.AMAZE)

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
        bookingsStorage.blockSlot(TimeSlot(from = LocalTime.parse("00:15"), to = LocalTime.parse("04:30")), 2)
        bookingsStorage.blockSlot(TimeSlot(from = LocalTime.parse("00:15"), to = LocalTime.parse("04:30")), 2)
        bookingsStorage.blockSlot(TimeSlot(from = LocalTime.parse("00:15"), to = LocalTime.parse("04:30")), 2)
        bookingsStorage.blockSlot(TimeSlot(from = LocalTime.parse("00:15"), to = LocalTime.parse("04:30")), 2)

        val expectedBookings = listOf(
            TimeSlot(from = LocalTime.parse("00:00"), to = LocalTime.parse("00:15")),
            TimeSlot(from = LocalTime.parse("04:30"), to = LocalTime.parse("09:00")),
            TimeSlot(from = LocalTime.parse("09:15"), to = LocalTime.parse("13:00")),
            TimeSlot(from = LocalTime.parse("13:15"), to = LocalTime.parse("17:00")),
            TimeSlot(from = LocalTime.parse("17:15"), to = LocalTime.parse("23:59"))
        )

        assertThat(bookingsStorage.availableSlots(RoomName.AMAZE)).isEqualTo(expectedBookings)
        assertThat(bookingsStorage.availableSlots(RoomName.BEAUTY)).isEqualTo(expectedBookings)
        assertThat(bookingsStorage.availableSlots(RoomName.INSPIRE)).isEqualTo(expectedBookings)
        assertThat(bookingsStorage.availableSlots(RoomName.STRIVE)).isEqualTo(expectedBookings)
    }
}
