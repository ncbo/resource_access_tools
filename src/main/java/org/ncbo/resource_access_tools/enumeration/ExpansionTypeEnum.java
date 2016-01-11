package org.ncbo.resource_access_tools.enumeration;

public enum ExpansionTypeEnum {

    IS_A_CLOSURE(1),
    MAPPING(2),
    DISTANCE(3),;
    private final int type;

    ExpansionTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

}
