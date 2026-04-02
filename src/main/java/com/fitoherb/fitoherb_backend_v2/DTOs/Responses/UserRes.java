package com.fitoherb.fitoherb_backend_v2.DTOs.Responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserRes {

    private String email;

    private String name;

    private LocalDate birthDate;

    private UserRole role;

    private String createdAt;
}
