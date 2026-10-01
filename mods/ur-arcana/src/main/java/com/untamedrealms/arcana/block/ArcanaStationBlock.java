package com.untamedrealms.arcana.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.arcana.network.ArcanaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The alchemy lab ({@code enchanter=false}) or the arcane enchanter. */
public class ArcanaStationBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ArcanaStationBlock> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.BOOL.fieldOf("enchanter").forGetter(b -> b.enchanter),
            propertiesCodec()
    ).apply(inst, ArcanaStationBlock::new));

    private static final VoxelShape TABLE = Shapes.or(Block.box(0, 12, 0, 16, 15, 16), Block.box(1, 0, 1, 15, 12, 15));
    private static final VoxelShape ALTAR = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(3, 4, 3, 13, 12, 13), Block.box(0, 12, 0, 16, 14, 16));

    public final boolean enchanter;

    public ArcanaStationBlock(boolean enchanter, Properties properties) {
        super(properties);
        this.enchanter = enchanter;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return enchanter ? ALTAR : TABLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) ArcanaNetwork.open(serverPlayer, pos, enchanter);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
