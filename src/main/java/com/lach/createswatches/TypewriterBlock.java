package com.lach.createswatches;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Swatch Typewriter: sits on storage (or is tuned to a logistics network), takes rotation through a shaft at its back. */
public class TypewriterBlock extends HorizontalKineticBlock implements IBE<TypewriterBlockEntity> {
    public static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_YELLOW).strength(1.5f, 6f).sound(SoundType.WOOD).noOcclusion();

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(2, 4, 5, 14, 9, 14),
            Block.box(3, 9, 9, 13, 16, 12));
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    public TypewriterBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(HORIZONTAL_FACING);
        int i = f.get2DDataValue();
        if (SHAPES[i] == null) SHAPES[i] = rotate(SHAPE_NORTH, f);
        return SHAPES[i];
    }

    private static VoxelShape rotate(VoxelShape shape, Direction to) {
        VoxelShape[] buf = {shape, Shapes.empty()};
        int times = (to.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buf[0].forAllBoxes((x1, y1, z1, x2, y2, z2) -> buf[1] = Shapes.or(buf[1], Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
            buf[0] = buf[1];
            buf[1] = Shapes.empty();
        }
        return buf[0];
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TypewriterBlockEntity be)
            be.setTuned(com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem.isTuned(stack));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Wrenches and the like keep their own use; a Swatch Card opens the GUI with its palette.
        if (!stack.isEmpty() && !stack.is(CreateSwatches.SWATCH_CARD.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof TypewriterBlockEntity be)
            sp.openMenu(be, pos);
    }

    @Override
    public Class<TypewriterBlockEntity> getBlockEntityClass() {
        return TypewriterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TypewriterBlockEntity> getBlockEntityType() {
        return CreateSwatches.TYPEWRITER_BE.get();
    }
}
