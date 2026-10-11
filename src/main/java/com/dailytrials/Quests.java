package com.dailytrials;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Lista de misiones diarias. ¡Añade o cambia las que quieras! */
public final class Quests {
    public record Quest(String name, Item item, int count, List<ItemStack> reward) {
        public Text describe(int mult) {
            MutableText t = Text.literal(name + ": entrega " + count + "x ").formatted(Formatting.YELLOW)
                    .append(item.getName().copy().formatted(Formatting.WHITE))
                    .append(Text.literal("  ➜  Recompensa: ").formatted(Formatting.GREEN));
            for (int i = 0; i < reward.size(); i++) {
                ItemStack r = reward.get(i);
                t.append(Text.literal((r.getCount() * mult) + "x ").formatted(Formatting.GOLD))
                        .append(r.getName().copy().formatted(Formatting.GOLD));
                if (i < reward.size() - 1) t.append(Text.literal(", "));
            }
            return t;
        }
    }

    private static ItemStack s(Item i, int n) { return new ItemStack(i, n); }

    public static final List<Quest> ALL = List.of(
        new Quest("Minero aplicado", Items.COAL, 24, List.of(s(Items.IRON_INGOT, 6), s(Items.COOKED_BEEF, 8))),
        new Quest("Panadero", Items.WHEAT, 32, List.of(s(Items.GOLD_INGOT, 3), s(Items.BREAD, 10))),
        new Quest("Herrero", Items.IRON_INGOT, 12, List.of(s(Items.DIAMOND, 1), s(ModContent.AMULETO, 1))),
        new Quest("Cantero", Items.COBBLESTONE, 64, List.of(s(Items.EMERALD, 5))),
        new Quest("Cazador de creepers", Items.GUNPOWDER, 8, List.of(s(Items.DIAMOND, 2), s(ModContent.AMULETO, 1))),
        new Quest("Limpia cementerios", Items.ROTTEN_FLESH, 16, List.of(s(Items.EMERALD, 3), s(Items.GOLDEN_APPLE, 1))),
        new Quest("Coleccionista de huesos", Items.BONE, 12, List.of(s(Items.IRON_INGOT, 8), s(Items.ARROW, 32))),
        new Quest("Hilandero", Items.STRING, 16, List.of(s(Items.GOLD_INGOT, 4), s(Items.ENDER_PEARL, 1))),
        new Quest("Redstone Rush", Items.REDSTONE, 32, List.of(s(Items.DIAMOND, 2), s(ModContent.AMULETO, 1))),
        new Quest("Leñador", Items.OAK_LOG, 48, List.of(s(Items.EMERALD, 4), s(Items.COOKED_PORKCHOP, 10)))
    );
}
