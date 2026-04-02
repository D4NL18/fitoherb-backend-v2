package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
public class LoginReq {

    @Email(message = MSG_EMAIL_INVALID)
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String email;

    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String password;
}