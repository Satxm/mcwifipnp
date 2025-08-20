package io.github.satxm.mcwifipnp.revprox.client;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;

import io.github.satxm.mcwifipnp.revprox.FetchedTunnel;
import io.github.satxm.mcwifipnp.revprox.InvalidAuthException;
import io.github.satxm.mcwifipnp.revprox.TunnelType;
import io.github.satxm.mcwifipnp.revprox.sakurafrp.SakuraFrpClient;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TunnelSelectionScreen extends GeneralSelectionScreen {
	static final ResourceLocation UNREACHABLE_SPRITE = ResourceLocation.withDefaultNamespace("server_list/unreachable");
	static final ResourceLocation PING_1_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_1");
	static final ResourceLocation PING_2_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_2");
	static final ResourceLocation PING_3_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_3");
	static final ResourceLocation PING_4_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_4");
	static final ResourceLocation PING_5_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_5");
	static final ResourceLocation PINGING_1_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_1");
	static final ResourceLocation PINGING_2_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_2");
	static final ResourceLocation PINGING_3_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_3");
	static final ResourceLocation PINGING_4_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_4");
	static final ResourceLocation PINGING_5_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_5");

	static final Component UNREACHABLE = Component.translatable("multiplayer.status.no_connection");
	static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");

	static final Logger LOGGER = LogUtils.getLogger();
	static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(3,
			new ThreadFactoryBuilder()
			.setNameFormat("Server Pinger #%d")
			.setDaemon(true)
			.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
			.build()
		);

	private final List<GeneralSelectionList.Option> options = new LinkedList<>();

	private static enum FetchState {
		INIT, FETCHING, IDLE,
	}
	private FetchState fetchState = FetchState.INIT;

	private final Screen rootScreen;
	private final TunnelType tunnelType;
	private Button okButton;

	public TunnelSelectionScreen(Screen lastScreen, Screen rootScreen, TunnelType tunnelType) {
		super(lastScreen, Component.translatable("mcwifipnp.tunnelSelection.title"), 40);
		this.rootScreen = rootScreen;
		this.tunnelType = tunnelType;
	}

	private void openTunnelManageLink() {
		ConfirmLinkScreen.confirmLinkNow(this, this.tunnelType.tunnelManagementUrl, true);
	}

	private void onFreshButtonClicked() {
//		GeneralSelectionList selectionList = this.getSelectionList();
//		List<GeneralSelectionList.Option> options = selectionList.children().stream()
//				.filter(GeneralSelectionList.Option.class::isInstance)
//				.map(GeneralSelectionList.Option.class::cast)
//				.collect(Collectors.toList());
		this.fetchState = FetchState.INIT;
		this.getSelectionList().refreshEntries();
	}

	@Override
	protected void initFooter() {
		Button backButton = this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_BACK, p_315824_ -> this.onClose()).width(74).build());
		this.okButton = this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_CONTINUE, p_315824_ -> this.onClose()).width(74).build());
		Button refreshButton = this.addRenderableWidget(
				Button.builder(Component.translatable("selectServer.refresh"), p_315824_ -> this.onFreshButtonClicked()).width(74).build());
		Button cancelButton = this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_CANCEL, p_315824_ -> this.minecraft.setScreen(this.rootScreen)).width(74).build());

		EqualSpacingLayout equalspacinglayout = new EqualSpacingLayout(308, 20, EqualSpacingLayout.Orientation.HORIZONTAL);
		equalspacinglayout.addChild(backButton);
		equalspacinglayout.addChild(this.okButton);
		equalspacinglayout.addChild(refreshButton);
		equalspacinglayout.addChild(cancelButton);
		equalspacinglayout.arrangeElements();
		FrameLayout.centerInRectangle(equalspacinglayout, 0, this.height - 30, this.width, 20);
	}

	@Override
	protected void populateOptions(Consumer<GeneralSelectionList.EntryBase> newEntry) {
		this.options.forEach(newEntry);

		if (this.fetchState == FetchState.INIT) {
			newEntry.accept(this.getSelectionList().new LoadingIndicator(Component.literal("Loading Nodes...")));
		}
	}

	@Override
	public void onSelectedChange() {
		GeneralSelectionList.EntryBase selected = this.getSelectionList().getSelected();
		boolean allowAction = selected instanceof GeneralSelectionList.Option;
		this.okButton.active = allowAction;
	}

	@Override
	public void onListConfirmed() {
		// TODO Auto-generated method stub

	}

	public static enum Status {
		INITIAL, PINGING, UNREACHABLE, SUCCESSFUL;
	}

	private static class Option extends GeneralSelectionList.Option {
		@Nullable
		FetchedTunnel tunnel;
		int ping;
		Status status = Status.INITIAL;

		private Option(GeneralSelectionList list) {
			list.super();
		}

		@Override
		public void render(GuiGraphics guiGraphics, int entryIndex, int yOrigin, int xOrigin, int width,
				int height, int xMouse, int yMouse, boolean hovered, float p_281423_) {
			super.render(guiGraphics, entryIndex, yOrigin, xOrigin, width, height, xMouse, yMouse, hovered, p_281423_);


			Component statusText = null;
			ResourceLocation statusIcon = PINGING_1_SPRITE;
			switch (this.status) {
			case INITIAL:
				break;
			case PINGING:
				int j1 = (int) (Util.getMillis() / 100L + entryIndex * 2 & 7L);
				if (j1 > 4) {
					j1 = 8 - j1;
				}
				statusIcon = switch (j1) {
				case 1 -> PINGING_2_SPRITE;
				case 2 -> PINGING_3_SPRITE;
				case 3 -> PINGING_4_SPRITE;
				case 4 -> PINGING_5_SPRITE;
				default -> PINGING_1_SPRITE;
				};
				statusText = PINGING_STATUS;
				break;
			case SUCCESSFUL:
				if (this.ping < 50L) {
					statusIcon = PING_5_SPRITE;
				} else if (this.ping < 100L) {
					statusIcon = PING_4_SPRITE;
				} else if (this.ping < 150L) {
					statusIcon = PING_3_SPRITE;
				} else if (this.ping < 200L) {
					statusIcon = PING_2_SPRITE;
				} else {
					statusIcon = PING_1_SPRITE;
				}
				statusText = Component.translatable("multiplayer.status.ping", this.ping);
				break;
			case UNREACHABLE:
				statusIcon = UNREACHABLE_SPRITE;
				statusText = UNREACHABLE;
				break;
			}

			Minecraft minecraft = this.getMinecraft();
			if (this.tunnel != null) {
				int k1 = xOrigin + width - 15;
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, statusIcon, k1, yOrigin, 10, 8);

				if (statusText != null) {
					int j = minecraft.font.width(statusText);
					int k = k1 - j - 5;
					guiGraphics.drawString(minecraft.font, statusText, k, yOrigin + 1, 0xFF808080);
				}
			}

//			if (statusText != null && xMouse >= k1 && xMouse <= k1 + 10 && yMouse >= yOrigin
//					&& yMouse <= yOrigin + 8) {
//				guiGraphics.setTooltipForNextFrame(Component.literal("QwQ"), xMouse, yMouse);
//			}
		}
	}

	private void parseFetchResult(List<FetchedTunnel> tunnels, Throwable err) {
		this.options.clear();

		if (err == null) {
			for (FetchedTunnel tunnel: tunnels) {
				Option option = new Option(this.getSelectionList());
				option.tunnel = tunnel;
				option.lines.add(GeneralSelectionList.OptionText.of(tunnel.name()));
				option.lines.add(GeneralSelectionList.OptionText.of(tunnel.description()));
				option.lines.add(GeneralSelectionList.OptionText.of(tunnel.hostname()));
				this.options.add(option);

				final String hostName = tunnel.hostname();
				option.status = Status.PINGING;
				THREAD_POOL.submit(SakuraFrpClient.pingNode(hostName, tunnel.tcpingPort(), (ping) -> {
					this.minecraft.execute(() -> {
						option.ping = ping;
						option.status = ping < 0 ? Status.UNREACHABLE : Status.SUCCESSFUL;
					});
				}));
			}

			if (this.tunnelType.tunnelManagementUrl != null) {
				Option status = new Option(this.getSelectionList());
				status.tunnel = null;
				status.lines.add(GeneralSelectionList.OptionText.translatable("mcwifipnp.tunnelSelection.cantFindTunnelHelp"));
				status.lines.add(GeneralSelectionList.OptionText.translatable("mcwifipnp.tunnelSelection.cantFindTunnelSuggestion"));
				status.lines.add(new GeneralSelectionList.OptionText(
						Component.literal(this.tunnelType.tunnelManagementUrl),
						0xFF0000FF,
						(dummy) -> this.openTunnelManageLink()
					));
				this.options.add(status);
			}
		} else if (err instanceof InvalidAuthException) {
			Option status = new Option(this.getSelectionList());
			status.tunnel = null;
			status.lines.add(new GeneralSelectionList.OptionText(
					Component.translatable("mcwifipnp.tunnelSelection.authFailed"), 0xFFFF0000, null));
			this.options.add(status);
		} else {
			Option status = new Option(this.getSelectionList());
			status.tunnel = null;
			status.lines.add(new GeneralSelectionList.OptionText(
					Component.translatable("mcwifipnp.tunnelSelection.fetchException"), 0xFFFF0000, null));
			status.lines.add(GeneralSelectionList.OptionText.of(err.getMessage()));
			this.options.add(status);
		}

		this.getSelectionList().refreshEntries();
		this.fetchState = FetchState.IDLE;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int xMouse, int yMouse, float p_283431_) {
		switch (this.fetchState) {
		case INIT:
			SakuraFrpClient.fetchTunnels().whenComplete((tunnels, err) -> {
				// Make sure we interpret the result from the main thread.
				this.minecraft.execute(() -> parseFetchResult(tunnels, err));
			});

			this.fetchState = FetchState.FETCHING;
			break;
		case FETCHING:
			break;
		case IDLE:
			break;
		default:
			break;
		}

		super.render(guiGraphics, xMouse, yMouse, p_283431_);
	}
}
