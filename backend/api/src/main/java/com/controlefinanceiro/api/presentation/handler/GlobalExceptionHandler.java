package com.controlefinanceiro.api.presentation.handler;

import com.controlefinanceiro.api.domain.exception.ConflitoException;
import com.controlefinanceiro.api.domain.exception.CredenciaisInvalidasException;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.presentation.dto.ErroResponse;
import com.controlefinanceiro.api.presentation.dto.ErroResponse.CampoErro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import com.controlefinanceiro.api.domain.exception.TentativasExcedidasException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;

// IMPORTANTE: observa todos os Controllers. Erro tratado aqui, em um lugar só.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Validação do DTO falhou (@Valid): devolve 400 apontando cada campo errado.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ErroResponse corpo = ErroResponse.deValidacao(
                HttpStatus.BAD_REQUEST.value(), "Dados inválidos.", campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    // JSON quebrado, vazio ou com tipo errado. Mensagem fixa: não repetimos o texto interno do Jackson.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonIlegivel(HttpMessageNotReadableException ex) {
        ErroResponse corpo = ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(), "Corpo da requisição inválido ou ilegível.");
        return ResponseEntity.badRequest().body(corpo);
    }

    // IMPORTANTE: rede de segurança. O detalhe vai só para o LOG; o cliente recebe mensagem genérica.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex) {
        log.error("Erro inesperado", ex);

        ErroResponse corpo = ErroResponse.de(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(corpo);

    }

    // 404: não existe ou não é do usuário (mesma resposta, de propósito).
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    // 409: conflito com algo que já existe (duplicado).
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> tratarConflito(ConflitoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    // 422: dados bem formados, mas o negócio não permite (ex.: saldo insuficiente).
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ErroResponse.de(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage()));
    }

    // Rota ou arquivo que não existe (ex.: GET /, /favicon.ico). É 404, não 500.
    // Sem log de erro: é ruído de navegador ou scanner, não falha do sistema.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado."));
    }

    // Header obrigatório não enviado (ex.: X-Usuario-Id). Erro do cliente: 400.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErroResponse> tratarHeaderAusente(MissingRequestHeaderException ex) {
        return ResponseEntity.badRequest().body(ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Cabeçalho obrigatório ausente: " + ex.getHeaderName() + "."));
    }

    // Valor com tipo errado (ex.: "abc" onde se espera número). Mensagem fixa, sem detalhes internos.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Valor inválido para o parâmetro '" + ex.getName() + "'."));
    }
    // 401: login com e-mail ou senha errados. Mesma mensagem para os dois casos, de propósito.
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErroResponse.de(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }
    // Corrida no UNIQUE: duas requisições passaram na checagem amigável e o banco barrou a segunda.
    // Mensagem genérica: o texto da exceção revela constraint e SQL.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.de(
                HttpStatus.CONFLICT.value(), "Já existe um registro com esses dados."));
    }
    // 429: excesso de tentativas de login. Retry-After diz, em segundos, quando tentar de novo.
    @ExceptionHandler(TentativasExcedidasException.class)
    public ResponseEntity<ErroResponse> tratarTentativasExcedidas(TentativasExcedidasException ex) {
        long segundos = ex.getSegundosParaLiberar();
        long minutos = (segundos + 59) / 60; // arredonda para cima

        String mensagem = "Muitas tentativas de login. Tente novamente em " + minutos + " minuto(s).";

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(segundos))
                .body(ErroResponse.de(HttpStatus.TOO_MANY_REQUESTS.value(), mensagem));
    }
    // Duas operações alteraram a mesma conta ao mesmo tempo (@Version): a segunda perde e pode repetir.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> tratarConflitoDeVersao(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.de(
                HttpStatus.CONFLICT.value(),
                "A conta foi alterada por outra operação. Tente novamente."));
    }

}

