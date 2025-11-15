package net.xiaoyu.custom_shutdown;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.*;
import net.minecraft.commands.*;
import org.apache.logging.log4j.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

@Mod(CustomShutdown.MOD_ID)
public class CustomShutdown {
    public static final String MOD_ID = "custom_shutdown";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

    private static int countdown = -1;
    private static int lastAnnouncedSecond = -1;

    public CustomShutdown(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (countdown > 0) {
            countdown--;
            int secondsLeft = countdown / 20;

            if (secondsLeft != lastAnnouncedSecond) {
                lastAnnouncedSecond = secondsLeft;
                sendShutdownMessage(secondsLeft);
            }
            
            if (countdown == 0) {
                // 在关机/重启之前保存所有
                if (ServerLifecycleHooks.getCurrentServer() != null) {
                    ServerLifecycleHooks.getCurrentServer().saveEverything(true, true, true);
                    LOGGER.info("Saving all data before shutdown/restart");
                }
                
                try {
                    boolean useRestart = Config.COMMON.useRestart.get();
                    
                    if (useRestart) {
                        // 重启
                        Runtime.getRuntime().exec("shutdown -r -t 0");
                        LOGGER.info("Executing restart command");
                    } else {
                        // 关机
                        Runtime.getRuntime().exec("shutdown -s -t 0");
                        LOGGER.info("Executing shutdown command");
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to execute shutdown/restart command", e);
                }
            }
            return;
        }

        String dateTimeStr = Config.COMMON.shutdownDateTime.get();

        if (!dateTimeStr.isEmpty()) {
            try {
                LocalDateTime shutdownDateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);

                if (!LocalDateTime.now().isBefore(shutdownDateTime)) {
                    countdown = 200;
                    lastAnnouncedSecond = -1;
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to parse shutdown date time: " + dateTimeStr, e);
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("custom_shutdown_cancel")
                .executes(context -> cancelShutdown(context.getSource()))
        );
    }

    private int cancelShutdown(CommandSourceStack source) {
        if (countdown > 0) {
            countdown = -1;
            lastAnnouncedSecond = -1;

            Config.COMMON.shutdownDateTime.set("");
            
            sendCancelMessage();

            return 1;
        }

        return 0;
    }

    private void sendShutdownMessage(int seconds) {
        boolean useRestart = Config.COMMON.useRestart.get();
        String actionKey = useRestart ? "restart" : "shutdown";
        
        MutableComponent actionComponent = Component.translatable("action." + actionKey);
        MutableComponent message = Component.translatable("message", seconds, actionComponent)
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/custom_shutdown_cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("hover")))
            );
        
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void sendCancelMessage() {
        boolean useRestart = Config.COMMON.useRestart.get();
        String actionKey = useRestart ? "restart" : "shutdown";
        
        MutableComponent actionComponent = Component.translatable("action." + actionKey);
        MutableComponent component = Component.translatable("cancelled", actionComponent);
        
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(component);
            }
        }
    }
}