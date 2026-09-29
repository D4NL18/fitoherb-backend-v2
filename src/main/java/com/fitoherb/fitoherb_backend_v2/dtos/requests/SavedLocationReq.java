package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import com.fitoherb.fitoherb_backend_v2.dtos.base.BaseLocationDto;
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
public class SavedLocationReq extends BaseLocationDto {

    @Override
    @NotBlank(message = MSG_REQUIRED_FIELD)
    public String getTitle() {
        return super.getTitle();
    }

    @Override
    @NotNull(message = MSG_REQUIRED_FIELD)
    public SavedLocationType getType() {
        return super.getType();
    }

    @Override
    @NotNull(message = MSG_REQUIRED_FIELD)
    public Double getLatitude() {
        return super.getLatitude();
    }

    @Override
    @NotNull(message = MSG_REQUIRED_FIELD)
    public Double getLongitude() {
        return super.getLongitude();
    }
}
