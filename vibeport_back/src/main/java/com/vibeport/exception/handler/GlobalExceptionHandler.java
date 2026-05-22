package com.vibeport.exception.handler;

import com.vibeport.commonVo.CommonResponse;
import com.vibeport.commonVo.ErrorLogVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice // @ResponseBody + @ControllerAdvice
public class GlobalExceptionHandler {

//    private final ErrorLogServiceImpl errorLogService;
//
//    public GlobalExceptionHandler(ErrorLogServiceImpl errorLogService) {
//        this.errorLogService = errorLogService;
//    }

    /**
     * Business logic related exceptions
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        log.error("handleBusinessException: {}", e.getMessage());
        final ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        final ErrorResponse response = new ErrorResponse(errorCode);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                .body(new ErrorResponse(e.getStatus(), e.getError(), "-1", e.getMessage()));
    }

    /**
     * Handles exceptions related to access denial.
     */
//    @ExceptionHandler(AccessDeniedException.class)
//    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
//        log.error("handleAccessDeniedException", ex);
//
//        // 에러 로그 생성 및 삽입
//		this.excuteErrorLog(request, ex);
//
//        final ErrorResponse response = new ErrorResponse(ErrorCode.HANDLE_ACCESS_DENIED);
//        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
//    }

    /**
     * Handles exceptions for invalid data binding in @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("handleMethodArgumentNotValidException", ex);

        // 에러 로그 생성 및 삽입
		this.excuteErrorLog(request, ex);

        final ErrorResponse response = new ErrorResponse(ErrorCode.INVALID_BINDING_ERROR);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions for invalid data binding with @ModelAttribute.
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        log.error("handleBindException", ex);

        // 에러 로그 생성 및 삽입
		this.excuteErrorLog(request, ex);

        final ErrorResponse response = new ErrorResponse(ErrorCode.INVALID_BINDING_ERROR);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions when a method argument is not the expected type.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.error("handleMethodArgumentTypeMismatchException", ex);
        // 에러 로그 생성 및 삽입
		this.excuteErrorLog(request, ex);

        final ErrorResponse response = new ErrorResponse(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    /**
     * Handles requests for missing static resources.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Static resource not found: {} {}", request.getMethod(), request.getRequestURI());

        final ErrorResponse response = new ErrorResponse(ErrorCode.POSTS_NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles all other exceptions, logs them to the database, and returns a generic 500 error.
     */
    @ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("handleAllExceptions", ex);

        // 에러 로그 생성 및 삽입
		this.excuteErrorLog(request, ex);

        final ErrorResponse response = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}

    /**
     * 에러 로그를 생성하고 데이터베이스에 삽입합니다.
     */
    private void excuteErrorLog(HttpServletRequest request, Exception ex) {
        ErrorLogVo errorLog = new ErrorLogVo();
        errorLog.setUri(request.getRequestURI());
        errorLog.setMethodNm(request.getMethod());
        errorLog.setMsg(ex.getMessage());

        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        errorLog.setStackTrace(sw.toString());
        errorLog.setTimestamp(LocalDateTime.now());

//		this.errorLogService.excuteErrorLog(errorLog);
    }

    @ExceptionHandler(BizException.class)
    public CommonResponse<Object> handleBizException(BizException ex, HttpServletRequest request, HttpServletResponse response) {

        ErrorLogVo errorLog = new ErrorLogVo();
        errorLog.setUri(request.getRequestURI());
        errorLog.setMethodNm(request.getMethod());
        errorLog.setMsg(ex.getMessage());

        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        errorLog.setStackTrace(sw.toString());
        errorLog.setTimestamp(LocalDateTime.now());

//        this.errorLogService.insertErrorLog(errorLog);
        response.setStatus(ex.getErrorCode().getStatus().value());

        return CommonResponse.error(ex.getErrorCode().getStatus(), ex.getMessage(), LocalDateTime.now());
    }
}
