package com.example.bookmanager.controller

import com.example.bookmanager.exception.AuthorNotFoundException
import com.example.bookmanager.exception.BookNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * アプリケーション全体のエラーをハンドリングするためのクラス。
 * @RestControllerAdvice をつけることで、全てのコントローラで発生した例外を横断的に捕捉。
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * ドメインのinitブロックなどからスローされるバリデーション例外をハンドルする。
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid input provided."
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    /**
     * 参照先の著者が見つからない場合の例外をハンドルする。
     * @return 404 Not Found
     */
    @ExceptionHandler(AuthorNotFoundException::class)
    fun handleAuthorNotFoundException(ex: AuthorNotFoundException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "One or more referenced authors do not exist."
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    /**
     * 参照先の書籍が見つからない場合の例外をハンドルする。
     * @return 404 Not Found
     */
    @ExceptionHandler(BookNotFoundException::class)
    fun handleBookNotFoundException(ex: BookNotFoundException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "The requested book does not exist."
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }


    /**
     * 上記で捕捉されなかった全ての予期せぬ例外をハンドルする。
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please contact support."
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * APIのエラーレスポンスの標準的な形式を定義するデータクラス。
     */
    data class ErrorResponse(
        val status: Int,
        val error: String,
        val message: String?
    )
}

