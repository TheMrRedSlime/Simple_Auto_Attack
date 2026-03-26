package org.simpleautoattack.SimpleAutoAttack;

import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoAttack implements ClientModInitializer {
    private static AutoAttackConfig config;
    public static KeyBinding afkKey;
    public static final KeyBinding.Category SIMPLE_AUTO_ATTACK_CATEGORY = KeyBinding.Category.create(Identifier.of("simpleautoattack"));
    
    public static boolean autoAfk = false;
    private long nextAttackTime = 0;

    @Override
    public void onInitializeClient() {
        // Register config
        AutoConfig.register(AutoAttackConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();
        afkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.simpleautoattack.autoafk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K , SIMPLE_AUTO_ATTACK_CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (config.enabled) { 
                if(afkKey.wasPressed()){
                    autoAfk = !autoAfk;
                }
                AutoMeleeTick(client);
            }
        });
    }

    // Add a static getter for the config
    public static AutoAttackConfig getConfig() {
        return config;
    }

    private void AutoMeleeTick(MinecraftClient mc) {
        if ((!autoAfk && !mc.options.attackKey.isPressed()) || mc.player == null || mc.world == null || mc.interactionManager == null
                || !(mc.player.getAttackCooldownProgress(0) >= 1)) {
            return;
        }

         if (config.limit && System.currentTimeMillis() < nextAttackTime) {
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
                float reach = (float) (mc.player.isCreative() ? 4.5 : (config.limit ? 2.7 : 3.0));
                Vec3d camera = mc.player.getCameraPosVec(1.0F);
                Vec3d rotation = mc.player.getRotationVec(1.0F);
                Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
                EntityHitResult result = ProjectileUtil.raycast(mc.player, camera, end, new Box(camera, end),
                        e -> !e.isSpectator() && e.isAttackable(), reach * reach);
                if (result != null && result.getEntity().isAlive()) {
                    if(config.limit){
                        Random random = new Random();
                        nextAttackTime = (System.currentTimeMillis() + random.nextInt(config.limitms) + 500);
                    }
                    mc.interactionManager.attackEntity(mc.player, result.getEntity());
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (entity.isAlive() && entity.isAttackable()) {
                if(config.limit){
                    Random random = new Random();
                    nextAttackTime = (System.currentTimeMillis() + random.nextInt(config.limitms) + 500);
                }
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
