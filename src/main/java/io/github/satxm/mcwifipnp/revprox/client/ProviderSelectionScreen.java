package io.github.satxm.mcwifipnp.revprox.client;

import java.util.function.Consumer;

import io.github.satxm.mcwifipnp.revprox.TunnelType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ProviderSelectionScreen extends GeneralSelectionScreen {
	private Button okButton, linkButton;

	public ProviderSelectionScreen(Screen lastScreen) {
		super(lastScreen, Component.translatable("mcwifipnp.providerScreen.title"), 40);
	}

	private void onLinkButtonClicked(Button button) {
		GeneralSelectionList.EntryBase selected = this.getSelectionList().getSelected();
		if (selected instanceof GeneralSelectionList.Option option) {
			TunnelType tunnelType = (TunnelType) option.userData;
			this.openLink(tunnelType.url);
		}
	}

	private void onHelpButtonClicked(Button button) {

	}

	private void openLink(String url) {
		ConfirmLinkScreen.confirmLinkNow(this, url, true);
	}

	@Override
	protected void initFooter() {
		this.okButton = this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_PROCEED, btn -> this.onListConfirmed()).width(74).build());
		this.linkButton = this.addRenderableWidget(
				Button.builder(Component.translatable("mcwifipnp.providerScreen.openLink"), this::onLinkButtonClicked).width(74).build());
		Button helpButton = this.addRenderableWidget(
				Button.builder(Component.translatable("mcwifipnp.providerScreen.help"), this::onHelpButtonClicked).width(74).build());
		Button backButton = this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_CANCEL, p_315824_ -> this.onClose()).width(74).build());

		EqualSpacingLayout esl = new EqualSpacingLayout(308, 20, EqualSpacingLayout.Orientation.HORIZONTAL);
		esl.addChild(this.okButton);
		esl.addChild(this.linkButton);
		esl.addChild(helpButton);
		esl.addChild(backButton);
		esl.arrangeElements();
		FrameLayout.centerInRectangle(esl, 0, this.height - 30, this.width, 20);
		return;
	}

	@Override
	protected void populateOptions(Consumer<GeneralSelectionList.EntryBase> newOption) {
		TunnelType.foreach((tunnelType) -> {
			GeneralSelectionList.Option entry = this.getSelectionList().new Option(tunnelType.getIcon());
			entry.lines.add(new GeneralSelectionList.OptionText(tunnelType.getDisplayName(), 0xFFFFFFFF, null));
			entry.lines.add(new GeneralSelectionList.OptionText(Component.empty(), 0xFFFFFFFF, null));
			entry.lines.add(new GeneralSelectionList.OptionText(
					Component.literal(tunnelType.url),
					0xFF0000FF,
					(dummy) -> this.openLink(tunnelType.url)
				));
			entry.userData = tunnelType;
			newOption.accept(entry);
		});
	}

	@Override
	public void onSelectedChange() {
		GeneralSelectionList.EntryBase selected = this.getSelectionList().getSelected();
		boolean allowAction = selected instanceof GeneralSelectionList.Option;
		this.okButton.active = allowAction;
		this.linkButton.active = allowAction;
	}

	@Override
	public void onListConfirmed() {
		this.minecraft.setScreen(new NodeSelectionScreen(this, this.lastScreen));
	}
}
