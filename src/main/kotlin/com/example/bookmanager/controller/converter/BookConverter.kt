package com.example.bookmanager.controller.converter

import com.example.bookmanager.controller.dto.BookResponse
import com.example.bookmanager.controller.dto.CreateBookRequest
import com.example.bookmanager.controller.dto.UpdateBookRequest
import com.example.bookmanager.service.dto.BookServiceOutput
import com.example.bookmanager.service.dto.CreateBookServiceInput
import com.example.bookmanager.service.dto.UpdateBookServiceInput
import java.util.UUID

/**
 * 書籍新規作成リクエストDTOからサービス実行用インプットを生成する
 */
fun CreateBookRequest.toServiceInput(): CreateBookServiceInput {
    return CreateBookServiceInput(
        title = this.title,
        price = this.price,
        authorIdList = this.authorIdList,
        status = this.status
    )
}

/**
 * サービス実行結果から書籍レスポンスDTOを生成する
 */
fun BookServiceOutput.toResponse(): BookResponse {
    return BookResponse(
        id = this.bookId,
        title = this.title,
        price = this.price,
        status = this.status,
        authorList = this.authorList.map { it.toResponse() },
    )
}

/**
 * 書籍IDと書籍更新リクエストDTOからサービス実行用インプットを生成する
 */
fun UpdateBookRequest.toServiceInput(bookId: UUID): UpdateBookServiceInput {
    return UpdateBookServiceInput(
        bookId = bookId,
        title = this.title,
        price = this.price,
        authorIdList = this.authorIdList,
        status = this.status
    )
}
