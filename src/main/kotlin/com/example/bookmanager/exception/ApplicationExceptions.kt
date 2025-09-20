package com.example.bookmanager.exception

/**
 * リクエストされた著者IDが見つからなかった場合にスローされる例外。
 */
class AuthorNotFoundException(message: String) : RuntimeException(message)

/**
 * リクエストされた書籍IDが見つからなかった場合にスローされる例外。
 */
class BookNotFoundException(message: String) : RuntimeException(message)