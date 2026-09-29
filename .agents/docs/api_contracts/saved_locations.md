# Contrato de API: Locais Salvos (`/saved-locations`)

Prefix: `/saved-locations`  
Autenticação: `Bearer Token` JWT  
Autorização: `@authorizationService.isAdminOrSeller()`

---

### 1. `GET /saved-locations`
Retorna todos os locais salvos vinculados ao usuário autenticado.

**Response 200 OK:**
```json
[
  {
    "id": "e3b0c442-98fc-1c14-9afb-4c8996fb9242",
    "title": "Minha Residência (Base Lauro)",
    "type": "BASE",
    "latitude": -12.8950,
    "longitude": -38.3200,
    "street": "Rua das Pitangueiras",
    "number": "120",
    "neighborhood": "Pitangueiras",
    "city": "Lauro de Freitas",
    "state": "BA",
    "postalCode": "42701-000",
    "fullAddress": "Rua das Pitangueiras, 120, Pitangueiras, Lauro de Freitas - BA",
    "notes": "Ponto de partida oficial",
    "createdAt": "2026-09-23T10:00:00"
  }
]
```

---

### 2. `GET /saved-locations/base`
Retorna a base ativa do vendedor autenticado (ou 204 No Content se não houver base).

---

### 3. `POST /saved-locations`
Cria um novo local salvo para o usuário autenticado. Se o `type` for `BASE`, substitui/desmarca bases anteriores.

**Request Body:**
```json
{
  "title": "Drogaria São Paulo - Pituba",
  "type": "FAVORITE",
  "latitude": -12.9984,
  "longitude": -38.4908,
  "street": "Av. Manoel Dias da Silva",
  "number": "1500",
  "neighborhood": "Pituba",
  "city": "Salvador",
  "state": "BA",
  "postalCode": "41830-000",
  "fullAddress": "Av. Manoel Dias da Silva, 1500, Pituba, Salvador - BA",
  "notes": "Falar com Dr. Carlos"
}
```

---

### 4. `PUT /saved-locations/{id}`
Atualiza dados de um local existente.

---

### 5. `DELETE /saved-locations/{id}`
Exclui um local do usuário autenticado.
