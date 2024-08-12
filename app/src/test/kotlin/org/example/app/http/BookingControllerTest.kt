package org.example.app.http

import org.assertj.core.api.Assertions.assertThat
import org.example.app.GenericSpringContextTest
import org.example.app.RoomName
import org.example.app.TimeSlot
import org.example.app.localTimeFrom
import org.example.app.service.BookingService
import org.jooq.generated.Tables
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

class BookingControllerTest : GenericSpringContextTest() {

    @Autowired
    private lateinit var bookingService: BookingService

    @BeforeEach
    fun beforeEachTest() {
        super.beforeEach()
        bookingService.resetInternalState()
    }

    @Test
    fun `test postBooking happy path`() {
        val request = HttpEntity(
            CreateBookingRequest(
                date = LocalDate.now(),
                numberOfPeople = 8,
                booking = TimeSlot(
                    from = localTimeFrom("07:00"),
                    to = localTimeFrom("07:30"),
                )
            )
        )

        val response = testRestTemplate.exchange(
            "/booking/",
            HttpMethod.POST,
            request,
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val respBody = objectMapper.readValue(response.body, CreateBookingResponse::class.java)
        assertThat(respBody.roomName).isEqualTo(RoomName.INSPIRE)

        val dbRecord = dslContext.selectFrom(Tables.BOOKINGS).fetchOne()
        assertThat(dbRecord!!.roomName).isEqualTo(RoomName.INSPIRE.toString())
        assertThat(dbRecord.fromTime).isEqualTo("07:00")
        assertThat(dbRecord.toTime).isEqualTo("07:30")
        assertThat(dbRecord.bookingSize).isEqualTo(8)
        assertThat(dbRecord.bookingDay).isEqualTo(LocalDate.now().toString())
    }

    @Test
    fun `test postBooking negative number of people`() {

        val request = HttpEntity(
            CreateBookingRequest(
                date = LocalDate.now(),
                numberOfPeople = -2,
                booking = TimeSlot(
                    from = localTimeFrom("07:00"),
                    to = localTimeFrom("07:30"),
                )
            )
        )
        val response = testRestTemplate.exchange(
            "/booking/",
            HttpMethod.POST,
            request,
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val respBody = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertThat(respBody.error).isEqualTo("Number of people cannot be a negative number, received '-2' in a request.")
    }

    @Test
    fun `test postBooking wrong date`() {
        val request = HttpEntity(
            CreateBookingRequest(
                date = LocalDate.now().plusDays(1),
                numberOfPeople = 2,
                booking = TimeSlot(
                    from = localTimeFrom("07:00"),
                    to = localTimeFrom("07:30"),
                )
            )
        )

        val response = testRestTemplate.exchange(
            "/booking/",
            HttpMethod.POST,
            request,
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val respBody = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertThat(respBody.error).isEqualTo(
            "Can only book rooms for today '${LocalDate.now()}', received '${
                LocalDate.now().plusDays(1)
            }' in a request."
        )
    }

    @Test
    fun `test postBooking all booked`() {
        val timeSlot = TimeSlot(
            from = localTimeFrom("07:00"),
            to = localTimeFrom("07:30"),
        )
        val request = HttpEntity(
            CreateBookingRequest(
                date = LocalDate.now(),
                numberOfPeople = 2,
                booking = timeSlot
            )
        )

        RoomName.entries.forEach {
            database.insertBooking(roomName = it, timeSlot = timeSlot, numberOfPeople = 2)
        }

        val response = testRestTemplate.exchange(
            "/booking/",
            HttpMethod.POST,
            request,
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        val respBody = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertThat(respBody.error).isEqualTo("No rooms ara available for 2 between 07:00 and 07:30.")
    }

    @Test
    fun `test postBooking maintenance window booking`() {
        val timeSlot = TimeSlot(
            from = localTimeFrom("09:00"),
            to = localTimeFrom("09:30"),
        )
        val request = HttpEntity(
            CreateBookingRequest(
                date = LocalDate.now(),
                numberOfPeople = 2,
                booking = timeSlot
            )
        )

        val response = testRestTemplate.exchange(
            "/booking/",
            HttpMethod.POST,
            request,
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        val respBody = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertThat(respBody.error).isEqualTo("Failed to block a room between 09:00 and 09:30 due to maintenance.")
    }

    @Test
    fun `test getAvailableSlots returns correct slots`() {
        val timeSlot = TimeSlot(
            from = localTimeFrom("07:00"),
            to = localTimeFrom("07:30"),
        )
        RoomName.entries.forEach {
            database.insertBooking(roomName = it, timeSlot = timeSlot, numberOfPeople = 2)
        }

        val response = testRestTemplate.exchange(
            "/booking/",
            HttpMethod.GET,
            HttpEntity("{}"),
            String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val respBody = objectMapper.readValue(response.body, GetAvailableSlotsResponse::class.java)

        val expectedInitialValues = listOf(
            TimeSlot(from = localTimeFrom("00:00"), to = localTimeFrom("07:00")),
            TimeSlot(from = localTimeFrom("07:30"), to = localTimeFrom("09:00")),
            TimeSlot(from = localTimeFrom("09:15"), to = localTimeFrom("13:00")),
            TimeSlot(from = localTimeFrom("13:15"), to = localTimeFrom("17:00")),
            TimeSlot(from = localTimeFrom("17:15"), to = localTimeFrom("23:59"))
        )
        RoomName.entries.forEach {
            assertThat(respBody.slots.getValue(it)).isEqualTo(expectedInitialValues)
        }
    }
}
