package io.github.satxm.mcwifipnp.revprox.client;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GeneralSelectionScreen extends Screen {
	protected final Screen lastScreen;
	protected final int footerHeight;

	// Instances
	private GeneralSelectionList selectionList;

	// Internal States
	private boolean initedOnce;

	public GeneralSelectionScreen(Screen lastScreen, Component title, int footerHeight) {
		super(title);
		this.lastScreen = lastScreen;
		this.footerHeight = footerHeight;
	}

	@Override
	protected void init() {
		if (this.initedOnce) {
			this.selectionList.setRectangle(this.width, this.height - this.footerHeight - 32, 0, 32);
		} else {
			this.initedOnce = true;
			this.selectionList = new GeneralSelectionList(this, this.minecraft,
				this.width, this.height - this.footerHeight - 32, 32, 36);
			this.selectionList.refreshEntries();
		}

		this.addRenderableWidget(this.selectionList);
		this.initFooter();
		this.onSelectedChange();
	}

	@Override
	public void onClose() {
		super.onClose();
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int xMouse, int yMouse, float p_283431_) {
		super.render(guiGraphics, xMouse, yMouse, p_283431_);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, -1);
	}

	protected final GeneralSelectionList getSelectionList() {
		return this.selectionList;
	}

	protected abstract void initFooter();
	protected abstract void populateOptions(Consumer<GeneralSelectionList.EntryBase> newOption);
	public abstract void onSelectedChange();
	public abstract void onListConfirmed();
}
