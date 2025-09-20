package com.example.bookmanager.controller

import com.example.bookmanager.controller.dto.CreateBookRequest
import com.example.bookmanager.controller.dto.UpdateBookRequest
import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.exception.BookNotFoundException
import com.example.bookmanager.service.BookService
import com.example.bookmanager.service.dto.AuthorServiceOutput
import com.example.bookmanager.service.dto.BookServiceOutput
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * BookControllerの単体テスト。
 */
@WebMvcTest(controllers = [BookController::class, GlobalExceptionHandler::class])
@Import(BookControllerTest.TestConfig::class)
class BookControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val bookService: BookService
) {
    /**
     * テスト専用の設定クラス。
     * @TestConfiguration を使い、テストのアプリケーションコンテキストにモックBeanを提供する。
     */
    @TestConfiguration
    class TestConfig {
        @Bean
        fun bookService(): BookService = mock()
    }

    // 共通で使用する著者情報テストデータ
    private val authorId = UUID.randomUUID()
    private val authorServiceOutput = AuthorServiceOutput(
        authorId = authorId,
        name = "著者A",
        birthDate = LocalDate.of(1995, 1, 1)
    )

    @Nested
    @DisplayName("POST /books - 書籍作成API")
    inner class CreateBook {
        @Test
        @DisplayName("正常系: 有効なリクエストで書籍が作成され、201 CREATEDと作成された書籍情報が返る")
        fun `should return 201 CREATED with the created book when request is valid`() {
            // リクエストDTOを作成
            val request = CreateBookRequest(
                title = "Kotlin spring boot",
                price = BigDecimal("3000"),
                authorIdList = listOf(authorId),
                status = "unpublished"
            )

            // サービスアウトプットを作成
            val bookId = UUID.randomUUID()
            val serviceOutput = BookServiceOutput(
                bookId = bookId,
                title = request.title,
                price = request.price,
                status = request.status,
                authorList = listOf(authorServiceOutput)
            )

            // モックを設定
            whenever(bookService.createBook(any())) doReturn serviceOutput

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                post("/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.title").value(request.title))
                .andExpect(jsonPath("$.price").value(request.price))
                .andExpect(jsonPath("$.status").value(request.status))
                .andExpect(jsonPath("$.authorList[0].id").value(request.authorIdList[0].toString()))
                .andExpect(jsonPath("$.authorList[0].name").value(authorServiceOutput.name))
                .andExpect(jsonPath("$.authorList[0].birthDate").value(authorServiceOutput.birthDate.toString()))
        }

        @Test
        @DisplayName("異常系: 存在しない著者IDを指定した場合、404 Not Foundが返る")
        fun `should return 404 Not Found when an author ID does not exist`() {
            // リクエストDTOを作成
            val request = CreateBookRequest(
                title = "Kotlin spring boot",
                price = BigDecimal("3000"),
                authorIdList = listOf(UUID.randomUUID()),
                status = "unpublished"
            )

            // モックを設定
            val errorMessage = "One or more authors not found."
            whenever(bookService.createBook(any())) doThrow AuthorNotFoundException(errorMessage)

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                post("/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value(errorMessage))
        }
    }

    @Nested
    @DisplayName("PUT /books/{bookId} - 書籍更新API")
    inner class UpdateBook {
        @Test
        @DisplayName("正常系: 存在する書籍IDに有効なリクエストで更新が成功し、200 OKと更新後の書籍情報が返る")
        fun `should return 200 OK with the updated book when book exists`() {
            // リクエストDTOを作成
            val request = UpdateBookRequest(
                title = "Kotlin spring boot",
                price = BigDecimal("3000"),
                authorIdList = listOf(authorId),
                status = "published"
            )

            // 更新対象書籍のID
            val bookId = UUID.randomUUID()

            // サービスアウトプットを作成
            val serviceOutput = BookServiceOutput(
                bookId = bookId,
                title = request.title,
                price = request.price,
                status = request.status,
                authorList = listOf(authorServiceOutput)
            )

            // モックを設定
            whenever(bookService.updateBook(any())) doReturn serviceOutput

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                put("/books/{bookId}", bookId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.title").value(request.title))
                .andExpect(jsonPath("$.price").value(request.price))
                .andExpect(jsonPath("$.status").value(request.status))
                .andExpect(jsonPath("$.authorList[0].id").value(request.authorIdList[0].toString()))
                .andExpect(jsonPath("$.authorList[0].name").value(authorServiceOutput.name))
                .andExpect(jsonPath("$.authorList[0].birthDate").value(authorServiceOutput.birthDate.toString()))
        }

        @Test
        @DisplayName("異常系: 存在しない書籍IDでリクエストした場合、404 Not Foundが返る")
        fun `should return 404 Not Found when book ID does not exist`() {
            // リクエストDTOを作成
            val request = UpdateBookRequest(
                title = "Kotlin spring boot",
                price = BigDecimal("3000"),
                authorIdList = listOf(authorId),
                status = "published"
            )

            // 更新対象書籍のID
            val notFoundBookId = UUID.randomUUID()

            // モックを設定
            val errorMessage = "Book with ID $notFoundBookId not found."
            whenever(bookService.updateBook(any())) doThrow BookNotFoundException(errorMessage)

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                put("/books/{bookId}", notFoundBookId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value(errorMessage))
        }
    }
}