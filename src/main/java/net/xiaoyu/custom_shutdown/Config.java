package net.xiaoyu.custom_shutdown;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static class Common {
        public final ModConfigSpec.ConfigValue<String> shutdownDateTime;
        public final ModConfigSpec.BooleanValue useRestart;

        Common(ModConfigSpec.Builder builder) {
            shutdownDateTime = builder
                .comment("Set the date and time for automatic shutdown/restart (yyyy-MM-dd-HH:mm:ss)")
                .define("shutdownDateTime", "");
                
            useRestart = builder
                .comment("Whether to switch to restart")
                .define("useRestart", false);
        }
    }

    public static final ModConfigSpec commonSpec;
    public static final Common COMMON;
    
    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}