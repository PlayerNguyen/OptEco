package me.playernguyen.opteco.permission;

public enum OptEcoPermission {

    ADMIN("opteco.admin"),
    EVERYTHING("opteco.*"),
    USER("opteco.user"),

    ADD("opteco.add"),
    TAKE("opteco.take"),
    CHECK("opteco.check"),
    ME("opteco.me"),
    PAY("opteco.pay"),
    RELOAD("opteco.reload"),
    SET("opteco.set"),
    CONFIRM("opteco.confirm"),
    CANCEL("opteco.cancel");

    private String permission;

    OptEcoPermission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

}
