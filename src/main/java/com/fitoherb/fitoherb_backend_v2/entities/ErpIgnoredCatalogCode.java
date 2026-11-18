package com.fitoherb.fitoherb_backend_v2.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "erp_ignored_catalog_codes")
@Entity(name = "erp_ignored_catalog_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "erpCode")
public class ErpIgnoredCatalogCode {

    @Id
    @Column(name = "erp_code")
    private Long erpCode;

    @Builder.Default
    @Column(name = "source", nullable = false, length = 30)
    private String source = "ADMIN_IGNORED";

    @Column(name = "product_name_snapshot", length = 255)
    private String productNameSnapshot;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "ignored_by", nullable = false, length = 150)
    private String ignoredBy;

    @Builder.Default
    @Column(name = "ignored_at", nullable = false)
    private LocalDateTime ignoredAt = LocalDateTime.now();
}
