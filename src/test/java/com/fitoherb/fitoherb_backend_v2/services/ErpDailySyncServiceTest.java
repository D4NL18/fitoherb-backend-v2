package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.DailySyncReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.MailReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.DailySyncRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.entities.*;
import com.fitoherb.fitoherb_backend_v2.enums.DailySyncMode;
import com.fitoherb.fitoherb_backend_v2.enums.ProductCurationStatus;
import com.fitoherb.fitoherb_backend_v2.exceptions.MailSendingException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ServiceUnavailableException;
import com.fitoherb.fitoherb_backend_v2.exceptions.UnprocessableEntityException;
import com.fitoherb.fitoherb_backend_v2.infra.clients.AiCatalogRefreshClient;
import com.fitoherb.fitoherb_backend_v2.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpDailySyncServiceTest {

    @Mock private ErpCatalogGateway erpCatalogGateway;
    @Mock private ErpCatalogCacheService erpCatalogCacheService;
    @Mock private ProductRepository productRepository;
    @Mock private ProductSkuRepository productSkuRepository;
    @Mock private ProductCategoryRepository categoryRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ErpSyncStateRepository syncStateRepository;
    @Mock private ErpSyncRunRepository syncRunRepository;
    @Mock private ErpIgnoredCatalogCodeRepository ignoredCatalogCodeRepository;
    @Mock private AiCatalogRefreshClient aiCatalogRefreshClient;
    @Mock private MailService mailService;
    @Spy private ProductVariantSanitizer variantSanitizer = new ProductVariantSanitizer();
    @Spy private ProductVisibilityCalculator visibilityCalculator = new ProductVisibilityCalculator(new ProductVariantSanitizer());
    @Spy private ErpVariantSuggestionService suggestionService = new ErpVariantSuggestionService();

    @InjectMocks
    private ErpDailySyncService dailySyncService;

    private ProductCategory defaultCategory;
    private Supplier defaultSupplier;
    private ErpSyncState syncState;

    @BeforeEach
    void setup() {
        defaultCategory = new ProductCategory();
        defaultCategory.setId("cat-fallback");
        defaultCategory.setName("Outros");
        defaultCategory.setSlug("outros");

        defaultSupplier = new Supplier();
        defaultSupplier.setId("sup-fallback");
        defaultSupplier.setName("Fitoherb");
        defaultSupplier.setSlug("fitoherb");

        syncState = new ErpSyncState();
        syncState.setId((short) 1);
        syncState.setAutoSyncEnabled(true);
        syncState.setBaselineCutoffAt(LocalDateTime.now().minusDays(7));
        syncState.setInProgress(false);

        lenient().when(syncStateRepository.tryAcquireLock(anyString(), any(), any())).thenReturn(1);
        lenient().when(syncStateRepository.findById((short) 1)).thenReturn(Optional.of(syncState));
        lenient().when(syncRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(ignoredCatalogCodeRepository.findAllErpCodes()).thenReturn(Collections.emptySet());
        lenient().when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.of(defaultCategory));
        lenient().when(supplierRepository.findBySlug(anyString())).thenReturn(Optional.of(defaultSupplier));
        lenient().when(aiCatalogRefreshClient.refreshCatalog()).thenReturn(true);
    }

    @Nested
    @DisplayName("P-411: Visibilidade Automática, Inativação Parcial Multi-SKU e Reativação")
    class VisibilityAndReactivationTests {

        @Test
        @DisplayName("C-17: Deve ocultar produto (active = false) quando 100% dos SKUs ficarem inativos, preservando foto e SKUs")
        void shouldHideProductWhenAllSkusBecomeInactive() {
            // Arrange
            Product product = new Product();
            product.setId("prod-1");
            product.setSlug("colageno-250g");
            product.setName("Colágeno 250g");
            product.setImagePath("colageno.webp");
            product.setActive(true);
            product.setCurationStatus(ProductCurationStatus.COMPLETE);

            ProductSku sku = new ProductSku();
            sku.setId("sku-1");
            sku.setProduct(product);
            sku.setErpCode(770201L);
            sku.setErpInactive(false);
            product.setSkus(new ArrayList<>(List.of(sku)));

            ErpCatalogItemSnapshot erpInactive = ErpCatalogItemSnapshot.builder()
                    .erpCode(770201L).description("COLAGENO 250G").inativoPro(true).inExterno01(true).build();

            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(erpInactive));
            when(productRepository.findAllWithSkus()).thenReturn(List.of(product));

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", false
            );

            // Assert
            assertFalse(product.getActive());
            assertTrue(sku.getErpInactive());
            assertEquals("colageno.webp", product.getImagePath());
            assertEquals(1, product.getSkus().size());
            assertEquals(1, res.getDeactivatedProducts());
            assertTrue(res.isAiCatalogRefreshed());
            verify(aiCatalogRefreshClient).refreshCatalog();
        }

        @Test
        @DisplayName("C-18: Deve inativar parcialmente sabor no Multi-SKU (erpInactive=true) removendo da projeção pública sem apagar SKU")
        void shouldPartiallyDeactivateFlavourWithoutDeletingSku() {
            // Arrange
            Product whey = new Product();
            whey.setId("prod-whey");
            whey.setSlug("whey-900g");
            whey.setName("100% Pure Whey 900g");
            whey.setActive(true);
            whey.setCurationStatus(ProductCurationStatus.COMPLETE);

            ProductSku baunilha = new ProductSku();
            baunilha.setId("sku-b");
            baunilha.setProduct(whey);
            baunilha.setErpCode(131476L);
            baunilha.setFlavour("Baunilha");
            baunilha.setErpInactive(false);
            baunilha.setSortOrder(0);

            ProductSku chocolate = new ProductSku();
            chocolate.setId("sku-c");
            chocolate.setProduct(whey);
            chocolate.setErpCode(131477L);
            chocolate.setFlavour("Chocolate");
            chocolate.setErpInactive(false);
            chocolate.setSortOrder(1);

            ProductSku morango = new ProductSku();
            morango.setId("sku-m");
            morango.setProduct(whey);
            morango.setErpCode(131478L);
            morango.setFlavour("Morango");
            morango.setErpInactive(false);
            morango.setSortOrder(2);

            whey.setSkus(new ArrayList<>(List.of(baunilha, chocolate, morango)));

            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(
                    ErpCatalogItemSnapshot.builder().erpCode(131476L).description("WHEY BAUNILHA").inativoPro(false).inExterno01(true).build(),
                    ErpCatalogItemSnapshot.builder().erpCode(131477L).description("WHEY CHOCO").inativoPro(true).inExterno01(true).build(),
                    ErpCatalogItemSnapshot.builder().erpCode(131478L).description("WHEY MORANGO").inativoPro(false).inExterno01(true).build()
            ));
            when(productRepository.findAllWithSkus()).thenReturn(List.of(whey));

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "SCHEDULED_INTERNAL", "SYSTEM_SCHEDULED", false
            );

            // Assert
            assertTrue(whey.getActive());
            assertTrue(chocolate.getErpInactive());
            assertEquals(3, whey.getSkus().size(), "SKU inativo não deve ser apagado");
            assertEquals(List.of("Baunilha", "Morango"), whey.getFlavours(), "Projeção pública deve excluir sabor inativo");
            assertEquals(1, res.getPartiallyDeactivatedProducts());
            verify(aiCatalogRefreshClient).refreshCatalog();
        }

        @Test
        @DisplayName("C-17: Deve reativar imediatamente produto e sabor quando inativo_pro voltar para 0")
        void shouldReactivateProductAndFlavourWhenErpReturnsActive() {
            // Arrange
            Product product = new Product();
            product.setId("prod-1");
            product.setSlug("colageno-250g");
            product.setName("Colágeno 250g");
            product.setActive(false);
            product.setCurationStatus(ProductCurationStatus.COMPLETE);
            product.setImagePath("colageno.webp");

            ProductSku sku = new ProductSku();
            sku.setId("sku-1");
            sku.setProduct(product);
            sku.setErpCode(770201L);
            sku.setFlavour("Neutro");
            sku.setErpInactive(true);
            sku.setSortOrder(0);
            product.setSkus(new ArrayList<>(List.of(sku)));

            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(
                    ErpCatalogItemSnapshot.builder().erpCode(770201L).description("COLAGENO 250G NEUTRO").inativoPro(false).inExterno01(true).build()
            ));
            when(productRepository.findAllWithSkus()).thenReturn(List.of(product));

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", false
            );

            // Assert
            assertTrue(product.getActive());
            assertFalse(sku.getErpInactive());
            assertEquals(List.of("Neutro"), product.getFlavours());
            assertEquals(1, res.getReactivatedProducts());
        }
    }

    @Nested
    @DisplayName("P-410 & P-412 & C-23: Criação de Novos Produtos Comerciais, Ignorados, Lock e Fail-Closed")
    class CreationIgnoredLockFailClosedTests {

        @Test
        @DisplayName("C-19 & P-412: Deve criar novo produto comercial ativo com imagePath=null, curationStatus=PENDING_IMAGE e active=true")
        void shouldCreateNewCommercialProductAsPendingImageAndActive() {
            // Arrange
            ErpCatalogItemSnapshot newCommercial = ErpCatalogItemSnapshot.builder()
                    .erpCode(991050L)
                    .description("CREATINA TURBO 300G")
                    .ean("7890009910501")
                    .brandName("BLACK SKULL")
                    .categoryLabel("CREATINAS")
                    .inativoPro(false)
                    .inExterno01(true)
                    .build();

            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(newCommercial));
            when(productRepository.findAllWithSkus()).thenReturn(Collections.emptyList());
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", false
            );

            // Assert
            ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(savedCaptor.capture());
            Product created = savedCaptor.getValue();

            assertNull(created.getImagePath());
            assertTrue(created.getActive());
            assertEquals(ProductCurationStatus.PENDING_IMAGE, created.getCurationStatus());
            assertEquals(1, created.getSkus().size());
            assertEquals(991050L, created.getSkus().get(0).getErpCode());
            assertEquals(1, res.getCreatedPendingProducts());
        }

        @Test
        @DisplayName("C-22 & P-413: Deve respeitar erp_ignored_catalog_codes e não recriar nem reativar código ignorado")
        void shouldSkipIgnoredCatalogCodes() {
            // Arrange
            ErpCatalogItemSnapshot ignoredItem = ErpCatalogItemSnapshot.builder()
                    .erpCode(990999L).description("SQUEEZE EVENTO").inativoPro(false).inExterno01(true).build();

            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(ignoredItem));
            when(productRepository.findAllWithSkus()).thenReturn(Collections.emptyList());
            when(ignoredCatalogCodeRepository.findAllErpCodes()).thenReturn(Set.of(990999L));

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", false
            );

            // Assert
            verify(productRepository, never()).save(any());
            assertEquals(0, res.getCreatedPendingProducts());
            assertEquals(1, res.getIgnoredCodesSkipped());
        }

        @Test
        @DisplayName("C-20 & P-410: Fail-closed quando ERP offline preserva produtos ativos e grava ErpSyncRun com erro sanitizado")
        void shouldFailClosedPreservingActiveProductsWhenErpUnavailable() {
            // Arrange
            when(erpCatalogGateway.fetchFullCatalog())
                    .thenThrow(new ServiceUnavailableException("ERP_CATALOG_UNAVAILABLE", "SQL Server Fitoherb offline"));

            // Act & Assert
            ServiceUnavailableException ex = assertThrows(
                    ServiceUnavailableException.class,
                    () -> dailySyncService.executeSync(
                            new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", false
                    )
            );
            assertEquals("ERP_CATALOG_UNAVAILABLE", ex.getCode());
            verify(productRepository, never()).save(any());
            verify(syncStateRepository).releaseLock(any());
            verify(syncRunRepository, atLeastOnce()).save(argThat(run ->
                    "FAILED".equals(run.getStatus()) && run.getErrorSummary() != null && !run.getErrorSummary().contains("jdbc:")
            ));
        }

        @Test
        @DisplayName("P-410: Deve detectar lock concorrente e retornar ALREADY_RUNNING ou lançar conflito conforme ifRunning")
        void shouldHandleConcurrentSyncLock() {
            // Arrange
            when(syncStateRepository.tryAcquireLock(anyString(), any(), any())).thenReturn(0);
            syncState.setInProgress(true);
            syncState.setActiveRunId("run-123");

            // Act & Assert (ifRunning = ACCEPT)
            DailySyncRes accepted = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", true
            );
            assertEquals("ALREADY_RUNNING", accepted.getStatus());
            assertEquals("run-123", accepted.getRunId());
            verifyNoInteractions(erpCatalogGateway);

            // Act & Assert (ifRunning = FAIL)
            assertThrows(
                    com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException.class,
                    () -> dailySyncService.executeSync(
                            new DailySyncReq(DailySyncMode.INCREMENTAL, false), "CLOUD_SCHEDULER", "CLOUD_SCHEDULER", false
                    )
            );
        }

        @Test
        @DisplayName("P-410: Deve acionar expurgo de runs > 90 dias e não abortar sync se AiCatalogRefreshClient falhar")
        void shouldPurgeOldSyncRunsAndSurviveAiRefreshFailure() {
            // Arrange
            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(Collections.emptyList());
            when(productRepository.findAllWithSkus()).thenReturn(Collections.emptyList());
            when(aiCatalogRefreshClient.refreshCatalog()).thenReturn(false);

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.STATUS_ONLY, false), "MANUAL_ADMIN", "admin@fitoherb.com", false
            );

            // Assert
            assertEquals("COMPLETED", res.getStatus());
            assertFalse(res.isAiCatalogRefreshed());
            verify(syncRunRepository).deleteOldRunsBefore(any(LocalDateTime.class));
            verify(syncStateRepository).releaseLock(any());
        }
    }

    @Nested
    @DisplayName("P-410: Agendamento Diário às 08:00h e Relatório por E-mail Parametrizado (Best-Effort)")
    class ScheduledAndEmailReportTests {

        @Test
        @DisplayName("Deve possuir método runScheduledDailySync anotado com @Scheduled às 08:00h no fuso America/Sao_Paulo")
        void shouldConfigureScheduledDailySyncAtEightAm() throws NoSuchMethodException {
            // Arrange & Act
            Method scheduledMethod = ErpDailySyncService.class.getDeclaredMethod("runScheduledDailySync");
            Scheduled scheduledAnnotation = scheduledMethod.getAnnotation(Scheduled.class);

            // Assert
            assertNotNull(scheduledAnnotation, "Método runScheduledDailySync deve possuir anotação @Scheduled");
            assertTrue(
                    scheduledAnnotation.cron().contains("0 0 8 * * *"),
                    "Expressão cron deve ter fallback para '0 0 8 * * *' (08:00h)"
            );
            assertEquals("America/Sao_Paulo", scheduledAnnotation.zone());
        }

        @Test
        @DisplayName("Deve enviar relatório por e-mail quando reportEmail estiver configurado com resumo e itens PENDING_IMAGE")
        void shouldSendEmailReportWhenReportEmailIsConfigured() {
            // Arrange
            ReflectionTestUtils.setField(dailySyncService, "reportEmail", "relatorio@exemplo.com");

            ErpCatalogItemSnapshot newCommercial = ErpCatalogItemSnapshot.builder()
                    .erpCode(991050L)
                    .description("CREATINA TURBO 300G")
                    .ean("7890009910501")
                    .brandName("BLACK SKULL")
                    .categoryLabel("CREATINAS")
                    .inativoPro(false)
                    .inExterno01(true)
                    .build();

            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(List.of(newCommercial));
            when(productRepository.findAllWithSkus()).thenReturn(Collections.emptyList());
            when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "SCHEDULED_INTERNAL", "SYSTEM_SCHEDULED", false
            );

            // Assert
            assertEquals("COMPLETED", res.getStatus());
            ArgumentCaptor<MailReq> mailCaptor = ArgumentCaptor.forClass(MailReq.class);
            verify(mailService, times(1)).sendEmail(mailCaptor.capture());

            MailReq sentMail = mailCaptor.getValue();
            assertEquals("relatorio@exemplo.com", sentMail.getEmail());
            assertNotNull(sentMail.getSubject());
            assertTrue(
                    sentMail.getSubject().contains("Sincronização") || sentMail.getSubject().contains("Relatório"),
                    "O assunto deve conter 'Sincronização' ou 'Relatório'"
            );
            assertNotNull(sentMail.getMessage());
            assertTrue(
                    sentMail.getMessage().contains("991050") || sentMail.getMessage().contains("CREATINA TURBO 300G") || sentMail.getMessage().contains("PENDING_IMAGE"),
                    "O corpo do e-mail deve conter resumo das contagens e produtos pendentes de imagem"
            );
        }

        @Test
        @DisplayName("Não deve enviar relatório por e-mail quando reportEmail estiver vazio ou em branco")
        void shouldNotSendEmailReportWhenReportEmailIsBlank() {
            // Arrange
            ReflectionTestUtils.setField(dailySyncService, "reportEmail", "   ");
            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(Collections.emptyList());
            when(productRepository.findAllWithSkus()).thenReturn(Collections.emptyList());

            // Act
            DailySyncRes res = dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "SCHEDULED_INTERNAL", "SYSTEM_SCHEDULED", false
            );

            // Assert
            assertEquals("COMPLETED", res.getStatus());
            verify(mailService, never()).sendEmail(any(MailReq.class));
        }

        @Test
        @DisplayName("Não deve falhar nem fazer rollback da sincronização se MailService lançar MailSendingException")
        void shouldNotFailSyncWhenMailServiceThrowsException() {
            // Arrange
            ReflectionTestUtils.setField(dailySyncService, "reportEmail", "relatorio@exemplo.com");
            when(erpCatalogGateway.fetchFullCatalog()).thenReturn(Collections.emptyList());
            when(productRepository.findAllWithSkus()).thenReturn(Collections.emptyList());
            doThrow(new MailSendingException("Erro SMTP simulado", new RuntimeException("Timeout")))
                    .when(mailService).sendEmail(any(MailReq.class));

            // Act
            DailySyncRes res = assertDoesNotThrow(() -> dailySyncService.executeSync(
                    new DailySyncReq(DailySyncMode.INCREMENTAL, false), "SCHEDULED_INTERNAL", "SYSTEM_SCHEDULED", false
            ));

            // Assert
            assertEquals("COMPLETED", res.getStatus());
            verify(mailService, times(1)).sendEmail(any(MailReq.class));
            verify(syncStateRepository).releaseLock(any());
        }
    }
}
