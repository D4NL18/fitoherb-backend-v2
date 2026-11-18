package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCatalogItemSnapshot {

    private Long erpCode;
    private String description;
    private String ean;
    private String brandName;
    private String supplierName;
    private String categoryLabel;
    private boolean inativoPro;
    private boolean inExterno01;
}
