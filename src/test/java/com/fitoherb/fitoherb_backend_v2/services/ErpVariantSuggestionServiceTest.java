package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SuggestVariantsRes;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErpVariantSuggestionServiceTest {

    private ErpVariantSuggestionService suggestionService;

    @BeforeEach
    void setup() {
        suggestionService = new ErpVariantSuggestionService();
    }

    @Test
    @DisplayName("P-407: Deve expandir abreviações CHOCO, MOR, BAUN e inferir topologia FLAVOUR_ONLY")
    void shouldExpandFlavourAbbreviationsAndInferTopology() {
        // Arrange
        List<ErpCatalogItemSnapshot> items = List.of(
                ErpCatalogItemSnapshot.builder().erpCode(131476L).description("100% PURE WHEY 900G BAUN PROBIOTICA").inativoPro(false).inExterno01(true).build(),
                ErpCatalogItemSnapshot.builder().erpCode(131477L).description("100% PURE WHEY 900G CHOCO PROBIOTICA").inativoPro(false).inExterno01(true).build(),
                ErpCatalogItemSnapshot.builder().erpCode(131478L).description("100% PURE WHEY 900G MOR PROBIOTICA").inativoPro(false).inExterno01(true).build()
        );

        // Act
        SuggestVariantsRes res = suggestionService.suggest(items, Map.of(), null);

        // Assert
        assertEquals(ProductTopology.FLAVOUR_ONLY, res.getSuggestedTopology());
        assertFalse(res.isHasInactiveItems());
        assertFalse(res.isHasConflictingItems());
        assertEquals("Baunilha", res.getItems().get(0).getSuggestedFlavour());
        assertEquals("Chocolate", res.getItems().get(1).getSuggestedFlavour());
        assertEquals("Morango", res.getItems().get(2).getSuggestedFlavour());
    }

    @Test
    @DisplayName("P-407 / Q-06: Deve expandir CAPSGEL, STAND UP, DISP., extrair gramaturas e emitir packageWarning em DISP./CX")
    void shouldExpandPackagingAbbreviationsAndEmitPackageWarning() {
        // Arrange
        List<ErpCatalogItemSnapshot> items = List.of(
                ErpCatalogItemSnapshot.builder().erpCode(20722L).description("ARREBITE ENERGIA CAPSGEL 60 CAPS GUARANAPIS").inativoPro(false).inExterno01(true).build(),
                ErpCatalogItemSnapshot.builder().erpCode(20723L).description("ARREBITE EXTRA FORTE STAND UP 120 CAPS").inativoPro(false).inExterno01(true).build(),
                ErpCatalogItemSnapshot.builder().erpCode(20724L).description("ARREBITE LIQUIDO DISP. 12X60ML").inativoPro(true).inExterno01(true).build()
        );

        // Act
        SuggestVariantsRes res = suggestionService.suggest(items, Map.of(20722L, "Arrebite Energia"), null);

        // Assert
        assertTrue(res.isHasInactiveItems());
        assertTrue(res.isHasConflictingItems());
        assertTrue(res.getItems().get(0).isAlreadyMappedElsewhere());
        assertEquals("Arrebite Energia", res.getItems().get(0).getConflictingProductName());
        assertNotNull(res.getItems().get(2).getPackageWarning());
        assertTrue(res.getItems().get(2).getPackageWarning().contains("Display/Caixa"));
    }

    @Test
    @DisplayName("P-407: Deve extrair volumes/gramaturas e inferir MATRIX quando houver sabor + peso")
    void shouldInferMatrixTopologyForFlavourAndWeight() {
        // Arrange
        List<ErpCatalogItemSnapshot> items = List.of(
                ErpCatalogItemSnapshot.builder().erpCode(810090L).description("BOPE PRE TREINO 150G LIMAO").inativoPro(false).inExterno01(true).build(),
                ErpCatalogItemSnapshot.builder().erpCode(810115L).description("BOPE PRE TREINO 300G LIMAO").inativoPro(false).inExterno01(true).build()
        );

        // Act
        SuggestVariantsRes res = suggestionService.suggest(items, Map.of(), null);

        // Assert
        assertEquals(ProductTopology.MATRIX, res.getSuggestedTopology());
        assertEquals("Limão", res.getItems().get(0).getSuggestedFlavour());
        assertEquals("150g", res.getItems().get(0).getSuggestedPresentation());
        assertEquals("300g", res.getItems().get(1).getSuggestedPresentation());
    }
}
