package br.com.luansantos.pessoas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que o valor e um CPF real: formato aceito com ou sem mascara e
 * digitos verificadores conferidos.
 *
 * Serve tanto no corpo da requisicao quanto em variavel de path, por isso
 * aceita PARAMETER alem de FIELD.
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

    String message() default "CPF invalido: informe 11 digitos validos, com ou sem pontuacao";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
