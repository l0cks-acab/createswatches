package com.lach.createswatches.client;

import com.lach.createswatches.CreateSwatches;
import com.lach.createswatches.ApplicatorData;
import com.lach.createswatches.ApplicatorItem;
import com.lach.createswatches.PlaceMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.List;
import com.lach.createswatches.net.CycleApplicatorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Applicator rendering and sneak + scroll selection. */
@EventBusSubscriber(modid = CreateSwatches.MODID, value = Dist.CLIENT)
public final class ApplicatorClient {
    private ApplicatorClient() {
    }

    @SubscribeEvent
    static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event) {
        TerminalScreen.clearCraftableCache();
    }

    @SubscribeEvent
    static void models(ModelEvent.RegisterAdditional event) {
        event.register(ApplicatorRenderer.BASE);
    }

    @SubscribeEvent
    static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private ApplicatorRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ApplicatorRenderer();
                return renderer;
            }
        }, CreateSwatches.APPLICATOR.get());
    }

    /** Outlines where the next click will place blocks. */
    @SubscribeEvent
    static void preview(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof ApplicatorItem)) held = mc.player.getOffhandItem();
        if (!(held.getItem() instanceof ApplicatorItem) || mc.player.isShiftKeyDown()) return;
        ApplicatorData d = ApplicatorItem.data(held);
        if (d.blocks().isEmpty() || d.mode() == PlaceMode.SINGLE) return;
        List<BlockPos> targets = d.mode().targets(mc.level, hit.getBlockPos(), hit.getDirection(), d.size());
        if (targets.isEmpty()) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (BlockPos p : targets)
            LevelRenderer.renderLineBox(pose, lines, new AABB(p).deflate(0.04), 1f, 0.72f, 0.3f, 0.9f);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    @SubscribeEvent
    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown() || event.getScrollDeltaY() == 0) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof ApplicatorItem)) return;
        PacketDistributor.sendToServer(new CycleApplicatorPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }
}
