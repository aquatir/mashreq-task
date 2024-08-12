import org.example.app.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

open class BookingException(override val message: String) : RuntimeException(message)
class BookingFallsForMaintenance(override val message: String) : BookingException(message)
class AllRoomsAreBookedException(override val message: String) : BookingException(message)
class ClientRequestException(override val message: String) : RuntimeException(message)

@ControllerAdvice
class ControllerAdvice {

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(BookingException::class)
    fun handleBookingException(ex: BookingException): ErrorResponse {
        return ErrorResponse(ex.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ClientRequestException::class)
    fun handleClientRequestException(ex: ClientRequestException): ErrorResponse {
        return ErrorResponse(ex.message)
    }
}
