package com.dailytrials;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.Difficulty;

/** Lógica de entrega de misiones, compartida por el Tablón y por Ramiro el Escribano. */
public final class QuestLogic {

    public static int multiplier(MinecraftServer server) {
        return server.getOverworld().getDifficulty() == Difficulty.HARD ? 2 : 1; // en Difícil, doble recompensa
    }

    private static void hablar(ServerPlayerEntity sp, String texto, Formatting color) {
        sp.sendMessage(Text.literal("[" + EscribanoEntity.NOMBRE + "] ").formatted(Formatting.GOLD)
                .append(Text.literal(texto).formatted(color)), false);
    }

    public static void entregar(ServerPlayerEntity sp) {
        MinecraftServer server = sp.getServer();
        if (server == null) return;

        DayState st = DayState.get(server);
        Quests.Quest q = Quests.ALL.get(st.questIndex % Quests.ALL.size());
        int mult = multiplier(server);

        hablar(sp, "¡Saludos, viajero! Esta es la misión de hoy:", Formatting.WHITE);
        sp.sendMessage(q.describe(mult), false);

        if (st.done.contains(sp.getUuid())) {
            hablar(sp, "Ya cumpliste la misión de hoy. Vuelve mañana.", Formatting.GREEN);
            return;
        }

        PlayerInventory inv = sp.getInventory();
        int have = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(q.item())) have += stack.getCount();
        }
        if (have < q.count()) {
            hablar(sp, "Aún te faltan objetos: llevas " + have + "/" + q.count() + ".", Formatting.RED);
            return;
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
        sp.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        hablar(sp, "🎉 ¡Excelente trabajo! Aquí tienes tu recompensa.", Formatting.GOLD);
    }
}
