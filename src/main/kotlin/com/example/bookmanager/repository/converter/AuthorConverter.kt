package com.example.bookmanager.repository.converter

import com.example.bookmanager.domain.Author
import org.jooq.Record
import org.jooq.generated.tables.Authors.AUTHORS

/**
 * jOOQのRecordをドメインのAuthorオブジェクトに変換する。
 */
fun Record.toAuthor(): Author {
    return Author(
//        id = this[AUTHORS.ID],
//        name = this[AUTHORS.NAME]!!,
//        birthDate = this[AUTHORS.BIRTH_DATE]!!,
        id = this.getOrThrow(AUTHORS.ID, "id"),
        name = this.getOrThrow(AUTHORS.NAME, "name"),
        birthDate = this.getOrThrow(AUTHORS.BIRTH_DATE, "birthDate"),
    )
}
