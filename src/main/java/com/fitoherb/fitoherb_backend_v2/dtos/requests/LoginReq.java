package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
@Schema(description = "Request object for user authentication")
public class LoginReq {

    @Schema(description = "User's registered email address", example = "daniel.marinho@example.com")
    @Email(message = MSG_EMAIL_INVALID)
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String email;

    @Schema(description = "User's account password", example = "P@ssword123!")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String password;
}