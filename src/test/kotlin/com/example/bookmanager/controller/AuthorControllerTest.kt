package com.example.bookmanager.controller

import com.example.bookmanager.controller.dto.AuthorRequest
import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.service.AuthorService
import com.example.bookmanager.service.dto.AuthorServiceOutput
import com.example.bookmanager.service.dto.BookServiceOutput
import com.example.bookmanager.service.dto.FindBookListByAuthorServiceOutput
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * AuthorControllerの単体テスト。
 */
@WebMvcTest(controllers = [AuthorController::class, GlobalExceptionHandler::class])
@Import(AuthorControllerTest.TestConfig::class)
class AuthorControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val authorService: AuthorService
) {

    /**
     * テスト専用の設定クラス。
     * @TestConfiguration を使い、テストのアプリケーションコンテキストにモックBeanを提供する。
     */
    @TestConfiguration
    class TestConfig {
        @Bean
        fun authorService(): AuthorService = mock()
    }

    @Nested
    @DisplayName("POST /authors - 著者作成API")
    inner class CreateAuthor {
        @Test
        @DisplayName("正常系: 有効なリクエストで著者が作成され、201 CREATEDと作成された著者情報が返る")
        fun `when valid request, should return 201 CREATED and created author`() {
            // リクエストDTOを作成
            val request = AuthorRequest(
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // サービスアウトプットを作成
            val authorId = UUID.randomUUID()
            val serviceOutput = AuthorServiceOutput(
                authorId = authorId,
                name = request.name,
                birthDate = request.birthDate
            )

            // モックを設定
            whenever(authorService.createAuthor(any())) doReturn serviceOutput

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                post("/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(authorId.toString()))
                .andExpect(jsonPath("$.name").value(request.name))
                .andExpect(jsonPath("$.birthDate").value(request.birthDate.toString()))
        }
    }

    @Nested
    @DisplayName("PUT /authors/{authorId} - 著者更新API")
    inner class UpdateAuthor {
        @Test
        @DisplayName("正常系: 存在する著者IDに対して有効なリクエストで更新が成功し、200 OKと更新後の著者情報が返る")
        fun `when author exists, should return 200 OK and updated author`() {
            // リクエストDTOを作成
            val request = AuthorRequest(
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 更新対象著者のID
            val authorId = UUID.randomUUID()

            // サービスアウトプットを作成
            val serviceOutput = AuthorServiceOutput(
                authorId = authorId,
                name = request.name,
                birthDate = request.birthDate
            )

            // モックを設定
            whenever(authorService.updateAuthor(any())) doReturn serviceOutput

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                put("/authors/{authorId}", authorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(authorId.toString()))
                .andExpect(jsonPath("$.name").value(request.name))
                .andExpect(jsonPath("$.birthDate").value(request.birthDate.toString()))
        }

        @Test
        @DisplayName("異常系: 存在しない著者IDでリクエストした場合、404 Not Foundが返る")
        fun `when author does not exist, should return 404 Not Found`() {
            // リクエストDTOを作成
            val request = AuthorRequest(
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 更新対象書籍のID
            val nonExistentId = UUID.randomUUID()

            // モックを設定
            val errorMessage = "Author not found."
            whenever(authorService.updateAuthor(any())) doThrow AuthorNotFoundException(errorMessage)

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(
                put("/authors/{authorId}", nonExistentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                // レスポンスを検証
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value(errorMessage))
        }
    }

    @Nested
    @DisplayName("GET /authors/{authorId}/books - 著者に紐づく書籍一覧取得API")
    inner class GetBooksByAuthor {
        @Test
        @DisplayName("正常系: 著者が存在する場合、200 OKと書籍リストが返る")
        fun `when author exists, should return 200 OK and book list`() {
            // 著者情報を作成
            val authorId = UUID.randomUUID()
            val author = AuthorServiceOutput(
                authorId = authorId,
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 書籍情報を作成
            val bookId = UUID.randomUUID()
            val book = BookServiceOutput(
                bookId = bookId,
                title = "Kotlin spring boot",
                price = BigDecimal("3000"),
                status = "unpublished",
                authorList = listOf(author)
            )

            // サービスアウトプットを作成
            val serviceOutput = FindBookListByAuthorServiceOutput(
                author = author,
                bookList = listOf(book)
            )

            // モックを設定
            whenever(authorService.findBooksByAuthor(authorId)) doReturn serviceOutput

            // テスト対象エンドポイントへリクエスト送信
            mockMvc.perform(get("/authors/{authorId}/books", authorId))
                // レスポンスを検証
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.author.id").value(authorId.toString()))
                .andExpect(jsonPath("$.author.name").value(serviceOutput.author.name))
                .andExpect(jsonPath("$.author.birthDate").value(serviceOutput.author.birthDate.toString()))
                .andExpect(jsonPath("$.bookList").isArray)
                .andExpect(jsonPath("$.bookList[0].id").value(serviceOutput.bookList[0].bookId.toString()))
                .andExpect(jsonPath("$.bookList[0].title").value(serviceOutput.bookList[0].title))
                .andExpect(jsonPath("$.bookList[0].price").value(serviceOutput.bookList[0].price))
                .andExpect(jsonPath("$.bookList[0].status").value(serviceOutput.bookList[0].status))
                .andExpect(jsonPath("$.bookList[0].authorList[0].id").value(serviceOutput.bookList[0].authorList[0].authorId.toString()))
                .andExpect(jsonPath("$.bookList[0].authorList[0].name").value(serviceOutput.bookList[0].authorList[0].name))
                .andExpect(jsonPath("$.bookList[0].authorList[0].birthDate").value(serviceOutput.bookList[0].authorList[0].birthDate.toString()))
        }
    }
}

