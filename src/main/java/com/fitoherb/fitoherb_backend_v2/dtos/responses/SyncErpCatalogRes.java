package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resumo da atualização do snapshot ERP em memória RAM")
public class SyncErpCatalogRes {

    private int processedCatalogItems;
    private int activeCatalogItems;
    private int inactiveCatalogItems;
    private int checkedSiteSkus;
    private int newlyInactivatedMappedProducts;
}
