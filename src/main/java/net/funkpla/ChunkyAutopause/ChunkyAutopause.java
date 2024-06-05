package net.funkpla.ChunkyAutopause;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.api.ChunkyAPIImpl;
import org.popcraft.chunky.platform.World;
import org.slf4j.Logger;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.ChunkyAPI;

import java.util.HashSet;
import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChunkyAutopause.MODID)
public class ChunkyAutopause {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "chunky_autopause";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean enabled;
    private static Chunky chunky;
    private static ChunkyAPI chunkyApi;
    private final HashSet<World> suspendedTasks;
    private static int resumeDeadline;
    private ResumeTimer resumeTimer;

    public ChunkyAutopause() {
        suspendedTasks = new HashSet<>();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        enabled = Config.enableOnStartup;
        resumeDeadline = Config.resumeWaitTicks;
        resumeTimer = new ResumeTimer(resumeDeadline);
    }


    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        chunky = ChunkyProvider.get();
        chunkyApi = new ChunkyAPIImpl(chunky);
        chunky.getServer().getWorlds().forEach(world -> {
            if (chunkyApi.isRunning(world.toString())) {
                LOGGER.info("Task running for: {}", world);
            } else LOGGER.info("Task not running for: {}", world);
        });
    }

    @SubscribeEvent
    public void onPlayerConnect(PlayerLoggedInEvent event) {
        if (!enabled) return;
        LOGGER.debug("Player logged in.");
        var count = Objects.requireNonNull(event.getEntity().getServer()).getPlayerCount();
        LOGGER.debug("Server population is :{}", count);
        suspend();
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!enabled) return;
        LOGGER.debug("Player logged out.");
        var count = Objects.requireNonNull(event.getEntity().getServer()).getPlayerCount();
        LOGGER.debug("Server population is :{}", count);
        if (count <= 1) resumeTimer.start();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        resumeTimer.tick();
    }

    private HashSet<World> getTasks() {
        var tasks = new HashSet<World>();
        chunky.getServer().getWorlds().forEach(world -> {
            if (chunkyApi.isRunning(world.getName())) {
                tasks.add(world);
            }
        });
        return tasks;
    }

    private void suspend() {
        LOGGER.info("Suspending chunky tasks");
        resumeTimer.cancel();
        getTasks().forEach(task -> {
            var name = task.getName();
            if (chunkyApi.pauseTask(name)) {
                suspendedTasks.add(task);
                LOGGER.debug("Suspended task {}", name);
            }
        });
    }

    private void resume() {
        LOGGER.info("Resuming Chunky Tasks");
        new HashSet<>(suspendedTasks).stream().forEach(task -> {
            var name = task.getName();
            if (chunkyApi.continueTask(name)) {
                suspendedTasks.remove(task);
                LOGGER.debug("Resumed task {}", name);
            }
        });
        assert suspendedTasks.isEmpty();
    }

    private class ResumeTimer {
        private int tickCount;
        private final int deadline;
        private boolean started;

        public ResumeTimer(int deadline) {
            tickCount = 0;
            started = false;
            this.deadline = deadline;
        }

        public void start() {
            tickCount = 0;
            started = true;
        }

        public void cancel() {
            started = false;
            tickCount = 0;
        }

        public void tick() {
            if (!started) return;
            tickCount++;
            if (tickCount >= deadline) {
                LOGGER.debug("Resume timer expired");
                resume();
                cancel();
            }
        }
    }
}
