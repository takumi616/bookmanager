package com.example.bookmanager.service.dto

import java.math.BigDecimal
import java.util.UUID

/**
 * 書籍新規作成エンドポイントのサービス実行用インプットを表すデータクラス。
 *
 * @property title タイトル
 * @property price 価格
 * @property authorIdList この書籍に関連する著者IDのリスト
 * @property status 出版状況
 */
data class CreateBookServiceInput(
    val title: String,
    val price: BigDecimal,
    val authorIdList: List<UUID>,
    val status: String
)

/**
 * 書籍情報更新エンドポイントのサービス実行用インプットを表すデータクラス。
 *
 * @property bookId 書籍ID
 * @property title タイトル
 * @property price 価格
 * @property authorIdList この書籍に関連する著者IDのリスト
 * @property status 出版状況
 */
data class UpdateBookServiceInput(
    val bookId: UUID,
    val title: String,
    val price: BigDecimal,
    val authorIdList: List<UUID>,
    val status: String
)

/**
 * 書籍情報更新エンドポイントのサービス実行用インプットを表すデータクラス。
 *
 * @property bookId 書籍ID
 * @property title タイトル
 * @property price 価格
 * @property status 出版状況
 * @property authorList この書籍に関連する著者のリスト
 */
data class BookServiceOutput(
    val bookId: UUID,
    val title: String,
    val price: BigDecimal,
    val status:String,
    val authorList: List<AuthorServiceOutput>
)

