package davi.spf.supportflow.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException e,
                                                                            HttpServletRequest servletRequest) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                404,
                "Not found",
                e.getMessage(),
                servletRequest.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRuleException(BusinessRuleException e,
                                                                        HttpServletRequest servletRequest) {

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                422,
                "Invalid Request",
                e.getMessage(),
                servletRequest.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e,
                                                                                 HttpServletRequest servletRequest) {

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                409,
                "Resource Conflict",
                e.getMessage(),
                servletRequest.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserNotActiveException(UserNotActiveException e,
                                                                                 HttpServletRequest servletRequest) {

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                400,
                "Not active",
                e.getMessage(),
                servletRequest.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
