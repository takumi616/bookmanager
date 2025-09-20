package com.example.bookmanager.repository

import com.example.bookmanager.domain.Book
import com.example.bookmanager.repository.converter.toBook
import com.example.bookmanager.repository.converter.toJooqEnum
import org.jooq.DSLContext
import org.jooq.generated.tables.BookAuthors.BOOK_AUTHORS
import org.jooq.generated.tables.Books.BOOKS
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class BookRepository(private val dsl: DSLContext) {
    /**
     * 新しい書籍をデータベースに保存する。
     *
     * @param book 保存する書籍情報
     * @return 保存した書籍情報
     */
    fun save(book: Book): Book {
        val record = dsl.insertInto(BOOKS)
            .set(BOOKS.TITLE, book.title)
            .set(BOOKS.PRICE, book.price)
            .set(BOOKS.STATUS, book.status.toJooqEnum())
            .returning()
            .fetchOne() ?: throw IllegalStateException("Failed to insert book and retrieve the record.")

        return record.toBook(book.authorIdList)
    }

    /**
     * 書籍と著者の関連を中間テーブル(book_authors)に保存する。
     *
     * @param bookId 保存する書籍ID
     * @param authorIdList 保存する著者IDのリスト
     */
    fun linkAuthorList(bookId: UUID, authorIdList: List<UUID>) {
        // 複数の著者IDに対してバッチINSERTを実行
        val queries = authorIdList.map { authorId ->
            dsl.insertInto(BOOK_AUTHORS)
                .set(BOOK_AUTHORS.BOOK_ID, bookId)
                .set(BOOK_AUTHORS.AUTHOR_ID, authorId)
        }
        dsl.batch(queries).execute()
    }

    /**
     * 書籍IDで書籍を1件取得する。
     * 著者IDリストも中間テーブルから取得する。
     *
     * @param bookId 検索条件の書籍ID
     * @return 書籍IDで検索された該当の書籍情報
     */
    fun findById(bookId: UUID): Book? {
        val record = dsl.selectFrom(BOOKS)
            .where(BOOKS.ID.eq(bookId))
            .fetchOne() ?: return null

        val authorIds = dsl.select(BOOK_AUTHORS.AUTHOR_ID)
            .from(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .fetch(BOOK_AUTHORS.AUTHOR_ID)
            .filterNotNull()

        return record.toBook(authorIds)
    }

    /**
     * 書籍情報を更新する。
     *
     * @param book 書籍の更新情報
     * @return 更新された書籍情報
     */
    fun update(book: Book): Book {
        require(book.id != null) { "Book ID must not be null for update." }

        val record = dsl.update(BOOKS)
            .set(BOOKS.TITLE, book.title)
            .set(BOOKS.PRICE, book.price)
            .set(BOOKS.STATUS, book.status.toJooqEnum())
            .where(BOOKS.ID.eq(book.id))
            .returning()
            .fetchOne()
            ?: throw IllegalStateException("Failed to update book or retrieve the updated record for ID: ${book.id}")

        return record.toBook(book.authorIdList)
    }

    /**
     * 指定された書籍IDに関連する著者情報を中間テーブルから全て削除する。
     *
     * @param bookId 書籍ID
     */
    fun deleteAuthorListByBookId(bookId: UUID) {
        dsl.deleteFrom(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .execute()
    }

    /**
     * 指定された著者IDに紐づく書籍のリストを取得する。
     * 各書籍に紐づく全ての著者IDも併せて取得する。
     *
     * @param authorId 著者ID
     * @return 著者IDに紐づく書籍のリスト
     */
    fun findBookListByAuthorId(authorId: UUID): List<Book> {
        // サブクエリ: 指定された著者が執筆した書籍のIDリストを取得
        val bookIdsByAuthor = dsl.select(BOOK_AUTHORS.BOOK_ID)
            .from(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))

        // メインクエリ: 該当する書籍の情報と、それらに関連する全ての著者IDの配列を取得
        val records = dsl.select(
            BOOKS.asterisk(),
            DSL.arrayAgg(BOOK_AUTHORS.AUTHOR_ID).`as`("author_ids")
        )
            .from(BOOKS)
            .join(BOOK_AUTHORS).on(BOOKS.ID.eq(BOOK_AUTHORS.BOOK_ID))
            .where(BOOKS.ID.`in`(bookIdsByAuthor))
            .groupBy(BOOKS.ID)
            .fetch()

        return records.map { record ->
            val authorIds = (record.getValue("author_ids") as Array<UUID>).toList()
            record.toBook(authorIds)
        }
    }
}