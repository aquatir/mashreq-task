package org.example.app

import org.assertj.core.api.Assertions.assertThat
import org.example.app.RoomName.AMAZE
import org.example.app.RoomName.BEAUTY
import org.example.app.RoomName.INSPIRE
import org.example.app.RoomName.STRIVE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RoomNameTest {

    companion object {
        @JvmStatic
        fun provideDataForRoomTest() = listOf(
            arrayOf(1, listOf(AMAZE, BEAUTY, INSPIRE, STRIVE)),
            arrayOf(2, listOf(AMAZE, BEAUTY, INSPIRE, STRIVE)),
            arrayOf(3, listOf(AMAZE, BEAUTY, INSPIRE, STRIVE)),
            arrayOf(4, listOf(BEAUTY, INSPIRE, STRIVE)),
            arrayOf(6, listOf(BEAUTY, INSPIRE, STRIVE)),
            arrayOf(7, listOf(BEAUTY, INSPIRE, STRIVE)),
            arrayOf(8, listOf(INSPIRE, STRIVE)),
            arrayOf(11, listOf(INSPIRE, STRIVE)),
            arrayOf(12, listOf(INSPIRE, STRIVE)),
            arrayOf(13, listOf(STRIVE)),
            arrayOf(19, listOf(STRIVE)),
            arrayOf(20, listOf(STRIVE)),
            arrayOf(21, emptyList<RoomName>()),
            // Add more data sets here
        )
    }

    @ParameterizedTest
    @MethodSource("provideDataForRoomTest")
    fun `test sufficientRooms returns correct rooms`(numberOfPeople: Int, sortedListOfRooms: List<RoomName>) {
        val rooms = RoomName.sufficientRooms(numberOfPeople)
        assertThat(rooms).isEqualTo(sortedListOfRooms)
    }
}
