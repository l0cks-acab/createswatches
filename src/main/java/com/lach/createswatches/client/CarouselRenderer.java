package com.lach.createswatches.client;

import com.lach.createswatches.CarouselBlockEntity;
import com.lach.createswatches.CreateSwatches;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Back shaft (without Flywheel), the spinning ring of swatch tiles and the palette on the monitor. */
public class CarouselRenderer extends KineticBlockEntityRenderer<CarouselBlockEntity> {
    private static final ResourceLocation WHITE = CreateSwatches.id("textures/block/chip.png");
    /** Flourish length in ticks; matches the block entity. */
    private static final float SPIN_TICKS = 36f;
    /** The ring is geared down from the input shaft. */
    private static final float GEARING = 1 / 6f;
    private static final float BASE = 6 / 16f, TOP = 9.4f / 16f;
    private static final float R0 = 5.0f / 16f, R1 = 7.3f / 16f, HALF_W = 0.32f / 16f;
    private static final int BLANK = 0x5A5048;

    public static dev.engine_room.flywheel.lib.model.baked.PartialModel STUB;

    /** Partial models must exist before models load. */
    public static void init() {
        if (STUB == null) STUB = dev.engine_room.flywheel.lib.model.baked.PartialModel.of(CreateSwatches.id("block/swatch_carousel/shaft_stub"));
    }

    public CarouselRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CarouselBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(STUB, state, state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getOpposite());
    }

    /** Colours for each tile: the palette in blocks, else the stocked blocks sorted round the hue wheel. */
    private static int[] ringColours(CarouselBlockEntity be) {
        int[] src = be.palette();
        if (src.length == 0) {
            List<Integer> cols = new ArrayList<>();
            for (Item item : be.showcase()) {
                BlockColorCache.Info info = BlockColorCache.get(item);
                if (info != null) cols.add(info.rgb());
            }
            cols.sort(Comparator.comparingDouble(CarouselRenderer::hueKey));
            src = cols.stream().mapToInt(Integer::intValue).toArray();
        }
        int[] out = new int[CarouselBlockEntity.RING];
        for (int i = 0; i < out.length; i++) out[i] = src.length == 0 ? BLANK : src[i * src.length / out.length];
        return out;
    }

    /** Greys go last, the rest by hue. */
    private static double hueKey(int rgb) {
        float[] hsb = java.awt.Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        return hsb[1] < 0.15f ? 2 + hsb[2] : hsb[0];
    }

    @Override
    protected void renderSafe(CarouselBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        super.renderSafe(be, pt, ms, buffers, light, overlay);
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        float yRot = -facing.toYRot() + 180;
        float left = be.typing() > 0 ? Math.max(0, be.typing() - pt) : 0;
        float progress = be.typing() > 0 ? 1 - left / SPIN_TICKS : 1;
        // Export or print gives the ring one extra eased turn.
        float flourish = (float) (360 * (0.5 - 0.5 * Math.cos(Math.PI * progress)));
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float angle = (time * be.getSpeed() * 3f / 10f * GEARING + flourish) % 360;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        int[] ring = ringColours(be);
        float step = 360f / ring.length;
        for (int i = 0; i < ring.length; i++) {
            ms.pushPose();
            ms.translate(0.5, BASE, 0.5);
            ms.mulPose(Axis.YP.rotationDegrees(angle + i * step));
            ms.translate(0, 0, R0);
            ms.mulPose(Axis.ZP.rotationDegrees(18)); // cards lean on their neighbour, like a filament swatch stand
            box(vc, ms.last(), -HALF_W, 0, 0, HALF_W, TOP - BASE, R1 - R0, ring[i], light, overlay);
            ms.popPose();
        }

        // Monitor screen: the palette as chips in two rows, or a stock bar chart when no palette is set.
        int[] pal = be.palette();
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(yRot));
        ms.translate(-0.5, -0.5, -0.5);
        PoseStack.Pose pose = ms.last();
        float z = 5.08f / 16f;
        if (pal.length > 0) {
            int shown = be.typing() > 0 ? Math.min(pal.length, (int) Math.ceil(progress * pal.length)) : pal.length;
            for (int i = 0; i < shown; i++) {
                int col = i % 4, row = i / 4;
                float x0 = (5.7f + col * 1.2f) / 16f, x1 = x0 + 1.0f / 16f;
                float y1 = (12.4f - row * 1.4f) / 16f, y0 = y1 - 1.0f / 16f;
                quad(vc, pose, x0, y0, x1, y1, z, pal[i], light);
            }
        } else {
            int n = Math.min(8, be.showcase().size());
            for (int i = 0; i < n; i++) {
                BlockColorCache.Info info = BlockColorCache.get(be.showcase().get(i));
                int rgb = info == null ? BLANK : info.rgb();
                float x0 = (5.7f + i * 0.6f) / 16f, x1 = x0 + 0.45f / 16f;
                float h = 2.8f * (1 - i / 10f);
                quad(vc, pose, x0, 8.9f / 16f, x1, (8.9f + h) / 16f, z, rgb, light);
            }
        }
        ms.popPose();
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light, int overlay) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        Matrix4f m = pose.pose();
        float[][] faces = {
                {0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0},
                {0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
                {-1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0},
                {1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1},
                {0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0},
        };
        for (float[] f : faces) {
            float[] uv = {0, 1, 1, 1, 1, 0, 0, 0};
            for (int v = 0; v < 4; v++)
                vc.addVertex(m, f[3 + v * 3], f[4 + v * 3], f[5 + v * 3]).setColor(r, g, b, 255).setUv(uv[v * 2], uv[v * 2 + 1])
                        .setOverlay(overlay).setLight(light).setNormal(pose, f[0], f[1], f[2]);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z, int rgb, int light) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        Matrix4f m = pose.pose();
        // Screens glow: full bright, no overlay tint.
        int lit = 0xF000F0;
        int ov = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        vc.addVertex(m, x1, y0, z).setColor(r, g, b, 255).setUv(1, 1).setOverlay(ov).setLight(lit).setNormal(pose, 0, 0, -1);
        vc.addVertex(m, x0, y0, z).setColor(r, g, b, 255).setUv(0, 1).setOverlay(ov).setLight(lit).setNormal(pose, 0, 0, -1);
        vc.addVertex(m, x0, y1, z).setColor(r, g, b, 255).setUv(0, 0).setOverlay(ov).setLight(lit).setNormal(pose, 0, 0, -1);
        vc.addVertex(m, x1, y1, z).setColor(r, g, b, 255).setUv(1, 0).setOverlay(ov).setLight(lit).setNormal(pose, 0, 0, -1);
    }

    @Override
    public boolean shouldRenderOffScreen(CarouselBlockEntity be) {
        return false;
    }
}
