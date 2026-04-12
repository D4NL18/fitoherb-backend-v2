package com.fitoherb.fitoherb_backend_v2.DTOs.Responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductRes {


    private String name;

    private String imageUrl;

    private String description;

    private ProductCategoryRes category;

    private SupplierRes supplier;
}
