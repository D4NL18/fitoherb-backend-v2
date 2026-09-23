# Dicionário de Dados: Módulo de Locais Salvos (`saved_locations`)

## 1. Visão Geral
Tabela responsável pela persistência de endereços e pontos de interesse de cada usuário/vendedor no ecossistema Fitoherb, permitindo o armazenamento de sua base de partida (residência/depósito) e paradas frequentes/clientes favoritados.

---

## 2. Estrutura da Tabela: `saved_locations`

| Coluna | Tipo SQL | Nulo? | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(36)` (UUID) | NÃO | Chave Primária |
| `title` | `VARCHAR(255)` | NÃO | Nome/Identificação do local (ex: "Drogaria FarmaVida") |
| `type` | `VARCHAR(30)` | NÃO | Tipo: `BASE` (Ponto de Partida) ou `FAVORITE` (Cliente/Parada) |
| `latitude` | `DOUBLE PRECISION` | NÃO | Coordenada geográfica de latitude (cálculo de rotas) |
| `longitude` | `DOUBLE PRECISION` | NÃO | Coordenada geográfica de longitude (cálculo de rotas) |
| `street` | `VARCHAR(255)` | SIM | Logradouro / Rua |
| `number` | `VARCHAR(50)` | SIM | Número do imóvel |
| `neighborhood` | `VARCHAR(150)` | SIM | Bairro |
| `city` | `VARCHAR(150)` | SIM | Município (ex: "Salvador", "Lauro de Freitas") |
| `state` | `VARCHAR(2)` | SIM | Unidade Federativa (ex: "BA") |
| `postal_code` | `VARCHAR(20)` | SIM | Código de Endereçamento Postal (CEP) |
| `full_address` | `VARCHAR(500)` | SIM | Endereço completo formatado para exibição |
| `notes` | `TEXT` | SIM | Observações ou dados de contato da visita |
| `user_id` | `VARCHAR(36)` | NÃO | Chave Estrangeira referenciando `users(id)` |
| `created_at` | `TIMESTAMP` | NÃO | Auditoria JPA de criação |
| `updated_at` | `TIMESTAMP` | SIM | Auditoria JPA de atualização |
| `created_by` | `VARCHAR(255)` | SIM | Auditoria JPA de criador |
| `updated_by` | `VARCHAR(255)` | SIM | Auditoria JPA de modificador |

---

## 3. Chaves e Índices
- **PK:** `PRIMARY KEY (id)`
- **FK:** `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE`
- **Índice 1:** `CREATE INDEX idx_saved_locations_user_id ON saved_locations(user_id);`
- **Índice 2:** `CREATE INDEX idx_saved_locations_user_type ON saved_locations(user_id, type);`
