package com.fitoherb.fitoherb_backend_v2.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.ScheduledRouteReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteSummaryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ScheduledRoute;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.ScheduledRouteMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ScheduledRouteRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduledRouteService {

    private final ScheduledRouteRepository repo;
    private final UserRepository userRepository;
    private final ScheduledRouteMapper mapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int HISTORY_DAYS = 7;
    private static final int FUTURE_DAYS = 30;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }
        String email = authentication.getName();
        return (User) userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado: " + email));
    }

    @Transactional
    public ScheduledRouteRes saveRoute(ScheduledRouteReq req) {
        User user = getAuthenticatedUser();
        LocalDate now = LocalDate.now();

        if (req.getRouteDate().isBefore(now.minusDays(HISTORY_DAYS)) || req.getRouteDate().isAfter(now.plusDays(FUTURE_DAYS))) {
            throw new IllegalArgumentException("Data da rota fora do limite permitido");
        }

        ScheduledRoute entity = repo.findByUserIdAndRouteDate(user.getId(), req.getRouteDate())
                .orElse(new ScheduledRoute());

        if (entity.getId() == null) {
            entity.setUser(user);
        }
        
        entity.setRouteDate(req.getRouteDate());
        entity.setDepartureTime(req.getDepartureTime());
        entity.setReturnToDepot(req.getReturnToDepot());

        try {
            entity.setDepot(objectMapper.writeValueAsString(req.getDepot()));
            entity.setStops(objectMapper.writeValueAsString(req.getStops()));
            if (req.getOptimizationResult() != null) {
                String optResultStr = objectMapper.writeValueAsString(req.getOptimizationResult());
                entity.setOptimizationResult(optResultStr);
                
                JsonNode optNode = objectMapper.readTree(optResultStr);
                if (optNode.has("metrics")) {
                    JsonNode metrics = optNode.get("metrics");
                    if (metrics.has("totalTimeMinutes")) entity.setTotalTimeMinutes(metrics.get("totalTimeMinutes").asDouble());
                    if (metrics.has("totalDistanceKm")) entity.setTotalDistanceKm(metrics.get("totalDistanceKm").asDouble());
                }
                
                if (optNode.has("optimizedRoute") && optNode.get("optimizedRoute").isArray()) {
                    entity.setStopsCount(optNode.get("optimizedRoute").size());
                } else if (optNode.has("stopsCount")) {
                    entity.setStopsCount(optNode.get("stopsCount").asInt());
                }
            } else {
                entity.setOptimizationResult(null);
                entity.setTotalTimeMinutes(null);
                entity.setTotalDistanceKm(null);
                
                JsonNode stopsNode = objectMapper.valueToTree(req.getStops());
                if (stopsNode.isArray()) {
                    entity.setStopsCount(stopsNode.size());
                } else {
                    entity.setStopsCount(0);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar JSON da rota", e);
            throw new DatabaseOperationException("Falha ao serializar dados", e);
        }

        try {
            entity = repo.save(entity);
            return toRes(entity);
        } catch (Exception e) {
            log.error("Erro ao salvar rota agendada", e);
            throw new DatabaseOperationException("Falha ao salvar rota agendada", e);
        }
    }

    public List<ScheduledRouteSummaryRes> getRoutesByDateRange() {
        User user = getAuthenticatedUser();
        LocalDate now = LocalDate.now();
        List<ScheduledRoute> routes = repo.findAllByUserIdAndRouteDateBetweenOrderByRouteDateAsc(
                user.getId(), now.minusDays(HISTORY_DAYS), now.plusDays(FUTURE_DAYS));
        
        return routes.stream().map(mapper::entityToSummaryRes).toList();
    }

    public List<LocalDate> getRouteDates() {
        User user = getAuthenticatedUser();
        LocalDate now = LocalDate.now();
        return repo.findRouteDatesByUserIdAndDateRange(
                user.getId(), now.minusDays(HISTORY_DAYS), now.plusDays(FUTURE_DAYS));
    }

    public ScheduledRouteRes getRouteByDate(LocalDate date) {
        User user = getAuthenticatedUser();
        ScheduledRoute route = repo.findByUserIdAndRouteDate(user.getId(), date)
                .orElseThrow(() -> new ResourceNotFoundException("Rota agendada não encontrada para a data: " + date));
        return toRes(route);
    }

    @Transactional
    public void deleteRouteByDate(LocalDate date) {
        User user = getAuthenticatedUser();
        ScheduledRoute route = repo.findByUserIdAndRouteDate(user.getId(), date)
                .orElseThrow(() -> new ResourceNotFoundException("Rota agendada não encontrada para a data: " + date));
        
        try {
            repo.delete(route);
        } catch (Exception e) {
            log.error("Erro ao excluir rota agendada", e);
            throw new DatabaseOperationException("Falha ao excluir rota agendada", e);
        }
    }

    @Transactional
    public void cleanupExpiredRoutes() {
        LocalDate cutoffDate = LocalDate.now().minusDays(HISTORY_DAYS);
        int deletedCount = repo.deleteByRouteDateBefore(cutoffDate);
        log.info("Cleanup de rotas agendadas executado. Quantidade removida: {}", deletedCount);
    }
    
    private ScheduledRouteRes toRes(ScheduledRoute entity) {
        ScheduledRouteRes res = mapper.entityToRes(entity);
        try {
            if (entity.getDepot() != null) res.setDepot(objectMapper.readValue(entity.getDepot(), Object.class));
            if (entity.getStops() != null) res.setStops(objectMapper.readValue(entity.getStops(), Object.class));
            if (entity.getOptimizationResult() != null) res.setOptimizationResult(objectMapper.readValue(entity.getOptimizationResult(), Object.class));
        } catch (JsonProcessingException e) {
            log.error("Erro ao deserializar JSON da rota", e);
        }
        return res;
    }
}
