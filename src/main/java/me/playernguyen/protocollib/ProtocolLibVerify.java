package me.playernguyen.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.playernguyen.OptEco;

public class ProtocolLibVerify {

    private static OptEco plugin = OptEco.getPlugin();
    public static boolean isHasProtocolLib() {
        return plugin.isProtocolLibEnabled();
    }

    public static ProtocolManager getProtocolManager() throws Exception {
        if (!isHasProtocolLib()) {
            throw new Exception("ProtocolLib not found!");
        } else {
            return ProtocolLibrary.getProtocolManager();
        }
    }

}
