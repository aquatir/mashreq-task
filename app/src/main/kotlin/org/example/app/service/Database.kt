package org.example.app.service

import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Database(
    private val dslContext: DSLContext,
){

    // Acquire transactional advisory lock. Most only be executed from @Transactional context
    // see: https://www.postgresql.org/docs/15/functions-admin.html
    fun acquireTransactionalLockBlocking(lockId: Long) {
        dslContext.execute("SELECT pg_advisory_xact_lock($lockId)")
    }
}
