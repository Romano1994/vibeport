package com.vibeport.exception.handler;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
