package io.github.satxm.mcwifipnp.client;

import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import io.github.satxm.mcwifipnp.revprox.TunnelData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TunnelSelectionList extends ObjectSelectionList<TunnelSelectionList.Entry> {
	// Sprite
	private static final ResourceLocation EDIT_HIGHLIGHTED_SPRITE =
			ResourceLocation.withDefaultNamespace("server_list/join_highlighted");
	private static final ResourceLocation EDIT_SPRITE =
			ResourceLocation.withDefaultNamespace("server_list/join");
	private static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE =
			ResourceLocation.withDefaultNamespace("server_list/move_up_highlighted");
	private static final ResourceLocation MOVE_UP_SPRITE =
			ResourceLocation.withDefaultNamespace("server_list/move_up");
	private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE =
			ResourceLocation.withDefaultNamespace("server_list/move_down_highlighted");
	private static final ResourceLocation MOVE_DOWN_SPRITE =
			ResourceLocation.withDefaultNamespace("server_list/move_down");
	private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE =
			ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
	private static final ResourceLocation CHECKBOX_SELECTED_SPRITE =
			ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
	private static final ResourceLocation CHECKBOX_HIGHLIGHTED_SPRITE =
			ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
	private static final ResourceLocation CHECKBOX_SPRITE =
			ResourceLocation.withDefaultNamespace("widget/checkbox");

	public final TunnelScreen owner;

	public TunnelSelectionList(TunnelScreen owner, Minecraft minecraft, int width, int height, int y, int itemHeight) {
		super(minecraft, width, height, y, itemHeight);
		this.owner = owner;
	}

	@Override
	public int getRowWidth() {
		return 305;
	}

	public void refreshEntries() {
		this.clearEntries();
		this.owner.tunnels.forEach(tunnelData -> this.addEntry(new Entry(tunnelData)));
	}

	@Override
	public boolean keyPressed(int keyCode, int p_99783_, int p_99784_) {
		TunnelSelectionList.Entry entry = this.getSelected();
		return entry != null && entry.keyPressed(keyCode, p_99783_, p_99784_)
				|| super.keyPressed(keyCode, p_99783_, p_99784_);
	}

	// Actions
	protected void swap(int from, int to) {
		this.owner.tunnels.swap(from, to);
		this.refreshEntries();
		Entry entry = this.getEntry(to);
		this.setSelected(entry);
		this.ensureVisible(entry);
	}

	private void onEntryDoubleClicked(Entry entry) {
		System.out.println("onEntryDoubleClicked " + entry.tunnelData.name);
	}

	public void onEdit(Entry entry) {
		System.out.println("Edit " + entry.tunnelData.name);
	}

	public void onEnableToggled(Entry entry) {
		System.out.println("onEnableToggled " + entry.tunnelData.name);
		entry.tunnelData.enabled = !entry.tunnelData.enabled;
	}

	/**
	 * Represent a virtual widget in each entry
	 */
	private static abstract class VirtualWidget {
		private final Rectangle clickable, iconArea;
		private final boolean showOnlyIfEntrySelected;

		/**
		 * @param clickable define the area that responds to mouse clicks
		 * @param iconArea define the area to draw the sprite. Default to clickable if set to null.
		 * @param showOnlyIfEntrySelected if true, this widget will only be visible when 1) cursor is hovering 2) in touch screen mode
		 */
		protected VirtualWidget(Rectangle clickable, @Nullable Rectangle iconArea, boolean showOnlyIfEntrySelected) {
			this.clickable = clickable;
			this.iconArea = iconArea == null ? clickable : iconArea;
			this.showOnlyIfEntrySelected = showOnlyIfEntrySelected;
		}

		/**
		 * @param xMouseRelative mouse x relative to the top-left of the entry
		 * @param yMouseRelative mouse y relative to the top-left of the entry
		 * @return
		 */
		protected boolean bounded(int xMouseRelative, int yMouseRelative) {
			return this.clickable.contains(xMouseRelative, yMouseRelative);
		}

		/**
		 * @param guiGraphics
		 * @param xOrigin absolute x reference
		 * @param yOrigin absolute y reference
		 * @param xMouse absolute x mouse position
		 * @param yMouse absolute y mouse position
		 * @param selected
		 */
		protected void draw(GuiGraphics guiGraphics, int xOrigin, int yOrigin, int xMouse, int yMouse, boolean selected) {
			if (this.showOnlyIfEntrySelected && !selected)
				return;

			if (!this.isVisible())
				return;

			Rectangle iconArea = this.iconArea == null ? this.clickable : this.iconArea;
			boolean bounded = this.bounded(xMouse - xOrigin, yMouse - yOrigin);
			ResourceLocation resloc = this.mapStateToSprite(bounded);
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resloc,
					xOrigin + iconArea.x, yOrigin + iconArea.y, iconArea.width, iconArea.height);
		}

		protected boolean handleMouseClick(double xMouseRelative, double yMouseRelative) {
			if (!this.isVisible())
				return false;

			if (this.bounded((int) xMouseRelative, (int) yMouseRelative)) {
				this.onClick();
				return true;
			} else {
				return false;
			}
		}

		protected boolean isVisible() {
			return true;
		}

		protected abstract ResourceLocation mapStateToSprite(boolean mouseOver);
		protected abstract void onClick();
	}

	public class Entry extends ObjectSelectionList.Entry<TunnelSelectionList.Entry> {
		private final List<VirtualWidget> widgets = new LinkedList<>();
		public final TunnelData tunnelData;

		// Internal State
		private long lastClickTime;

		public Entry(TunnelData tunnelData) {
			this.tunnelData = tunnelData;

			// Checkbox
			VirtualWidget checkbox = new VirtualWidget(
					new Rectangle(2, 6, 20, 20),
					null,
					false) {
				@Override
				protected ResourceLocation mapStateToSprite(boolean mouseOver) {
					if (Entry.this.tunnelData.enabled) {
						return mouseOver ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
					} else {
						return mouseOver ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
					}
				}

				@Override
				protected void onClick() {
					onEnableToggled(Entry.this);
				}
			};
			widgets.add(checkbox);

			// Move up
			VirtualWidget moveUp = new VirtualWidget(
					new Rectangle(26, 0, 16, 16),
					new Rectangle(26, 0, 32, 32),
					true) {
				@Override
				protected boolean isVisible() {
					int iEntry = TunnelSelectionList.this.children().indexOf(Entry.this);
					return iEntry > 0;
				}

				@Override
				protected ResourceLocation mapStateToSprite(boolean mouseOver) {
					return mouseOver ? MOVE_UP_HIGHLIGHTED_SPRITE : MOVE_UP_SPRITE;
				}

				@Override
				protected void onClick() {
					int i = TunnelSelectionList.this.children().indexOf(Entry.this);
					TunnelSelectionList.this.swap(i, i - 1);
				}
			};
			widgets.add(moveUp);

			// Move down
			VirtualWidget moveDown = new VirtualWidget(
					new Rectangle(26, 16, 16, 16),
					new Rectangle(26, 0, 32, 32),
					true) {
				@Override
				protected boolean isVisible() {
					int iEntry = TunnelSelectionList.this.children().indexOf(Entry.this);
					return iEntry < TunnelSelectionList.this.getItemCount() - 1;
				}

				@Override
				protected ResourceLocation mapStateToSprite(boolean mouseOver) {
					return mouseOver ? MOVE_DOWN_HIGHLIGHTED_SPRITE : MOVE_DOWN_SPRITE;
				}

				@Override
				protected void onClick() {
					int i = TunnelSelectionList.this.children().indexOf(Entry.this);
					TunnelSelectionList.this.swap(i, i + 1);
				}
			};
			widgets.add(moveDown);

			// Edit
			VirtualWidget edit = new VirtualWidget(
					new Rectangle(26 + 16, 0, 16, 32),
					new Rectangle(26, 0, 32, 32),
					true) {
				@Override
				protected ResourceLocation mapStateToSprite(boolean mouseOver) {
					return mouseOver ? EDIT_HIGHLIGHTED_SPRITE : EDIT_SPRITE;
				}

				@Override
				protected void onClick() {
					TunnelSelectionList.this.onEdit(Entry.this);
				}
			};
			widgets.add(edit);
		}

		@Override
		public Component getNarration() {
			return Component.literal("114514");
		}

		@Override
		public void render(GuiGraphics guiGraphics, int entryIndex, int yOrigin, int xOrigin, int width,
				int height, int xMouse, int yMouse, boolean hovered, float p_281423_) {
			int xIcon = xOrigin + 26;
			int xString = xOrigin + 26 + 32 + 3;

			// Draw tunnel name
			guiGraphics.drawString(TunnelSelectionList.this.minecraft.font, this.tunnelData.name, xString,
					yOrigin + 1, -1);

			// Draw tunnel description
			String line2 = "[" + this.tunnelData.name + "] " + this.tunnelData.desc;
			guiGraphics.drawString(TunnelSelectionList.this.minecraft.font, Component.literal(line2), xString, yOrigin + 12, 0xFF808080);

			// Draw Hostname
			String line3 = this.tunnelData.host;
			guiGraphics.drawString(TunnelSelectionList.this.minecraft.font, Component.literal(line3), xString, yOrigin + 12 + 9, 0xFF808080);

			// Draw tunnel icon
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.tunnelData.tunnelType.getIcon(), xIcon, yOrigin, 0.0F, 0.0F, 32, 32, 32, 32);

			boolean focused = TunnelSelectionList.this.minecraft.options.touchscreen().get() || hovered;

			if (focused) {
				guiGraphics.fill(xOrigin, yOrigin, xOrigin + 32 + 26, yOrigin + 32, 0xA0909090);
			}
			widgets.forEach(clickableArea -> clickableArea.draw(guiGraphics, xOrigin, yOrigin, xMouse, yMouse, focused));
		}

		public boolean mouseClicked(double xMouse, double yMouse, int keyCode) {
			double xMouseRelative = xMouse - TunnelSelectionList.this.getRowLeft();
			double yMouseRelative = yMouse - TunnelSelectionList.this.getRowTop(TunnelSelectionList.this.children().indexOf(this));

			for (VirtualWidget widget: widgets) {
				if (widget.handleMouseClick(xMouseRelative, yMouseRelative)) {
					TunnelSelectionList.this.owner.setSelected(this);
					return true;
				}
			}

			TunnelSelectionList.this.owner.setSelected(this);
			if (Util.getMillis() - this.lastClickTime < 250L) {
				TunnelSelectionList.this.onEntryDoubleClicked(this);
				return true;
			}

			this.lastClickTime = Util.getMillis();
			return super.mouseClicked(xMouse, yMouse, keyCode);
		}

		@Override
		public boolean keyPressed(int keyCode, int p_99876_, int p_99877_) {
			if (Screen.hasShiftDown()) {
				int i = TunnelSelectionList.this.children().indexOf(this);
				if (i == -1) {
					return true;
				}

				if (keyCode == 264 && i < TunnelSelectionList.this.getItemCount() - 1 || keyCode == 265 && i > 0) {
					TunnelSelectionList.this.swap(i, keyCode == 264 ? i + 1 : i - 1);
					return true;
				}
			}

			if (32 == keyCode) { // Space
				TunnelSelectionList.this.onEnableToggled(this);
				return true;
			} else if (257 == keyCode || 335== keyCode) { // Enter
				TunnelSelectionList.this.onEdit(this);
				return true;
			}

			return super.keyPressed(keyCode, p_99876_, p_99877_);
		}

	}
}
