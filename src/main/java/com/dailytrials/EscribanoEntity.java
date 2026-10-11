package com.dailytrials;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

/**
 * Ramiro el Escribano: el NPC que recibe las misiones.
 * Es un aldeano quieto, invulnerable y con nombre visible. Clic derecho = ver/entregar misión.
 * Para cambiarle el nombre, edita NOMBRE.
 */
public class EscribanoEntity extends VillagerEntity {
    public static final String NOMBRE = "Ramiro el Escribano";

    public EscribanoEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
        this.setCustomName(Text.literal(NOMBRE).formatted(Formatting.GOLD));
        this.setCustomNameVisible(true);
        this.setInvulnerable(true);
        this.setPersistent();
        this.setAiDisabled(true);
        this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.LIBRARIAN));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity sp) {
            QuestLogic.entregar(sp);
        }
        return ActionResult.SUCCESS;
    }
}
