package com.dailytrials;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** El Tablón de Misiones: aquí también se puede entregar la misión del día. */
public class QuestBoardBlock extends Block {
    public QuestBoardBlock(Settings settings) { super(settings); }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (player instanceof ServerPlayerEntity sp) {
            QuestLogic.entregar(sp);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }
}
