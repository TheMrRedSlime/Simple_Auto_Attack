package org.simpleautoattack.SimpleAutoAttack;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import org.lwjgl.glfw.GLFW;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
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
    public boolean AFKMode = false;
    final KeyBinding AFKKeybind = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "org.simpleautoattack.AFK_Key",
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_K,             
                "org.simpleautoattack.AttackKeys"
            )
        );

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

        if(AFKKeybind.wasPressed() && mc.player != null){
            AFKMode = !AFKMode;
        }

        if ((!mc.options.keyAttack.isPressed() && !AFKMode) || mc.player == null || mc.world == null || mc.interactionManager == null || !(mc.player.getAttackCooldownProgress(0) >= 1)) return;

        if (Math.random() < 0.2 && config.limit) return;

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
                float reach = (float) (mc.player.isCreative() ? 4.5 : (2.7 + (Math.random() * 0.3)));
                Vec3d camera = mc.player.getCameraPosVec(1.0F);
                Vec3d rotation = mc.player.getRotationVec(1.0F);
                Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
                EntityHitResult result = ProjectileUtil.raycast(mc.player, camera, end, new Box(camera, end),
                        e -> !e.isSpectator() && e.isAttackable(), reach * reach);
                if (result != null && result.getEntity().isAlive()) {
                    Entity entity = result.getEntity();
                    Vec3d hitPos = result.getPos();
                    Box entityBox = entity.getBoundingBox();
                    
                    double relX = (hitPos.x - entityBox.minX) / entityBox.getXLength();
                    double relZ = (hitPos.z - entityBox.minZ) / entityBox.getZLength();
                    
                    double margin = 0.15;
                    if (relX >= margin && relX <= (1.0 - margin) && 
                        relZ >= margin && relZ <= (1.0 - margin)) {
                        
                        mc.interactionManager.attackEntity(mc.player, result.getEntity());
                        mc.player.swingHand(Hand.MAIN_HAND);
                        
                        if (config.limit) {
                            reach = (float) (mc.player.isCreative() ? 4.5 : (2.7 + (Math.random() * 0.3)));
                        }
                    }
                }
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            
            if (entity.isAlive() && entity.isAttackable()) {
                Vec3d hitPos = entityHit.getPos();
                Box entityBox = entity.getBoundingBox();
                
                double relX = (hitPos.x - entityBox.minX) / entityBox.getXLength();
                double relZ = (hitPos.z - entityBox.minZ) / entityBox.getZLength();

                double margin = 0.15;
                if (((relX >= margin && relX <= (1.0 - margin) && relZ >= margin && relZ <= (1.0 - margin))) && config.limit) {
                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                } else if (!config.limit){
                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }
}
