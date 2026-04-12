package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
@Schema(description = "Request object for updating user's security credentials")
public class PasswordUpdateReq {

    @Schema(
            description = "New password following security policy (requires uppercase, lowercase, numbers and special characters)",
            example = "FitoHerb@2026!"
    )
    @NotBlank(message = MSG_REQUIRED_FIELD)
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_STRING_LENGTH, message = MSG_PASSWORD_SIZE)
    @Pattern(regexp = PASSWORD_REGEX, message = MSG_PASSWORD_INVALID)
    private String password;

}