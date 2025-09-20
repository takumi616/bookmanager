package com.example.bookmanager.repository.converter

import org.jooq.Record

/**
 * jOOQのRecordから非nullが期待される値を取得する。
 */
fun <T> Record.getOrThrow(field: org.jooq.Field<T>, fieldName: String): T {
    return this[field] ?: throw IllegalStateException("Field '$fieldName' is unexpectedly null in the database record.")
}
