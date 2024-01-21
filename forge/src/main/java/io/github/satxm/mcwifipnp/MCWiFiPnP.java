package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
    public static final String MODID = "mcwifipnp";

    public MCWiFiPnP() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::ChangeButton);
    }

    @SubscribeEvent
    public void ChangeButton(ScreenEvent.InitScreenEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = event.getScreen();
        if (screen instanceof PauseScreen && event.getListenersList().size() != 0) {
            for (int k = 0; k < event.getListenersList().size(); k++) {
                Button ShareToLanOld = (Button) event.getListenersList().get(k);
                if (ShareToLanOld.getMessage().getString()
                        .equals(new TranslatableComponent("menu.shareToLan").getString())) {
                    Button ShareToLanNew = new Button(ShareToLanOld.x, ShareToLanOld.y, ShareToLanOld.getWidth(), ShareToLanOld.getHeight(), new TranslatableComponent("menu.shareToLan"),
                            (button) -> client.setScreen(new ShareToLanScreenNew(screen)));
                    ShareToLanNew.active = ShareToLanOld.active;
                    event.removeListener(ShareToLanOld);
                    event.addListener(ShareToLanNew);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MCWiFiPnPUnit.ReadingConfig(event.getServer());
        DeOpCommands.register(event.getServer().getCommands().getDispatcher());
        OpCommand.register(event.getServer().getCommands().getDispatcher());
        WhitelistCommand.register(event.getServer().getCommands().getDispatcher());
        BanIpCommands.register(event.getServer().getCommands().getDispatcher());
        BanListCommands.register(event.getServer().getCommands().getDispatcher());
        BanPlayerCommands.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
    }

    public static void setMaxPlayers(IntegratedServer server, int num) {
        PlayerList playerList = server.getPlayerList();
        playerList.maxPlayers = num;
    }

}
