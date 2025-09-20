package com.example.bookmanager.service

import java.util.UUID
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import com.example.bookmanager.domain.Author
import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.repository.AuthorRepository
import com.example.bookmanager.repository.BookRepository
import com.example.bookmanager.service.converter.toAuthorServiceOutput
import com.example.bookmanager.service.converter.toBookServiceOutput
import com.example.bookmanager.service.dto.AuthorServiceOutput
import com.example.bookmanager.service.dto.CreateAuthorServiceInput
import com.example.bookmanager.service.dto.FindBookListByAuthorServiceOutput
import com.example.bookmanager.service.dto.UpdateAuthorServiceInput

@Service
class AuthorService
    (private val authorRepository: AuthorRepository,
     private val bookRepository: BookRepository
) {

    /**
     * 新しい著者を作成する。
     *
     * @param serviceInput 作成する著者情報を持つサービスインプットデータ
     * @return 作成された著者情報を持つサービスアウトプットデータ
     */
    @Transactional
    fun createAuthor(serviceInput: CreateAuthorServiceInput): AuthorServiceOutput {
        // サービスインプットから著者ドメインオブジェクトを生成
        val author = Author(
            name = serviceInput.name,
            birthDate = serviceInput.birthDate
        )

        // 著者を登録
        val createdAuthor = authorRepository.save(author)

        // 作成された著者情報からサービス実行結果を生成
        return createdAuthor.toAuthorServiceOutput()
    }

    /**
     * 既存の著者情報を更新する。
     *
     * @param serviceInput 著者の更新情報を持つサービスインプットデータ
     * @return 更新された著者情報を持つサービスアウトプットデータ
     */
    @Transactional
    fun updateAuthor(serviceInput: UpdateAuthorServiceInput): AuthorServiceOutput {
        // 更新対象の著者が存在するか確認
        val existingAuthor = authorRepository.findById(serviceInput.authorId)
            ?: throw AuthorNotFoundException("Author not found.")

        // サービスインプットから書籍ドメインオブジェクトを生成
        val author = existingAuthor.copy(
            name = serviceInput.name,
            birthDate = serviceInput.birthDate
        )

        // 著者を更新
        val updatedAuthor = authorRepository.update(author)

        // 更新された著者情報からサービス実行結果を生成
        return updatedAuthor.toAuthorServiceOutput()
    }

    /**
     * 既存の著者情報を更新する。
     *
     * @param authorId 著者ID
     * @return 著者と著者に紐づく書籍リストを持つサービスアウトプットデータ
     */
    @Transactional(readOnly = true)
    fun findBooksByAuthor(authorId: UUID): FindBookListByAuthorServiceOutput {
        // 著者が存在するか確認
        val author = authorRepository.findById(authorId)
            ?: throw AuthorNotFoundException("Author with ID $authorId not found.")

        // 取得した著者情報から著者サービス実行結果を生成
        val authorServiceOutput = author.toAuthorServiceOutput()

        // 著者に紐づく書籍を取得
        val bookList = bookRepository.findBookListByAuthorId(authorId)
        if (bookList.isEmpty()) {
            // 書籍がない場合、著者情報と空の書籍リストを含むサービス実行結果を返す
            return FindBookListByAuthorServiceOutput(author = authorServiceOutput, bookList = emptyList())
        }

        // 取得した全書籍から、関連する全ての著者IDを重複なく収集
        val authorIdList = bookList.flatMap { it.authorIdList }.toSet()

        // 全ての著者情報を取得
        val authorList = authorRepository.findByIdList(authorIdList.toList()).associateBy { it.id!! }

        // 書籍ごとに著者情報をマッピングし、ServiceOutputのリストを生成
        val bookOutputList = bookList.map { book ->
            val authorsForBook = book.authorIdList.mapNotNull { authorList[it] }
            book.toBookServiceOutput(authorsForBook)
        }

        // 著者情報と書籍リストからサービス実行結果を生成
        return FindBookListByAuthorServiceOutput(
            author = authorServiceOutput,
            bookList = bookOutputList
        )
    }
}
