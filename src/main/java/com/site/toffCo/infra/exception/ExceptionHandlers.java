package com.site.toffCo.infra.exception;

import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.exception.item.ItemNotFound;
import com.site.toffCo.infra.exception.item.QuantidadInvalid;
import com.site.toffCo.infra.exception.product.CategoryNotExisting;
import com.site.toffCo.infra.exception.product.ProductNotFound;
import com.site.toffCo.infra.exception.user.EmailIsExisting;
import com.site.toffCo.infra.exception.user.InvalidRefreshToken;
import com.site.toffCo.infra.exception.user.UserNotFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j // Ativa o "log.error" e "log.info" do padrão SLF4J/Lombok
@RestControllerAdvice
public class ExceptionHandlers extends ResponseEntityExceptionHandler {

    // ============================== USER CUSTOM EXCEPTIONS ==============================

    @ExceptionHandler(InvalidRefreshToken.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(InvalidRefreshToken exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid Refresh Token");

        //Boa prática: Registrar no log do servidor quando alguém tentar usar um token bizarro
        log.warn("Tentativa de refresh com token inválido ou expirado.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFound exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found");

        log.warn("Tentativa de cadastrar usuario");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(EmailIsExisting.class)
    public ResponseEntity<ProblemDetail> handleEmailIsExisting(EmailIsExisting exception) {
        ProblemDetail  problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Email already exists");
        log.warn("Tentativa de cadastrar usuario com email ja existente");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    //============================== PRODUCT ==============================

    @ExceptionHandler(ProductNotFound.class)
    public ResponseEntity<ProblemDetail> handleProductNotFound(ProductNotFound exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Product not found");
        log.warn("Tentativa de buscar produto");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(CategoryNotExisting.class)
    public ResponseEntity<ProblemDetail> handleCategoryNotExisting(CategoryNotExisting exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Category not exists");
        log.warn("Tentativa de buscar produto pro categoria");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    //============================== CAR ==============================

    @ExceptionHandler(CarNotFound.class)
    public  ResponseEntity<ProblemDetail> handleCarNotFound(CarNotFound exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Car not found");
        log.warn("Tentativa de buscar car");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    //============================== ITEM ==============================

    @ExceptionHandler(ItemNotFound.class)
    public ResponseEntity<ProblemDetail> handleItemNotFound(ItemNotFound exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Item not found");
        log.warn("Tentativa de buscar item");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(QuantidadInvalid.class)
    public ResponseEntity<ProblemDetail> handleQuantidadInvalid(QuantidadInvalid exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Quantidad invalida");
        log.warn("Tentativa de buscar quantidad invalida");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ============================== VALIDATION OVERRIDES ==============================

    // Captura erros de validação (@Valid, @NotBlank, @NotNull, etc.)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Method argument not valid");

        // Extrai os campos específicos que falharam na validação
        List<String> erros = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();

        problem.setProperty("erros", erros); // Adiciona a lista de erros no JSON final do RFC 7807
        return ResponseEntity.badRequest().body(problem);
    }

    // Captura erros de sintaxe no JSON enviado (ex: mandar string onde deveria ser número)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request body format");
        return ResponseEntity.badRequest().body(problem);
    }

    // ============================== GLOBAL FALLBACK ==============================

    // Erro "Coringa" para qualquer falha inesperada (banco caiu, erro de lógica, npe, etc.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericError(Exception exception) {
        // Grava o erro real com o StackTrace completo no console/arquivo de log do servidor para você debugar
        log.error("Unexpected error occurred: ", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred, please contact support."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ============================== SECURITY EXCEPTIONS ==============================

    // Cai aqui se a senha estiver errada
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "E-mail ou senha incorretos."
        );

        log.warn("Tentativa de login com senha incorreta ou usuário inexistente.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    // Cai aqui se o usuário estiver bloqueado, desativado, etc.
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(org.springframework.security.core.AuthenticationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Falha na autenticação: " + exception.getMessage()
        );

        log.warn("Falha geral de autenticação: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
}

