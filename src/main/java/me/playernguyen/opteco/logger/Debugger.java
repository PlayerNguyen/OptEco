package me.playernguyen.opteco.logger;

public interface Debugger {

    void log(String var1);

    void info(String var1);

    void warn(String var1);

    void error(String var1);

    void notice(String var1);

    void printException(Exception exception);

}
