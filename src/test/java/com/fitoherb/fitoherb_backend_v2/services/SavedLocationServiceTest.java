package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SavedLocationReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SavedLocationRes;
import com.fitoherb.fitoherb_backend_v2.entities.SavedLocation;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.enums.SavedLocationType;
import com.fitoherb.fitoherb_backend_v2.enums.UserRole;
import com.fitoherb.fitoherb_backend_v2.mappers.SavedLocationMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.SavedLocationRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedLocationServiceTest {

    @Mock
    private SavedLocationRepository savedLocationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SavedLocationMapper savedLocationMapper;

    @InjectMocks
    private SavedLocationService savedLocationService;

    private User sellerUser;
    private SavedLocation locationEntity;
    private SavedLocationRes locationRes;

    @BeforeEach
    void setup() {
        sellerUser = new User();
        sellerUser.setId("seller-uuid-123");
        sellerUser.setEmail("vendedor@fitoherb.com");
        sellerUser.setName("Vendedor Fitoherb");
        sellerUser.setRole(UserRole.SELLER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sellerUser.getEmail(), null, sellerUser.getAuthorities())
        );

        locationEntity = new SavedLocation();
        locationEntity.setId("loc-uuid-1");
        locationEntity.setTitle("Drogaria Pituba");
        locationEntity.setType(SavedLocationType.FAVORITE);
        locationEntity.setLatitude(-12.9984);
        locationEntity.setLongitude(-38.4908);
        locationEntity.setUser(sellerUser);

        locationRes = new SavedLocationRes();
        locationRes.setId("loc-uuid-1");
        locationRes.setTitle("Drogaria Pituba");
        locationRes.setType(SavedLocationType.FAVORITE);
        locationRes.setLatitude(-12.9984);
        locationRes.setLongitude(-38.4908);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve listar todos os locais salvos do usuário autenticado")
    void getMySavedLocationsSuccess() {
        when(userRepository.findByEmail(sellerUser.getEmail())).thenReturn(Optional.of(sellerUser));
        when(savedLocationRepository.findAllByUserIdOrderByCreatedAtDesc(sellerUser.getId())).thenReturn(List.of(locationEntity));
        when(savedLocationMapper.entityToRes(locationEntity)).thenReturn(locationRes);

        List<SavedLocationRes> result = savedLocationService.getMySavedLocations();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Drogaria Pituba", result.get(0).getTitle());
        verify(savedLocationRepository).findAllByUserIdOrderByCreatedAtDesc(sellerUser.getId());
    }

    @Test
    @DisplayName("Deve retornar a base ativa do vendedor se existir")
    void getMyBaseLocationSuccess() {
        locationEntity.setType(SavedLocationType.BASE);
        locationRes.setType(SavedLocationType.BASE);

        when(userRepository.findByEmail(sellerUser.getEmail())).thenReturn(Optional.of(sellerUser));
        when(savedLocationRepository.findFirstByUserIdAndType(sellerUser.getId(), SavedLocationType.BASE))
                .thenReturn(Optional.of(locationEntity));
        when(savedLocationMapper.entityToRes(locationEntity)).thenReturn(locationRes);

        Optional<SavedLocationRes> result = savedLocationService.getMyBaseLocation();

        assertTrue(result.isPresent());
        assertEquals(SavedLocationType.BASE, result.get().getType());
    }

    @Test
    @DisplayName("Deve desmarcar bases anteriores ao criar um novo local do tipo BASE (Regra P-101)")
    void createBaseLocationUnsetsPrevious() {
        SavedLocationReq req = new SavedLocationReq();
        req.setTitle("Minha Nova Casa");
        req.setType(SavedLocationType.BASE);
        req.setLatitude(-12.8992);
        req.setLongitude(-38.3242);

        when(userRepository.findByEmail(sellerUser.getEmail())).thenReturn(Optional.of(sellerUser));
        when(savedLocationMapper.reqToEntity(req)).thenReturn(locationEntity);
        when(savedLocationRepository.save(any(SavedLocation.class))).thenReturn(locationEntity);
        when(savedLocationMapper.entityToRes(locationEntity)).thenReturn(locationRes);

        SavedLocationRes result = savedLocationService.create(req);

        assertNotNull(result);
        verify(savedLocationRepository).unsetPreviousBaseLocations(
                sellerUser.getId(),
                SavedLocationType.FAVORITE,
                SavedLocationType.BASE
        );
        verify(savedLocationRepository).save(any(SavedLocation.class));
    }

    @Test
    @DisplayName("Deve excluir local salvo pertencente ao usuário")
    void deleteLocationSuccess() {
        when(userRepository.findByEmail(sellerUser.getEmail())).thenReturn(Optional.of(sellerUser));
        when(savedLocationRepository.findById("loc-uuid-1")).thenReturn(Optional.of(locationEntity));

        savedLocationService.delete("loc-uuid-1");

        verify(savedLocationRepository).delete(locationEntity);
    }
}
