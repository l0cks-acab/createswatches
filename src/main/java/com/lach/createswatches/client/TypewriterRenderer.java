package com.lach.createswatches.client;

import com.lach.createswatches.CreateSwatches;
import com.lach.createswatches.TypewriterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Shaft (without Flywheel), the sliding carriage, bobbing keys and the palette typed onto the paper. */
public class TypewriterRenderer extends KineticBlockEntityRenderer<TypewriterBlockEntity> {
    public static PartialModel CARRIAGE, KEYS;
    private static final ResourceLocation WHITE = CreateSwatches.id("textures/block/chip.png");
    /** Typing length in ticks; matches the block entity. */
    private static final float TYPE_TICKS = 36f;

    public TypewriterRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /** Partial models must exist before models load. */
    public static void init() {
        if (CARRIAGE != null) return;
        CARRIAGE = PartialModel.of(CreateSwatches.id("block/swatch_typewriter/carriage"));
        KEYS = PartialModel.of(CreateSwatches.id("block/swatch_typewriter/keys"));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(TypewriterBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getOpposite());
    }

    @Override
    protected void renderSafe(TypewriterBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        super.renderSafe(be, pt, ms, buffers, light, overlay);
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        float yRot = -facing.toYRot() + 180;
        float left = be.typing() > 0 ? Math.max(0, be.typing() - pt) : 0;
        float progress = be.typing() > 0 ? 1 - left / TYPE_TICKS : 1;
        // The carriage creeps left as it types, then returns with the bell.
        float slide = be.typing() > 2 ? -progress * 3 / 16f : 0;
        float bob = be.typing() > 4 && (be.typing() / 2) % 2 == 0 ? -0.6f / 16f : 0;

        VertexConsumer cutout = buffers.getBuffer(RenderType.cutout());
        CachedBuffers.partial(KEYS, state).center().rotateYDegrees(yRot).uncenter().translate(0, bob, 0).light(light).renderInto(ms, cutout);
        CachedBuffers.partial(CARRIAGE, state).center().rotateYDegrees(yRot).uncenter().translate(slide, 0, 0).light(light).renderInto(ms, cutout);

        int[] pal = be.palette();
        if (pal.length == 0) return;
        int shown = be.typing() > 0 ? Math.min(pal.length, (int) Math.ceil(progress * pal.length)) : pal.length;
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(yRot));
        ms.translate(-0.5 + slide, -0.5, -0.5);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        PoseStack.Pose pose = ms.last();
        // Paper front face sits at z = 9.9 px, spanning x 4..12, y 9.5..15.5. Chips go in rows of four, like typed lines.
        float z = 9.9f / 16f;
        for (int i = 0; i < shown; i++) {
            int col = i % 4, row = i / 4;
            float x0 = (4.6f + col * 1.8f) / 16f, x1 = x0 + 1.4f / 16f;
            float y1 = (15.0f - row * 2.2f) / 16f, y0 = y1 - 1.4f / 16f;
            quad(vc, pose, x0, y0, x1, y1, z, pal[i], light, overlay);
            // A typed "label" line beside each row of chips.
        }
        for (int row = 0; row <= (shown - 1) / 4; row++) {
            float y1 = (15.0f - row * 2.2f - 0.55f) / 16f, y0 = y1 - 0.3f / 16f;
            float x0 = (4.6f + Math.min(4, shown - row * 4) * 1.8f) / 16f;
            if (x0 < 11.4f / 16f) quad(vc, pose, x0, y0, 11.4f / 16f, y1, z, 0x5A5048, light, overlay);
        }
        ms.popPose();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z, int rgb, int light, int overlay) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        Matrix4f m = pose.pose();
        // Faces north (towards the typist in the model's own frame).
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, 255).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, 255).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, 255).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, 255).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
    }

    @Override
    public boolean shouldRenderOffScreen(TypewriterBlockEntity be) {
        return false;
    }
}
