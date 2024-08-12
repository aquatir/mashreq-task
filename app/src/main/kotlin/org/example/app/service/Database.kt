package org.example.app.service

import org.example.app.RoomName
import org.example.app.TimeSlot
import org.jooq.DSLContext
import org.jooq.generated.Tables.BOOKINGS
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// TODO: Add tests for methods
@Service
class Database(
    private val dslContext: DSLContext,
) {

    // Acquire transactional advisory lock. Most only be executed from @Transactional context
    // see: https://www.postgresql.org/docs/15/functions-admin.html
    fun acquireTransactionalLockBlocking(lockId: Long) {
        dslContext.execute("SELECT pg_advisory_xact_lock($lockId)")
    }

    fun selectBookingsForDay(date: LocalDate): Map<RoomName, List<TimeSlot>> {
        return dslContext.select(BOOKINGS.ROOM_NAME, BOOKINGS.FROM_TIME, BOOKINGS.TO_TIME)
            .from(BOOKINGS)
            .where(BOOKINGS.BOOKING_DAY.eq(date.toString()))
            .fetch()
            .groupBy { it[BOOKINGS.ROOM_NAME] }
            .mapKeys { RoomName.valueOf(it.key) }
            .mapValues { mapEntry ->
                mapEntry.value.map { record ->
                    TimeSlot(
                        from = LocalTime.parse(record[BOOKINGS.FROM_TIME]),
                        to = LocalTime.parse(record[BOOKINGS.TO_TIME]),
                    )
                }
            }
    }

    fun insertBooking(roomName: RoomName, timeSlot: TimeSlot, numberOfPeople: Int) {
        dslContext.insertInto(BOOKINGS)
            .set(BOOKINGS.ID, UUID.randomUUID())
            .set(BOOKINGS.ROOM_NAME, roomName.toString())
            .set(BOOKINGS.BOOKING_DAY, LocalDate.now().toString())
            .set(BOOKINGS.FROM_TIME, timeSlot.from.toString())
            .set(BOOKINGS.TO_TIME, timeSlot.to.toString())
            .set(BOOKINGS.BOOKING_SIZE, numberOfPeople)
            .execute()
    }
}
