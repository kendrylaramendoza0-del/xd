package com.dailytrials;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** Alma Oscura: lo que sueltan las Sombras. Con 4 y un diamante se fabrica un Amuleto de Protección. */
public class AlmaOscuraItem extends Item {
    public AlmaOscuraItem(Settings settings) { super(settings); }

    @Override
    public boolean hasGlint(ItemStack stack) { return true; }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Un fragmento de una Sombra.").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("4 almas + 1 diamante = Amuleto de Protección").formatted(Formatting.GRAY));
    }
}
