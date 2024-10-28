package io.github.satxm.mcwifipnp.mixin;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GridLayout.class)
public interface AccessorGridLayout {
    @Accessor
    List<LayoutElement> getChildren();
}