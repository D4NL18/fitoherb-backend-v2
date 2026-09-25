package com.fitoherb.fitoherb_backend_v2.enums;

public enum SavedLocationType {

    BASE("base"),

    FAVORITE("favorite");

    private final String type;

    SavedLocationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
