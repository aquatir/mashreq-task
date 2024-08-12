package org.example.app

import org.assertj.core.api.Assertions.assertThat
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
}
