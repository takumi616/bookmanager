package com.example.bookmanager.service.dto

import java.time.LocalDate
import java.util.UUID

/**
 * 著者新規作成エンドポイントのサービス実行用インプットを表すデータクラス。
 *
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class CreateAuthorServiceInput(
    val name: String,
    val birthDate: LocalDate
)

/**
 * 著者情報更新エンドポイントのサービス実行用インプットを表すデータクラス。
 *
 * @property authorId 著者ID
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class UpdateAuthorServiceInput(
    val authorId: UUID,
    val name: String,
    val birthDate: LocalDate
)

/**
 * 著者新規作成、更新エンドポイントのサービス実行結果を表すデータクラス。
 *
 * @property authorId 著者ID
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class AuthorServiceOutput(
    val authorId: UUID,
    val name: String,
    val birthDate: LocalDate
)

/**
 * 著者に紐づく書籍一覧を取得するエンドポイントのサービス実行結果を表すデータクラス。
 *
 * @property author 著者取得サービスの実行結果
 * @property bookList 書籍一覧取得サービスの実行結果
 */
data class FindBookListByAuthorServiceOutput(
    val author: AuthorServiceOutput,
    val bookList: List<BookServiceOutput>
)


