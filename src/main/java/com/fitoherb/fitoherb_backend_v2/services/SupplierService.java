package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SupplierReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SupplierRes;
import com.fitoherb.fitoherb_backend_v2.entities.Supplier;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.SupplierMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.SupplierRepository;
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

@RequiredArgsConstructor
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    private final SupplierMapper supplierMapper;

    private FileStorageService fileStorageService;

    public List<SupplierRes> getAllSuppliers() {
        List<Supplier> supplierList = this.supplierRepository.findAll();

        return supplierMapper.toResList(supplierList);
    }

    public SupplierRes getSupplierBySlug(String slug) {
        Supplier supplier = this.supplierRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with slug: " + slug));
        SupplierRes supplierRes = supplierMapper.entityToRes(supplier);
        return supplierRes;
    }

    public Page<SupplierRes>  getAllSuppliersPaginated(String search, int page, String sortField, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        org.springframework.data.domain.Page<Supplier> supplierPage = supplierRepository.findAllFiltered(searchTerm, pageable);

        return supplierPage.map(supplierMapper::entityToRes);
    }

    @Transactional
    public Supplier createSupplier(SupplierReq supplierReq, MultipartFile image) {
        if(this.supplierRepository.findByName(supplierReq.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Supplier with that name already exists");
        }

        String generatedSlug = StringUtils.toSlug(supplierReq.getName());

        if(this.supplierRepository.findBySlug(generatedSlug).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "A supplier  with a similar name already exists (Slug conflict: " + generatedSlug + ")"
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
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with slug: " + slug));

        String generatedSlug = StringUtils.toSlug(supplierReq.getName());

        if (!supplier.getSlug().equals(generatedSlug)) {
            if (this.supplierRepository.findBySlug(generatedSlug).isPresent()) {
                throw new ResourceAlreadyExistsException(
                        "A supplier with a similar name already exists (Slug conflict: " + generatedSlug + ")"
                );
            }
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
            throw new DatabaseOperationException("Failed to update supplier in database.");
        }
    }

    @Transactional
    public void deleteSupplierBySlug(String slug) {
        Supplier supplier = this.supplierRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with slug: " + slug));
        try {
            fileStorageService.deleteSupplierImage(supplier.getImagePath());
            this.supplierRepository.delete(supplier);
        }catch(Exception e) {
            throw new DatabaseOperationException("Failed to delete supplier. Ensure there are no records linked to this account.");
        }
    }
}
