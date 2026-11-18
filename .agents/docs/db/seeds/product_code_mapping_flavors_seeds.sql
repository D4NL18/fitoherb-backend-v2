-- ============================================================================
-- Seed SQL Realista e Idempotente: Mapeamento ERP por SKU (P-400 a P-409)
-- Localização Mandatória: .agents/docs/db/seeds/product_code_mapping_flavors_seeds.sql
-- Conformidade LGPD: 100% dados comerciais de catálogo e operadores fictícios
-- Ordem de Execução: Pais (categories, suppliers, aliases, erp_catalog_items, products)
--                    -> Filhos (product_skus, human_review_queue, erp_mapping_audit)
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. Categorias de Produto (product_categories)
-- ----------------------------------------------------------------------------
INSERT INTO public.product_categories (id, name, slug, image_path, created_at, created_by)
VALUES
    ('11111111-1111-4111-8111-000000000001', 'Suplementos Esportivos', 'suplementos-esportivos', NULL, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('11111111-1111-4111-8111-000000000002', 'Produtos Naturais', 'produtos-naturais', NULL, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('11111111-1111-4111-8111-000000000003', 'Chás e Infusões', 'chas-e-infusoes', NULL, NOW(), 'catalogo.admin@fitoherb-local.test')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Fornecedores / Marcas do Site (suppliers)
-- ----------------------------------------------------------------------------
INSERT INTO public.suppliers (id, name, slug, image_path, is_highlighted, created_at, created_by)
VALUES
    ('22222222-2222-4222-8222-000000000001', 'Max Titanium', 'max-titanium', NULL, TRUE, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('22222222-2222-4222-8222-000000000002', 'Black Skull', 'black-skull', NULL, TRUE, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('22222222-2222-4222-8222-000000000003', 'Stevita', 'stevita', NULL, FALSE, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('22222222-2222-4222-8222-000000000004', 'Gengibre Real', 'gengibre-real', NULL, FALSE, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('22222222-2222-4222-8222-000000000005', 'Arrebite', 'arrebite', NULL, FALSE, NOW(), 'catalogo.admin@fitoherb-local.test'),
    ('22222222-2222-4222-8222-000000000006', 'Fitoherb Natural', 'fitoherb-natural', NULL, TRUE, NOW(), 'catalogo.admin@fitoherb-local.test')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Aliases de Marca Site <-> ERP Atrio (supplier_brand_aliases)
-- ----------------------------------------------------------------------------
INSERT INTO public.supplier_brand_aliases (
    id, supplier_id, erp_brand_name, normalized_erp_brand, confidence, notes, created_at, created_by
)
SELECT
    '33333333-3333-4333-8333-000000000001',
    s.id,
    'GUARANAPIS',
    'guaranapis',
    'CONFIRMED',
    'Fornecedor Arrebite no site cadastrado sob a marca GUARANAPIS no ERP Atrio.',
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.suppliers s
WHERE s.slug = 'arrebite'
ON CONFLICT (supplier_id, normalized_erp_brand) DO NOTHING;

INSERT INTO public.supplier_brand_aliases (
    id, supplier_id, erp_brand_name, normalized_erp_brand, confidence, notes, created_at, created_by
)
SELECT
    '33333333-3333-4333-8333-000000000002',
    s.id,
    'BLACKSKULL',
    'blackskull',
    'CONFIRMED',
    'Variação ortográfica sem espaço utilizada em descrições antigas do ERP.',
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.suppliers s
WHERE s.slug = 'black-skull'
ON CONFLICT (supplier_id, normalized_erp_brand) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. Catálogo Espelhado ERP Atrio (erp_catalog_items)
--    (Inclui ativos, inativos em Externo01.Produto e não mapeados para busca US-P4)
-- ----------------------------------------------------------------------------
INSERT INTO public.erp_catalog_items (
    erp_code, empresa_id, description, ean, brand_name, supplier_name, category_label, inativo, in_externo_view, last_synced_at
)
VALUES
    -- 100% Pure Whey 900g (Max Titanium)
    (131476, 1, '100% PURE WHEY 900G BAUNILHA POTE', '7896311770011', 'MAX TITANIUM', NULL, 'PROTEINAS', FALSE, TRUE, NOW()),
    (131477, 1, '100% PURE WHEY 900G CHOCO POTE', '7896311770028', 'MAX TITANIUM', NULL, 'PROTEINAS', FALSE, TRUE, NOW()),
    (131478, 1, '100% PURE WHEY 900G MORANGO POTE', '7896311770035', 'MAX TITANIUM', NULL, 'PROTEINAS', FALSE, TRUE, NOW()),

    -- B.O.P.E 150g e 300g (Black Skull - Matriz 2x3)
    (810090, 1, 'BOPE PRE TREINO 150G LIMAO', '7898600900010', 'BLACK SKULL', NULL, 'PRE-TREINO', FALSE, TRUE, NOW()),
    (810091, 1, 'BOPE PRE TREINO 150G FRUTAS VERMELHAS', '7898600900027', 'BLACK SKULL', NULL, 'PRE-TREINO', FALSE, TRUE, NOW()),
    (810092, 1, 'BOPE PRE TREINO 150G MELANCIA', '7898600900034', 'BLACK SKULL', NULL, 'PRE-TREINO', FALSE, TRUE, NOW()),
    (810115, 1, 'BOPE PRE TREINO 300G LIMAO', '7898601150010', 'BLACK SKULL', NULL, 'PRE-TREINO', FALSE, TRUE, NOW()),
    (810116, 1, 'BOPE PRE TREINO 300G FRUTAS VERMELHAS', '7898601150027', 'BLACK SKULL', NULL, 'PRE-TREINO', FALSE, TRUE, NOW()),
    (810117, 1, 'BOPE PRE TREINO 300G MELANCIA', '7898601150034', 'BLACK SKULL', NULL, 'PRE-TREINO', FALSE, TRUE, NOW()),

    -- Stevita Líquido (Somente Apresentação 25ml / 60ml)
    (580000, 1, 'ADOCANTE STEVITA LIQUIDO 25ML', '7896182000101', 'STEVITA', NULL, 'ADOCANTES', FALSE, TRUE, NOW()),
    (580001, 1, 'ADOCANTE STEVITA LIQUIDO 60ML', '7896182000200', 'STEVITA', NULL, 'ADOCANTES', FALSE, TRUE, NOW()),

    -- Cristais de Gengibre (31106 a 31111)
    (31106, 1, 'CRISTAIS DE GENGIBRE TRADICIONAL 40G', '7898111031106', 'GENGIBRE REAL', NULL, 'NATURAIS', FALSE, TRUE, NOW()),
    (31107, 1, 'CRISTAIS DE GENGIBRE MENTA 40G', '7898111031107', 'GENGIBRE REAL', NULL, 'NATURAIS', FALSE, TRUE, NOW()),
    (31108, 1, 'CRISTAIS DE GENGIBRE LIMAO 40G', '7898111031108', 'GENGIBRE REAL', NULL, 'NATURAIS', FALSE, TRUE, NOW()),
    (31109, 1, 'CRISTAIS DE GENGIBRE CANELA 40G', '7898111031109', 'GENGIBRE REAL', NULL, 'NATURAIS', FALSE, TRUE, NOW()),
    (31110, 1, 'CRISTAIS DE GENGIBRE MEL 40G', '7898111031110', 'GENGIBRE REAL', NULL, 'NATURAIS', FALSE, TRUE, NOW()),
    (31111, 1, 'CRISTAIS DE GENGIBRE PROPOLIS 40G', '7898111031111', 'GENGIBRE REAL', NULL, 'NATURAIS', FALSE, TRUE, NOW()),

    -- Arrebite (cadastrado no ERP como GUARANAPIS, códigos 20722 a 20724)
    (20722, 1, 'ARREBITE ENERGIA CAPSGEL 60 CAPS GUARANAPIS', '7898222020722', 'GUARANAPIS', NULL, 'ENERGETICOS', FALSE, TRUE, NOW()),
    (20723, 1, 'ARREBITE EXTRA FORTE STAND UP 120 CAPS', '7898222020723', 'GUARANAPIS', NULL, 'ENERGETICOS', FALSE, TRUE, NOW()),
    (20724, 1, 'ARREBITE LIQUIDO DISP. 12X60ML', '7898222020724', 'GUARANAPIS', NULL, 'ENERGETICOS', FALSE, TRUE, NOW()),

    -- Produto Simples Ativo
    (440100, 1, 'CHA DE CAMOMILA ORGANICO 30G FITOHERB', '7898333440100', 'FITOHERB NATURAL', NULL, 'CHAS', FALSE, TRUE, NOW()),

    -- Item Inativo no ERP ainda presente em Externo01.Produto (P-406 - INACTIVE_ALERT)
    (770201, 1, 'COLAGENO HIDROLISADO NEUTRO 250G', '7898444770201', 'MAX TITANIUM', NULL, 'SUPLEMENTOS', TRUE, TRUE, NOW()),

    -- Candidatos disponíveis (ainda não vinculados) para testes de Busca/Importação em Lote (P-407) e Fila Humana (P-409)
    (990001, 1, 'CREATINA MONOHIDRATADA 300G UN BLACK SKULL', '7898609900011', 'BLACK SKULL', NULL, 'CREATINAS', FALSE, TRUE, NOW()),
    (990002, 1, 'CREATINA MONOHIDRATADA DISP. 12X100G BLACK SKULL', '7898609900028', 'BLACK SKULL', NULL, 'CREATINAS', FALSE, TRUE, NOW()),
    (990003, 1, 'WHEY ISOLADO 900G COOKIES E CREAM INATIVO', '7896311990003', 'MAX TITANIUM', NULL, 'PROTEINAS', TRUE, FALSE, NOW())
ON CONFLICT (erp_code) DO UPDATE SET
    description     = EXCLUDED.description,
    ean             = EXCLUDED.ean,
    brand_name      = EXCLUDED.brand_name,
    category_label  = EXCLUDED.category_label,
    inativo         = EXCLUDED.inativo,
    in_externo_view = EXCLUDED.in_externo_view,
    last_synced_at  = NOW();

-- ----------------------------------------------------------------------------
-- 5. Produtos do Catálogo (products) - arrays saneados e deduplicados (P-401)
-- ----------------------------------------------------------------------------
-- Topologia 2: Somente Sabor (ex.: saneado de {"Baunilha","Chocolate e Morango."})
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000001',
    '100% Pure Whey 900g',
    '100-pure-whey-900g-max-titanium',
    NULL,
    'Proteína concentrada do soro do leite em embalagem de 900g.',
    ARRAY['Baunilha', 'Chocolate', 'Morango'],
    NULL,
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'suplementos-esportivos' AND s.slug = 'max-titanium'
ON CONFLICT DO NOTHING;

-- Topologia 4: Matriz Sabor x Apresentação (2x3 = 6 SKUs; projeções deduplicadas!)
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000002',
    'B.O.P.E',
    'bope-black-skull',
    NULL,
    'Pré-treino concentrado em duas apresentações e três sabores.',
    ARRAY['Limão', 'Frutas Vermelhas', 'Melancia'],
    ARRAY['150g', '300g'],
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'suplementos-esportivos' AND s.slug = 'black-skull'
ON CONFLICT DO NOTHING;

-- Topologia 3: Somente Apresentação (ex.: saneado de {"25ml e 60ml."})
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000003',
    'Stevita Líquido',
    'stevita-liquido-stevita',
    NULL,
    'Adoçante natural líquido à base de estévia.',
    NULL,
    ARRAY['25ml', '60ml'],
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'produtos-naturais' AND s.slug = 'stevita'
ON CONFLICT DO NOTHING;

-- Topologia 2: Somente Sabor (Cristais de Gengibre - 6 SKUs 31106 a 31111)
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000004',
    'Cristais de Gengibre',
    'cristais-de-gengibre-gengibre-real',
    NULL,
    'Pastilhas cristalizadas de gengibre em sabores sortidos.',
    ARRAY['Tradicional', 'Menta', 'Limão', 'Canela', 'Mel', 'Própolis'],
    NULL,
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'produtos-naturais' AND s.slug = 'gengibre-real'
ON CONFLICT DO NOTHING;

-- Topologia 3: Arrebite (Site 'Arrebite' <-> ERP 'GUARANAPIS', 3 apresentações 20722-20724)
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000005',
    'Arrebite Energia',
    'arrebite-energia-arrebite',
    NULL,
    'Suplemento energético natural à base de guaraná e cafeína.',
    NULL,
    ARRAY['60 Cápsulas', '120 Cápsulas', 'Display 12x60ml'],
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'suplementos-esportivos' AND s.slug = 'arrebite'
ON CONFLICT DO NOTHING;

-- Topologia 1: Produto Simples (1 SKU sem sabor nem apresentação)
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000006',
    'Chá de Camomila Orgânico 30g',
    'cha-de-camomila-organico-30g-fitoherb-natural',
    NULL,
    'Flores de camomila orgânicas desidratadas para infusão.',
    NULL,
    NULL,
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'chas-e-infusoes' AND s.slug = 'fitoherb-natural'
ON CONFLICT DO NOTHING;

-- Cenário INACTIVE_ALERT (P-406): Produto mapeado cujo SKU ficou inativo no ERP
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000007',
    'Colágeno Hidrolisado Neutro 250g',
    'colageno-hidrolisado-neutro-250g-max-titanium',
    NULL,
    'Colágeno hidrolisado em pó sem sabor.',
    NULL,
    NULL,
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'suplementos-esportivos' AND s.slug = 'max-titanium'
ON CONFLICT DO NOTHING;

-- Cenário UNMAPPED + Fila Humana (P-408, P-409): Produto legado ainda sem SKU vinculado
INSERT INTO public.products (id, name, slug, image_path, description, flavours, presentation, category_id, supplier_id, created_at, created_by)
SELECT
    '44444444-4444-4444-8444-000000000008',
    'Creatina Monohidratada Black Skull',
    'creatina-monohidratada-black-skull',
    NULL,
    'Creatina monohidratada pura para força e desempenho.',
    NULL,
    NULL,
    c.id,
    s.id,
    NOW(),
    'catalogo.admin@fitoherb-local.test'
FROM public.product_categories c, public.suppliers s
WHERE c.slug = 'suplementos-esportivos' AND s.slug = 'black-skull'
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 6. SKUs de Produto (product_skus)
-- ----------------------------------------------------------------------------
-- 6.1. 100% Pure Whey 900g (131476, 131477, 131478)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT v.id, p.id, v.erp_code, v.flavour, NULL, v.variant_key, v.ean, v.sort_order, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
CROSS JOIN (VALUES
    ('55555555-5555-4555-8555-000000000001', 131476::bigint, 'Baunilha', 'baunilha::', '7896311770011', 0),
    ('55555555-5555-4555-8555-000000000002', 131477::bigint, 'Chocolate', 'chocolate::', '7896311770028', 1),
    ('55555555-5555-4555-8555-000000000003', 131478::bigint, 'Morango', 'morango::', '7896311770035', 2)
) AS v(id, erp_code, flavour, variant_key, ean, sort_order)
WHERE p.slug = '100-pure-whey-900g-max-titanium'
ON CONFLICT DO NOTHING;

-- 6.2. B.O.P.E (810090-810092 para 150g; 810115-810117 para 300g)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT v.id, p.id, v.erp_code, v.flavour, v.presentation, v.variant_key, v.ean, v.sort_order, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
CROSS JOIN (VALUES
    ('55555555-5555-4555-8555-000000000011', 810090::bigint, 'Limão', '150g', 'limao::150g', '7898600900010', 0),
    ('55555555-5555-4555-8555-000000000012', 810091::bigint, 'Frutas Vermelhas', '150g', 'frutas vermelhas::150g', '7898600900027', 1),
    ('55555555-5555-4555-8555-000000000013', 810092::bigint, 'Melancia', '150g', 'melancia::150g', '7898600900034', 2),
    ('55555555-5555-4555-8555-000000000014', 810115::bigint, 'Limão', '300g', 'limao::300g', '7898601150010', 3),
    ('55555555-5555-4555-8555-000000000015', 810116::bigint, 'Frutas Vermelhas', '300g', 'frutas vermelhas::300g', '7898601150027', 4),
    ('55555555-5555-4555-8555-000000000016', 810117::bigint, 'Melancia', '300g', 'melancia::300g', '7898601150034', 5)
) AS v(id, erp_code, flavour, presentation, variant_key, ean, sort_order)
WHERE p.slug = 'bope-black-skull'
ON CONFLICT DO NOTHING;

-- 6.3. Stevita Líquido (580000 -> 25ml, 580001 -> 60ml)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT v.id, p.id, v.erp_code, NULL, v.presentation, v.variant_key, v.ean, v.sort_order, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
CROSS JOIN (VALUES
    ('55555555-5555-4555-8555-000000000021', 580000::bigint, '25ml', '::25ml', '7896182000101', 0),
    ('55555555-5555-4555-8555-000000000022', 580001::bigint, '60ml', '::60ml', '7896182000200', 1)
) AS v(id, erp_code, presentation, variant_key, ean, sort_order)
WHERE p.slug = 'stevita-liquido-stevita'
ON CONFLICT DO NOTHING;

-- 6.4. Cristais de Gengibre (31106 a 31111)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT v.id, p.id, v.erp_code, v.flavour, NULL, v.variant_key, v.ean, v.sort_order, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
CROSS JOIN (VALUES
    ('55555555-5555-4555-8555-000000000031', 31106::bigint, 'Tradicional', 'tradicional::', '7898111031106', 0),
    ('55555555-5555-4555-8555-000000000032', 31107::bigint, 'Menta', 'menta::', '7898111031107', 1),
    ('55555555-5555-4555-8555-000000000033', 31108::bigint, 'Limão', 'limao::', '7898111031108', 2),
    ('55555555-5555-4555-8555-000000000034', 31109::bigint, 'Canela', 'canela::', '7898111031109', 3),
    ('55555555-5555-4555-8555-000000000035', 31110::bigint, 'Mel', 'mel::', '7898111031110', 4),
    ('55555555-5555-4555-8555-000000000036', 31111::bigint, 'Própolis', 'propolis::', '7898111031111', 5)
) AS v(id, erp_code, flavour, variant_key, ean, sort_order)
WHERE p.slug = 'cristais-de-gengibre-gengibre-real'
ON CONFLICT DO NOTHING;

-- 6.5. Arrebite (20722, 20723, 20724 - Alias GUARANAPIS)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT v.id, p.id, v.erp_code, NULL, v.presentation, v.variant_key, v.ean, v.sort_order, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
CROSS JOIN (VALUES
    ('55555555-5555-4555-8555-000000000041', 20722::bigint, '60 Cápsulas', '::60 capsulas', '7898222020722', 0),
    ('55555555-5555-4555-8555-000000000042', 20723::bigint, '120 Cápsulas', '::120 capsulas', '7898222020723', 1),
    ('55555555-5555-4555-8555-000000000043', 20724::bigint, 'Display 12x60ml', '::display 12x60ml', '7898222020724', 2)
) AS v(id, erp_code, presentation, variant_key, ean, sort_order)
WHERE p.slug = 'arrebite-energia-arrebite'
ON CONFLICT DO NOTHING;

-- 6.6. Produto Simples (Chá de Camomila Orgânico 30g -> 440100)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT '55555555-5555-4555-8555-000000000051', p.id, 440100, NULL, NULL, '__SIMPLE__', '7898333440100', 0, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
WHERE p.slug = 'cha-de-camomila-organico-30g-fitoherb-natural'
ON CONFLICT DO NOTHING;

-- 6.7. Produto com Alerta Inativo no ERP (INACTIVE_ALERT -> 770201)
INSERT INTO public.product_skus (id, product_id, erp_code, flavour, presentation, normalized_variant_key, ean, sort_order, created_at, created_by)
SELECT '55555555-5555-4555-8555-000000000061', p.id, 770201, NULL, NULL, '__SIMPLE__', '7898444770201', 0, NOW(), 'catalogo.admin@fitoherb-local.test'
FROM public.products p
WHERE p.slug = 'colageno-hidrolisado-neutro-250g-max-titanium'
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 7. Fila de Decisão Humana (human_review_queue) - Protocolo Zero Chutes (P-409)
-- ----------------------------------------------------------------------------
INSERT INTO public.human_review_queue (
    id, product_id, proposed_flavour, proposed_presentation, normalized_variant_key,
    reason_code, doubt_summary, candidate_erp_items, web_evidence_urls, status, created_at, created_by
)
SELECT
    '66666666-6666-4666-8666-000000000001',
    p.id,
    NULL,
    NULL,
    '__SIMPLE__',
    'PACKAGING_AMBIGUITY',
    'Encontrados 2 candidatos ativos no ERP para Black Skull Creatina (990001 unitário 300g vs 990002 display 12x100g). Confirmar qual SKU físico corresponde ao produto publicado no site.',
    '[
        {"erp_code": 990001, "description": "CREATINA MONOHIDRATADA 300G UN BLACK SKULL", "ean": "7898609900011", "brand_name": "BLACK SKULL", "inativo": false, "in_externo_view": true, "score": 0.91},
        {"erp_code": 990002, "description": "CREATINA MONOHIDRATADA DISP. 12X100G BLACK SKULL", "ean": "7898609900028", "brand_name": "BLACK SKULL", "inativo": false, "in_externo_view": true, "score": 0.88}
    ]'::jsonb,
    '["https://catalogo.fabricante-exemplo.local/black-skull/creatina-300g"]'::jsonb,
    'PENDING',
    NOW(),
    'curadoria.bot@fitoherb-local.test'
FROM public.products p
WHERE p.slug = 'creatina-monohidratada-black-skull'
ON CONFLICT (product_id, normalized_variant_key) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 8. Trilha Auditável (erp_mapping_audit)
-- ----------------------------------------------------------------------------
INSERT INTO public.erp_mapping_audit (
    id, product_id, product_sku_id, normalized_variant_key, previous_erp_code, new_erp_code,
    mapping_source, confidence_score, evidence_summary, actor_email, created_at
)
SELECT
    '77777777-7777-4777-8777-000000000001',
    s.product_id,
    s.id,
    s.normalized_variant_key,
    NULL,
    s.erp_code,
    'AUTO_EXACT',
    1.0000,
    'Match determinístico via supplier_brand_aliases (Arrebite <-> GUARANAPIS) e expansão de abreviação CAPSGEL.',
    'curadoria.bot@fitoherb-local.test',
    NOW()
FROM public.product_skus s
WHERE s.erp_code = 20722
  AND NOT EXISTS (
      SELECT 1 FROM public.erp_mapping_audit a WHERE a.id = '77777777-7777-4777-8777-000000000001'
  );

INSERT INTO public.erp_mapping_audit (
    id, product_id, product_sku_id, normalized_variant_key, previous_erp_code, new_erp_code,
    mapping_source, confidence_score, evidence_summary, actor_email, created_at
)
SELECT
    '77777777-7777-4777-8777-000000000002',
    s.product_id,
    s.id,
    s.normalized_variant_key,
    NULL,
    s.erp_code,
    'WEB_VERIFIED',
    0.9850,
    'Saneamento de "Chocolate e Morango." em sabores atômicos confirmado contra descrição ERP 131476-131478.',
    'catalogo.admin@fitoherb-local.test',
    NOW()
FROM public.product_skus s
WHERE s.erp_code = 131477
  AND NOT EXISTS (
      SELECT 1 FROM public.erp_mapping_audit a WHERE a.id = '77777777-7777-4777-8777-000000000002'
  );

-- ----------------------------------------------------------------------------
-- 9. Seeds Incrementais Onda 4 (US-P7 / US-P8: P-406, P-410 a P-413)
-- ----------------------------------------------------------------------------
INSERT INTO public.erp_sync_state (
    id, baseline_cutoff_at, auto_sync_enabled, last_successful_sync_at, in_progress, lock_acquired_at, active_run_id, updated_at
)
VALUES (1, NOW() - INTERVAL '7 days', TRUE, NOW() - INTERVAL '1 day', FALSE, NULL, NULL, NOW())
ON CONFLICT (id) DO NOTHING;

UPDATE public.products
SET active = TRUE, curation_status = 'COMPLETED'
WHERE slug IN (
    '100-pure-whey-900g-max-titanium', 'bope-black-skull', 'stevita-liquido-stevita',
    'cristais-de-gengibre-gengibre-real', 'arrebite-energia-arrebite',
    'cha-de-camomila-organico-30g-fitoherb-natural', 'creatina-monohidratada-black-skull'
);

UPDATE public.products
SET active = FALSE, curation_status = 'COMPLETED'
WHERE slug = 'colageno-hidrolisado-neutro-250g-max-titanium';

INSERT INTO public.erp_ignored_catalog_codes (
    erp_code, source, product_name_snapshot, reason, ignored_by, ignored_at
)
VALUES (
    990999,
    'ADMIN_IGNORED',
    'SQUEEZE PROMOCIONAL INTERNO EVENTO 500ML',
    'Brinde promocional interno — não exibir no catálogo público.',
    'catalogo.admin@fitoherb-local.test',
    NOW() - INTERVAL '1 day'
)
ON CONFLICT (erp_code) DO NOTHING;

INSERT INTO public.erp_sync_runs (
    id, trigger_type, triggered_by, status, started_at, finished_at,
    deactivated_count, reactivated_count, partial_variant_updated_count, created_count, skipped_ignored_count, error_summary
)
VALUES (
    '88888888-8888-4888-8888-000000000001',
    'CLOUD_SCHEDULER',
    'CLOUD_SCHEDULER',
    'SUCCESS',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day' + INTERVAL '4 seconds',
    1, 0, 1, 1, 1, NULL
)
ON CONFLICT (id) DO NOTHING;

COMMIT;
