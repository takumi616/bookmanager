package com.example.bookmanager.repository

import java.util.UUID
import org.jooq.DSLContext
import org.jooq.generated.tables.Authors.AUTHORS
import org.springframework.stereotype.Repository
import com.example.bookmanager.domain.Author
import com.example.bookmanager.repository.converter.toAuthor

@Repository
class AuthorRepository(private val dsl: DSLContext) {

    /**
     * 新しい著者をデータベースに保存する。
     *
     * @param author 保存する著者情報
     * @return 保存した著者情報
     */
    fun save(author: Author): Author {
        val newRecord = dsl.insertInto(AUTHORS)
            .set(AUTHORS.NAME, author.name)
            .set(AUTHORS.BIRTH_DATE, author.birthDate)
            .returning()
            .fetchOne()
            ?: throw IllegalStateException("Failed to insert author and retrieve the new record.")

        return newRecord.toAuthor()
    }

    /**
     * 著者情報を更新する。
     *
     * @param author 著者の更新情報
     * @return 更新された著者情報
     */
    fun update(author: Author): Author {
        require(author.id != null) { "Author ID must not be null for update." }

        val updatedRecord = dsl.update(AUTHORS)
            .set(AUTHORS.NAME, author.name)
            .set(AUTHORS.BIRTH_DATE, author.birthDate)
            .where(AUTHORS.ID.eq(author.id))
            .returning()
            .fetchOne()
            ?: throw IllegalStateException("Failed to update author or retrieve the updated record.")

        return updatedRecord.toAuthor()
    }

    /**
     * 指定されたIDリストに一致する著者の件数を取得する。
     * 書籍登録時の著者存在チェックに使用。
     *
     * @param authorIdList 著者IDのリスト
     * @return 著者の取得件数
     */
    fun countByIdList(authorIdList: List<UUID>): Int {
        if (authorIdList.isEmpty()) return 0
        return dsl.selectCount()
            .from(AUTHORS)
            .where(AUTHORS.ID.`in`(authorIdList))
            .fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 指定されたIDリストに一致する著者のリストを取得する。
     * APIレスポンスの組み立てに使用。
     *
     * @param authorIdList 著者IDのリスト
     * @return 著者情報のリスト
     */
    fun findByIdList(authorIdList: List<UUID>): List<Author> {
        if (authorIdList.isEmpty()) return emptyList()
        return dsl.selectFrom(AUTHORS)
            .where(AUTHORS.ID.`in`(authorIdList))
            .fetch()
            .map { it.toAuthor() }
    }

    /**
     * IDで著者を1件取得する。
     * 存在しない場合はnullを返却。
     *
     * @param authorId 著者ID
     * @return 検索条件の著者IDに該当する著者情報
     */
    fun findById(authorId: UUID): Author? {
        return dsl.selectFrom(AUTHORS)
            .where(AUTHORS.ID.eq(authorId))
            .fetchOne()
            ?.toAuthor()
    }
}
