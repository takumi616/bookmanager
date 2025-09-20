package com.example.bookmanager.repository

import com.example.bookmanager.domain.Author
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.UUID
import org.jooq.generated.tables.Authors.AUTHORS
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/**
 * AuthorRepositoryテスト。
 * @Testcontainers を使い、テスト用のPostgreSQLコンテナを起動する。
 */
@SpringBootTest
@Testcontainers
@Import(AuthorRepository::class)
class AuthorRepositoryTest {

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var authorRepository: AuthorRepository

    companion object {
        // テスト用のPostgreSQLコンテナを定義
        @Container
        private val postgreSQLContainer = PostgreSQLContainer("postgres:15.2-alpine")

        // 実行時にTestcontainerの接続情報をSpring Bootのプロパティに設定する
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
        dsl.deleteFrom(AUTHORS).execute()
    }

    @Nested
    @DisplayName("saveメソッド")
    inner class Save {
        @Test
        @DisplayName("正常系: 新しい著者を正しく保存できる")
        fun `should save a new author successfully`() {
            // 新規作成する著者情報
            val author = Author(
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // 著者登録
            val createdAuthor = authorRepository.save(author)

            // 引数で渡した著者情報が正しく登録できているか検証
            assertThat(createdAuthor.id).isNotNull()
            assertThat(createdAuthor.name).isEqualTo(author.name)
            assertThat(createdAuthor.birthDate).isEqualTo(author.birthDate)

            // DBに登録したレコードが存在するか確認
            val count = dsl.fetchCount(AUTHORS, AUTHORS.ID.eq(createdAuthor.id))
            assertThat(count).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("updateメソッド")
    inner class Update {
        @Test
        @DisplayName("正常系: 既存の著者を正しく更新できる")
        fun `should update an existing author`() {
            // 既存の著者データを作成
            val toCreate = Author(
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )
            val createdAuthor = authorRepository.save(toCreate)

            // 名前を変更して更新
            val toUpdate = createdAuthor.copy(name = "著者B")
            val updatedAuthor = authorRepository.update(toUpdate)

            // 戻り値の著者情報が正しく更新されているか検証
            val updatedId = requireNotNull(updatedAuthor.id) { "Updated author must have a non-null ID" }
            assertThat(updatedId).isEqualTo(createdAuthor.id)
            assertThat(updatedAuthor.name).isEqualTo(toUpdate.name)
            assertThat(updatedAuthor.birthDate).isEqualTo(createdAuthor.birthDate)

            // DB内のレコードに更新が正しく反映されているか確認
            val foundAuthor = authorRepository.findById(updatedId)
            assertThat(foundAuthor?.name).isEqualTo(toUpdate.name)
        }

        @Test
        @DisplayName("異常系: IDがnullのAuthorで更新しようとした場合、IllegalArgumentExceptionをスローする")
        fun `should throw IllegalArgumentException when updating author with null id`() {
            // IDがNullである著者情報を作成
            val authorWithNullId = Author(
                id = null,
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )

            // テスト対象メソッドを呼び出し、例外をキャッチ
            val exception = assertThrows<IllegalArgumentException> {
                authorRepository.update(authorWithNullId)
            }
            assertThat(exception.message).isEqualTo("Author ID must not be null for update.")
        }
    }

    @Nested
    @DisplayName("countByIdListメソッド")
    inner class CountByIdList {
        @Test
        @DisplayName("正常系: 複数の存在するIDで検索した場合、正しい件数を返す")
        fun `should return correct count for existing ids`() {
            // 著者情報を登録
            val author1 = authorRepository.save(
                Author(name = "著者1", birthDate = LocalDate.of(1990, 1, 1))
            )
            val author2 = authorRepository.save(
                Author(name = "著者2", birthDate = LocalDate.of(1991, 1, 1))
            )
            val authorIdList = listOf(requireNotNull(author1.id), requireNotNull(author2.id))

            // テスト対象メソッド呼び出し
            val count = authorRepository.countByIdList(authorIdList)

            // 取得した件数の検証
            assertThat(count).isEqualTo(2)
        }

        @Test
        @DisplayName("空のIDリストで検索した場合、0を返す")
        fun `should return 0 when the ID list is empty`() {
            // テスト対象メソッド呼び出し
            val count = authorRepository.countByIdList(emptyList())

            // 取得した件数を検証
            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("findByIdList / countByIdList メソッド")
    inner class FindByIdList {
        @Test
        @DisplayName("正常系: 複数の存在するIDで検索した場合、正しいリストと件数を返す")
        fun `should return correct list and count for existing ids`() {
            // 複数の著者情報を登録
            val author1 = authorRepository.save(
                Author(name = "著者1", birthDate = LocalDate.of(1990, 1, 1))
            )
            val authorId1 = requireNotNull(author1.id) { "Created author must have a non-null ID" }
            val author2 = authorRepository.save(
                Author(name = "著者2", birthDate = LocalDate.of(1991, 1, 1))
            )
            val authorId2 = requireNotNull(author2.id) { "Created author must have a non-null ID" }

            // テスト対象メソッド呼び出し
            val authorIdList = listOf(authorId1, authorId2)
            val foundAuthorList = authorRepository.findByIdList(authorIdList)
            val count = authorRepository.countByIdList(authorIdList)

            // 取得した著者一覧のレコードを検証
            assertThat(count).isEqualTo(2)
            assertThat(foundAuthorList).hasSize(2)
            assertThat(foundAuthorList.map { it.id }).containsExactlyInAnyOrderElementsOf(authorIdList)
        }

        @Test
        @DisplayName("空のIDリストで検索した場合、空のリストと件数0を返す")
        fun `should return an empty list and count 0 when the ID list is empty`() {
            // 空のIDリストを作成
            val emptyIdList = emptyList<UUID>()

            // テスト対象メソッド呼び出し
            val foundAuthorList = authorRepository.findByIdList(emptyIdList)
            val count = authorRepository.countByIdList(emptyIdList)

            // 取得した著者リストが空であること、件数が0件であることを確認
            assertThat(foundAuthorList).isEmpty()
            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("findByIdメソッド")
    inner class FindById {
        @Test
        @DisplayName("正常系: 存在するIDで検索した場合、著者情報を返す")
        fun `when id exists, should return author`() {
            // 著者情報を登録
            val author = Author(
                name = "著者A",
                birthDate = LocalDate.of(1995, 1, 1)
            )
            val createdAuthor = authorRepository.save(author)

            // テスト対象メソッド呼び出し
            val createdAuthorId = requireNotNull(createdAuthor.id) { "Created author must have a non-null ID" }
            val foundAuthor = authorRepository.findById(createdAuthorId)

            // 取得したレコードを検証
            assertThat(foundAuthor).isNotNull
            assertThat(foundAuthor?.id).isEqualTo(createdAuthorId)
            assertThat(foundAuthor?.name).isEqualTo(createdAuthor.name)
            assertThat(foundAuthor?.birthDate).isEqualTo(createdAuthor.birthDate)
        }

        @Test
        @DisplayName("存在しないIDで検索した場合、nullを返す")
        fun `when id does not exist, should return null`() {
            val nonExistentId = UUID.randomUUID()

            // テスト対象メソッド呼び出し
            val foundAuthor = authorRepository.findById(nonExistentId)

            // レコードが取得できていないことを確認
            assertThat(foundAuthor).isNull()
        }
    }
}
