package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.ShareToLanScreenNew;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;
import java.util.List;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {
    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout;visitWidgets(Ljava/util/function/Consumer;)V"), method = "createPauseMenu", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void buttonOverride(CallbackInfo ci, GridLayout gridlayout, GridLayout.RowHelper helper) {
        if (gridlayout != null) {
            final List<LayoutElement> buttons = ((AccessorGridLayout) gridlayout).getChildren();
            for (int i = 0; i < buttons.size(); i++) {
                LayoutElement widget = buttons.get(i);
                boolean isShareToLan = this.buttonHasText(widget, "menu.shareToLan");
                if (widget instanceof Button button) {
                    if (isShareToLan) {
                        Button newButton = Button.builder(Component.translatable("menu.shareToLan"), b -> {
                            this.minecraft.setScreen(new ShareToLanScreenNew(this));
                        }).bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();
                        newButton.active = this.minecraft.hasSingleplayerServer() && !this.minecraft.getSingleplayerServer().isPublished();
                        buttons.set(i,newButton);
                    }
                }

            }
        }
    }

    private static boolean buttonHasText(LayoutElement element, String... translationKeys) {
        if (element instanceof Button button) {
            Component component = button.getMessage();
            ComponentContents textContent = component.getContents();

            return textContent instanceof TranslatableContents && Arrays.stream(translationKeys)
                    .anyMatch(s -> ((TranslatableContents) textContent).getKey().equals(s));
        }
        return false;
    }
}