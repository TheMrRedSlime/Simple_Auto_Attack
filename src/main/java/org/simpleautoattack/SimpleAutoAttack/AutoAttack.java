package org.simpleautoattack.SimpleAutoAttack;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoAttack implements ClientModInitializer {
    private static AutoAttackConfig config;

    @Override
    public void onInitializeClient() {
        // Register config
        AutoConfig.register(AutoAttackConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();

        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (config.enabled) {  // Only run if enabled in config
                AutoMeleeTick(client);
            }
        });
    }

    // Add a static getter for the config
    public static AutoAttackConfig getConfig() {
        return config;
    }

    private void AutoMeleeTick(MinecraftClient mc) {
        if (!mc.options.keyAttack.isPressed() || mc.player == null || mc.world == null || mc.interactionManager == null
                || !(mc.player.getAttackCooldownProgress(0) >= 1) || (Math.random() < 0.2 && config.bypass)) {
            return;
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            if (config.alwaysAttack) {
                mc.player.resetLastAttackedTicks();
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState blockState = mc.world.getBlockState(blockPos);

            if (blockState.getCollisionShape(mc.world, blockPos).isEmpty() || blockState.getHardness(mc.world, blockPos) == 0.0F) {
                float reach = (float) (mc.player.isCreative() ? 4.5 : (2.7 + Math.random() * 0.3));
                Vec3d camera = mc.player.getCameraPosVec(1.0F);
                Vec3d rotation = mc.player.getRotationVec(1.0F);
                Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
                EntityHitResult result = ProjectileUtil.raycast(mc.player, camera, end, new Box(camera, end),
                        e -> !e.isSpectator() && e.isAttackable(), reach * reach);
                if (result != null && result.getEntity().isAlive()) {
                    mc.interactionManager.attackEntity(mc.player, result.getEntity());
                    mc.player.swingHand(Hand.MAIN_HAND);
                    if (config.bypass){
                        reach = (float) (mc.player.isCreative() ? 4.5 : (2.7 + Math.random() * 0.3));
                    }
                }
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (entity.isAlive() && entity.isAttackable()) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
