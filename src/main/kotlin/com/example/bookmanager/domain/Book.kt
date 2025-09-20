package com.example.bookmanager.domain

import java.math.BigDecimal
import java.util.UUID

/**
 * 書籍の出版状況を表すEnum。
 */
enum class PublicationStatus {
    PUBLISHED,
    UNPUBLISHED
}

/**
 * 書籍を表すドメインエンティティ。
 *
 * @property id 書籍ID
 * @property title 書籍のタイトル
 * @property price 価格
 * @property authorIdList この書籍に関連する著者のリスト
 * @property status 出版状況
 */
data class Book(
    val id: UUID? = null,
    val title: String,
    val price: BigDecimal,
    val authorIdList: List<UUID>,
    val status: PublicationStatus
) {
    init {
        // タイトルが空でないこと
        require(title.isNotBlank()) { "Title cannot be blank." }
        // 価格が0以上であること
        require(price >= BigDecimal.ZERO) { "Price must be non-negative." }
        // 最低1人の著者がいること
        require(authorIdList.isNotEmpty()) { "A book must have at least one author." }
    }
}

