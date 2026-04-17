package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;
import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_STRING_SIZE;

@Getter
@Setter
@Schema(description = "Request object for email sending")
public class MailReq {

    @Schema(description = "E-mail address that will receive the mail", example = "daniel.marinho@example.com")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    @Email(message = MSG_EMAIL_INVALID)
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    String email;

    @Schema(description = "Title that will be in the email subject", example = "This is a test email subject")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    String subject;

    @Schema(description = "Message that will be in the email body", example = "This is a test email message")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    String message;
}
