package com.dailytrials;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** Amuleto de Protección: si lo llevas en el inventario, te salva de un castigo (y se consume). */
public class AmuletoItem extends Item {
    public AmuletoItem(Settings settings) { super(settings); }

    @Override
    public boolean hasGlint(ItemStack stack) { return true; }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Llévalo en el inventario: te salva de un castigo.").formatted(Formatting.GRAY));
    }
}
