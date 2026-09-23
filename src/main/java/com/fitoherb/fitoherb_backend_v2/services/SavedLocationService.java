package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.SavedLocationReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.SavedLocationRes;
import com.fitoherb.fitoherb_backend_v2.entities.SavedLocation;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.enums.SavedLocationType;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.SavedLocationMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.SavedLocationRepository;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SavedLocationService {

    private final SavedLocationRepository savedLocationRepository;
    private final UserRepository userRepository;
    private final SavedLocationMapper savedLocationMapper;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }
        String email = authentication.getName();
        return (User) userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado: " + email));
    }

    public List<SavedLocationRes> getMySavedLocations() {
        User user = getAuthenticatedUser();
        List<SavedLocation> locations = savedLocationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        return locations.stream()
                .map(savedLocationMapper::entityToRes)
                .collect(Collectors.toList());
    }

    public Optional<SavedLocationRes> getMyBaseLocation() {
        User user = getAuthenticatedUser();
        return savedLocationRepository.findFirstByUserIdAndType(user.getId(), SavedLocationType.BASE)
                .map(savedLocationMapper::entityToRes);
    }

    public SavedLocationRes getById(String id) {
        User user = getAuthenticatedUser();
        SavedLocation location = savedLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local salvo não encontrado: " + id));

        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!location.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new AccessDeniedException("Acesso negado a este local salvo");
        }

        return savedLocationMapper.entityToRes(location);
    }

    @Transactional
    public SavedLocationRes create(SavedLocationReq req) {
        User user = getAuthenticatedUser();

        // Regra P-101: Se for BASE, atualiza anteriores para FAVORITE para garantir base única
        if (req.getType() == SavedLocationType.BASE) {
            savedLocationRepository.unsetPreviousBaseLocations(user.getId(), SavedLocationType.FAVORITE, SavedLocationType.BASE);
        }

        try {
            SavedLocation entity = savedLocationMapper.reqToEntity(req);
            entity.setUser(user);
            SavedLocation saved = savedLocationRepository.save(entity);
            return savedLocationMapper.entityToRes(saved);
        } catch (Exception e) {
            log.error("Erro ao salvar local para o usuário {}", user.getEmail(), e);
            throw new DatabaseOperationException("Falha ao salvar local no banco de dados", e);
        }
    }

    @Transactional
    public SavedLocationRes update(String id, SavedLocationReq req) {
        User user = getAuthenticatedUser();
        SavedLocation location = savedLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local salvo não encontrado: " + id));

        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!location.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new AccessDeniedException("Acesso negado a este local salvo");
        }

        if (req.getType() == SavedLocationType.BASE) {
            savedLocationRepository.unsetPreviousBaseLocations(user.getId(), SavedLocationType.FAVORITE, SavedLocationType.BASE);
        }

        try {
            savedLocationMapper.updateEntityFromReq(req, location);
            SavedLocation updated = savedLocationRepository.save(location);
            return savedLocationMapper.entityToRes(updated);
        } catch (Exception e) {
            log.error("Erro ao atualizar local {}", id, e);
            throw new DatabaseOperationException("Falha ao atualizar local no banco de dados", e);
        }
    }

    @Transactional
    public void delete(String id) {
        User user = getAuthenticatedUser();
        SavedLocation location = savedLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local salvo não encontrado: " + id));

        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!location.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new AccessDeniedException("Acesso negado a este local salvo");
        }

        try {
            savedLocationRepository.delete(location);
        } catch (Exception e) {
            log.error("Erro ao excluir local {}", id, e);
            throw new DatabaseOperationException("Falha ao excluir local do banco de dados", e);
        }
    }
}
