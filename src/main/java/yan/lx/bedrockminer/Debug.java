package yan.lx.bedrockminer;

import yan.lx.bedrockminer.config.Config;

public class Debug {
    public static void info(String var1, Object... var2) {
        if (Config.INSTANCE.debug) {
            BedrockMinerMod.LOGGER.info(var1, var2);
        }
    }

    public static void info(Object obj) {
        if (Config.INSTANCE.debug) {
            BedrockMinerMod.LOGGER.info(obj.toString());
        }
    }

    public static void info() {
        info("");
    }


}
