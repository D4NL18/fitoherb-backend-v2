package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.responses.ErpCatalogItemSnapshot;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SuggestVariantsRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SuggestedVariantItemRes;
import com.fitoherb.fitoherb_backend_v2.enums.ProductTopology;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ErpVariantSuggestionService {

    private static final String PACKAGE_WARNING_MESSAGE =
            "Embalagem coletiva detectada no ERP (Display/Caixa). Confirme se o produto do site refere-se à unidade ou ao display.";

    private static final Pattern COLLECTIVE_PACK_PATTERN =
            Pattern.compile("(?i)(?:\\bDISP\\.?|\\bDISPLAY\\b|\\bCX\\b|\\bCAIXA\\b|\\b\\d+\\s*X\\s*\\d+)");

    private static final Pattern WEIGHT_VOLUME_PATTERN =
            Pattern.compile("(?i)\\b(\\d+(?:[.,]\\d+)?\\s*(?:KG|G|GR|MG|ML|L|CAPS|CAP|CP|UN|SACHES|TABLETES))\\b");

    private static final Map<Pattern, String> FLAVOUR_PATTERNS = new LinkedHashMap<>();

    static {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ;
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:BAUNILHA|BAUN\\.?)\\b", flags), "Baunilha");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:CHOCOLATE\\s+BRANCO|CHOC\\.?\\s+BRANCO)\\b", flags), "Chocolate Branco");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:CHOCOLATE|CHOCO|CHOC\\.?)\\b", flags), "Chocolate");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:MORANGO|MOR\\.?)\\b", flags), "Morango");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:LIMAO|LIMÃO|LIM\\.?)\\b", flags), "Limão");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:MELANCIA|MELANC\\.?)\\b", flags), "Melancia");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:FRUIT\\s+PUNCH|FRUTAS\\s+VERMELHAS)\\b", flags), "Fruit Punch");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:COOKIES(?:\\s+AND\\s+CREAM)?|COOK\\.?)\\b", flags), "Cookies");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:LARANJA|LAR\\.?)\\b", flags), "Laranja");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\bUVA\\b", flags), "Uva");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:ABACAXI|ABAC\\.?)\\b", flags), "Abacaxi");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:MARACUJA|MARACUJÁ)\\b", flags), "Maracujá");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:TANGERINA|TANG\\.?)\\b", flags), "Tangerina");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:MACA\\s+VERDE|MAÇÃ\\s+VERDE)\\b", flags), "Maçã Verde");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\bBANANA\\b", flags), "Banana");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\bCOCO\\b", flags), "Coco");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:AMENDOIM|PAÇOCA|PACOCA)\\b", flags), "Amendoim");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\bDOCE\\s+DE\\s+LEITE\\b", flags), "Doce de Leite");
        FLAVOUR_PATTERNS.put(Pattern.compile("\\b(?:NEUTRO|NATURAL|SEM\\s+SABOR)\\b", flags), "Neutro");
    }

    public SuggestVariantsRes suggest(
            List<ErpCatalogItemSnapshot> items,
            Map<Long, String> conflictingOwnersByErpCode,
            String excludeProductSlug
    ) {
        if (items == null || items.isEmpty()) {
            return SuggestVariantsRes.builder()
                    .suggestedTopology(ProductTopology.SIMPLE)
                    .hasInactiveItems(false)
                    .hasConflictingItems(false)
                    .items(Collections.emptyList())
                    .build();
        }

        Map<Long, String> conflicts = conflictingOwnersByErpCode != null
                ? conflictingOwnersByErpCode
                : Collections.emptyMap();

        List<SuggestedVariantItemRes> suggestedItems = new ArrayList<>();
        Set<String> distinctFlavours = new LinkedHashSet<>();
        Set<String> distinctPresentations = new LinkedHashSet<>();
        boolean hasInactive = false;
        boolean hasConflict = false;

        for (ErpCatalogItemSnapshot item : items) {
            String desc = item.getDescription() != null ? item.getDescription().trim() : "";
            String flavour = extractFlavour(desc);
            String presentation = extractPresentation(desc);
            String warning = COLLECTIVE_PACK_PATTERN.matcher(desc).find() ? PACKAGE_WARNING_MESSAGE : null;

            String conflictingOwner = conflicts.get(item.getErpCode());
            boolean alreadyMapped = conflictingOwner != null && !conflictingOwner.isBlank();

            if (item.isInativoPro()) {
                hasInactive = true;
            }
            if (alreadyMapped) {
                hasConflict = true;
            }
            if (flavour != null) {
                distinctFlavours.add(flavour.toLowerCase(Locale.ROOT));
            }
            if (presentation != null) {
                distinctPresentations.add(presentation.toLowerCase(Locale.ROOT));
            }

            suggestedItems.add(SuggestedVariantItemRes.builder()
                    .erpCode(item.getErpCode())
                    .erpDescription(desc)
                    .suggestedFlavour(flavour)
                    .suggestedPresentation(presentation)
                    .inativoPro(item.isInativoPro())
                    .inExterno01(item.isInExterno01())
                    .alreadyMappedElsewhere(alreadyMapped)
                    .conflictingProductName(alreadyMapped ? conflictingOwner : null)
                    .packageWarning(warning)
                    .build());
        }

        // Quando todos os SKUs têm exatamente o mesmo peso/volume e variam apenas em sabor (ex.: Whey 900g Baunilha/Choco/Morango),
        // o peso faz parte do produto base e a topologia é FLAVOUR_ONLY.
        if (distinctPresentations.size() == 1 && distinctFlavours.size() > 1) {
            for (SuggestedVariantItemRes item : suggestedItems) {
                item.setSuggestedPresentation(null);
            }
            distinctPresentations.clear();
        }

        ProductTopology topology = inferGroupTopology(suggestedItems, distinctFlavours, distinctPresentations);

        return SuggestVariantsRes.builder()
                .suggestedTopology(topology)
                .hasInactiveItems(hasInactive)
                .hasConflictingItems(hasConflict)
                .items(suggestedItems)
                .build();
    }

    public String extractFlavour(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        for (Map.Entry<Pattern, String> entry : FLAVOUR_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(description).find()) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String extractPresentation(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        Matcher matcher = WEIGHT_VOLUME_PATTERN.matcher(description);
        if (matcher.find()) {
            String raw = matcher.group(1).trim().replaceAll("\\s+", "");
            String upper = raw.toUpperCase(Locale.ROOT);
            if (upper.endsWith("CAPS") || upper.endsWith("CAP") || upper.endsWith("CP")) {
                String digits = upper.replaceAll("\\D", "");
                return digits + " Cápsulas";
            }
            if (upper.endsWith("GR")) {
                return raw.substring(0, raw.length() - 2).toLowerCase(Locale.ROOT) + "g";
            }
            return raw.toLowerCase(Locale.ROOT);
        }
        if (description.toUpperCase(Locale.ROOT).contains("CAPSGEL")) {
            return "Cápsulas";
        }
        if (description.toUpperCase(Locale.ROOT).contains("STAND UP")) {
            return "Stand Up / Refil";
        }
        return null;
    }

    public String formatCommercialName(String erpDescription) {
        if (erpDescription == null || erpDescription.isBlank()) {
            return "Produto ERP";
        }
        String expanded = erpDescription.trim()
                .replaceAll("(?i)\\bCAPSGEL\\b", "Cápsulas")
                .replaceAll("(?i)\\bCHOCO\\b", "Chocolate")
                .replaceAll("(?i)\\bBAUN\\b", "Baunilha")
                .replaceAll("(?i)\\bDISP\\.?\\b", "Display")
                .replaceAll("\\s+", " ");

        String[] words = expanded.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            if (!word.isEmpty() && Character.isDigit(word.charAt(0))) {
                sb.append(word.toLowerCase(Locale.ROOT));
            } else if (word.length() <= 2) {
                sb.append(word.toLowerCase(Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString().trim();
    }

    private ProductTopology inferGroupTopology(
            List<SuggestedVariantItemRes> items,
            Set<String> distinctFlavours,
            Set<String> distinctPresentations
    ) {
        boolean anyFlavour = !distinctFlavours.isEmpty();
        boolean anyPresentation = !distinctPresentations.isEmpty();

        if (anyFlavour && anyPresentation) {
            return ProductTopology.MATRIX;
        }
        if (anyFlavour) {
            return ProductTopology.FLAVOUR_ONLY;
        }
        if (anyPresentation) {
            return ProductTopology.PRESENTATION_ONLY;
        }
        return ProductTopology.SIMPLE;
    }
}
