package com.untamedrealms.arsenal.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.arsenal.network.ArsenalNetwork;
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

/** A crafting station; using it opens the station window. */
public class StationBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<StationBlock> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Station.CODEC.fieldOf("station").forGetter(b -> b.station),
            propertiesCodec()
    ).apply(inst, StationBlock::new));

    private static final VoxelShape FORGE = Shapes.or(Block.box(0, 0, 0, 16, 10, 16), Block.box(0, 10, 0, 16, 12, 2));
    private static final VoxelShape RACK_NS = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape RACK_EW = Block.box(7, 0, 0, 9, 16, 16);
    private static final VoxelShape BENCH = Shapes.or(Block.box(0, 12, 0, 16, 15, 16), Block.box(1, 0, 1, 15, 12, 15));

    public final Station station;

    public StationBlock(Station station, Properties properties) {
        super(properties);
        this.station = station;
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
        return switch (station) {
            case FORGE -> FORGE;
            case TANNING_RACK -> state.getValue(FACING).getAxis() == Direction.Axis.Z ? RACK_NS : RACK_EW;
            case WORKBENCH -> BENCH;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) ArsenalNetwork.openStation(serverPlayer, pos, station);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
