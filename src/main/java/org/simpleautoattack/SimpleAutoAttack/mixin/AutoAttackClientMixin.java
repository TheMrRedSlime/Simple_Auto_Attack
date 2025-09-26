package org.simpleautoattack.SimpleAutoAttack.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;

//import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class AutoAttackClientMixin {
    @Shadow
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    public ClientPlayerEntity player;

    // Prevents breaking blocks when holding a weapon
    @Inject(method = "handleBlockBreaking(Z)V", at = @At("HEAD"), cancellable = true)
    public void onHandleBlockBreaking(boolean isBreakPressed, CallbackInfo info) {
        // Get config instance
        AutoAttackConfig config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();
        
        // Only prevent block breaking if the config option is enabled
        if (config.preventBlockBreaking && isBreakPressed && player != null) {
            ItemStack mainHandItem = player.getMainHandStack();
            String itemName = mainHandItem.getItem().toString().toLowerCase();

            // Use item tag for swords, string check for tridents
            if (itemName.contains("sword") || itemName.contains("Sword") || itemName.contains("trident")) {
                interactionManager.cancelBlockBreaking();
                info.cancel();
            }
        }
    }
}
