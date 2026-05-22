package com.vibeport.commonVo;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;


@ToString
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private final int code;
    private final HttpStatus status;
    private final String message;
    private final T data;
    private final LocalDateTime time;


    public CommonResponse(HttpStatus status, String message, T data, LocalDateTime time) {
        //지울지 고민 중...
        this.code = status.value();
        this.status = status;
        this.message = message;
        this.data = data;
        this.time = time;
    }

    public static <T> CommonResponse<T> of(HttpStatus status, String message, T data, LocalDateTime time) {
        return new CommonResponse<>(status, message, data, time);
    }

    public static <T> CommonResponse<T> error(HttpStatus status, String message, LocalDateTime time) {
        return CommonResponse.of(status, message, null, time);
    }

    public static <T> CommonResponse<T> ok(HttpStatus status, String message, T data) {
        if (message == null) {
            message = "성공적으로 호출하였습니다.";
        }
        return CommonResponse.of(status, message, data, LocalDateTime.now());
    }

    public static <T> CommonResponse<T> ok( T data) {
        return CommonResponse.ok(HttpStatus.OK, null, data);
    }


}

