package com.fitoherb.fitoherb_backend_v2.utils.validations;

public class ValidationConstants {

    private ValidationConstants() {
        throw new IllegalStateException("Utility class");
    }

    // TAMANHOS (LENGTHS)
    public static final int MAX_STRING_LENGTH = 255;
    public static final int MAX_TEXT_LENGTH = 5000;
    public static final int MIN_STRING_LENGTH = 3;
    public static final int MIN_PASSWORD_LENGTH = 8;

    // EXPRESSÕES REGULARES (REGEX)
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#]).*$";
    public static final String DATE_FORMAT = "dd-MM-yyyy";
    public static final String SLUG_REGEX = "^[a-z0-9-]+$";


    // MENSAGENS PADRONIZADAS (MESSAGES)

    // Obrigatório (NotNull / NotBlank)
    public static final String MSG_REQUIRED_FIELD = "This field cannot be blank or null";

    // E-mail
    public static final String MSG_EMAIL_INVALID = "E-mail must be valid";

    // Tamanhos (Sizes)
    public static final String MSG_STRING_SIZE = "This field must be between " + MIN_STRING_LENGTH + " and " + MAX_STRING_LENGTH + " characters";
    public static final String MSG_TEXT_SIZE = "This field must be between " + MIN_STRING_LENGTH + " and " + MAX_TEXT_LENGTH + " characters";
    public static final String MSG_PASSWORD_SIZE = "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_STRING_LENGTH + " characters";

    // Datas (Past)
    public static final String MSG_DATE_PAST = "Date must be in the past";

    // Senha
    public static final String MSG_PASSWORD_INVALID = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";

    // SLUG
    public static final String MSG_SLUG_INVALID = "The slug must contain only lowercase letters, numbers, and hyphens";
}