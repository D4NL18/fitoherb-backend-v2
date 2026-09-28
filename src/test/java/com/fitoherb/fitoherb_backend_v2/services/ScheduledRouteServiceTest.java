package com.fitoherb.fitoherb_backend_v2.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.ScheduledRouteReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteRes;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.ScheduledRouteSummaryRes;
import com.fitoherb.fitoherb_backend_v2.entities.ScheduledRoute;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.mappers.ScheduledRouteMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.ScheduledRouteRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduledRouteServiceTest {

    @Mock
    private ScheduledRouteRepository repo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ScheduledRouteMapper mapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ScheduledRouteService service;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId("user-1");
        mockUser.setEmail("test@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurity() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn(mockUser.getEmail());
        lenient().when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
    }

    @Nested
    class SaveRoute {
        @Test
        void saveRoute_Success() throws Exception {
            setupSecurity();
            ScheduledRouteReq req = new ScheduledRouteReq();
            req.setRouteDate(LocalDate.now().plusDays(2));
            req.setDepartureTime("08:00");
            req.setDepot(Map.of("lat", 0, "lng", 0));
            req.setStops(List.of(Map.of("lat", 1, "lng", 1)));

            when(repo.findByUserIdAndRouteDate(mockUser.getId(), req.getRouteDate())).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            
            ScheduledRoute savedEntity = new ScheduledRoute();
            savedEntity.setId("route-1");
            when(repo.save(any())).thenReturn(savedEntity);
            when(mapper.entityToRes(any())).thenReturn(new ScheduledRouteRes());

            ScheduledRouteRes res = service.saveRoute(req);
            
            assertNotNull(res);
            verify(repo).save(any());
        }

        @Test
        void saveRoute_RejectsPastDate() {
            setupSecurity();
            ScheduledRouteReq req = new ScheduledRouteReq();
            req.setRouteDate(LocalDate.now().minusDays(10));

            assertThrows(IllegalArgumentException.class, () -> service.saveRoute(req));
        }

        @Test
        void saveRoute_RejectsFutureDate() {
            setupSecurity();
            ScheduledRouteReq req = new ScheduledRouteReq();
            req.setRouteDate(LocalDate.now().plusDays(40));

            assertThrows(IllegalArgumentException.class, () -> service.saveRoute(req));
        }
    }

    @Nested
    class GetRoutes {
        @Test
        void getRoutesByDateRange_Success() {
            setupSecurity();
            when(repo.findAllByUserIdAndRouteDateBetweenOrderByRouteDateAsc(anyString(), any(), any()))
                    .thenReturn(List.of(new ScheduledRoute()));
            when(mapper.entityToSummaryRes(any())).thenReturn(new ScheduledRouteSummaryRes());

            List<ScheduledRouteSummaryRes> list = service.getRoutesByDateRange();
            
            assertFalse(list.isEmpty());
            assertEquals(1, list.size());
        }

        @Test
        void getRouteDates_Success() {
            setupSecurity();
            when(repo.findRouteDatesByUserIdAndDateRange(anyString(), any(), any()))
                    .thenReturn(List.of(LocalDate.now()));

            List<LocalDate> list = service.getRouteDates();
            
            assertFalse(list.isEmpty());
        }
    }

    @Nested
    class Cleanup {
        @Test
        void cleanupExpiredRoutes_Success() {
            when(repo.deleteByRouteDateBefore(any())).thenReturn(5);
            
            assertDoesNotThrow(() -> service.cleanupExpiredRoutes());
            verify(repo).deleteByRouteDateBefore(any());
        }
    }
}
