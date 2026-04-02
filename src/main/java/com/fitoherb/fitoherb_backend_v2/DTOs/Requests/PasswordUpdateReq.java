package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
public class PasswordUpdateReq {

    @NotBlank(message = MSG_REQUIRED_FIELD)
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_STRING_LENGTH, message = MSG_PASSWORD_SIZE)
    @Pattern(regexp = PASSWORD_REGEX, message = MSG_PASSWORD_INVALID)
    private String password;

}
