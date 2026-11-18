package com.fitoherb.fitoherb_backend_v2.entities;

import com.fitoherb.fitoherb_backend_v2.enums.ErpMappingStatus;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "products")
@Entity(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "flavours")
    private List<String> flavours;

    @Column(name = "presentation")
    private List<String> presentation;

    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "curation_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'COMPLETE'")
    private ProductCurationStatus curationStatus = ProductCurationStatus.COMPLETE;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductSku> skus = new ArrayList<>();

    @Transient
    private ErpMappingStatus erpMappingStatus;

    public ErpMappingStatus getErpMappingStatus() {
        if (erpMappingStatus != null) {
            return erpMappingStatus;
        }
        if (skus == null || skus.isEmpty() || skus.stream().anyMatch(s -> s.getErpCode() == null)) {
            return ErpMappingStatus.UNMAPPED;
        }
        if (skus.stream().anyMatch(s -> Boolean.TRUE.equals(s.getErpInactive()))) {
            return ErpMappingStatus.INACTIVE_ALERT;
        }
        return ErpMappingStatus.COMPLETE;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

}
