package org.example.app.controller

import org.example.app.service.BookingService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class BookingController(
    private val bookingService: BookingService,
) {

}
