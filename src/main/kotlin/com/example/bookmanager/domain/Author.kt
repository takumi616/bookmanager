package com.example.bookmanager.domain

import java.time.LocalDate
import java.util.UUID

/**
 * 著者を表すドメインエンティティ。
 *
 * @property id 著者ID
 * @property name 著者名
 * @property birthDate 生年月日
 */
data class Author(
    val id: UUID? = null,
    val name: String,
    val birthDate: LocalDate,
) {
    init {
        // 著者名が空でないことを保証
        require(name.isNotBlank()) { "Author name cannot be blank." }
        // 生年月日が現在の日付よりも過去であることを保証
        require(birthDate.isBefore(LocalDate.now())) { "Birth date must be in the past." }
    }
}

