# Contratos de API: Rotas Agendadas com Calendário (Scheduled Routes)

## Autenticação
Todos os endpoints exigem autenticação via Bearer Token JWT e autorização de `ROLE_SELLER` ou `ROLE_ADMIN` (`@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")`).

## Endpoints

### 1. Salvar ou Substituir Rota (Upsert)
- **Endpoint:** `POST /scheduled-routes`
- **Descrição:** Salva uma nova rota ou substitui a existente para a data informada.

**Request Body (application/json):**
```json
{
  "route_date": "2026-09-28",
  "departure_time": "08:00",
  "return_to_depot": true,
  "depot": {
    "lat": -8.047562,
    "lng": -34.876964,
    "address": "Rua Exemplo, 123"
  },
  "stops": [
    {
      "id": "stop-1",
      "lat": -8.057838,
      "lng": -34.882897,
      "address": "Av. Exemplo, 456"
    }
  ],
  "optimization_result": {
    "total_distance_km": 15.2,
    "total_time_minutes": 45,
    "optimized_route_order": ["stop-1"]
  }
}
```

**Response (201 Created):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "route_date": "2026-09-28",
  "departure_time": "08:00",
  "return_to_depot": true,
  "stops_count": 1,
  "total_time_minutes": 45.0,
  "total_distance_km": 15.2,
  "created_at": "2026-09-28T18:21:46Z"
}
```
**Erros Possíveis:** 422 Unprocessable Entity (data fora da janela), 400 Bad Request, 401 Unauthorized, 403 Forbidden.

### 2. Listar Resumos de Rotas
- **Endpoint:** `GET /scheduled-routes`
- **Descrição:** Retorna as rotas do usuário dentro da janela de visibilidade (hoje-7 dias até hoje+30 dias).

**Response (200 OK):**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "route_date": "2026-09-28",
    "departure_time": "08:00",
    "stops_count": 1,
    "total_time_minutes": 45.0,
    "total_distance_km": 15.2
  }
]
```

### 3. Listar Apenas Datas (Para o Calendário)
- **Endpoint:** `GET /scheduled-routes/dates`
- **Descrição:** Endpoint otimizado que retorna apenas uma lista de datas que possuem rotas cadastradas para popular os indicadores visuais do calendário.

**Response (200 OK):**
```json
[
  "2026-09-28",
  "2026-09-30",
  "2026-10-05"
]
```

### 4. Obter Rota Específica por Data
- **Endpoint:** `GET /scheduled-routes/{date}`
- **Parâmetros de Path:** `date` (Formato yyyy-MM-dd)
- **Descrição:** Retorna os dados completos da rota de uma data específica.

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "route_date": "2026-09-28",
  "departure_time": "08:00",
  "return_to_depot": true,
  "depot": {
    "lat": -8.047562,
    "lng": -34.876964,
    "address": "Rua Exemplo, 123"
  },
  "stops": [
    {
      "id": "stop-1",
      "lat": -8.057838,
      "lng": -34.882897,
      "address": "Av. Exemplo, 456"
    }
  ],
  "optimization_result": {
    "total_distance_km": 15.2,
    "total_time_minutes": 45,
    "optimized_route_order": ["stop-1"]
  },
  "created_at": "2026-09-28T18:21:46Z"
}
```
**Erros Possíveis:** 404 Not Found (sem rota na data).

### 5. Excluir Rota Específica por Data
- **Endpoint:** `DELETE /scheduled-routes/{date}`
- **Parâmetros de Path:** `date` (Formato yyyy-MM-dd)
- **Descrição:** Deleta a rota programada para a data especificada.

**Response (204 No Content)**
