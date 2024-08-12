package org.example.app

import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class BookingsStorage(
//    private val database: Database,
) {

    private val maintenanceSlots = listOf(
        TimeSlot(from = LocalTime.parse("09:00"), to = LocalTime.parse("09:15")),
        TimeSlot(from = LocalTime.parse("13:00"), to = LocalTime.parse("13:15")),
        TimeSlot(from = LocalTime.parse("17:00"), to = LocalTime.parse("17:15")),
    )

    private val availableSlots: Map<RoomName, MutableList<TimeSlot>> = initialize()

    private fun initialize(): Map<RoomName, MutableList<TimeSlot>> {
        val map = HashMap<RoomName, MutableList<TimeSlot>>()
        RoomName.entries.forEach { roomName ->
            map[roomName] = mutableListOf(TimeSlot(from = time("00:00"), to = time("23:59")))
        }
        return map
    }

    init {
        RoomName.entries.forEach { roomName ->
            maintenanceSlots.forEach { timeSlot ->
                blockSlot(roomName, timeSlot)
            }
        }
    }

    fun availableSlots(roomName: RoomName): List<TimeSlot> = this.availableSlots.getValue(roomName)


    private fun blockSlot(roomName: RoomName, timeSlot: TimeSlot) {
        val roomSlots = availableSlots.getValue(roomName)

        val iterator = roomSlots.iterator()
        var blocked = false
        while (iterator.hasNext()) {
            val freeSlots = iterator.next()

            if (freeSlots.from.isBeforeOrEqual(timeSlot.to) && freeSlots.to.isAfterOrEqual(timeSlot.from)) {
                blocked = true
                iterator.remove()

                roomSlots.add(TimeSlot(from = freeSlots.from, to = timeSlot.from))
                roomSlots.add(TimeSlot(from = timeSlot.to, to = freeSlots.to))
                break
            }
        }
        if (!blocked) {
            throw RuntimeException("no slots")
        }

        roomSlots.sortBy { it.from }
    }

    private fun time(str: String): LocalTime = LocalTime.parse(str)

    private fun LocalTime.isBeforeOrEqual(otherTime: LocalTime): Boolean = this.isBefore(otherTime) || this == otherTime
    private fun LocalTime.isAfterOrEqual(otherTime: LocalTime): Boolean = this.isAfter(otherTime) || this == otherTime
}
