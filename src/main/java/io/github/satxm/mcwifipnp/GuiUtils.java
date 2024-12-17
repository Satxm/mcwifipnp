package io.github.satxm.mcwifipnp;

import java.util.List;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public class GuiUtils {
	public static <T extends AbstractWidget> T findWidget(List<?> list, Class<T> cls, String vanillaLangKey) {
		for (Object child : list) {
			if (!(child instanceof AbstractWidget widget))
				continue;

			// We only look for AbstractWidget
			if (cls.isAssignableFrom(widget.getClass())) {
				Component component = widget.getMessage();
				if (component.getContents() instanceof TranslatableContents) {
					TranslatableContents content = (TranslatableContents) component.getContents();
					if (content.getKey().equals(vanillaLangKey)) {
						return (T) widget;
					} else {
						Object[] args = content.getArgs();
						if (args.length == 0)
							continue;

						if (!(args[0] instanceof MutableComponent))
							continue;

						MutableComponent mutableComponent = (MutableComponent) args[0];
						if (!(component.getContents() instanceof TranslatableContents))
							continue;

						if (!(mutableComponent.getContents() instanceof TranslatableContents))
							continue;

						content = (TranslatableContents) mutableComponent.getContents();
						if (content.getKey().equals(vanillaLangKey)) {
							return (T) widget;
						}
					}
				}
			}
		}

		return null;
	}
}
