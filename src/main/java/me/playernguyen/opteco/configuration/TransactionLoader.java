package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEco;

import java.io.File;

public class TransactionLoader extends LoaderAbstract {

    public TransactionLoader(File file) {
        super(new File(OptEco.getInstance().getDataFolder(), file+".yml"));
    }

    @Override
    public void saveDefault() {

    }
}
