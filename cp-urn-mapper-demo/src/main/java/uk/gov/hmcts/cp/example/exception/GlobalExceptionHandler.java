package uk.gov.hmcts.cp.example.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CaseUrnValidationException.class)
    public ResponseEntity<String> handleCaseUrnValidation(CaseUrnValidationException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(CaseUrnNotFoundException.class)
    public ResponseEntity<String> handleCaseUrnNotFound(CaseUrnNotFoundException ex) {
        log.warn(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(CaseUrnCertificateException.class)
    public ResponseEntity<String> handleCertificateError(CaseUrnCertificateException ex) {
        log.error(ex.getMessage(), ex.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleDownstreamError(RestClientResponseException ex) {
        log.warn("Downstream error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
    }
}
