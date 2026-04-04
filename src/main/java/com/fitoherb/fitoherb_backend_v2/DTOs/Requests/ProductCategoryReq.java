package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;
import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_REQUIRED_FIELD;


@Getter
@Setter
public class ProductCategoryReq {

    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;
}
