package com.example.bookmanager.service

import com.example.bookmanager.domain.Book
import com.example.bookmanager.domain.PublicationStatus
import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.exception.BookNotFoundException
import com.example.bookmanager.repository.BookRepository
import com.example.bookmanager.repository.AuthorRepository
import com.example.bookmanager.service.converter.toBookServiceOutput
import com.example.bookmanager.service.dto.BookServiceOutput
import com.example.bookmanager.service.dto.CreateBookServiceInput
import com.example.bookmanager.service.dto.UpdateBookServiceInput
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository
) {

    /**
     * 新しい書籍を著者との関連を含めて作成する。
     *
     * @param serviceInput 作成する書籍情報を持つサービスインプットデータ
     * @return 作成された書籍情報を持つサービスアウトプットデータ
     */
    @Transactional
    fun createBook(serviceInput: CreateBookServiceInput): BookServiceOutput {
        // 出版ステータスをEnumへ変換
        val status = try {
            PublicationStatus.valueOf(serviceInput.status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid status value: '${serviceInput.status}'. Must be one of ${PublicationStatus.entries.map { it.name.lowercase() }}"
            )
        }

        // サービスインプットから書籍ドメインオブジェクトを生成
        val book = Book(
            title = serviceInput.title,
            price = serviceInput.price,
            authorIdList = serviceInput.authorIdList,
            status = status
        )

        // 著者の件数を取得
        val authorIdList = book.authorIdList
        val authorsCount = authorRepository.countByIdList(authorIdList)
        if (authorsCount != authorIdList.size) {
            throw AuthorNotFoundException("One or more specified authors could not be found.")
        }

        // 著者リストを取得
        val authorList = authorRepository.findByIdList(authorIdList)

        // 書籍を登録
        val createdBook = bookRepository.save(book)
        val createdBookId = createdBook.id!!

        // 中間テーブルに関連を保存
        bookRepository.linkAuthorList(createdBookId, authorIdList)

        // 作成された書籍情報からサービス実行結果を生成
        return createdBook.toBookServiceOutput(authorList)
    }

    /**
     * 既存の書籍情報を更新する。
     *
     * @param serviceInput 書籍の更新情報を持つサービスインプットデータ
     * @return 更新された書籍情報を持つサービスアウトプットデータ
     */
    @Transactional
    fun updateBook(serviceInput: UpdateBookServiceInput): BookServiceOutput {
        // 出版ステータスをEnumへ変換
        val status = try {
            PublicationStatus.valueOf(serviceInput.status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid status value: '${serviceInput.status}'. Must be one of ${PublicationStatus.entries.map { it.name.lowercase() }}"
            )
        }

        // 更新対象の書籍が存在するか確認
        val existingBook = bookRepository.findById(serviceInput.bookId)
            ?: throw BookNotFoundException("Book not found.") as Throwable

        // 出版状況を確認し、出版済みステータスを未出版に変更しようとしている場合は例外を発生させる
        if (existingBook.status == PublicationStatus.PUBLISHED && serviceInput.status == PublicationStatus.UNPUBLISHED.name.lowercase()) {
            throw IllegalArgumentException("Cannot change status from 'published' to 'unpublished'.")
        }

        // リクエストで指定された著者IDが存在するか確認
        val distinctAuthorIdList = serviceInput.authorIdList.distinct()
        val existingAuthorListCount = authorRepository.countByIdList(distinctAuthorIdList)
        if (existingAuthorListCount != distinctAuthorIdList.size) {
            throw AuthorNotFoundException("One or more specified authors could not be found.")
        }

        // サービスインプットから書籍ドメインオブジェクトを生成
        val book = existingBook.copy(
            title = serviceInput.title,
            price = serviceInput.price,
            authorIdList = distinctAuthorIdList,
            status = status
        )

        // 書籍を更新
        val updatedBook = bookRepository.update(book)
        bookRepository.deleteAuthorListByBookId(serviceInput.bookId)
        bookRepository.linkAuthorList(serviceInput.bookId, distinctAuthorIdList)

        // 著者情報を取得
        val authorList = authorRepository.findByIdList(updatedBook.authorIdList)

        // 更新された書籍情報と取得した著者情報のリストからサービス実行結果を生成
        return updatedBook.toBookServiceOutput(authorList)
    }
}
