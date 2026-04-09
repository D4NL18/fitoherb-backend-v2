package com.fitoherb.fitoherb_backend_v2.DTOs.Responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductCategoryRes {

    private String id;

    private String name;

    private String slug;

    private LocalDateTime createdAt;

    private String imageUrl;
}
