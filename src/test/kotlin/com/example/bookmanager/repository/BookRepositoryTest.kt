package com.example.bookmanager.repository

import com.example.bookmanager.domain.Author
import com.example.bookmanager.domain.Book
import com.example.bookmanager.domain.PublicationStatus
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.generated.enums.PublicationStatus as JooqPublicationStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate
import org.jooq.generated.tables.Authors.AUTHORS
import org.jooq.generated.tables.Books.BOOKS
import org.jooq.generated.tables.BookAuthors.BOOK_AUTHORS
import java.util.UUID

/**
 * BookRepositoryテスト。
 * @Testcontainers を使い、テスト用のPostgreSQLコンテナを起動する。
 */
@SpringBootTest
@Testcontainers
class BookRepositoryTest {

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var bookRepository: BookRepository

    @Autowired
    private lateinit var authorRepository: AuthorRepository

    companion object {
        @Container
        private val postgreSQLContainer = PostgreSQLContainer("postgres:15.2-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgreSQLContainer::getUsername)
            registry.add("spring.datasource.password", postgreSQLContainer::getPassword)
            registry.add("spring.sql.init.mode") { "always" }
        }
    }

    // 各テストの前にDBをクリーンな状態にする
    @BeforeEach
    fun cleanup() {
        dsl.deleteFrom(BOOK_AUTHORS).execute()
        dsl.deleteFrom(BOOKS).execute()
        dsl.deleteFrom(AUTHORS).execute()
    }

    @Nested
    @DisplayName("saveメソッド")
    inner class Save {
        @Test
        @DisplayName("新しい書籍を正しく保存できる")
        fun `should save a new book successfully`() {
            // 著書を登録
            val author = authorRepository.save(
                Author(name = "著者A", birthDate = LocalDate.of(1980, 1, 1))
            )

            // 登録する書籍情報
            val book = Book(
                title = "Kotlin web backend",
                price = BigDecimal("3300.00"),
                authorIdList = listOf(requireNotNull(author.id)),
                status = PublicationStatus.UNPUBLISHED
            )

            // テスト対象メソッド呼び出し
            val createdBook = bookRepository.save(book)

            // 登録した書籍情報の検証
            assertThat(createdBook.id).isNotNull()
            assertThat(createdBook.title).isEqualTo(book.title)
            assertThat(createdBook.price).isEqualTo(book.price)
            assertThat(createdBook.authorIdList).isEqualTo(book.authorIdList)
            assertThat(createdBook.status).isEqualTo(book.status)

            // DBから登録したレコードを取得し、登録情報を検証
            val fetchedBook = dsl.selectFrom(BOOKS).where(BOOKS.ID.eq(createdBook.id)).fetchOne()
            assertThat(fetchedBook).isNotNull
            assertThat(fetchedBook?.get(BOOKS.TITLE)).isEqualTo(book.title)
            assertThat(fetchedBook?.get(BOOKS.PRICE)).isEqualTo(book.price)
            assertThat(fetchedBook?.get(BOOKS.STATUS)).isEqualTo(JooqPublicationStatus.unpublished)
        }
    }

    @Nested
    @DisplayName("linkAuthorListメソッド")
    inner class LinkAuthorList {
        @Test
        @DisplayName("正常系: 書籍と複数の著者を正しく関連付けできる")
        fun `should link multiple authors to a book`() {
            // 著者情報、書籍情報を登録
            val author1 = authorRepository.save(
                Author(name = "著者A", birthDate = LocalDate.of(1980, 1, 1))
            )
            val authorId1 = requireNotNull(author1.id)
            val author2 = authorRepository.save(
                Author(name = "著者B", birthDate = LocalDate.of(1985, 1, 1))
            )
            val authorId2 = requireNotNull(author2.id)
            val book = bookRepository.save(
                Book(
                    title = "Kotlin web backend",
                    price = BigDecimal("3300"),
                    authorIdList = listOf(authorId1, authorId2),
                    status = PublicationStatus.UNPUBLISHED
                )
            )
            val bookId = requireNotNull(book.id)

            // テスト対象メソッド呼び出し
            bookRepository.linkAuthorList(bookId, listOf(authorId1, authorId2))

            // DBから該当レコードを取得し件数を検証
            val linkCount = dsl.fetchCount(BOOK_AUTHORS, BOOK_AUTHORS.BOOK_ID.eq(book.id))
            assertThat(linkCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("findByIdメソッド")
    inner class FindById {
        @Test
        @DisplayName("存在するIDで検索した場合、著者情報も含めて書籍を返す")
        fun `should return a book with authors when id exists`() {
            // 著者、書籍を登録をする
            val author = authorRepository.save(
                Author(name = "著者A", birthDate = LocalDate.of(1980, 1, 1))
            )
            val book = bookRepository.save(
                Book(title = "Kotlin web backend", price = BigDecimal("3300"), authorIdList = listOf(requireNotNull(author.id)), status = PublicationStatus.UNPUBLISHED)
            )
            bookRepository.linkAuthorList(requireNotNull(book.id), book.authorIdList)

            // テスト対象メソッド呼び出し
            val foundBook = bookRepository.findById(requireNotNull(book.id))

            // 取得した書籍の検証
            assertThat(foundBook).isNotNull
            assertThat(foundBook?.id).isEqualTo(book.id)
            assertThat(foundBook?.title).isEqualTo(book.title)
            assertThat(foundBook?.price).isEqualTo(book.price)
            assertThat(foundBook?.authorIdList).containsExactly(requireNotNull(author.id))
            assertThat(foundBook?.status).isEqualTo(book.status)
        }

        @Test
        @DisplayName("存在しないIDで検索した場合、nullを返す")
        fun `should return null when id does not exist`() {
            // テスト対象メソッド呼び出し
            val foundBook = bookRepository.findById(UUID.randomUUID())

            // 取得結果の検証
            assertThat(foundBook).isNull()
        }
    }

    @Nested
    @DisplayName("updateメソッド")
    inner class Update {
        @Test
        @DisplayName("既存の書籍情報を正しく更新できる")
        fun `should update book details successfully`() {
            // 著者、書籍を登録
            val author = authorRepository.save(
                Author(name = "著者A", birthDate = LocalDate.of(1980, 1, 1))
            )
            val book = bookRepository.save(
                Book(
                    title = "Kotlin web backend",
                    price = BigDecimal("3300"),
                    authorIdList = listOf(requireNotNull(author.id)),
                    status = PublicationStatus.UNPUBLISHED
                )
            )
            bookRepository.linkAuthorList(requireNotNull(book.id), book.authorIdList)

            // テスト対象メソッド呼び出し
            val bookToUpdate = book.copy(
                title = "Kotlin web backend 改訂版",
                status = PublicationStatus.PUBLISHED
            )
            val updatedBook = bookRepository.update(bookToUpdate)

            // 更新結果の検証
            assertThat(updatedBook.title).isEqualTo(bookToUpdate.title)
            assertThat(updatedBook.status).isEqualTo(PublicationStatus.PUBLISHED)

            // DBから更新したレコードを取得して更新結果を検証
            val foundBook = bookRepository.findById(requireNotNull(updatedBook.id))
            assertThat(foundBook?.title).isEqualTo(bookToUpdate.title)
            assertThat(foundBook?.status).isEqualTo(bookToUpdate.status)
        }
    }

    @Nested
    @DisplayName("deleteAuthorListByBookIdメソッド")
    inner class DeleteAuthorListByBookId {
        @Test
        @DisplayName("正常系: 指定した書籍IDに関連する全ての著者リンクを削除できる")
        fun `should delete all author links for a given book id`() {
            // 著者情報、書籍情報を登録
            val author1 = authorRepository.save(
                Author(name = "著者A", birthDate = LocalDate.of(1980, 1, 1))
            )
            val authorId1 = requireNotNull(author1.id)
            val author2 = authorRepository.save(
                Author(name = "著者B", birthDate = LocalDate.of(1985, 1, 1))
            )
            val authorId2 = requireNotNull(author2.id)
            val book = bookRepository.save(
                Book(
                    title = "Kotlin web backend",
                    price = BigDecimal("3300"),
                    authorIdList = listOf(authorId1, authorId2),
                    status = PublicationStatus.UNPUBLISHED
                )
            )
            val bookId = requireNotNull(book.id)
            bookRepository.linkAuthorList(bookId, listOf(authorId1, authorId2))

            // `bookId`の著者が2人存在ことを確認
            assertThat(dsl.fetchCount(BOOK_AUTHORS, BOOK_AUTHORS.BOOK_ID.eq(bookId))).isEqualTo(2)

            // テスト対象メソッド呼び出し
            bookRepository.deleteAuthorListByBookId(bookId)

            // DBから削除されていることを確認
            val linkCountAfterDelete = dsl.fetchCount(BOOK_AUTHORS, BOOK_AUTHORS.BOOK_ID.eq(bookId))
            assertThat(linkCountAfterDelete).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("findBookListByAuthorIdメソッド")
    inner class FindBookListByAuthorId {
        @Test
        @DisplayName("特定の著者に紐づく書籍リストを、共著者情報も含めて正しく取得できる")
        fun `should return correct book list including co-authored books for a given author`() {
            // 著者を登録
            val authorA = authorRepository.save(Author(name = "著者A", birthDate = LocalDate.of(1980, 1, 1)))
            val authorB = authorRepository.save(Author(name = "著者B", birthDate = LocalDate.of(1985, 1, 1)))
            val authorIdA = requireNotNull(authorA.id)
            val authorIdB = requireNotNull(authorB.id)

            // 書籍を3冊登録
            val book1 = bookRepository.save(
                Book(
                    title = "Kotlin web backend",
                    price = BigDecimal("3300"),
                    authorIdList = listOf(authorIdA, authorIdB),
                    status = PublicationStatus.UNPUBLISHED
                )
            )
            bookRepository.linkAuthorList(requireNotNull(book1.id), book1.authorIdList)
            val book2 = bookRepository.save(
                Book(
                    title = "Kotlin server side",
                    price = BigDecimal("3000"),
                    authorIdList = listOf(authorIdA),
                    status = PublicationStatus.UNPUBLISHED
                )
            )
            bookRepository.linkAuthorList(requireNotNull(book2.id), book2.authorIdList)
            bookRepository.save(
                Book(
                    title = "Kotlin microservices",
                    price = BigDecimal("4000"),
                    authorIdList = listOf(authorIdB),
                    status = PublicationStatus.UNPUBLISHED
                )
            ).let { bookRepository.linkAuthorList(requireNotNull(it.id), it.authorIdList) }

            // テスト対象メソッド呼び出し
            val bookListForAuthorA = bookRepository.findBookListByAuthorId(authorIdA)

            // 該当の書籍を取得できているか検証
            assertThat(bookListForAuthorA).hasSize(2)
            assertThat(bookListForAuthorA.map { it.id }).containsExactlyInAnyOrder(book1.id, book2.id)

            // 共著の本(book1)の著者リストが(A, B)両方含まれていることを確認
            val foundBook1 = bookListForAuthorA.find { it.id == book1.id }
            assertThat(foundBook1?.authorIdList).containsExactlyInAnyOrder(authorIdA, authorIdB)
        }

        @Test
        @DisplayName("著者に紐づく書籍がない場合、空のリストを返す")
        fun `should return an empty list when author has no books`() {
            // 著者を登録
            val author = authorRepository.save(
                Author(name = "著者A", birthDate = LocalDate.of(2000, 1, 1))
            )

            // テスト対象メソッド呼び出し
            val bookList = bookRepository.findBookListByAuthorId(requireNotNull(author.id))

            // 取得結果を検証
            assertThat(bookList).isEmpty()
        }
    }
}
