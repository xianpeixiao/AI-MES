package com.aimes.service.ai;

public enum AiProviderType {
    COZE("coze"),
    DEEPSEEK("deepseek"),
    AUTO("auto");

    private final String wireValue;

    AiProviderType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AiProviderType fromWireValue(String value) {
        if (value == null) {
            return COZE;
        }
        for (AiProviderType type : values()) {
            if (type.wireValue.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return COZE;
    }
}
