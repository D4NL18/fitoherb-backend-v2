package com.fitoherb.fitoherb_backend_v2.controllers;

import com.fitoherb.fitoherb_backend_v2.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migrations")
@RequiredArgsConstructor
public class DataMigrationController {

    private final ProductService productService;

    @PostMapping("/product-slugs")
    public ResponseEntity<String> migrateProductSlugs() {
        productService.migrateAllProductSlugs();
        return ResponseEntity.ok("Todos os slugs de produtos foram migrados com sucesso para o novo formato!");
    }
}
