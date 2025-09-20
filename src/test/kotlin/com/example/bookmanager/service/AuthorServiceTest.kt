package com.example.bookmanager.service

import com.example.bookmanager.domain.Author
import com.example.bookmanager.domain.Book
import com.example.bookmanager.domain.PublicationStatus
import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.repository.AuthorRepository
import com.example.bookmanager.repository.BookRepository
import com.example.bookmanager.service.dto.CreateAuthorServiceInput
import com.example.bookmanager.service.dto.UpdateAuthorServiceInput
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
 * AuthorServiceテスト。
 */
@ExtendWith(MockitoExtension::class)
class AuthorServiceTest {

    // テスト対象のクラス
    @InjectMocks
    private lateinit var authorService: AuthorService

    // モック化する依存関係
    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Mock
    private lateinit var bookRepository: BookRepository

    // 著者テストデータ
    private val authorId1 = UUID.randomUUID()
    private val authorId2 = UUID.randomUUID()
    private val author1 = Author(id = authorId1, name = "著者A", birthDate = LocalDate.now().minusYears(30))
    private val author2 = Author(id = authorId2, name = "著者B", birthDate = LocalDate.now().minusYears(40))

    @Nested
    @DisplayName("createAuthorメソッド")
    inner class CreateAuthor {
        @Test
        @DisplayName("正常系: 新しい著者を正しく作成できる")
        fun `should create a new author successfully`() {
            // サービスインプットを定義
            val input = CreateAuthorServiceInput(
                name = "テスト著者",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 作成される著者情報の期待値
            val createdAuthor = Author(
                id = UUID.randomUUID(),
                name = input.name,
                birthDate = input.birthDate
            )

            // AuthorドメインモデルのCaptorを設定
            val authorCaptor = argumentCaptor<Author>()

            // モックを設定
            whenever(authorRepository.save(any())) doReturn createdAuthor

            // テスト対象メソッド呼び出し
            val output = authorService.createAuthor(input)

            // サービスアウトプットの検証
            assertThat(output.authorId).isEqualTo(createdAuthor.id)
            assertThat(output.name).isEqualTo(createdAuthor.name)

            // saveメソッドが1回呼ばれたことを確認し、その時の引数をキャプチャ
            verify(authorRepository).save(authorCaptor.capture())

            // キャプチャした引数の中身を検証する
            val authorPassedToSave = authorCaptor.firstValue
            assertThat(authorPassedToSave.id).isNull()
            assertThat(authorPassedToSave.name).isEqualTo(input.name)
            assertThat(authorPassedToSave.birthDate).isEqualTo(input.birthDate)
        }
    }

    @Nested
    @DisplayName("updateAuthorメソッド")
    inner class UpdateAuthor {
        @Test
        @DisplayName("正常系: 既存の著者を正しく更新できる")
        fun `should update an existing author successfully`() {
            // サービスインプットを定義
            val authorId = UUID.randomUUID()
            val input = UpdateAuthorServiceInput(
                authorId = authorId,
                name = "テスト著者(改名)",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 更新対象の著者情報
            val existingAuthor = Author(
                id = authorId,
                name = "テスト著者",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 更新された著者情報の期待値
            val updatedAuthor = Author(
                id = input.authorId,
                name = input.name,
                birthDate = input.birthDate
            )

            // Authorオブジェクトを捕獲するためのCaptorを準備
            val authorCaptor = argumentCaptor<Author>()

            // モックを設定
            whenever(authorRepository.findById(authorId)) doReturn existingAuthor
            whenever(authorRepository.update(any())) doReturn updatedAuthor

            // テスト対象のメソッド呼び出し
            val output = authorService.updateAuthor(input)

            // サービスアウトプットの検証
            assertThat(output.authorId).isEqualTo(updatedAuthor.id)
            assertThat(output.name).isEqualTo(updatedAuthor.name)

            // updateメソッドが1回呼ばれたことを確認し、その時の引数をキャプチャ
            verify(authorRepository).update(authorCaptor.capture())

            // キャプチャした引数の中身を検証
            val authorPassedToUpdate = authorCaptor.firstValue
            assertThat(authorPassedToUpdate.id).isEqualTo(input.authorId)
            assertThat(authorPassedToUpdate.name).isEqualTo(input.name)
            assertThat(authorPassedToUpdate.birthDate).isEqualTo(input.birthDate)

            // 他のメソッド呼び出し有無を検証
            verify(authorRepository).findById(authorId)
        }

        @Test
        @DisplayName("異常系: 更新対象の著者が存在しない場合、AuthorNotFoundExceptionをスローする")
        fun `should throw AuthorNotFoundException when author to update does not exist`() {
            // サービスインプットを定義
            val authorId = UUID.randomUUID()
            val input = UpdateAuthorServiceInput(
                authorId = authorId,
                name = "テスト著者(改名)",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // モックを設定
            whenever(authorRepository.findById(authorId)) doReturn null

            // テスト対象のメソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<AuthorNotFoundException> {
                authorService.updateAuthor(input)
            }
            assertThat(exception.message).isEqualTo("Author not found.")

            // 後続の処理が実行されていないことを確認
            verify(authorRepository, never()).update(any())
        }
    }

    @Nested
    @DisplayName("findBooksByAuthorメソッド")
    inner class FindBooksByAuthor {
        @Test
        @DisplayName("正常系: 共著者がいる書籍も含め、著者に紐づく書籍リストを正しく取得できる")
        fun `should find books by author successfully including co-authored books`() {
            // 取得する書籍一覧を定義
            val targetAuthor = author1
            val targetAuthorId = requireNotNull(targetAuthor.id) { "Test fixture 'author1' must have a non-null ID" }
            val book1 = Book(
                id = UUID.randomUUID(),
                title = "著者Aの書籍",
                price = BigDecimal("2000"),
                authorIdList = listOf(authorId1),
                status = PublicationStatus.PUBLISHED
            )
            val book2 = Book(
                id = UUID.randomUUID(),
                title = "著者Aと著者Bの共著",
                price = BigDecimal("3000"),
                authorIdList = listOf(authorId1, authorId2),
                status = PublicationStatus.PUBLISHED
            )
            val bookListByAuthor = listOf(book1, book2)
            val allRelatedAuthorList = listOf(author1, author2)

            // モックを設定
            whenever(authorRepository.findById(targetAuthorId)) doReturn targetAuthor
            whenever(bookRepository.findBookListByAuthorId(targetAuthorId)) doReturn bookListByAuthor
            whenever(authorRepository.findByIdList(listOf(authorId1, authorId2))) doReturn allRelatedAuthorList

            // テスト対象メソッド呼び出し
            val output = authorService.findBooksByAuthor(targetAuthorId)

            // サービスアウトプットの検証
            assertThat(output.author.authorId).isEqualTo(targetAuthorId)
            assertThat(output.bookList).hasSize(2)

            // 共著の書籍（book2）を取得し、その著者リストに共著者（author2）が正しく含まれているか検証
            val coAuthoredBookOutput = output.bookList.find { it.bookId == book2.id }
            assertThat(coAuthoredBookOutput).isNotNull
            assertThat(coAuthoredBookOutput!!.authorList.map { it.authorId }).containsExactlyInAnyOrder(authorId1, authorId2)

            // 単著の本（book1）の著者リストが正しいか検証
            val singleAuthoredBookOutput = output.bookList.find { it.bookId == book1.id }
            assertThat(singleAuthoredBookOutput).isNotNull
            assertThat(singleAuthoredBookOutput!!.authorList.map { it.authorId }).containsExactly(authorId1)
        }

        @Test
        @DisplayName("異常系: 対象の著者が存在しない場合、AuthorNotFoundExceptionをスローする")
        fun `should throw AuthorNotFoundException when author for finding books does not exist`() {
            // モックを設定
            val authorId = UUID.randomUUID()
            whenever(authorRepository.findById(authorId)) doReturn null

            // テスト対象メソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<AuthorNotFoundException> {
                authorService.findBooksByAuthor(authorId)
            }
            assertThat(exception.message).isEqualTo("Author with ID $authorId not found.")

            // 後続の処理が実行されないことを確認
            verify(bookRepository, never()).findBookListByAuthorId(any())
        }

        @Test
        @DisplayName("正常系: 著者に紐づく書籍がない場合、著者情報と空の書籍リストを返す")
        fun `should return author with empty book list when author has no books`() {
            // 著者IDを設定
            val targetAuthor = author1
            val targetAuthorId = requireNotNull(targetAuthor.id)

            // モックを設定
            whenever(authorRepository.findById(targetAuthorId)) doReturn targetAuthor
            whenever(bookRepository.findBookListByAuthorId(targetAuthorId)) doReturn emptyList()

            // テスト対象メソッド呼び出し
            val output = authorService.findBooksByAuthor(targetAuthorId)

            // サービスアウトプットの検証
            assertThat(output.author.authorId).isEqualTo(targetAuthorId)
            assertThat(output.bookList).isEmpty()

            // 後続の処理が実行されないことを確認
            verify(authorRepository, never()).findByIdList(any())
        }
    }
}
