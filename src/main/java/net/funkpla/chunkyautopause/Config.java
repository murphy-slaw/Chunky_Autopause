package net.funkpla.chunkyautopause;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
private static final ModConfigSpec.Builder BUILDER = new net.neoforged.neoforge.common.ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue ENABLE_ON_STARTUP = BUILDER
            .comment("Whether to enable automatically pausing Chunky on startup")
            .define("enableOnStartup",true);
private static final net.neoforged.neoforge.common.ModConfigSpec.IntValue RESUME_WAIT_TICKS = BUILDER
        .comment("How many ticks to wait until resuming Chunky tasks after the last player logs out")
            .defineInRange("resumeWaitTicks", 600, 0, Integer.MAX_VALUE);

    public static boolean enableOnStartup;
    public static int resumeWaitTicks;
    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enableOnStartup = ENABLE_ON_STARTUP.get();
        resumeWaitTicks = RESUME_WAIT_TICKS.get();
    }
}
