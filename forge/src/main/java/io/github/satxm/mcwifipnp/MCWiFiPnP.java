package io.github.satxm.mcwifipnp;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
    public static final String MODID = "mcwifipnp";

    public MCWiFiPnP() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener((final ScreenEvent.Init.Post evt) -> {
            onAfterInitScreen(evt.getScreen().getMinecraft(), evt.getScreen(), evt.getListenersList(), evt::addListener, evt::removeListener);
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MCWiFiPnPUnit.ReadingConfig(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DeOpCommands.register(event.getDispatcher());
        OpCommand.register(event.getDispatcher());
        WhitelistCommand.register(event.getDispatcher());
        BanIpCommands.register(event.getDispatcher());
        BanListCommands.register(event.getDispatcher());
        BanPlayerCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
    }

    public static void setMaxPlayers(IntegratedServer server, int num) {
        PlayerList playerList = server.getPlayerList();
        playerList.maxPlayers = num;
    }

    public static void onAfterInitScreen(Minecraft minecraft, Screen screen, List<GuiEventListener> children,
            Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
        if (screen instanceof PauseScreen pauseScreen && screen.getClass() == PauseScreen.class) {
            if (pauseScreen.showsPauseMenu()) {
                findButton(children, "menu.shareToLan").ifPresent(button -> {
                    Button newButton = Button.builder(Component.translatable("menu.shareToLan"), $ -> {
                        minecraft.setScreen(new ShareToLanScreenNew(screen));
                    }).bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();
                    remove.accept(button);
                    add.accept(newButton);
                });
            }
        }
    }

    private static Optional<Button> findButton(List<GuiEventListener> widgets, String s) {
        for (GuiEventListener widget : widgets) {
            if (widget instanceof Button button) {
                if (button.getMessage().equals(Component.translatable("menu.shareToLan")))
                    return Optional.of(button);
            }
        }
        return Optional.empty();
    }

}
