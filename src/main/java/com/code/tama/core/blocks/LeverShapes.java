package com.code.tama.core.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Reproduces the vanilla lever's collision/selection shapes for every
 * combination of AttachFace + FACING
 *
 * These boxes match net.minecraft.world.level.block.LeverBlock's vanilla
 * shape tables (a thin 6px-wide, 8px-deep, 6px-tall box pivoting out from
 * the face it's attached to).
 */
public final class LeverShapes {

    private LeverShapes() {
    }

    // Floor lever, pointing along the Z axis (north/south), the "long" axis vanilla uses for FLOOR.
    private static final VoxelShape FLOOR_Z = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 0.6D, 0.75D);
    // Floor lever, pointing along the X axis, vanilla rotates the same shape for east/west floor levers.
    private static final VoxelShape FLOOR_X = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 0.6D, 0.75D);

    private static final VoxelShape CEILING_Z = Shapes.box(0.25D, 0.4D, 0.25D, 0.75D, 1.0D, 0.75D);
    private static final VoxelShape CEILING_X = Shapes.box(0.25D, 0.4D, 0.25D, 0.75D, 1.0D, 0.75D);

    private static final VoxelShape NORTH_WALL = Shapes.box(0.25D, 0.2D, 0.5D, 0.75D, 0.8D, 1.0D);
    private static final VoxelShape SOUTH_WALL = Shapes.box(0.25D, 0.2D, 0.0D, 0.75D, 0.8D, 0.5D);
    private static final VoxelShape WEST_WALL = Shapes.box(0.5D, 0.2D, 0.25D, 1.0D, 0.8D, 0.75D);
    private static final VoxelShape EAST_WALL = Shapes.box(0.0D, 0.2D, 0.25D, 0.5D, 0.8D, 0.75D);

    private static final Map<AttachFace, Map<Direction, VoxelShape>> SHAPES = buildTable();

    private static Map<AttachFace, Map<Direction, VoxelShape>> buildTable() {
        Map<AttachFace, Map<Direction, VoxelShape>> table = new HashMap<>();

        Map<Direction, VoxelShape> floor = new HashMap<>();
        floor.put(Direction.NORTH, FLOOR_Z);
        floor.put(Direction.SOUTH, FLOOR_Z);
        floor.put(Direction.EAST, FLOOR_X);
        floor.put(Direction.WEST, FLOOR_X);
        table.put(AttachFace.FLOOR, floor);

        Map<Direction, VoxelShape> ceiling = new HashMap<>();
        ceiling.put(Direction.NORTH, CEILING_Z);
        ceiling.put(Direction.SOUTH, CEILING_Z);
        ceiling.put(Direction.EAST, CEILING_X);
        ceiling.put(Direction.WEST, CEILING_X);
        table.put(AttachFace.CEILING, ceiling);

        Map<Direction, VoxelShape> wall = new HashMap<>();
        wall.put(Direction.NORTH, NORTH_WALL);
        wall.put(Direction.SOUTH, SOUTH_WALL);
        wall.put(Direction.WEST, WEST_WALL);
        wall.put(Direction.EAST, EAST_WALL);
        table.put(AttachFace.WALL, wall);

        return table;
    }

    public static VoxelShape shapeFor(AttachFace face, Direction facing) {
        return SHAPES.get(face).get(facing);
    }
}
