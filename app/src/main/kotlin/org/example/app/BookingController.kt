package org.example.app

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class BookingController(
    private val bookingService: BookingService,
) {

}
