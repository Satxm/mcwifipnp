package io.github.satxm.mcwifipnp.revprox.client;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;

import io.github.satxm.mcwifipnp.revprox.FetchedTunnel;
import io.github.satxm.mcwifipnp.revprox.SakuraFrpClient;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class NodeSelectionScreen extends GeneralSelectionScreen {
	static final Logger LOGGER = LogUtils.getLogger();
	static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(5,
			new ThreadFactoryBuilder()
			.setNameFormat("Server Pinger #%d")
			.setDaemon(true)
			.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
			.build()
		);

	private final List<GeneralSelectionList.Option> options = new LinkedList<>();

	private static enum FetchState {
		INIT,
		FETCHING,
		IDLE,
	}
	private FetchState fetchState = FetchState.INIT;

	private final Screen rootScreen;
	private Button okButton;

	public NodeSelectionScreen(Screen lastScreen, Screen rootScreen) {
		super(lastScreen, Component.translatable("mcwifipnp.nodeScreen.title"), 40);
		this.rootScreen = rootScreen;
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

	private void parseFetchResult(List<FetchedTunnel> tunnels) {
		Function<String, GeneralSelectionList.Option> newOption = (text) -> {
			GeneralSelectionList.Option option = this.getSelectionList().new Option(null);
			option.lines.add(GeneralSelectionList.OptionText.of(text));
			return option;
		};

		this.options.clear();

		for (FetchedTunnel tunnel: tunnels) {
			GeneralSelectionList.Option option = this.getSelectionList().new Option(null);
			option.lines.add(GeneralSelectionList.OptionText.of(tunnel.name()));
			option.lines.add(GeneralSelectionList.OptionText.of(tunnel.description()));
			option.lines.add(GeneralSelectionList.OptionText.of(tunnel.hostname()));
			this.options.add(option);
		}

		this.getSelectionList().refreshEntries();
		this.fetchState = FetchState.IDLE;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int xMouse, int yMouse, float p_283431_) {
		switch (this.fetchState) {
		case INIT:
			SakuraFrpClient.fetchTunnels().whenComplete((json, err) -> {
				this.minecraft.execute(() -> parseFetchResult(json));
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
