package net.sweenus.simplybows.upgrade;

public enum RuneEtching {
    NONE("none"),
    PAIN("pain"),
    GRACE("grace"),
    BOUNTY("bounty"),
    CHAOS("chaos");

    private final String id;

    RuneEtching(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static RuneEtching fromId(String id) {
        for (RuneEtching value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return NONE;
    }
}
