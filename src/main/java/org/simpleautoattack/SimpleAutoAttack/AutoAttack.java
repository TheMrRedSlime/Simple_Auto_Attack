package org.simpleautoattack.SimpleAutoAttack;

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
import java.util.Random;

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

        if ((!AFKMode && !mc.options.keyAttack.isPressed()) ||
            mc.player == null ||
            mc.world == null ||
            mc.interactionManager == null ||
            !(mc.player.getAttackCooldownProgress(0) >= 1)) return;

        if (Math.random() < 0.667 && config.limit) return;

        HitResult target = mc.crosshairTarget;

        if (target.getType() == HitResult.Type.MISS) {
            if (config.alwaysAttack) {
                mc.player.resetLastAttackedTicks();
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else if (target.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) target;
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState blockState = mc.world.getBlockState(blockPos);

            if (blockState.getCollisionShape(mc.world, blockPos).isEmpty() || blockState.getHardness(mc.world, blockPos) == 0.0F) {
                attackRaycastEntity(mc, blockHit.getBlockPos());
            }

        } else if (target.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) target;
            Entity entity = entityHit.getEntity();
            attackEntity(mc, entity);
        }
    }

    private void attackRaycastEntity(MinecraftClient mc, BlockPos pos) {
        float reach = (float) (mc.player.isCreative() ? 4.5 : (config.limit ? 2.7 : 3.0));
        Vec3d camera = mc.player.getCameraPosVec(1.0F);
        Vec3d rotation = mc.player.getRotationVec(1.0F);
        Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);

        EntityHitResult result = ProjectileUtil.raycast(mc.player, camera, end, new Box(camera, end),
                e -> !e.isSpectator() && e.isAttackable(), reach * reach);

        if (result != null && result.getEntity().isAlive()) {
            attackEntity(mc, result.getEntity());
        }
    }

    private void attackEntity(MinecraftClient mc, Entity entity) {
    if (!entity.isAlive() || !entity.isAttackable()) return;
        try {
            Random random = new Random();
            Thread.sleep(random.nextInt(101) + 100);
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
