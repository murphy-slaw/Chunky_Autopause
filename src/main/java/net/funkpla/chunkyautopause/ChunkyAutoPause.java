package net.funkpla.chunkyautopause;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.api.ChunkyAPIImpl;
import org.popcraft.chunky.platform.World;
import org.slf4j.Logger;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.ChunkyAPI;

import java.util.HashSet;
import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChunkyAutoPause.MODID)
public class ChunkyAutoPause {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "chunkyautopause";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean enabled;
    private static Chunky chunky;
    private static ChunkyAPI chunkyApi;
    private final HashSet<World> suspendedTasks;
    private ResumeTimer resumeTimer;

    public ChunkyAutoPause(IEventBus modEventBus, ModContainer container) {
        suspendedTasks = new HashSet<>();
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        Provider.register(this);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent ignoredEvent) {
        setEnabled(Config.enableOnStartup);
        resumeTimer = new ResumeTimer(Config.resumeWaitTicks);
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
    public void onPlayerConnect(PlayerEvent.PlayerLoggedInEvent event) {
        if (!isEnabled()) return;
        var count = Objects.requireNonNull(event.getEntity().getServer()).getPlayerCount();
        LOGGER.debug("Player logged in. Server population is :{}", count);
        suspend();
    }

    @SubscribeEvent
    public void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        LOGGER.debug("Registering commands, maybe?");
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        AutoPauseEnableCommand.register(commandDispatcher);
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!isEnabled()) return;
        var count = Objects.requireNonNull(event.getEntity().getServer()).getPlayerCount();
        LOGGER.debug("Player logged out. Server population is :{}", count);
        if (count <= 1) resumeTimer.start();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
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
        new HashSet<>(suspendedTasks).forEach(task -> {
            var name = task.getName();
            if (chunkyApi.continueTask(name)) {
                suspendedTasks.remove(task);
                LOGGER.debug("Resumed task {}", name);
            }
        });
        assert suspendedTasks.isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.debug("Enabled:{}", this.enabled);
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
