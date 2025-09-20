package com.example.bookmanager.repository.converter

import java.util.UUID
import org.jooq.Record
import org.jooq.generated.tables.Books.BOOKS
import org.jooq.generated.enums.PublicationStatus as JooqPublicationStatus
import com.example.bookmanager.domain.Book
import com.example.bookmanager.domain.PublicationStatus

/**
 * ドメインのPublicationStatusをjOOQ生成のEnumに変換する。
 */
fun PublicationStatus.toJooqEnum(): JooqPublicationStatus = when (this) {
    PublicationStatus.PUBLISHED -> JooqPublicationStatus.published
    PublicationStatus.UNPUBLISHED -> JooqPublicationStatus.unpublished
}

/**
 * jOOQ生成のEnumをドメインのPublicationStatusに変換する。
 */
private fun JooqPublicationStatus.toDomainEnum(): PublicationStatus = when (this) {
    JooqPublicationStatus.published -> PublicationStatus.PUBLISHED
    JooqPublicationStatus.unpublished -> PublicationStatus.UNPUBLISHED
}

/**
 * jOOQのRecordをドメインのBookオブジェクトに変換する。
 */
fun Record.toBook(authorIdList: List<UUID>): Book {
    return Book(
//        id = this[BOOKS.ID],
//        title = this[BOOKS.TITLE]!!,
//        price = this[BOOKS.PRICE]!!,
//        status = this[BOOKS.STATUS]!!.toDomainEnum(),
        id = this.getOrThrow(BOOKS.ID, "id"),
        title = this.getOrThrow(BOOKS.TITLE, "title"),
        price = this.getOrThrow(BOOKS.PRICE, "price"),
        status = this.getOrThrow(BOOKS.STATUS, "status").toDomainEnum(),
        authorIdList = authorIdList
    )
}
