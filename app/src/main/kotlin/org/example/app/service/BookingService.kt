package org.example.app.service

import AllRoomsAreBookedException
import BookingFallsForMaintenance
import org.example.app.RoomName
import org.example.app.TimeSlot
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class BookingService(
    private val database: Database
) {

    companion object {
        // should be repeated between replicas to make sure it's exclusive, but should not be repeated between different use-cases
        // to simplify, here we use a long integer
        const val LOCK_ID = 9876642341L
    }

    // Hardcoded list of maintenance windows
    // More extensible design would be to save the maintenance window into config / database
    private val maintenanceSlots = listOf(
        TimeSlot(from = LocalTime.parse("09:00"), to = LocalTime.parse("09:15")),
        TimeSlot(from = LocalTime.parse("13:00"), to = LocalTime.parse("13:15")),
        TimeSlot(from = LocalTime.parse("17:00"), to = LocalTime.parse("17:15")),
    )

    // a sorted list of available intervals for each room
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

    @Transactional // to support locking between multiple replicas with acquireTransactionalLockBlocking
    fun availableSlots(roomName: RoomName): List<TimeSlot> {
        database.acquireTransactionalLockBlocking(LOCK_ID)
        updateAvailableSlots()
        return this.availableSlots.getValue(roomName)
    }

    /**
     * Find a room and block it, throws exception if it's not possible
     *
     * @throws BookingFallsForMaintenance if [timeSlot] falls on [maintenanceSlots]
     * @throws AllRoomsAreBookedException if none of the rooms are available
     */
    @Transactional // to support locking between multiple replicas with acquireTransactionalLockBlocking
    fun blockSlot(timeSlot: TimeSlot, blockedFor: Int): RoomName {
        // sync access on this replica layer
        return synchronized(this) {
            checkNotMaintenanceWindow(timeSlot)
            database.acquireTransactionalLockBlocking(LOCK_ID)

            updateAvailableSlots()
            val sufficientlyLargeRooms = RoomName.sufficientRooms(blockedFor)

            for (room in sufficientlyLargeRooms) {
                val blocked = tryBlockSlot(room, timeSlot)
                if (blocked) {
                    return@synchronized room
                }
            }
            throw AllRoomsAreBookedException("No rooms ara available for $blockedFor between ${timeSlot.from} and ${timeSlot.to}.")
        }
    }

    // Read data from DB to update a list of available slots
    private fun updateAvailableSlots() {

    }

    /**
     * Check if timeSlot falls on a maintenance window to throw a specific exception.
     * Could drop this check completely if general exception is enough.
     */
    private fun checkNotMaintenanceWindow(timeSlot: TimeSlot) {
        maintenanceSlots.forEach { maintenanceSlot ->
            if (
                (timeSlot.from.isAfterOrEqual(maintenanceSlot.from) && timeSlot.from.isBeforeOrEqual(maintenanceSlot.to))
                || (timeSlot.to.isAfterOrEqual(maintenanceSlot.from) && timeSlot.to.isBeforeOrEqual(maintenanceSlot.to))
            ) {
                throw BookingFallsForMaintenance("Failed to block a room between ${timeSlot.from} and ${timeSlot.to} due to maintenance.")
            }
        }
    }

    /**
     * Try to block a specific room if it's available, return true on success, and false on failure
     */
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
