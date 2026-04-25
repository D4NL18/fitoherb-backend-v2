package com.fitoherb.fitoherb_backend_v2.utils.validations;

public class ValidationConstants {

    private ValidationConstants() {
        throw new IllegalStateException("Utility class");
    }

    private static final String CARACTERES_SUFIXO = " caracteres";
    private static final String E = " e ";

    // TAMANHOS (LENGTHS)
    public static final int MAX_STRING_LENGTH = 255;
    public static final int MAX_TEXT_LENGTH = 5000;
    public static final int MIN_STRING_LENGTH = 3;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_ARRAY_SIZE = 20;

    // EXPRESSÕES REGULARES (REGEX)
    @SuppressWarnings("java:S2068")
    public static final String PASSWORD_VALIDATION_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#]).*$";
    public static final String DATE_FORMAT = "dd-MM-yyyy";
    public static final String SLUG_REGEX = "^[a-z0-9-]+$";


    // MENSAGENS PADRONIZADAS (MESSAGES)

    // Obrigatório (NotNull / NotBlank)
    public static final String MSG_REQUIRED_FIELD = "Este campo não pode estar vazio ou nulo";

    // E-mail
    public static final String MSG_EMAIL_INVALID = "O e-mail deve ser válido";

    // Tamanhos (Sizes)
    public static final String MSG_STRING_SIZE = "Este campo deve ter entre " + MIN_STRING_LENGTH + E + MAX_STRING_LENGTH + CARACTERES_SUFIXO;
    public static final String MSG_TEXT_SIZE = "Este campo deve ter entre " + MIN_STRING_LENGTH + E + MAX_TEXT_LENGTH + CARACTERES_SUFIXO;
    public static final String MSG_PASSWORD_SIZE = "A senha deve ter entre " + MIN_PASSWORD_LENGTH + E + MAX_STRING_LENGTH + CARACTERES_SUFIXO;
    public static final String MSG_ARRAY_SIZE = "O tamanho máximo da lista é " + MAX_ARRAY_SIZE;

    // Datas (Past)
    public static final String MSG_DATE_PAST = "A data deve estar no passado";

    // Senha
    public static final String MSG_PASSWORD_INVALID = "A senha deve conter pelo menos uma letra maiúscula, uma letra minúscula, um número e um caractere especial";

    // SLUG
    public static final String MSG_SLUG_INVALID = "O slug deve conter apenas letras minúsculas, números e hífens";
}