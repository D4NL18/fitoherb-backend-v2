package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.MailReq;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestErrorMessage;
import com.fitoherb.fitoherb_backend_v2.infra.exceptions.RestValidationErrorMessage;
import com.fitoherb.fitoherb_backend_v2.services.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("emails")
@Tag(name = "Emails", description = "Operations related to email communications, such as contact forms and system notifications.")
public class EmailController {

    private final MailService mailService;

    @Operation(
            summary = "Send contact email",
            description = "Processes a contact form submission and sends an email to the specified address. Used for customer support and lead generation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = RestValidationErrorMessage.class),
                            examples = @ExampleObject(name = "Validation Error", value = "{\"status\": \"BAD_REQUEST\", \"message\": \"Validation failed for one or more fields\", \"errors\": {\"email\": \"must be a well-formed email address\"}}"))),
            @ApiResponse(responseCode = "500", description = "Internal error or mail server failure",
                    content = @Content(schema = @Schema(implementation = RestErrorMessage.class),
                            examples = @ExampleObject(name = "Mail Error", value = "{\"status\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Falha ao enviar e-mail de boas-vindas\" }")))
    })
    @PostMapping("/send-contact")
    public ResponseEntity<Void> sendContactEmail(@RequestBody @Valid MailReq mailReq) {
        mailService.sendEmail(mailReq);
        return ResponseEntity.ok().build();
    }
}