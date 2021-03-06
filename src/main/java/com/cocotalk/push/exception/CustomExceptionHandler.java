package com.cocotalk.push.exception;

import com.cocotalk.push.dto.common.response.Response;
import com.cocotalk.push.dto.common.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static com.cocotalk.push.dto.common.response.ResponseStatus.NO_VALUES;


@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Response<?>> serverException(CustomException e) {
        ResponseStatus status = e.getStatus();
        log.error("PushException : " + status.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.OK).body(new Response<>(status));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<?>> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
        String objName = bindingResult.getAllErrors().get(0).getObjectName();
        String code = bindingResult.getAllErrors().get(0).getCodes()[0];
        log.error("MethodArgumentNotValidException : " + errorMessage +" ("+ code +") at " + objName);
        return ResponseEntity.status(HttpStatus.OK).body(new Response<>(NO_VALUES));
    }

    @ExceptionHandler(NoSuchMethodError.class)
    public ResponseEntity<Response<?>> noSuchException(NoSuchMethodError e) {
        e.printStackTrace();
        String parseMessage="DeserializationContext";
        /*
         * dto 입력 과정에서 잘못된 값이 들어온 경우
         * ex) LocalDate birth에 빈문자열 ""를 넣은 경우
         */
        if(e.getMessage()!=null && e.getMessage().contains(parseMessage))
            return ResponseEntity.status(HttpStatus.OK).body(new Response<>(ResponseStatus.PARSE_ERROR));
        return ResponseEntity.status(HttpStatus.OK).body(new Response<>(ResponseStatus.INTERNAL_SERVER_ERROR));
    }

}