package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
@Schema(description = "Request object for new user registration")
public class RegisterReq {

    @Schema(description = "Unique email address for the account", example = "daniel.marinho@example.com")
    @Email(message = MSG_EMAIL_INVALID)
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String email;

    @Schema(description = "Full name of the user", example = "Daniel Marinho")
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;

    @Schema(
            description = "Secure password following security policy (uppercase, lowercase, numbers, and special characters)",
            example = "FitoHerb@2026!"
    )
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_STRING_LENGTH, message = MSG_PASSWORD_SIZE)
    @Pattern(
            regexp = PASSWORD_VALIDATION_REGEX,
            message = MSG_PASSWORD_INVALID
    )
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String password;

    @Schema(description = "User's date of birth in dd-MM-yyyy format", example = "25-10-1995")
    @Past(message = MSG_DATE_PAST)
    @NotNull(message = MSG_REQUIRED_FIELD)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private LocalDate birthDate;

    @Schema(description = "User access level within the system", example = "USER")
    @NotNull(message = MSG_REQUIRED_FIELD)
    private UserRole role;
}