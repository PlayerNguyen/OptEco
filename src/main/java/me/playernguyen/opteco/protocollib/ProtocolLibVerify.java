package me.playernguyen.opteco.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.playernguyen.opteco.OptEcoImplementation;

public class ProtocolLibVerify extends OptEcoImplementation {

    public boolean isHasProtocolLib() {
        return getPlugin().isProtocolLibEnabled();
    }

    public ProtocolManager getProtocolManager() throws Exception {
        if (!isHasProtocolLib()) {
            throw new Exception("ProtocolLib not found!");
        } else {
            return ProtocolLibrary.getProtocolManager();
        }
    }



}
