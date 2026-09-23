package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import com.fitoherb.fitoherb_backend_v2.enums.SavedLocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_REQUIRED_FIELD;

@Getter
@Setter
@Schema(description = "Request object for creating or updating a saved location")
public class SavedLocationReq {

    @Schema(description = "User-facing title/identifier for the place", example = "Drogaria São Paulo - Pituba")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String title;

    @Schema(description = "Location type (BASE for departure point, FAVORITE for frequent stop/client)", example = "FAVORITE")
    @NotNull(message = MSG_REQUIRED_FIELD)
    private SavedLocationType type;

    @Schema(description = "Geographic latitude for internal routing calculations", example = "-12.9984")
    @NotNull(message = MSG_REQUIRED_FIELD)
    private Double latitude;

    @Schema(description = "Geographic longitude for internal routing calculations", example = "-38.4908")
    @NotNull(message = MSG_REQUIRED_FIELD)
    private Double longitude;

    @Schema(description = "Street name / Thoroughfare", example = "Av. Manoel Dias da Silva")
    private String street;

    @Schema(description = "Building / House number", example = "1500")
    private String number;

    @Schema(description = "Neighborhood / District", example = "Pituba")
    private String neighborhood;

    @Schema(description = "City name", example = "Salvador")
    private String city;

    @Schema(description = "Federation Unit / State code", example = "BA")
    private String state;

    @Schema(description = "Postal Code (CEP)", example = "41830-000")
    private String postalCode;

    @Schema(description = "Pre-formatted full human-readable address", example = "Av. Manoel Dias da Silva, 1500, Pituba, Salvador - BA")
    private String fullAddress;

    @Schema(description = "Optional notes or commercial contact details", example = "Falar com Dr. Carlos, farmacêutico responsável")
    private String notes;
}
