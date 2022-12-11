package io.github.satxm.mcwifipnp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import io.github.satxm.mcwifipnp.SetCommandsAllowed;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;

@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin implements SetCommandsAllowed {
    @Shadow
    private LevelSettings settings;

    @Unique
    @Override
    public void setCommandsAllowed(boolean allowCommands) {
        this.settings = new LevelSettings(settings.levelName(), settings.gameType(), settings.hardcore(),
                settings.difficulty(), allowCommands, settings.gameRules(), settings.getDataPackConfig());
    }
}
