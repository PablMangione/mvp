// src/main/java/com/acainfo/mvp/config/ApiResponseAdvice.java
package com.acainfo.mvp.config;

import com.acainfo.mvp.dto.common.ApiErrorResponseDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.acainfo.mvp.controller")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // No envolver si:
        // 1. Ya es ApiResponseDto
        // 2. Es ResponseEntity<ApiResponseDto>
        // 3. Es un error (ApiErrorResponseDto)
        // 4. Es un endpoint de Swagger

        String className = returnType.getParameterType().getSimpleName();

        if (returnType.getParameterType().equals(ApiResponseDto.class)) {
            return false;
        }

        if (returnType.getParameterType().equals(ResponseEntity.class)) {
            // Verificar el tipo genérico
            if (returnType.getGenericParameterType().toString().contains("ApiResponseDto")) {
                return false;
            }
        }

        // No envolver errores
        if (returnType.getParameterType().equals(ApiErrorResponseDto.class)) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // Si es null, devolver respuesta vacía exitosa
        if (body == null) {
            return ApiResponseDto.success(null);
        }

        // Si ya es ApiResponseDto o error, no tocar
        if (body instanceof ApiResponseDto || body instanceof ApiErrorResponseDto) {
            return body;
        }

        // Si es ResponseEntity, extraer el body
        if (body instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) body;
            Object responseBody = responseEntity.getBody();

            // Si el body ya es ApiResponseDto, devolverlo tal cual
            if (responseBody instanceof ApiResponseDto) {
                return body;
            }

            // Si no, envolverlo
            return ResponseEntity
                    .status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(ApiResponseDto.success(responseBody));
        }

        return ApiResponseDto.success(body);
    }
}