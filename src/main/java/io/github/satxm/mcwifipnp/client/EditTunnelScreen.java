package io.github.satxm.mcwifipnp.client;

import javax.annotation.Nullable;

import io.github.satxm.mcwifipnp.revprox.TunnelData;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class EditTunnelScreen extends Screen {
	private static final Component TITLE_ADD = Component.literal("添加隧道");
	private static final Component TITLE_EDIT = Component.literal("编辑隧道");

	protected final Screen lastScreen;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	@Nullable
	public TunnelData tunnelData;

	@Nullable
	private Button doneButton;

	protected EditTunnelScreen(Screen lastScreen, @Nullable TunnelData tunnelData) {
		super(tunnelData == null ? TITLE_ADD : TITLE_EDIT);
		this.lastScreen = lastScreen;
		this.tunnelData = tunnelData;
	}

	private void onButtonClicked(Button button) {

	}

	private void onDoneButtonClicked(Button button) {

	}

	@Override
	protected void init() {
		this.layout.addTitleHeader(this.tunnelData == null ? TITLE_ADD : TITLE_EDIT, this.font);

		OptionsList optionList = new OptionsList(this.minecraft, this.width, this.layout, this, this.font);
		Button btn1 = Button.builder(Component.literal("btn1"), this::onButtonClicked).build();
		Button btn2 = Button.builder(Component.literal("btn2"), this::onButtonClicked).build();
		Button btn3 = Button.builder(Component.literal("btn3"), this::onButtonClicked).build();
		Button btn4 = Button.builder(Component.literal("btn4"), this::onButtonClicked).build();
		Button btn5 = Button.builder(Component.literal("btn5"), this::onButtonClicked).build();
		Button btn6 = Button.builder(Component.literal("btn6"), this::onButtonClicked).build();
		Button btn7 = Button.builder(Component.literal("btn7"), this::onButtonClicked).build();
		Button btn8 = Button.builder(Component.literal("btn8"), this::onButtonClicked).build();
		optionList.add(btn1);
		optionList.add(btn2);
		optionList.add(btn3);
		optionList.add(btn4);
		optionList.add(btn5);
		optionList.add(btn6);
		optionList.add(btn7);
		optionList.add(btn8);

		this.layout.addToContents(optionList);
		LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.doneButton = linearlayout
				.addChild(Button.builder(CommonComponents.GUI_DONE, this::onDoneButtonClicked).build());
		linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
	}

	@Override
	public void onClose() {
		super.onClose();
		this.minecraft.setScreen(this.lastScreen);
	}
}
