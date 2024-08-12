package org.example.app

import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Database(
    private val dslContext: DSLContext,
){

}
