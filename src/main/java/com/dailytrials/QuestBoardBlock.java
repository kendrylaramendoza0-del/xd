package com.dailytrials;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

/** El Tablón de Misiones: aquí se entrega la misión del día. */
public class QuestBoardBlock extends Block {
    public QuestBoardBlock(Settings settings) { super(settings); }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        MinecraftServer server = world.getServer();
        if (server == null || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

        DayState st = DayState.get(server);
        Quests.Quest q = Quests.ALL.get(st.questIndex % Quests.ALL.size());
        int mult = multiplier(server);

        sp.sendMessage(Text.literal("📜 Misión de hoy").formatted(Formatting.AQUA, Formatting.BOLD), false);
        sp.sendMessage(q.describe(mult), false);

        if (st.done.contains(sp.getUuid())) {
            sp.sendMessage(Text.literal("✔ Ya entregaste la misión de hoy. Vuelve mañana.").formatted(Formatting.GREEN), false);
            return ActionResult.CONSUME;
        }

        PlayerInventory inv = sp.getInventory();
        int have = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(q.item())) have += stack.getCount();
        }
        if (have < q.count()) {
            sp.sendMessage(Text.literal("Te faltan objetos: llevas " + have + "/" + q.count()).formatted(Formatting.RED), false);
            return ActionResult.CONSUME;
        }

        int remaining = q.count();
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(q.item())) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
        for (ItemStack r : q.reward()) {
            ItemStack copy = r.copy();
            copy.setCount(Math.min(copy.getMaxCount(), r.getCount() * mult));
            inv.offerOrDrop(copy);
        }
        sp.addExperience(30);
        st.done.add(sp.getUuid());
        st.markDirty();
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1f, 1f);
        sp.sendMessage(Text.literal("🎉 ¡Misión completada! Recompensa entregada.").formatted(Formatting.GOLD, Formatting.BOLD), false);
        return ActionResult.CONSUME;
    }

    public static int multiplier(MinecraftServer server) {
        return server.getOverworld().getDifficulty() == Difficulty.HARD ? 2 : 1; // en Difícil, doble recompensa
    }
}
