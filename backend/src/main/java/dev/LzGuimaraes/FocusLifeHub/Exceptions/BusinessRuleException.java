package dev.LzGuimaraes.FocusLifeHub.Exceptions;

/**
 * Violação de uma regra de negócio que deve retornar 400 (Bad Request) com a
 * mensagem em linguagem natural para o usuário — ex.: "a soma das classes da
 * Carteira Ideal deve ser 100%". Diferente de erro de validação de campo
 * (MethodArgumentNotValidException), aqui a mensagem já vem pronta.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
