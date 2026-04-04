package com.fitoherb.fitoherb_backend_v2.infra.exceptions;

import com.fitoherb.fitoherb_backend_v2.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestValidationErrorMessage> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        RestValidationErrorMessage response = new RestValidationErrorMessage(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields",
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<RestErrorMessage> handleResourceAlreadyExists(ResourceAlreadyExistsException exception) {
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.CONFLICT, exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestErrorMessage> handleMessageNotReadable(HttpMessageNotReadableException ex) {

        String message = "Invalid data format. Please ensure that dates are in the correct format and all fields match their expected types.";

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.BAD_REQUEST, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<RestErrorMessage> invalidCredentialsHandler(Exception exception) {

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.UNAUTHORIZED, "E-mail or password invalid.");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<RestErrorMessage> resourceNotFoundHandler(ResourceNotFoundException exception) {
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.NOT_FOUND, exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<RestErrorMessage> unauthorizedAccessHandler(UnauthorizedAccessException exception) {
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.FORBIDDEN, exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestErrorMessage> methodNotSupportedHandler(HttpRequestMethodNotSupportedException ex) {

        String message = String.format("The HTTP method '%s' is not supported for this endpoint.", ex.getMethod());

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.METHOD_NOT_ALLOWED, message);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<RestErrorMessage> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {

        RestErrorMessage errorResponse = new RestErrorMessage(
                HttpStatus.FORBIDDEN,
                "Access denied: You do not have the necessary permissions to access this resource."
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<RestErrorMessage> handleInvalidTokenException(InvalidTokenException ex) {
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<RestErrorMessage> handleInvalidDataAccess(org.springframework.dao.InvalidDataAccessApiUsageException ex) {
        String message = "Invalid request: check if the sort field or query parameters are correct.";

        if (ex.getMessage() != null && ex.getMessage().contains("Could not resolve attribute")) {
            try {
                String field = ex.getMessage().split("'")[1];
                message = "Invalid sort field: " + field;
            } catch (Exception ignored) {}
        }

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RestErrorMessage> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String message = String.format("The parameter '%s' should be of type '%s'", ex.getName(), ex.getRequiredType().getSimpleName());
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<RestErrorMessage> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        String message = "Data integrity violation. This email might already be in use by another user.";
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.CONFLICT, message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorMessage> handleGenericException(Exception ex) {
        ex.printStackTrace();

        RestErrorMessage errorResponse = new RestErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact the administrator."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
