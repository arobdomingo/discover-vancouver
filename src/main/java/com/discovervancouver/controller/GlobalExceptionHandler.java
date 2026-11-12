package com.discovervancouver.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSourceResolvable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.discovervancouver.dto.ApiErrorResponse;
import com.discovervancouver.exception.WeatherUnavailableException;
import com.discovervancouver.exception.ResourceNotFoundException;
import com.discovervancouver.web.RequestIdFilter;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler{

    private static final Logger logger =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestBodyValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(fieldError -> addError(
                errors,
                fieldError.getField(),
                fieldError.getDefaultMessage()
            ));

        ApiErrorResponse response = createBadRequestResponse(
            "Request body validation failed",
            request,
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleParameterValidation(
        HandlerMethodValidationException exception,
        HttpServletRequest request
    ){
        Map<String, List<String>> errors = new LinkedHashMap<>();

        for(ParameterValidationResult result : exception.getParameterValidationResults()){
            String parameterName = result
                .getMethodParameter()
                .getParameterName();

            if (parameterName == null){
                parameterName = "parameter";
            }

            for (MessageSourceResolvable error : result.getResolvableErrors()){
                addError(errors, parameterName, error.getDefaultMessage());
            }
        }

        ApiErrorResponse response = createBadRequestResponse(
            "Request parameter validation failed",
            request,
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    private void addError(
    Map<String, List<String>> errors, String field, String message
    ) {
        errors.computeIfAbsent(field, ignored -> new ArrayList<>()
        ).add(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        addError(
            errors,
            exception.getName(),
            "Invalid value: " + exception.getValue()
        );

        ApiErrorResponse response = createBadRequestResponse(
            "A request parameter has the wrong type",
            request,
            errors
        );

        return ResponseEntity.badRequest().body(response);

    }

    private ApiErrorResponse createBadRequestResponse(
        String message,
        HttpServletRequest request,
        Map<String, List<String>> errors
    ) {
        return new ApiErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI(),
            RequestIdFilter.from(request),
            errors
        );
    }

    @ExceptionHandler(WeatherUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleWeatherUnavailable(
        WeatherUnavailableException exception,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI(),
            RequestIdFilter.from(request),
            Map.of()
        );

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }

    @ExceptionHandler({
        ResourceNotFoundException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(
        Exception exception,
        HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return errorResponse(
            HttpStatus.BAD_REQUEST,
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        logger.error(
            "Unhandled request failure",
            exception
        );
        return errorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request,
            Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
        HttpStatus status,
        String message,
        HttpServletRequest request,
        Map<String, List<String>> errors
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            RequestIdFilter.from(request),
            errors
        ));
    }

}
