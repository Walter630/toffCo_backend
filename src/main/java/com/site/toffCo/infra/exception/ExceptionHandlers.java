package com.site.toffCo.infra.exception;

import com.site.toffCo.infra.exception.carrinho.CarIsEmpty;
import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.exception.item.ItemNotFound;
import com.site.toffCo.infra.exception.item.QuantidadInvalid;
import com.site.toffCo.infra.exception.nota.NfNotFound;
import com.site.toffCo.infra.exception.odoo.OdooBusinessException;
import com.site.toffCo.infra.exception.payment.PaymentInvalidForm;
import com.site.toffCo.infra.exception.payment.PaymentNotFound;
import com.site.toffCo.infra.exception.stock.InsufficientStock;
import com.site.toffCo.module.produto.domain.exception.CategoryNotExisting;
import com.site.toffCo.module.produto.domain.exception.ProductNotFound;
import com.site.toffCo.infra.exception.user.EmailIsExisting;
import com.site.toffCo.infra.exception.user.InvalidRefreshToken;
import com.site.toffCo.infra.exception.user.UserNotFound;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlers extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidRefreshToken.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(InvalidRefreshToken exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), "INVALID_REFRESH_TOKEN");
    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "USER_NOT_FOUND");
    }

    @ExceptionHandler(EmailIsExisting.class)
    public ResponseEntity<ApiErrorResponse> handleEmailIsExisting(EmailIsExisting exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "EMAIL_ALREADY_EXISTS");
    }

    @ExceptionHandler(ProductNotFound.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "PRODUCT_NOT_FOUND");
    }

    @ExceptionHandler(CategoryNotExisting.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotExisting(CategoryNotExisting exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "CATEGORY_NOT_FOUND");
    }

    @ExceptionHandler(NfNotFound.class)
    public ResponseEntity<ApiErrorResponse> handleNfNotFound(NfNotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "INVOICE_NOT_FOUND");
    }

    @ExceptionHandler(CarNotFound.class)
    public ResponseEntity<ApiErrorResponse> handleCarNotFound(CarNotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "CART_NOT_FOUND");
    }

    @ExceptionHandler(CarIsEmpty.class)
    public ResponseEntity<ApiErrorResponse> handleCarIsEmpty(CarIsEmpty exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "EMPTY_CART");
    }

    @ExceptionHandler(ItemNotFound.class)
    public ResponseEntity<ApiErrorResponse> handleItemNotFound(ItemNotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "ITEM_NOT_FOUND");
    }

    @ExceptionHandler(QuantidadInvalid.class)
    public ResponseEntity<ApiErrorResponse> handleQuantidadInvalid(QuantidadInvalid exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "INVALID_QUANTITY");
    }

    @ExceptionHandler(InsufficientStock.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStock(InsufficientStock exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                exception.getMessage(),
                "INSUFFICIENT_STOCK",
                Map.of(
                        "productId", exception.getProductId(),
                        "available", exception.getAvailable(),
                        "requested", exception.getRequested()
                )
        ));
    }

    @ExceptionHandler(PaymentNotFound.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentNotFound(PaymentNotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "ORDER_NOT_FOUND");
    }

    @ExceptionHandler(PaymentInvalidForm.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentInvalidForm(PaymentInvalidForm exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "INVALID_PAYMENT");
    }

    @ExceptionHandler(OdooBusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleOdooBusinessException(OdooBusinessException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "ODOO_BUSINESS_ERROR");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Conflito de integridade no banco", exception);
        return error(HttpStatus.CONFLICT, "Conflito com um registro existente.", "DATA_CONFLICT");
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflito de integridade no banco: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        "Conflito de concorrencia. Por favor, tente novamente.",
                        "CONFLICT",
                        Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "INVALID_REQUEST");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "A requisição contém dados inválidos.",
                "VALIDATION_ERROR",
                Map.of("errors", errors)
        ));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "O corpo da requisição possui formato ou valor inválido.",
                "INVALID_REQUEST_BODY"
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.", "UNAUTHORIZED");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "Falha na autenticação.", "UNAUTHORIZED");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericError(Exception exception) {
        log.error("Unexpected error occurred", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.", "INTERNAL_ERROR");
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            String code
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                message == null || message.isBlank() ? status.getReasonPhrase() : message,
                code
        ));
    }
}
