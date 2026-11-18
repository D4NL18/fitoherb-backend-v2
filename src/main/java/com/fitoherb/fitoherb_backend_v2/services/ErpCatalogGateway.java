package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;

import java.util.List;

public interface ErpCatalogGateway {

    List<ErpCatalogItemSnapshot> fetchFullCatalog();
}
