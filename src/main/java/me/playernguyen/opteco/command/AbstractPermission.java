package me.playernguyen.opteco.command;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class AbstractPermission extends OptEcoImplementation {

    private ArrayList<OptEcoPermission> permissions = new ArrayList<>();

    public AbstractPermission() { }

    public ArrayList<OptEcoPermission> getPermissions() {
        return permissions;
    }

    public boolean addPermissions(OptEcoPermission permission) {
        return getPermissions().add(permission);
    }

    public boolean removePermissions(OptEcoPermission permission) {
        return getPermissions().remove(permission);
    }

    public boolean checkPermission(CommandSender sender) {
        if (sender.isOp()) return true;
        for (OptEcoPermission permissions : permissions) {
            if (sender.hasPermission(permissions.getPermission())) return true;
        }
        return false;
    }

}
