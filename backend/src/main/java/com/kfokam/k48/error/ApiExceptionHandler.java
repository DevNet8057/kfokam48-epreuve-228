package com.kfokam.k48.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException exception) {
        return response(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "La requête est invalide." : "Le champ " + fieldError.getField() + " est invalide.";
        return response(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT", "Le corps de la requête est invalide.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConstraint(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "CONTRAINTE_VIOLEE", "Cette opération entre en conflit avec une donnée existante.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleUnknownRoute(NoResourceFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "ROUTE_INCONNUE", "La ressource demandée est introuvable.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Une erreur interne est survenue.");
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(Objects.requireNonNull(code), Objects.requireNonNull(message)));
    }
}
