package com.example.bookmanager.service

import com.example.bookmanager.domain.Author
import com.example.bookmanager.domain.Book
import com.example.bookmanager.domain.PublicationStatus
import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.exception.BookNotFoundException
import com.example.bookmanager.repository.AuthorRepository
import com.example.bookmanager.repository.BookRepository
import com.example.bookmanager.service.dto.CreateBookServiceInput
import com.example.bookmanager.service.dto.UpdateBookServiceInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * BookServiceテスト。
 */
@ExtendWith(MockitoExtension::class)
class BookServiceTest {

    @InjectMocks
    private lateinit var bookService: BookService

    @Mock
    private lateinit var bookRepository: BookRepository

    @Mock
    private lateinit var authorRepository: AuthorRepository

    // 共通で使用する著者のテストデータ
    private val authorId1 = UUID.randomUUID()
    private val authorId2 = UUID.randomUUID()
    private val author1 = Author(id = authorId1, name = "テスト著者A", birthDate = LocalDate.now().minusYears(30))
    private val author2 = Author(id = authorId2, name = "テスト著者B", birthDate = LocalDate.now().minusYears(40))

    @Nested
    @DisplayName("createBookメソッド")
    inner class CreateBook {
        @Test
        @DisplayName("正常系: 新しい書籍を正しく作成できる")
        fun `should create a new book successfully`() {
            // サービスインプットを定義
            val input = CreateBookServiceInput(
                title = "Kotlin spring boot",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1, authorId2),
                status = "unpublished"
            )

            // BookドメインモデルのCaptorを設定
            val bookCaptor = argumentCaptor<Book>()

            // 作成される書籍情報の期待値
            val createdBook = Book(
                id = UUID.randomUUID(),
                title = input.title,
                price = input.price,
                authorIdList = input.authorIdList,
                status = PublicationStatus.valueOf(input.status.uppercase()),
            )

            // モックを設定
            whenever(authorRepository.countByIdList(any())) doReturn 2
            whenever(authorRepository.findByIdList(any())) doReturn listOf(author1, author2)
            whenever(bookRepository.save(any())) doReturn createdBook

            // テスト対象メソッドの呼び出し
            val output = bookService.createBook(input)

            // サービスアウトプットの検証
            assertThat(output.bookId).isEqualTo(createdBook.id)
            assertThat(output.title).isEqualTo(createdBook.title)
            assertThat(output.price).isEqualTo(createdBook.price)
            assertThat(output.status).isEqualTo(createdBook.status.toString().lowercase())
            assertThat(output.authorList).hasSize(2)

            // saveメソッドが1回呼ばれたことを確認し、その時の引数をキャプチャ
            verify(bookRepository).save(bookCaptor.capture())

            // キャプチャした引数の中身を検証
            val bookPassedToSave = bookCaptor.firstValue
            assertThat(bookPassedToSave.id).isNull()
            assertThat(bookPassedToSave.title).isEqualTo(input.title)
            assertThat(bookPassedToSave.price).isEqualTo(input.price)
            assertThat(bookPassedToSave.authorIdList).isEqualTo(input.authorIdList)
            assertThat(bookPassedToSave.status).isEqualTo(PublicationStatus.valueOf(input.status.uppercase()))

            // linkAuthorListが正しい引数で呼ばれたことを確認
            verify(bookRepository).linkAuthorList(createdBook.id!!, createdBook.authorIdList)
        }

        @Test
        @DisplayName("異常系: 出版ステータスがunpublished、もしくはpublishedではない場合、IllegalArgumentExceptionをスローする")
        fun `should throw IllegalArgumentException when a stauts is incorrect`() {
            // サービスインプットを定義
            val input = CreateBookServiceInput(
                title = "Kotlin spring boot",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1, authorId2),
                status = "unpublish"
            )

            // テスト対象メソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<IllegalArgumentException> {
                bookService.createBook(input)
            }
            assertThat(exception.message).isEqualTo("Invalid status value: '${input.status}'. Must be one of ${PublicationStatus.entries.map { it.name.lowercase() }}")

            // 後続の処理が実行されていないことを確認
            verify(authorRepository, never()).countByIdList(any())
        }

        @Test
        @DisplayName("異常系: 存在しない著者IDが含まれている場合、AuthorNotFoundExceptionをスローする")
        fun `should throw AuthorNotFoundException when an author does not exist`() {
            // サービスインプットを定義
            val input = CreateBookServiceInput(
                title = "Kotlin spring boot",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1, UUID.randomUUID()),
                status = "unpublished"
            )

            // モックを設定
            whenever(authorRepository.countByIdList(any())) doReturn 1

            // テスト対象メソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<AuthorNotFoundException> {
                bookService.createBook(input)
            }
            assertThat(exception.message).isEqualTo("One or more specified authors could not be found.")
            verify(authorRepository, never()).findByIdList(any())
        }
    }

    @Nested
    @DisplayName("updateBookメソッド")
    inner class UpdateBook {
        @Test
        @DisplayName("正常系: 既存の書籍を正しく更新できる")
        fun `should update an existing book successfully`() {
            // サービスインプットを定義
            val bookId = UUID.randomUUID()
            val input = UpdateBookServiceInput(
                bookId = bookId,
                title = "Kotlin spring boot 改訂版",
                price = BigDecimal("2500"),
                authorIdList = listOf(authorId2),
                status = "published"
            )

            // 更新対象の書籍情報
            val existingBook = Book(
                id = bookId,
                title = "Kotlin spring boot",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1),
                status = PublicationStatus.PUBLISHED
            )

            // 更新された書籍情報の期待値
            val updatedBook = existingBook.copy(
                title = input.title,
                price = input.price,
                authorIdList = input.authorIdList,
                status = PublicationStatus.valueOf(input.status.uppercase())
            )

            // BookドメインモデルのCaptorを設定
            val bookCaptor = argumentCaptor<Book>()

            // モックを設定
            whenever(bookRepository.findById(bookId)) doReturn existingBook
            whenever(authorRepository.countByIdList(any())) doReturn 1
            whenever(bookRepository.update(any())) doReturn updatedBook
            whenever(authorRepository.findByIdList(any())) doReturn listOf(author2)

            // テスト対象メソッドを実行
            val output = bookService.updateBook(input)

            // サービスアウトプットの検証
            assertThat(output.bookId).isEqualTo(updatedBook.id)
            assertThat(output.title).isEqualTo(updatedBook.title)
            assertThat(output.price).isEqualTo(updatedBook.price)
            assertThat(output.authorList.map { it.authorId }).containsExactly(authorId2)
            assertThat(output.status).isEqualTo(updatedBook.status.toString().lowercase())

            // updateメソッドが1回呼ばれたことを確認し、その時の引数をキャプチャ
            verify(bookRepository).update(bookCaptor.capture())

            // キャプチャした引数の中身を検証
            val bookPassedToUpdate = bookCaptor.firstValue
            assertThat(bookPassedToUpdate.id).isEqualTo(input.bookId)
            assertThat(bookPassedToUpdate.title).isEqualTo(input.title)
            assertThat(bookPassedToUpdate.price).isEqualTo(input.price)
            assertThat(bookPassedToUpdate.authorIdList).isEqualTo(input.authorIdList)
            assertThat(bookPassedToUpdate.status).isEqualTo(PublicationStatus.valueOf(input.status.uppercase()))

            // 他のメソッド呼び出しを検証
            verify(bookRepository).deleteAuthorListByBookId(bookId)
            verify(bookRepository).linkAuthorList(updatedBook.id!!, updatedBook.authorIdList)
        }

        @Test
        @DisplayName("異常系: 出版ステータスがunpublished、もしくはpublishedではない場合、IllegalArgumentExceptionをスローする")
        fun `should throw IllegalArgumentException when a stauts is incorrect`() {
            // サービスインプットを定義
            val bookId = UUID.randomUUID()
            val input = UpdateBookServiceInput(
                bookId = bookId,
                title = "Kotlin spring boot 改訂版",
                price = BigDecimal("2500"),
                authorIdList = listOf(authorId2),
                status = "publish"
            )

            // テスト対象メソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<IllegalArgumentException> {
                bookService.updateBook(input)
            }
            assertThat(exception.message).isEqualTo("Invalid status value: '${input.status}'. Must be one of ${PublicationStatus.entries.map { it.name.lowercase() }}")

            // 後続の処理が実行されていないことを確認
            verify(bookRepository, never()).findById(any())
        }

        @Test
        @DisplayName("異常系: 更新対象の書籍が存在しない場合、BookNotFoundExceptionをスローする")
        fun `should throw BookNotFoundException when book to update does not exist`() {
            // サービスインプットを定義
            val bookId = UUID.randomUUID()
            val input = UpdateBookServiceInput(
                bookId = bookId,
                title = "Kotlin spring boot 改訂版",
                price = BigDecimal("2500"),
                authorIdList = listOf(authorId2),
                status = "published"
            )

            //モックを設定
            whenever(bookRepository.findById(bookId)) doReturn null

            // テスト対象のメソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<BookNotFoundException> {
                bookService.updateBook(input)
            }
            assertThat(exception.message).isEqualTo("Book not found.")

            // 後続の処理が実行されていないことを確認
            verify(authorRepository, never()).countByIdList(any())
        }

        @Test
        @DisplayName("異常系: 出版済みから未出版へのステータス変更はIllegalArgumentExceptionをスローする")
        fun `should throw IllegalArgumentException when changing status from published to unpublished`() {
            // サービスインプットを定義
            val bookId = UUID.randomUUID()
            val input = UpdateBookServiceInput(
                bookId = bookId,
                title = "Kotlin spring boot 改訂版",
                price = BigDecimal("2500"),
                authorIdList = listOf(authorId2),
                status = "unpublished"
            )

            // 更新対象の書籍情報
            val existingBook = Book(
                id = bookId,
                title = "Kotlin spring boot",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1),
                status = PublicationStatus.PUBLISHED
            )

            // モックを設定
            whenever(bookRepository.findById(bookId)) doReturn existingBook

            // テスト対象のメソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<IllegalArgumentException> {
                bookService.updateBook(input)
            }
            assertThat(exception.message).isEqualTo("Cannot change status from 'published' to 'unpublished'.")

            // 後続の処理が実行されていないことを確認
            verify(authorRepository, never()).countByIdList(any())
        }

        @Test
        @DisplayName("異常系: 存在しない著者IDが含まれている場合、AuthorNotFoundExceptionをスローする")
        fun `should throw AuthorNotFoundException when an author does not exist`() {
            // サービスインプットを定義
            val bookId = UUID.randomUUID()
            val input = UpdateBookServiceInput(
                bookId = bookId,
                title = "Kotlin spring boot 改訂版",
                price = BigDecimal("2500"),
                authorIdList = listOf(authorId2, UUID.randomUUID()),
                status = "published"
            )

            // 更新対象の書籍情報
            val existingBook = Book(
                id = bookId,
                title = "Kotlin spring boot",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1),
                status = PublicationStatus.PUBLISHED
            )

            // モックを設定
            whenever(bookRepository.findById(bookId)) doReturn existingBook
            whenever(authorRepository.countByIdList(any())) doReturn 1

            // テスト対象のメソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<AuthorNotFoundException> {
                bookService.updateBook(input)
            }
            assertThat(exception.message).isEqualTo("One or more specified authors could not be found.")

            // 後続の処理が実行されていないことを確認
            verify(bookRepository, never()).update(any())
        }
    }
}
