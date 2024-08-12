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
                tryBlockSlot(roomName, timeSlot)
            }
        }
    }

    fun availableSlots(roomName: RoomName): List<TimeSlot> = this.availableSlots.getValue(roomName)

    fun blockSlot(timeSlot: TimeSlot, blockedFor: Int) {
        val potentialRooms = RoomName.sufficientRooms(blockedFor)

        var blocked = false
        for (room in potentialRooms) {
            blocked = tryBlockSlot(room, timeSlot)
            if (blocked) {
                break
            }
        }
        if (!blocked) {
            throw RuntimeException("failed to block room")
        }
    }

    private fun tryBlockSlot(roomName: RoomName, timeSlot: TimeSlot): Boolean {
        val roomSlots = availableSlots.getValue(roomName)

        val iterator = roomSlots.iterator()
        var blocked = false
        while (iterator.hasNext()) {
            val freeSlot = iterator.next()

            // if can feet the timeSlot into free slot => fit it in, potentially splitting the free slots list
            if (freeSlot.from.isBeforeOrEqual(timeSlot.from) && freeSlot.to.isAfterOrEqual(timeSlot.to)) {
                blocked = true
                iterator.remove()

                if (freeSlot.from != timeSlot.from) {
                    roomSlots.add(TimeSlot(from = freeSlot.from, to = timeSlot.from))
                }
                if (timeSlot.to != freeSlot.to) {
                    roomSlots.add(TimeSlot(from = timeSlot.to, to = freeSlot.to))
                }

                break
            }
        }
        roomSlots.sortBy { it.from }
        return blocked
    }

    private fun time(str: String): LocalTime = LocalTime.parse(str)

    private fun LocalTime.isBeforeOrEqual(otherTime: LocalTime): Boolean = this.isBefore(otherTime) || this == otherTime
    private fun LocalTime.isAfterOrEqual(otherTime: LocalTime): Boolean = this.isAfter(otherTime) || this == otherTime
}
