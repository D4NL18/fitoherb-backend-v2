package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class ProductServiceSpecTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testFilters() {
        Specification<Product> spec = Specification.where((root, query, cb) -> cb.conjunction());

        List<String> categories = Arrays.asList("ervas");
        spec = spec.and((root, query, cb) -> root.get("category").get("slug").in(categories));

        List<String> suppliers = Arrays.asList("nature-labs");
        spec = spec.and((root, query, cb) -> root.get("supplier").get("slug").in(suppliers));

        Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 10));
        System.out.println("TOTAL ELEMENTS: " + page.getTotalElements());
    }
}
