package com.mapletalk.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.mapletalk.dto.ApiError;

/**
 * Single, consistent error-response mechanism for every controller. Business
 * exceptions stay HTTP-agnostic in the service layer; this class is the only
 * place that maps them to a status code and a safe (never internals-leaking)
 * response body.
 *
 * Does NOT intercept JWT authentication failures — those are handled inside
 * the security filter chain (JwtAuthFilter / SecurityConfig's
 * authenticationEntryPoint) before a request ever reaches a controller, so
 * missing/malformed/invalid JWTs continue to produce 401 exactly as before.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		return status(HttpStatus.BAD_REQUEST, new ApiError(
				HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleMalformedBody(HttpMessageNotReadableException ex) {
		return status(HttpStatus.BAD_REQUEST, new ApiError(
				HttpStatus.BAD_REQUEST.value(), "Malformed request body"));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return status(HttpStatus.BAD_REQUEST, new ApiError(
				HttpStatus.BAD_REQUEST.value(), "Invalid value for parameter '" + ex.getName() + "'"));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
		return status(HttpStatus.BAD_REQUEST, new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
		return status(HttpStatus.UNAUTHORIZED, new ApiError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
	}

	@ExceptionHandler(NotRequestRecipientException.class)
	public ResponseEntity<ApiError> handleForbidden(NotRequestRecipientException ex) {
		return status(HttpStatus.FORBIDDEN, new ApiError(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
	}

	@ExceptionHandler({
			UserNotFoundException.class,
			FriendRequestNotFoundException.class,
			IllegalStateException.class // authenticated user vanished mid-session — see service Javadoc
	})
	public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
		String message = ex instanceof IllegalStateException ? "User not found" : ex.getMessage();
		return status(HttpStatus.NOT_FOUND, new ApiError(HttpStatus.NOT_FOUND.value(), message));
	}

	@ExceptionHandler({
			EmailAlreadyExistsException.class,
			DuplicateFriendRequestException.class,
			AlreadyFriendsException.class,
			FriendRequestAlreadyProcessedException.class
	})
	public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
		return status(HttpStatus.CONFLICT, new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		log.error("Unhandled exception while processing request", ex);
		return status(HttpStatus.INTERNAL_SERVER_ERROR, new ApiError(
				HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"));
	}

	private static ResponseEntity<ApiError> status(HttpStatus status, ApiError body) {
		return ResponseEntity.status(status).body(body);
	}

}
