package com.example.bookmanager.controller

import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import com.example.bookmanager.service.AuthorService
import com.example.bookmanager.controller.converter.toCreateAuthorServiceInput
import com.example.bookmanager.controller.converter.toUpdateAuthorServiceInput
import com.example.bookmanager.controller.converter.toResponse
import com.example.bookmanager.controller.dto.AuthorRequest
import com.example.bookmanager.controller.dto.AuthorResponse
import com.example.bookmanager.controller.dto.GetBookListResponse

@RestController
@RequestMapping("/authors")
class AuthorController(private val authorService: AuthorService) {
    /**
     * POST /authors
     * 新しい著者を作成するエンドポイント。
     *
     * @param request 著者の登録情報リクエスト
     * @return 登録された著者情報レスポンス
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAuthor(@RequestBody request: AuthorRequest): AuthorResponse {
        // リクエストDTOからサービスインプットを生成
        val serviceInput = request.toCreateAuthorServiceInput()

        // サービス実行（著者新規作成）
        val serviceOutput = authorService.createAuthor(serviceInput)

        // サービスの実行結果からレスポンスDTOを生成して返却
        return serviceOutput.toResponse()
    }

    /**
     * PUT /authors/{authorId}
     * 既存の著者情報を更新するエンドポイント。
     *
     * @param authorId URLパスから取得する更新対象の著者ID
     * @param request 著者の更新情報リクエスト
     * @return 更新された著者情報レスポンス
     */
    @PutMapping("/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    fun updateAuthor(@PathVariable authorId: UUID, @RequestBody request: AuthorRequest): AuthorResponse {
        // URLパスから取得した著者IDとリクエストDTOからサービスインプットを生成
        val serviceInput = request.toUpdateAuthorServiceInput(authorId)

        // サービス実行（著者情報更新）
        val serviceOutput = authorService.updateAuthor(serviceInput)

        // サービス実行結果からレスポンスDTOを生成して返却
        return serviceOutput.toResponse()
    }

    /**
     * GET /authors/{authorId}/books
     * 特定の著者に紐づく書籍リストを取得するエンドポイント。
     *
     * @param authorId URLパスから取得する著者ID
     * @return 書籍情報のリストを含むレスポンスDTO
     */
    @GetMapping("/{authorId}/books")
    @ResponseStatus(HttpStatus.OK)
    fun getBookListByAuthor(@PathVariable authorId: UUID): GetBookListResponse {
        // サービス実行（著者IDに紐づく書籍一覧取得）
        val serviceOutput = authorService.findBooksByAuthor(authorId)

        // サービス実行結果からレスポンスDTOを生成して返却
        val authorResponse = serviceOutput.author.toResponse()
        val bookResponseList = serviceOutput.bookList.map { it.toResponse() }
        return GetBookListResponse(
            author = authorResponse,
            bookList = bookResponseList
        )
    }
}