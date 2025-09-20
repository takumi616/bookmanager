package com.example.bookmanager.controller.converter

import com.example.bookmanager.controller.dto.AuthorRequest
import com.example.bookmanager.controller.dto.AuthorResponse
import java.util.UUID

import com.example.bookmanager.service.dto.AuthorServiceOutput
import com.example.bookmanager.service.dto.CreateAuthorServiceInput
import com.example.bookmanager.service.dto.UpdateAuthorServiceInput

/**
 * 著者新規作成リクエストDTOからサービス実行用インプットを生成する
 */
fun AuthorRequest.toCreateAuthorServiceInput(): CreateAuthorServiceInput {
    return CreateAuthorServiceInput(
        name = this.name,
        birthDate = this.birthDate
    )
}

/**
 * サービス実行結果から著者レスポンスDTOを生成する
 */
fun AuthorServiceOutput.toResponse(): AuthorResponse {
    return AuthorResponse(
        id = this.authorId,
        name = this.name,
        birthDate = this.birthDate
    )
}

/**
 * 書籍IDと書籍更新リクエストDTOからサービス実行用インプットを生成する
 */
fun AuthorRequest.toUpdateAuthorServiceInput(authorId: UUID): UpdateAuthorServiceInput {
    return UpdateAuthorServiceInput(
        authorId = authorId,
        name = this.name,
        birthDate = this.birthDate
    )
}
