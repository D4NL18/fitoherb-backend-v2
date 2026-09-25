package com.fitoherb.fitoherb_backend_v2.dtos.responses;

import com.fitoherb.fitoherb_backend_v2.dtos.base.BaseLocationDto;
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
public class SavedLocationRes extends BaseLocationDto {

    @Schema(description = "Unique UUID identifier of the location", example = "e3b0c442-98fc-1c14-9afb-4c8996fb9242")
    private String id;

    @Schema(description = "Creation date formatted string", example = "23-09-2026 10:00:00")
    private String createdAt;
}
