package com.fitoherb.fitoherb_backend_v2.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Table(name = "product_skus")
@Entity(name = "product_skus")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "erp_code", nullable = false, unique = true)
    private Long erpCode;

    @Column(name = "flavour", length = 100)
    private String flavour;

    @Column(name = "presentation", length = 100)
    private String presentation;

    @Builder.Default
    @Column(name = "normalized_variant_key", nullable = false, length = 220)
    private String normalizedVariantKey = "__SIMPLE__";

    @Column(name = "ean", length = 20)
    private String ean;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "erp_inactive", nullable = false)
    private Boolean erpInactive = false;

    @Builder.Default
    @Column(name = "erp_inactive_confirmed", nullable = false)
    private Boolean erpInactiveConfirmed = false;

    @Column(name = "erp_last_verified_at")
    private LocalDateTime erpLastVerifiedAt;

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
