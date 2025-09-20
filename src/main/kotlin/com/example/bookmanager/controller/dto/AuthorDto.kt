package com.example.bookmanager.controller.dto

import java.time.LocalDate
import java.util.UUID

/**
 * 著者新規作成、更新APIへのリクエストを表すデータクラス。
 *
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class AuthorRequest(
    val name: String,
    val birthDate: LocalDate
)

/**
 * 著者情報のAPIレスポンスを表すデータクラス。
 *
 * @property id 著者ID
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class AuthorResponse(
    val id: UUID,
    val name: String,
    val birthDate: LocalDate,
)

