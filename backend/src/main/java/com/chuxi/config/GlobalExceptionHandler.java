package com.chuxi.config;

import com.chuxi.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 处理所有未捕获异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("未捕获异常：{}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail("服务器内部错误"));
    }

    // 处理参数校验异常（配合 @Valid）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(R.fail(message));
    }

    // 客户端输入错误（JSON 解析失败 / 路径变量类型不匹配 / 缺参 / 媒体类型不支持 / 绑定错误）：
    // 属调用方问题，返回 400 而非 500，避免错误监控被客户端噪音污染
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMediaTypeNotSupportedException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<R<Void>> handleClientError(Exception e) {
        log.warn("客户端请求不合法：{}", e.getMessage());
        return ResponseEntity.badRequest().body(R.fail("请求参数不合法"));
    }

    // 处理非法参数异常：只回通用消息，异常细节仅入日志，避免内部信息外泄
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());
        return ResponseEntity.badRequest().body(R.fail("请求参数不合法"));
    }

    // 处理实体未找到异常
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<R<Void>> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(R.fail("资源不存在"));
    }
}
