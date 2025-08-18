package io.github.satxm.mcwifipnp.revprox.client;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GeneralSelectionList extends ObjectSelectionList<GeneralSelectionList.EntryBase> {
	public final GeneralSelectionScreen owner;

	public GeneralSelectionList(GeneralSelectionScreen owner, Minecraft minecraft, int width, int height, int y, int itemHeight) {
		super(minecraft, width, height, y, itemHeight);

		this.owner = owner;
	}

	public final ResourceLocation iconDefault =
			ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");

	@Override
	public void setSelected(@Nullable EntryBase entry) {
		super.setSelected(entry);
		this.owner.onSelectedChange();
	}

	public void refreshEntries() {
		this.clearEntries();
		this.owner.populateOptions(this::addEntry);
	}

	@Override
	public int getRowWidth() {
		return 305;
	}

	public static abstract class EntryBase extends ObjectSelectionList.Entry<GeneralSelectionList.EntryBase> {

	}

	public class LoadingIndicator extends EntryBase {
		private final Component text;
		public LoadingIndicator(Component text) {
			this.text = text;
		}

		@Override
		public void render(GuiGraphics guiGraphics, int entryIndex, int yOrigin, int xOrigin, int width, int height,
				int xMouse, int yMouse, boolean hovered, float p_281811_) {
			Minecraft minecraft = GeneralSelectionList.this.minecraft;
			int i = yOrigin + height / 2 - 9 / 2;
			guiGraphics.drawString(minecraft.font, this.text,
					minecraft.screen.width / 2 - minecraft.font.width(this.text) / 2, i, -1);
			String s = LoadingDotsText.get(Util.getMillis());
			guiGraphics.drawString(minecraft.font, s, minecraft.screen.width / 2 - minecraft.font.width(s) / 2, i + 9,
					0xFF808080);
		}

		@Override
		public Component getNarration() {
			return this.text;
		}
	}

	public static record OptionText(Component text, int color, @Nullable Consumer<Option> onClicked) {
		public static OptionText of(String text) {
			return new OptionText(Component.literal(text), 0xFFFFFFFF, null);
		}
	}

	public class Option extends EntryBase {
		private final int[] Y_LINES = new int[] {1, 12, 12 + 9};
		public List<OptionText> lines = new LinkedList<>();

		public final ResourceLocation icon;
		public Object userData;

		// Internal State
		private long lastClickTime;

		public Option(@Nullable ResourceLocation icon) {
			this.icon = icon == null ? GeneralSelectionList.this.iconDefault : icon;
		}

		@Override
		public Component getNarration() {
			return Component.literal("114514");
		}

		@Override
		public void render(GuiGraphics guiGraphics, int entryIndex, int yOrigin, int xOrigin, int width,
				int height, int xMouse, int yMouse, boolean hovered, float p_281423_) {
			int xString = xOrigin + 32 + 3;

			Minecraft minecraft = GeneralSelectionList.this.minecraft;

			// Draw lines
			int i = 0;
			for (OptionText line: this.lines) {
				boolean isHyperLink = line.onClicked() != null;
				int yLine = Y_LINES[i];
				Component text = line.text();

				if (isHyperLink) {
					int xMouseRelative = xMouse - xOrigin;
					int yMouseRelative = yMouse - yOrigin;
					int textWidth = minecraft.font.width(text);
					if (xMouseRelative >= xString && xMouseRelative < xString + textWidth &&
							yMouseRelative >= yLine && yMouseRelative < yLine + minecraft.font.lineHeight) {
						text = text.copy().withStyle(style -> style.withUnderlined(true));
					}
				}

				guiGraphics.drawString(minecraft.font, text, xString, yOrigin + yLine, line.color());
				i++;
			}

			// Draw icon
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.icon, xOrigin, yOrigin, 0.0F, 0.0F, 32, 32, 32, 32);
		}

		@Override
		public boolean mouseClicked(double xMouse, double yMouse, int keyCode) {
			int xOrigin = GeneralSelectionList.this.getRowLeft();
			int yOrigin = GeneralSelectionList.this.getRowTop(GeneralSelectionList.this.children().indexOf(this));

			int xString = xOrigin + 32 + 3;

			// Handle hyperlink
			int i = 0;
			for (OptionText line: this.lines) {
				boolean isHyperLink = line.onClicked() != null;
				int yLine = Y_LINES[i];
				Component text = line.text();

				if (isHyperLink) {
					double xMouseRelative = xMouse - xOrigin;
					double yMouseRelative = yMouse - yOrigin;
					int textWidth = minecraft.font.width(text);
					if (xMouseRelative >= xString && xMouseRelative < xString + textWidth &&
							yMouseRelative >= yLine && yMouseRelative < yLine + minecraft.font.lineHeight) {
						line.onClicked().accept(this);
						return true;
					}
				}

				i++;
			}

			// Handle double click
			if (Util.getMillis() - this.lastClickTime < 250L) {
				GeneralSelectionList.this.owner.onListConfirmed();
				return true;
			}
			this.lastClickTime = Util.getMillis();

			return super.mouseClicked(xMouse, yMouse, keyCode);
		}

		@Override
		public boolean keyPressed(int keyCode, int p_99691_, int p_99692_) {
			if (CommonInputs.selected(keyCode)) {
				GeneralSelectionList.this.owner.onListConfirmed();
				return true;
			}

			return super.keyPressed(keyCode, p_99691_, p_99692_);
		}
	}
}
