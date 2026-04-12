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
@Schema(description = "Request object for updating user profile information")
public class UserReq {

    @Schema(description = "Full name of the user", example = "Daniel Marinho")
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;

    @Schema(description = "User's date of birth in dd-MM-yyyy format", example = "25-10-1995")
    @Past(message = MSG_DATE_PAST)
    @NotNull(message = MSG_REQUIRED_FIELD)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private LocalDate birthDate;

    @Schema(description = "User access level within the system", example = "ADMIN")
    @NotNull(message = MSG_REQUIRED_FIELD)
    private UserRole role;
}