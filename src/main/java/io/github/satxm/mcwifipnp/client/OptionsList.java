package io.github.satxm.mcwifipnp.client;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OptionsList extends ContainerObjectSelectionList<OptionsList.Entry> {
	public final static int COLUMN_WIDTH = 150;
	public final static int GAP = 10;

	public final Screen screen;
	public final Font font;

	public OptionsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout, Screen screen, Font font) {
		super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 25);
		this.centerListVertically = false;
		this.screen = screen;
		this.font = font;
	}

	public void add(AbstractWidget widgets) {
		this.add(widgets, null);
	}

	public void add(AbstractWidget left, @Nullable AbstractWidget right) {
		this.addEntry(new OptionsList.Entry(left, right));
	}

	public void add(Component labelLeft, AbstractWidget left) {
		this.addEntry(new OptionsList.LabeledEntry(labelLeft, left, null, null));
	}

	public void add(Component labelLeft, AbstractWidget left, Component labelRight, AbstractWidget right) {
		this.addEntry(new OptionsList.LabeledEntry(labelLeft, left, labelRight, right));
	}

	@Override
	public int getRowWidth() {
		return 340;
	}

	/**
	 * One row with two widgets.
	 * If there is only one widget, it and its label will use the entire row.
	 */
	protected class Entry extends ContainerObjectSelectionList.Entry<OptionsList.Entry> {
		protected final List<AbstractWidget> children;
		protected static final int X_OFFSET = 160;

		Entry(AbstractWidget left, @Nullable AbstractWidget right) {
			this.children = right == null ? ImmutableList.of(left) : ImmutableList.of(left, right);
		}

		@Override
		public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			int xStart = OptionsList.this.screen.width / 2 - COLUMN_WIDTH - GAP / 2;

			for (AbstractWidget abstractwidget : this.children) {
				abstractwidget.setPosition(xStart, j);
				abstractwidget.render(guiGraphics, n, o, f);
				xStart += COLUMN_WIDTH + GAP;
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.children;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return this.children;
		}
	}

	/**
	 * One row with two widgets and their labels.
	 * Labels are left aligned, widgets are right aligned.
	 * If there is only one widget, it and its label will use the entire row.
	 */
	protected class LabeledEntry extends Entry {
		protected final AbstractWidget left, right;
		protected final Component labelLeft, labelRight;

		LabeledEntry(Component labelLeft, AbstractWidget left, Component labelRight, AbstractWidget right) {
			super(left, right);
			this.left = left;
			this.right = right;
			this.labelLeft = labelLeft;
			this.labelRight = labelRight;
		}

		@Override
		public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			int xStart = OptionsList.this.screen.width / 2 - COLUMN_WIDTH - GAP / 2;

			guiGraphics.drawString(OptionsList.this.font, this.labelLeft, xStart,
					j + (this.left.getHeight() - 9) / 2, 16777215);

			if (this.right == null)
				xStart += COLUMN_WIDTH + GAP;

			this.left.setPosition(xStart + (COLUMN_WIDTH - this.left.getWidth()), j);
			this.left.render(guiGraphics, n, o, f);

			if (this.right == null)
				return;

			xStart += COLUMN_WIDTH + GAP;
			guiGraphics.drawString(OptionsList.this.font, this.labelRight, xStart,
					j + (this.right.getHeight() - 9) / 2, 16777215);
			this.right.setPosition(xStart + (COLUMN_WIDTH - this.right.getWidth()), j);
			this.right.render(guiGraphics, n, o, f);
		}
	}
}
