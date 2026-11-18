package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.entities.ProductSku;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_VARIANT_INVALID;

@Component
public class ProductVariantSanitizer {

    public List<String> sanitizeList(List<String> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> deduplicated = new LinkedHashMap<>();
        for (String rawItem : rawList) {
            if (rawItem == null || rawItem.isBlank()) {
                continue;
            }
            String[] tokens = rawItem.split("(?i)\\s+e\\s+|\\s*/\\s*|,");
            for (String token : tokens) {
                String cleaned = cleanText(token);
                if (!cleaned.isEmpty() && !isPunctuationOnly(cleaned)) {
                    String normKey = normalizeForComparison(cleaned);
                    deduplicated.putIfAbsent(normKey, cleaned);
                }
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    public String sanitizeSingle(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException(MSG_VARIANT_INVALID);
        }
        String cleaned = cleanText(raw);
        if (cleaned.isEmpty() || isPunctuationOnly(cleaned)) {
            throw new IllegalArgumentException(MSG_VARIANT_INVALID);
        }
        return cleaned;
    }

    public String sanitizeSingleOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        return sanitizeSingle(raw);
    }

    public String buildNormalizedVariantKey(String flavour, String presentation) {
        String cleanFlavour = flavour != null && !flavour.isBlank() ? normalizeForComparison(flavour) : "";
        String cleanPres = presentation != null && !presentation.isBlank() ? normalizeForComparison(presentation) : "";
        if (cleanFlavour.isEmpty() && cleanPres.isEmpty()) {
            return "__SIMPLE__";
        }
        return cleanFlavour + "::" + cleanPres;
    }

    public List<String> projectActiveFlavours(List<ProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductSku> sorted = skus.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getErpInactive()))
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .toList();

        Map<String, String> distinct = new LinkedHashMap<>();
        for (ProductSku sku : sorted) {
            if (sku.getFlavour() != null && !sku.getFlavour().isBlank()) {
                String clean = cleanText(sku.getFlavour());
                if (!clean.isEmpty()) {
                    distinct.putIfAbsent(normalizeForComparison(clean), clean);
                }
            }
        }
        return new ArrayList<>(distinct.values());
    }

    public List<String> projectActivePresentations(List<ProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductSku> sorted = skus.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getErpInactive()))
                .sorted(Comparator.comparingInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .toList();

        Map<String, String> distinct = new LinkedHashMap<>();
        for (ProductSku sku : sorted) {
            if (sku.getPresentation() != null && !sku.getPresentation().isBlank()) {
                String clean = cleanText(sku.getPresentation());
                if (!clean.isEmpty()) {
                    distinct.putIfAbsent(normalizeForComparison(clean), clean);
                }
            }
        }
        return new ArrayList<>(distinct.values());
    }

    private String cleanText(String input) {
        if (input == null) {
            return "";
        }
        String collapsed = input.trim().replaceAll("\\s+", " ");
        while (collapsed.endsWith(".")) {
            collapsed = collapsed.substring(0, collapsed.length() - 1).trim();
        }
        return collapsed;
    }

    private boolean isPunctuationOnly(String input) {
        return input.replaceAll("[\\p{Punct}\\s]+", "").isEmpty();
    }

    public String normalizeForComparison(String input) {
        if (input == null) {
            return "";
        }
        String cleaned = cleanText(input).toLowerCase(Locale.ROOT);
        String deaccented = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return deaccented.trim();
    }
}
