package me.playernguyen.opteco.permission;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum OptEcoPermission {
    ADMIN("opteco.admin"),
    USER("opteco.user"),
    ADD("opteco.add"),
    TAKE("opteco.take"),
    SET("opteco.set"),
    CHECK("opteco.check"),
    ME("opteco.me"),
    PAY("opteco.pay"),
    CONFIRM("opteco.confirm"),
    CANCEL("opteco.cancel"),
    TOP("opteco.top"),
    RELOAD("opteco.reload");

    private final String node;

    OptEcoPermission(String node) {
        this.node = node;
    }
}
