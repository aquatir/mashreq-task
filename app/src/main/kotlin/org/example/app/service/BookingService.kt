package org.example.app.service

import org.example.app.RoomName
import org.example.app.TimeSlot
import org.example.app.http.AllRoomsAreBookedException
import org.example.app.http.BookingFallsForMaintenance
import org.example.app.localTimeFrom
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

/**
 * Keeps available free slots inside availableSlots map at all times
 */
@Service
class BookingService(
    private val database: Database
) {

    companion object {
        // DB lock keys. The value should be repeated between replicas to make sure it's exclusive, but should not be
        // repeated between different use-cases. to simplify, here we use a long integer
        // TODO: should most likely be a parameter computed from unique service name in real deployment
        const val LOCK_ID = 9876642341L
    }

    // Hardcoded list of maintenance windows
    // TODO: More extensible design would be to save the maintenance window into config / database
    private val maintenanceSlots = listOf(
        TimeSlot(from = LocalTime.parse("09:00"), to = LocalTime.parse("09:15")),
        TimeSlot(from = LocalTime.parse("13:00"), to = LocalTime.parse("13:15")),
        TimeSlot(from = LocalTime.parse("17:00"), to = LocalTime.parse("17:15")),
    )

    // a sorted list of available intervals for each room
    private var availableSlots: Map<RoomName, MutableList<TimeSlot>> = initialize()

    private fun initialize(): Map<RoomName, MutableList<TimeSlot>> {
        val map = HashMap<RoomName, MutableList<TimeSlot>>()
        RoomName.entries.forEach { roomName ->
            map[roomName] = mutableListOf(TimeSlot(from = localTimeFrom("00:00"), to = localTimeFrom("23:59")))
        }

        return map
    }

    init {
        addMaintenanceSlots()
    }

    @Transactional // to support locking between multiple replicas with acquireTransactionalLockBlocking
    fun availableSlots(
        from: LocalTime? = null,
        to: LocalTime? = null,
    ): Map<RoomName, List<TimeSlot>> = synchronized(this) { // sync to preserve internal state
        database.acquireTransactionalLockBlocking(LOCK_ID)
        updateAvailableSlots()
        RoomName.entries.associateWith { availableSlots(roomName = it, from = from, to = to) }
    }

    private fun availableSlots(
        roomName: RoomName,
        from: LocalTime? = null,
        to: LocalTime? = null,
    ): List<TimeSlot> {
        val fromNonNull = from ?: localTimeFrom("00:00")
        val toNonNull = to ?: localTimeFrom("23:59")

        val slots = this.availableSlots.getValue(roomName)
        val result = mutableListOf<TimeSlot>()

        for (slot in slots) {
            when {
                // skipping: before target interval
                slot.to <= fromNonNull -> continue

                // 'from' is outside interval, but 'to' is inside => cutting the slot
                // make sure we don't add slot with the same from/to time after cutting
                slot.from <= fromNonNull && slot.to in fromNonNull..toNonNull -> if (slot.to != fromNonNull) {
                    result.add(slot.copy(from = fromNonNull))
                }

                // slot is fully inside required brackets
                slot.from in fromNonNull..toNonNull && slot.to in fromNonNull..toNonNull -> result.add(slot.copy())

                // 'from' is inside interval, but 'to' is outside => cutting the slot
                // make sure we don't add slot with the same from/to time after cutting
                slot.from in fromNonNull..toNonNull && slot.to >= toNonNull -> if (slot.from != toNonNull) {
                    result.add(slot.copy(to = toNonNull))
                }

                // skipping slot: after target interval
                // also breaking, because slots are sorter, and if this happens once, it will happen for all future slots
                // as well
                slot.from >= toNonNull -> break
            }
        }

        return result
    }

    /**
     * Find a room and block it, throws exception if it's not possible
     *
     * @throws BookingFallsForMaintenance if [timeSlot] falls on [maintenanceSlots]
     * @throws AllRoomsAreBookedException if none of the rooms are available
     */
    @Transactional // to support locking between multiple replicas with acquireTransactionalLockBlocking
    fun blockSlot(timeSlot: TimeSlot, blockedFor: Int): RoomName {
        // Sync access on this replica layer to be extra sure no 2 updates happen as the same which is needed due to internal state management in memory
        return synchronized(this) {
            checkNotMaintenanceWindow(timeSlot)
            database.acquireTransactionalLockBlocking(LOCK_ID)

            updateAvailableSlots()
            val sufficientlyLargeRooms = RoomName.sufficientRooms(blockedFor)

            for (room in sufficientlyLargeRooms) {
                val blocked = tryBlockSlotInMemory(room, timeSlot)
                if (blocked) {
                    database.insertBooking(
                        roomName = room,
                        timeSlot = timeSlot,
                        numberOfPeople = blockedFor,
                    )
                    return@synchronized room
                }
            }

            throw AllRoomsAreBookedException("No rooms ara available for $blockedFor between ${timeSlot.from} and ${timeSlot.to}.")
        }
    }

    /**
     * Test helper to reset internal state between test runs. Don't use for production code
     */
    fun resetInternalState() {
        availableSlots = initialize()
        addMaintenanceSlots()
    }

    private fun addMaintenanceSlots() {
        RoomName.entries.forEach { roomName ->
            maintenanceSlots.forEach { timeSlot ->
                tryBlockSlotInMemory(roomName, timeSlot)
            }
        }
    }

    // Read data from DB to update a list of available slots
    private fun updateAvailableSlots() {
        val bookings = database.selectBookingsForDay(date = LocalDate.now())
        bookings.forEach { (room, existingBookings) ->
            existingBookings.forEach { oneBooking ->
                tryBlockSlotInMemory(room, oneBooking)
            }
        }
    }

    /**
     * Check if timeSlot falls on a maintenance window to throw a specific exception.
     * Could drop this check completely if general exception is enough.
     */
    private fun checkNotMaintenanceWindow(timeSlot: TimeSlot) {
        maintenanceSlots.forEach { maintenanceSlot ->
            if (
                (timeSlot.from >= maintenanceSlot.from && timeSlot.from <= maintenanceSlot.to)
                || (timeSlot.to >= maintenanceSlot.from && timeSlot.to <= maintenanceSlot.to)
            ) {
                throw BookingFallsForMaintenance("Failed to block a room between ${timeSlot.from} and ${timeSlot.to} due to maintenance.")
            }
        }
    }

    /**
     * Try to block a specific room if it's available, return true on success, and false on failure
     */
    private fun tryBlockSlotInMemory(roomName: RoomName, timeSlot: TimeSlot): Boolean {
        val roomSlots = availableSlots.getValue(roomName)

        val iterator = roomSlots.iterator()
        var blocked = false
        while (iterator.hasNext()) {
            val freeSlot = iterator.next()

            // if we can fit the timeSlot into free slot => fit it in, potentially splitting the free slots list
            if (freeSlot.from <= timeSlot.from && freeSlot.to >= timeSlot.to) {
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
}
