package com.pearldatadirect.ibanvalidator.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AeIbanValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AeIban {

  String message() default "Invalid UAE (AE) IBAN";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
