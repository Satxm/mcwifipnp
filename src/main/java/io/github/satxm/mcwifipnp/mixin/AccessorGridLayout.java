package io.github.satxm.mcwifipnp.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;

@Mixin(GridLayout.class)
public interface AccessorGridLayout {
	@Accessor
	List<LayoutElement> getChildren();
}