package com.tracker.config;

import com.tracker.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.tracker.controller")
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Apply to all controller methods
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // If the body is already an ApiResponse (e.g., from an exception handler), return as is.
        if (body instanceof ApiResponse) {
            return body;
        }

        // String responses cause ClassCastException with StringHttpMessageConverter if wrapped dynamically,
        // unless handled carefully. For now, since our APIs return DTOs, this is mostly safe.
        // If it's a string, we could serialize it manually, but we don't have endpoints returning raw strings right now.
        if (body instanceof String) {
            // This is a naive workaround. In a production app, we configure Jackson to handle this.
            return body; 
        }

        // Do not wrap byte arrays (used for file exports like Excel, CSV, PDF)
        if (body instanceof byte[]) {
            return body;
        }

        return ApiResponse.builder()
                .success(true)
                .message("Operation completed successfully")
                .data(body)
                .build();
    }
}
