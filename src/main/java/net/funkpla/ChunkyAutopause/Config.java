package net.funkpla.ChunkyAutopause;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ChunkyAutopause.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_ON_STARTUP = BUILDER
            .comment("Whether to enable automatically pausing Chunky on startup")
            .define("enableOnStartup",true);
    private static final ForgeConfigSpec.IntValue RESUME_WAIT_TICKS = BUILDER
            .comment("How many ticks to wait until resuming Chunky tasks after the last player logs out")
            .defineInRange("resumeWaitTicks", 600, 0, Integer.MAX_VALUE);

    public static boolean enableOnStartup;
    public static int resumeWaitTicks;
    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enableOnStartup = ENABLE_ON_STARTUP.get();
        resumeWaitTicks = RESUME_WAIT_TICKS.get();
    }
}
