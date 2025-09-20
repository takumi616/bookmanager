package com.example.bookmanager.controller.dto

import java.math.BigDecimal
import java.util.UUID

/**
 * 書籍新規作成APIへのリクエストを表すデータクラス。
 *
 * @property title タイトル
 * @property price 価格
 * @property authorIdList この書籍に関連する著者IDのリスト
 * @property status 出版状況
 */
data class CreateBookRequest(
    val title: String,
    val price: BigDecimal,
    val authorIdList: List<UUID>,
    val status: String
)

/**
 * 書籍更新APIへのリクエストを表すデータクラス。
 *
 * @property title タイトル
 * @property price 価格
 * @property authorIdList この書籍に関連する著者IDのリスト
 * @property status 出版状況(未出版/出版済み)
 */
data class UpdateBookRequest(
    val title: String,
    val price: BigDecimal,
    val authorIdList: List<UUID>,
    val status: String
)

/**
 * 書籍情報のAPIレスポンスを表すデータクラス。
 *
 * @property id 書籍ID
 * @property title タイトル
 * @property price 価格
 * @property status 出版状況(未出版/出版済み)
 * @property authorList この書籍に関連する著者のリスト
 */
data class BookResponse(
    val id: UUID,
    val title: String,
    val price: BigDecimal,
    val status: String,
    val authorList: List<AuthorResponse>
)

/**
 * 著者に紐づく書籍を取得するAPIのレスポンスを表すデータクラス。
 * @property author 著者情報のレスポンスDTO
 * @property bookList 書籍情報のレスポンスDTOのリスト
 */
data class GetBookListResponse(
    val author: AuthorResponse,
    val bookList: List<BookResponse>
)

