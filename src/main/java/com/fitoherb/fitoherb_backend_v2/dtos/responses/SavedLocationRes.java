package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.enums.SavedLocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Response object representing a saved location or customer delivery point")
public class SavedLocationRes {

    @Schema(description = "Unique UUID identifier of the location", example = "e3b0c442-98fc-1c14-9afb-4c8996fb9242")
    private String id;

    @Schema(description = "Title or name of the place", example = "Drogaria São Paulo - Pituba")
    private String title;

    @Schema(description = "Type of location (BASE or FAVORITE)", example = "FAVORITE")
    private SavedLocationType type;

    @Schema(description = "Geographic latitude", example = "-12.9984")
    private Double latitude;

    @Schema(description = "Geographic longitude", example = "-38.4908")
    private Double longitude;

    @Schema(description = "Street name / Thoroughfare", example = "Av. Manoel Dias da Silva")
    private String street;

    @Schema(description = "Building / House number", example = "1500")
    private String number;

    @Schema(description = "Neighborhood / District", example = "Pituba")
    private String neighborhood;

    @Schema(description = "City name", example = "Salvador")
    private String city;

    @Schema(description = "State code", example = "BA")
    private String state;

    @Schema(description = "Postal Code (CEP)", example = "41830-000")
    private String postalCode;

    @Schema(description = "Pre-formatted full human-readable address", example = "Av. Manoel Dias da Silva, 1500, Pituba, Salvador - BA")
    private String fullAddress;

    @Schema(description = "Commercial notes or contact name", example = "Falar com Dr. Carlos")
    private String notes;

    @Schema(description = "Creation date formatted string", example = "23-09-2026 10:00:00")
    private String createdAt;
}
