package com.example.bookmanager.controller

import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

import com.example.bookmanager.service.BookService
import com.example.bookmanager.controller.converter.toServiceInput
import com.example.bookmanager.controller.converter.toResponse
import com.example.bookmanager.controller.dto.BookResponse
import com.example.bookmanager.controller.dto.CreateBookRequest
import com.example.bookmanager.controller.dto.UpdateBookRequest

@RestController
@RequestMapping("/books")
class BookController(private val bookService: BookService) {
    /**
     * POST /books
     * 新しい書籍を作成するエンドポイント。
     *
     * @param request 書籍の登録情報リクエスト
     * @return 登録された書籍情報レスポンス
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createBook(@RequestBody request: CreateBookRequest): BookResponse {
        // リクエストDTOからサービスインプットを生成
        val serviceInput = request.toServiceInput()

        // サービス実行（書籍新規作成）
        val serviceOutput = bookService.createBook(serviceInput)

        // サービスの実行結果からレスポンスDTOを生成して返却
        return serviceOutput.toResponse()
    }

    /**
     * PUT /books/{bookId}
     * 既存の書籍の情報を更新するエンドポイント。
     *
     * @param bookId URLパスから取得する更新対象の書籍ID
     * @param request 書籍の更新情報リクエスト
     * @return 更新された書籍情報レスポンス
     */
    @PutMapping("/{bookId}")
    @ResponseStatus(HttpStatus.OK)
    fun updateBook(@PathVariable bookId: UUID, @RequestBody request: UpdateBookRequest): BookResponse {
        // URLパスから取得した書籍IDとリクエストDTOからサービスインプットを生成
        val serviceInput = request.toServiceInput(bookId)

        // サービス実行（書籍情報更新）
        val serviceOutput = bookService.updateBook(serviceInput)

        // サービス実行結果からレスポンスDTOを生成して返却
        return serviceOutput.toResponse()
    }
}