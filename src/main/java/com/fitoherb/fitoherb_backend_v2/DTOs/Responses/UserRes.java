package com.fitoherb.fitoherb_backend_v2.DTOs.Responses;

import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Response object representing user profile data")
public class UserRes {

    @Schema(description = "User's registered email address", example = "daniel.marinho@example.com")
    private String email;

    @Schema(description = "User's full name", example = "Daniel Marinho")
    private String name;

    @Schema(description = "User's date of birth", example = "25-10-1995")
    private LocalDate birthDate;

    @Schema(description = "User's access role in the system", example = "ADMIN")
    private UserRole role;

    @Schema(description = "Formatted timestamp of account creation", example = "12-04-2026 14:30:00")
    private String createdAt;
}