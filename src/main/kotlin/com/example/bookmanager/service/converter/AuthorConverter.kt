package com.example.bookmanager.service.converter

import com.example.bookmanager.domain.Author
import com.example.bookmanager.service.dto.AuthorServiceOutput

/**
 * 著者ドメインデータからサービス実行結果を生成する
 */
fun Author.toAuthorServiceOutput(): AuthorServiceOutput {
    require(this.id != null) {
        "Cannot create a response from a non-persisted Book entity."
    }

    return AuthorServiceOutput(
        authorId = this.id,
        name = this.name,
        birthDate = this.birthDate
    )
}