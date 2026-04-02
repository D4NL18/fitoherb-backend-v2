package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
public class UserReq {

    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;

    @Past(message = MSG_DATE_PAST)
    @NotNull(message = MSG_REQUIRED_FIELD)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private LocalDate birthDate;

    @NotNull(message = MSG_REQUIRED_FIELD)
    private UserRole role;
}