package com.code.tama.core.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A "tertiary lever" - functionally like a vanilla lever, except it has THREE
 * states (0, 1, 2) instead of two (off/on).
 *
 * Right-click: 0 -> 1 -> 2 (capped at 2, does nothing if already at 2)
 * Shift + right-click: 2 -> 1 -> 0 (capped at 0, does nothing if already at 0)
 *
 * Redstone output (relative to the block's FACING property, i.e. the
 * direction the lever "points"):
 *  - POWER 0: emits 15 out of the FACING side, 8 out of the side 90 degrees
 *             to the left of FACING (when viewed from above).
 *  - POWER 1: emits nothing on all sides.
 *  - POWER 2: emits 15 out of the side opposite FACING, and 15 out of the
 *             side 90 degrees to the left of FACING.
 */
public class TertiaryLeverBlock extends FaceAttachedHorizontalDirectionalBlock {

    public static final MapCodec<TertiaryLeverBlock> CODEC = simpleCodec(TertiaryLeverBlock::new);

    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 2);

    public TertiaryLeverBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(POWER, 0)
                        .setValue(FACING, Direction.NORTH)
                        .setValue(FACE, AttachFace.WALL)
        );
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, FACING, FACE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LeverShapes.shapeFor(state.getValue(FACE), state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState blockState;
            if (direction.getAxis() == Direction.Axis.Y) {
                blockState = this.defaultBlockState()
                        .setValue(FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
                        .setValue(FACING, context.getHorizontalDirection());
            } else {
                blockState = this.defaultBlockState()
                        .setValue(FACE, AttachFace.WALL)
                        .setValue(FACING, direction.getOpposite());
            }

            // Double-check that the state can actually survive here before returning it
            if (blockState.canSurvive(context.getLevel(), context.getClickedPos())) {
                return blockState.setValue(POWER, 0);
            }
        }
        return null;
    }

    private static AttachFace getAttachFace(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.UP) {
            return AttachFace.FLOOR;
        } else {
            return clickedFace == Direction.DOWN ? AttachFace.FLOOR : AttachFace.WALL;
        }
    }

    private static Direction clickedHorizontalFacing(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            return face;
        }
        return context.getHorizontalDirection().getOpposite();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int current = state.getValue(POWER);
        boolean movingDown = player.isShiftKeyDown();
        int next = movingDown ? Math.max(0, current - 1) : Math.min(2, current + 1);

        if (next == current) {
            return InteractionResult.CONSUME;
        }

        BlockState newState = state.setValue(POWER, next);
        level.setBlock(pos, newState, 11);
        this.updateNeighbours(newState, level, pos);

        float pitch = next == 1 ? 0.5F : (movingDown ? 0.4F : 0.6F);
        level.playSound(
                player,
                pos,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.3F,
                pitch
        );
        level.gameEvent(player, next > current ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);

        return InteractionResult.SUCCESS;
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        // Also notify the block this lever is mounted against (the wall /
        // floor / ceiling block behind it), exactly like vanilla's lever.
        Direction mountedAgainst = attachmentSurfaceDirection(state);
        BlockPos neighborPos = pos.relative(mountedAgainst);
        level.updateNeighborsAt(neighborPos, this);
    }

    /**
     * Direction from this block TOWARD the surface it is mounted on
     * (the wall/floor/ceiling block behind it). For a wall lever this is
     * simply the opposite of FACING (FACING points away from the wall,
     * out into the room).
     */
    private static Direction attachmentSurfaceDirection(BlockState state) {
        switch (state.getValue(FACE)) {
            case CEILING:
                return Direction.UP;
            case FLOOR:
                return Direction.DOWN;
            default:
                return state.getValue(FACING).getOpposite();
        }
    }

    /**
     * The direction the lever "points": for wall-mounted levers (the normal
     * case used by this block's spec) this is simply FACING.
     */
    private static Direction pointingDirection(BlockState state) {
        return state.getValue(FACING);
    }

    /** 90 degrees to the left of FACING, viewed from above (i.e. clockwise -1 / counter-clockwise). */
    private static Direction leftOfFacing(BlockState state) {
        return pointingDirection(state).getCounterClockWise();
    }

    private static Direction oppositeOfFacing(BlockState state) {
        return pointingDirection(state).getOpposite();
    }

    /**
     * Computes the redstone signal this block emits out of the given side,
     * per the spec:
     *  POWER 0: FACING -> 15, left-of-FACING -> 8
     *  POWER 1: nothing
     *  POWER 2: opposite-of-FACING -> 15, left-of-FACING -> 15
     */
    private static int signalForSide(BlockState state, Direction side) {
        int power = state.getValue(POWER);
        switch (power) {
            case 0: {
                if (side == pointingDirection(state)) return 15;
                if (side == leftOfFacing(state)) return 8;
                return 0;
            }
            case 2: {
                if (side == oppositeOfFacing(state)) return 15;
                if (side == leftOfFacing(state)) return 15;
                return 0;
            }
            default:
                return 0;
        }
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 'direction' here is the direction FROM the neighbor TO this block
        // (i.e. pointing into this block). The side of THIS block facing the
        // neighbor is the opposite.
        Direction sideOfThisBlock = direction.getOpposite();
        return signalForSide(state, sideOfThisBlock);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}