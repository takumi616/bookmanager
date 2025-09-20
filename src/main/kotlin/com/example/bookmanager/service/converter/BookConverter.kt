package com.example.bookmanager.service.converter

import com.example.bookmanager.domain.Author
import com.example.bookmanager.domain.Book
import com.example.bookmanager.service.dto.AuthorServiceOutput
import com.example.bookmanager.service.dto.BookServiceOutput

/**
 * 書籍ドメインデータからサービス実行結果を生成する
 */
fun Book.toBookServiceOutput(authorList: List<Author>): BookServiceOutput {
    require(this.id != null) {
        "Cannot create a response from a non-persisted Book entity."
    }

    // authorsフィールドの変換処理
    val authorServiceOutputList = authorList.map { author ->
        require(author.id != null) {
            "Cannot create a response from a non-persisted Book entity."
        }
        AuthorServiceOutput(
            authorId = author.id,
            name = author.name,
            birthDate = author.birthDate
        )
    }

    return BookServiceOutput(
        bookId = this.id,
        title = this.title,
        price = this.price,
        status = this.status.name.lowercase(),
        authorList = authorServiceOutputList
    )
}