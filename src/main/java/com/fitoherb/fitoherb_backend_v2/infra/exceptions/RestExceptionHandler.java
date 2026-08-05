package com.fitoherb.fitoherb_backend_v2.infra.exceptions;

import com.fitoherb.fitoherb_backend_v2.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RestValidationErrorMessage> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                if (error instanceof org.springframework.validation.FieldError fieldError) {
                    fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
                } else {
                    fieldErrors.put(paramName, error.getDefaultMessage());
                }
            });
        });

        RestValidationErrorMessage response = new RestValidationErrorMessage(
                HttpStatus.BAD_REQUEST,
                "Falha na validação de um ou mais campos",
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestValidationErrorMessage> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        RestValidationErrorMessage response = new RestValidationErrorMessage(
                HttpStatus.BAD_REQUEST,
                "Falha na validação de um ou mais campos",
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

        String message = "Formato de dados inválido. Certifique-se de que as datas estão no formato correto e todos os campos correspondem aos tipos esperados.";

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.BAD_REQUEST, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<RestErrorMessage> invalidCredentialsHandler(Exception exception) {

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");

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

        String message = String.format("O método HTTP '%s' não é suportado para este endpoint.", ex.getMethod());

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.METHOD_NOT_ALLOWED, message);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<RestErrorMessage> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {

        RestErrorMessage errorResponse = new RestErrorMessage(
                HttpStatus.FORBIDDEN,
                "Acesso negado: Você não possui as permissões necessárias para acessar este recurso."
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<RestErrorMessage> handleInvalidTokenException(InvalidTokenException ex) {
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<RestErrorMessage> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        String message = "Requisição inválida: verifique se o campo de ordenação ou os parâmetros de consulta estão corretos.";
        String rawMessage = ex.getMessage();

        if (rawMessage != null && rawMessage.contains("Could not resolve attribute")) {
            var matcher = java.util.regex.Pattern.compile("'([^']*)'").matcher(rawMessage);
            if (matcher.find()) {
                message = "Campo de ordenação inválido: " + matcher.group(1);
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestErrorMessage(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RestErrorMessage> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        String typeName = (requiredType != null) ? requiredType.getSimpleName() : "tipo desconhecido";
        String message = String.format("O parâmetro '%s' deve ser do tipo '%s'", ex.getName(), typeName);
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.BAD_REQUEST, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestErrorMessage> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Violação de integridade de dados. O recurso já está sendo utilizado ou existe um conflito de dados.";
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.CONFLICT, message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorMessage> handleGenericException(Exception ex) {
        log.error("Exceção não tratada capturada: ", ex);
        RestErrorMessage errorResponse = new RestErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Por favor, entre em contato com o administrador."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestErrorMessage> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestErrorMessage(HttpStatus.BAD_REQUEST, "O arquivo excedeu o limite de armazenamento (10MB)."));
    }
}