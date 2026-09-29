# Modelagem de Banco de Dados: Rotas Agendadas com Calendário (Scheduled Routes)

## Tabela: `scheduled_routes`

A tabela `scheduled_routes` armazenará as rotas salvas pelos vendedores para datas específicas, incluindo os dados em formato JSONB para flexibilidade de armazenamento das paradas e do resultado da otimização.

### Colunas
- **id**: `VARCHAR(36)` - Chave Primária (UUID).
- **user_id**: `VARCHAR(36)` - Foreign Key referenciando `users(id)`. Modificador: `NOT NULL`, `ON DELETE CASCADE`.
- **route_date**: `DATE` - Data agendada da rota. `NOT NULL`.
- **departure_time**: `VARCHAR(10)` - Horário de partida estimado (ex: '08:00').
- **return_to_depot**: `BOOLEAN` - Flag indicando se a rota retorna à base (`DEFAULT true`).
- **depot**: `JSONB` - Representação do `LocationPointDto` com lat, lng e endereço do ponto de partida. `NOT NULL`.
- **stops**: `JSONB` - Array contendo os `DeliveryStopDto` de cada parada da rota. `NOT NULL`.
- **optimization_result**: `JSONB` - Resultado da otimização, serializado (pode ser null se a rota não tiver passado pela otimização IA).
- **total_time_minutes**: `DOUBLE PRECISION` - Campo desnormalizado para listagem e resumo rápidos da duração total da rota.
- **total_distance_km**: `DOUBLE PRECISION` - Campo desnormalizado para listagem rápida da quilometragem.
- **stops_count**: `INTEGER` - Quantidade total de paradas desnormalizada.
- **created_at**: `TIMESTAMP` - Auditoria JPA Padrão.
- **updated_at**: `TIMESTAMP` - Auditoria JPA Padrão.
- **created_by**: `VARCHAR(255)` - Auditoria JPA Padrão.
- **updated_by**: `VARCHAR(255)` - Auditoria JPA Padrão.

### Constraints
- `UNIQUE(user_id, route_date)`: Garante a regra de negócio P-204 (apenas uma rota por usuário por dia).

### Índices (Performance & Maintenance)
- `idx_scheduled_routes_user_id`: Otimiza buscas por rotas de um usuário.
- `idx_scheduled_routes_user_date`: (`user_id`, `route_date`) - Otimiza a busca por uma data específica para um usuário.
- `idx_scheduled_routes_route_date`: Otimiza o job de cleanup diário que exclui rotas baseadas na data.

## Script de Migração Flyway Sugerido (V_XX__create_scheduled_routes.sql)

```sql
CREATE TABLE scheduled_routes (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    route_date DATE NOT NULL,
    departure_time VARCHAR(10),
    return_to_depot BOOLEAN DEFAULT true,
    depot JSONB NOT NULL,
    stops JSONB NOT NULL,
    optimization_result JSONB,
    total_time_minutes DOUBLE PRECISION,
    total_distance_km DOUBLE PRECISION,
    stops_count INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_scheduled_routes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_scheduled_routes_user_date UNIQUE (user_id, route_date)
);

CREATE INDEX idx_scheduled_routes_user_id ON scheduled_routes(user_id);
CREATE INDEX idx_scheduled_routes_user_date ON scheduled_routes(user_id, route_date);
CREATE INDEX idx_scheduled_routes_route_date ON scheduled_routes(route_date);
```
