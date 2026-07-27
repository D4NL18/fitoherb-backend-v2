package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SupplierReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.SupplierMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ProductRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.SupplierRepository;
import com.fitoherb.fitoherb_backend_v2.entities.Product;
import com.fitoherb.fitoherb_backend_v2.utils.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    private final SupplierMapper supplierMapper;

    private final FileStorageService fileStorageService;

    private static final String SUPPLIER_NOT_FOUND_MSG = "Fornecedor não encontrado com slug: ";

    public SupplierRes getSupplierBySlug(String slug) {
        Supplier supplier = this.supplierRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPLIER_NOT_FOUND_MSG + slug));

        SupplierRes res = supplierMapper.entityToRes(supplier);

        res.setCount(this.supplierRepository.countProductsBySupplierSlug(supplier.getSlug()));

        return res;
    }

    public List<SupplierRes> getAllSuppliers() {
        List<Supplier> supplierList = this.supplierRepository.findAll(Sort.by("name"));
        List<SupplierRes> resList = supplierMapper.toResList(supplierList);

        enrichWithProductCount(resList);

        return resList;
    }

    public Page<SupplierRes> getAllSuppliersPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        org.springframework.data.domain.Page<Supplier> supplierPage = supplierRepository.findAllFiltered(searchTerm, pageable);

        Page<SupplierRes> resPage = supplierPage.map(supplierMapper::entityToRes);

        enrichWithProductCount(resPage.getContent());

        return resPage;
    }

    private void enrichWithProductCount(List<SupplierRes> dtos) {
        Map<String, Long> countsMap = supplierRepository.countProductsPerSupplier()
                .stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Long) obj[1]
                ));

        dtos.forEach(dto -> {
            dto.setCount(countsMap.getOrDefault(dto.getSlug(), 0L).intValue());
        });
    }

    @Transactional
    public Supplier createSupplier(SupplierReq supplierReq, MultipartFile image) {
        if(this.supplierRepository.findByName(supplierReq.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Já existe um fornecedor com esse nome");
        }

        String generatedSlug = StringUtils.toSlug(supplierReq.getName());

        if(this.supplierRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Já existe um fornecedor com um nome similar (Conflito de slug: " + generatedSlug + ")"
            );
        }

        String fileName = null;
        if (image != null && !image.isEmpty()) {
            fileName = fileStorageService.storeSupplierImage(image);
        }

        Supplier supplier = supplierMapper.reqToEntity(supplierReq);
        supplier.setSlug(generatedSlug);
        supplier.setImagePath(fileName);

        return supplierRepository.save(supplier);
    }

    @Transactional
    public void updateSupplierBySlug(SupplierReq supplierReq, String slug,MultipartFile image) {
        Supplier supplier = this.supplierRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPLIER_NOT_FOUND_MSG + slug));

        String generatedSlug = StringUtils.toSlug(supplierReq.getName());

        if (!supplier.getSlug().equals(generatedSlug) && this.supplierRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Já existe um fornecedor com um nome similar (Conflito de slug: " + generatedSlug + ")"
            );
        }

        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteSupplierImage(supplier.getImagePath());
            String newFileName = fileStorageService.storeSupplierImage(image);
            supplier.setImagePath(newFileName);
        }

        try {
            supplierMapper.updateEntityFromReq(supplierReq, supplier);
            supplier.setSlug(generatedSlug);

            supplierRepository.save(supplier);
        } catch (Exception e) {
            throw new DatabaseOperationException("Falha ao atualizar fornecedor no banco de dados.", e);
        }
    }

    @Transactional
    public void deleteSupplierBySlug(String slug, boolean cascade) {
        Supplier supplier = this.supplierRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(SUPPLIER_NOT_FOUND_MSG + slug));
        
        if (cascade) {
            List<Product> products = productRepository.findBySupplierId(supplier.getId());
            for (Product p : products) {
                fileStorageService.deleteProductImage(p.getImagePath());
                productRepository.delete(p);
            }
        }

        try {
            fileStorageService.deleteSupplierImage(supplier.getImagePath());
            this.supplierRepository.delete(supplier);
        }catch(Exception e) {
            throw new DatabaseOperationException("Falha ao excluir fornecedor. Verifique se não há registros vinculados a este cadastro.", e);
        }
    }
}
