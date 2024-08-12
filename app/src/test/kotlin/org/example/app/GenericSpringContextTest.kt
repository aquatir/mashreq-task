package org.example.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.app.service.Database
import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles

/**
 * Extend this class to run tests using Spring Context
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class GenericSpringContextTest {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var database: Database

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @AfterEach
    fun afterEach() {
        reset()
    }

    @BeforeEach
    fun beforeEach() {
        reset()
    }

    private fun reset() {
        dslContext.truncate(Tables.BOOKINGS).execute()
    }
}
