package io.github.satxm.mcwifipnp.client;

import io.github.satxm.mcwifipnp.revprox.TunnelList;
import io.github.satxm.mcwifipnp.revprox.TunnelType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TunnelScreen extends Screen {
	private static final Component TITLE = Component.translatable("mcwifipnp.tunnelScreen.title");
	private static final Component STATUS = Component.translatable("mcwifipnp.tunnelScreen.status");
	private static final Component ENABLED = Component.translatable("mcwifipnp.tunnelScreen.enabled");
	private static final Component DISABLED = Component.translatable("mcwifipnp.tunnelScreen.disabled");
	private static final Component COPY_URL = Component.translatable("mcwifipnp.tunnelScreen.copyURL");
	private static final Component ADD = Component.translatable("mcwifipnp.tunnelScreen.add");

	protected final Screen lastScreen;
	public final TunnelList tunnels = new TunnelList();

	// Instances
	private TunnelSelectionList proxyList;
	private Button editButton, copyUrlButton;
	private CycleButton<Boolean> enableButton;
	private Button deleteButton;

	// Internal States
	private boolean initedOnce;

	public TunnelScreen(Screen lastScreen) {
		super(TITLE);
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		TunnelType.uploadIcons(this.minecraft.getTextureManager());

		if (this.initedOnce) {
			this.proxyList.setRectangle(this.width, this.height - 64 - 32, 0, 32);
		} else {
			this.initedOnce = true;
			this.proxyList = new TunnelSelectionList(this, this.minecraft,
				this.width, this.height - 64 - 32, 32, 36);
			this.proxyList.refreshEntries();
		}

		this.addRenderableWidget(this.proxyList);

		this.enableButton = this.addRenderableWidget(
				CycleButton.booleanBuilder(ENABLED, DISABLED)
				.create(STATUS, (cycleButton, newValue) -> {
					TunnelSelectionList.Entry selected = this.proxyList.getSelected();
					if (selected == null)
						return;

					this.proxyList.onEnableToggled(selected);
					this.onSelectedChange();
				})
		);
		this.enableButton.setWidth(100);

		this.copyUrlButton = this.addRenderableWidget(Button.builder(COPY_URL, p_293604_ -> {
		}).width(100).build());

		Button addButton = this.addRenderableWidget(Button.builder(ADD, p_293603_ -> {
			this.minecraft.setScreen(new EditTunnelScreen(this, null));
		}).width(100).build());

		this.editButton = this
				.addRenderableWidget(Button.builder(Component.translatable("selectServer.edit"), p_99715_ -> {
					TunnelSelectionList.Entry selected = this.proxyList.getSelected();
					if (selected == null)
						return;

					this.proxyList.onEdit(selected);
				}).width(74).build());

		this.deleteButton = this.addRenderableWidget(
			Button.builder(Component.translatable("selectServer.delete"), p_99710_ -> {

				}).width(74).build());

		Button refreshButton = this.addRenderableWidget(
			Button.builder(Component.translatable("selectServer.refresh"),
				p_99706_ -> {}
			).width(74).build());

		Button backButton = this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_BACK, p_315824_ -> this.onClose()).width(74).build());

		LinearLayout linearlayout = LinearLayout.vertical();
		EqualSpacingLayout equalspacinglayout = linearlayout
				.addChild(new EqualSpacingLayout(308, 20, EqualSpacingLayout.Orientation.HORIZONTAL));
		equalspacinglayout.addChild(this.enableButton);
		equalspacinglayout.addChild(this.copyUrlButton);
		equalspacinglayout.addChild(addButton);
		linearlayout.addChild(SpacerElement.height(4));
		EqualSpacingLayout equalspacinglayout1 = linearlayout.addChild(
			new EqualSpacingLayout(308, 20, EqualSpacingLayout.Orientation.HORIZONTAL)
		);
		equalspacinglayout1.addChild(this.editButton);
		equalspacinglayout1.addChild(this.deleteButton);
		equalspacinglayout1.addChild(refreshButton);
		equalspacinglayout1.addChild(backButton);
		linearlayout.arrangeElements();
		FrameLayout.centerInRectangle(linearlayout, 0, this.height - 64, this.width, 64);
		this.onSelectedChange();
	}

	@Override
	public void onClose() {
		super.onClose();
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	public void render(GuiGraphics p_281617_, int p_281629_, int p_281983_, float p_283431_) {
		super.render(p_281617_, p_281629_, p_281983_, p_283431_);
		p_281617_.drawCenteredString(this.font, this.title, this.width / 2, 20, -1);
	}

	public void onSelectedChange() {
		TunnelSelectionList.Entry selected = this.proxyList.getSelected();
		boolean allowAction = selected != null;
		this.enableButton.active = allowAction;
		this.copyUrlButton.active = allowAction;
		this.editButton.active = allowAction;
		this.deleteButton.active = allowAction;

		if (selected == null)
			return;

		this.enableButton.setValue(selected.tunnelData.enabled);
	}
}
