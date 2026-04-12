package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import com.fitoherb.fitoherb_backend_v2.entities.ProductCategory;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
public class ProductReq {

    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;

    @Size(min = MIN_STRING_LENGTH, max = MAX_TEXT_LENGTH, message = MSG_TEXT_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String description;

    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String categorySlug;

    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String supplierSlug;
}
