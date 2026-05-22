package com.vibeport.exception.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    /*
     * @ModelAttribute 으로 binding error 발생시 BindException 발생
     */
    INVALID_BINDING_ERROR(HttpStatus.BAD_REQUEST, "400", "ModelAttribute Binding Error"),

    /*
     * 400 BAD_REQUEST: 잘못된 요청
     */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "Bad Request"),

    /*
     * Authentication 객체가 필요한 권한을 보유하지 않은 경우
     */
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "400", "Access is Denied"),

    /*
     * 404 NOT_FOUND: 리소스를 찾을 수 없음
     */
    POSTS_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "Not Found"),

    /*
     * 405 METHOD_NOT_ALLOWED: 허용되지 않은 Request Method 호출
     */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "Method Not Allowed"),

    /*
     * @Valid가 붙은 파라미터에 대해 검증 실패시 발생
     */
    INVALID_INPUT_VALUE(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Invalid Input Value"),

    /*
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Internal Server Error");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
