package com.vibeport.exception.handler;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 시스템 오류가 아닌 업무로직에서 발생한 예외를 처리
 */
@Getter
@Setter
@ToString
public class BusinessException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final LocalDateTime timestamp = LocalDateTime.now();

	private final int status; // HTTP status code

	private final String error; // 에러

	private final String message; // 메세지

	public BusinessException(int status, String error, String message) {
		super(message);
		this.status = status;
		this.error = error;
		this.message = message;
	}

	public BusinessException(String message) {
		super(message);
		this.status = 500;
		this.error = "";
		this.message = message;
	}
}
