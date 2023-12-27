package org.geelato.web.platform.m.base.rest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.validation.ConstraintViolationException;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.exception.TestException;
import org.geelato.core.orm.DaoException;
import org.geelato.utils.BeanValidators;
import org.geelato.utils.UIDGenerator;
import org.geelato.web.platform.PlatformRuntimeException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;


/**
 * 自定义ExceptionHandler，专门处理Restful异常.
 **/
// 会被Spring-MVC自动扫描，但又不属于Controller的annotation。
@ControllerAdvice
public class PlatformExceptionHandler extends ResponseEntityExceptionHandler {
    /**
     * 处理JSR311 Validation异常.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(value = {ConstraintViolationException.class})
    public final ResponseEntity<?> handleException(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = BeanValidators.extractPropertyAndMessage(ex.getConstraintViolations());
        String body = JSON.toJSONString(errors);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaTypes.TEXT_PLAIN_UTF_8));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = {PlatformRuntimeException.class})
    public final ResponseEntity<?> handleException(PlatformRuntimeException ex, WebRequest request) {
        ApiResult apiResult=new ApiResult();
        ex.printStackTrace();
        apiResult.setCode(ex.getCode());
        apiResult.setMsg(ex.getMsg());
        String content="错误详细已经做了统一记录,请复制并依据该标识向系统管理员进行沟通交流,标识依据是:;"+ UIDGenerator.generate();
        apiResult.setData(content);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaTypes.JSON_UTF_8));
        return handleExceptionInternal(ex, apiResult, headers, HttpStatus.BAD_REQUEST, request);
    }
    /**
     * 处理RestException.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(value = {RestException.class})
    public final ResponseEntity<?> handleException(RestException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaTypes.TEXT_PLAIN_UTF_8));
        return handleExceptionInternal(ex, ex.getMessage(), headers, ex.status, request);
    }

    /**
     * 处理数据唯一约束问题
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(value = {DuplicateKeyException.class})
    public final ResponseEntity<?> handleException(DuplicateKeyException ex, WebRequest request) {
        logger.error(ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaTypes.TEXT_PLAIN_UTF_8));
        return handleExceptionInternal(ex, "数据不满足唯一约束要求。", headers, HttpStatus.BAD_REQUEST, request);
    }


    /**
     * 处理除以上问题之后的其它问题
     */
    @org.springframework.web.bind.annotation.ExceptionHandler
    public final ResponseEntity<?> handleOtherException(Exception ex, WebRequest request) {
        logger.error("Exception", ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaTypes.TEXT_PLAIN_UTF_8));
        return handleExceptionInternal(ex, ex.getMessage(), headers, HttpStatus.BAD_REQUEST, request);
    }


    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status, WebRequest request) {

        return handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }
}
