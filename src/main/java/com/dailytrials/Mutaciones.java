package com.dailytrials;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/** Aquí vive la lógica de cada mutación. */
public final class Mutaciones {
    private static boolean reentrante = false;
    private static boolean heridasAplicado = false;

    public static void registrar() {
        // ----- Daño: caídas, perlas, flechas de esqueleto, piel de cristal -----
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (reentrante || !(entity instanceof ServerPlayerEntity)) return true;
            if (Mutacion.ACTIVAS.isEmpty()) return true;

            float mult = 1f;
            boolean caida = source.isOf(DamageTypes.FALL);
            // una perla de ender hace exactamente 5 de daño de caída con fallDistance = 0
            boolean perla = caida && entity.fallDistance == 0 && amount == 5.0f;
            if (perla) {
                if (Mutacion.activa(Mutacion.PERLAS)) mult *= 3f;
            } else if (caida && Mutacion.activa(Mutacion.CAIDAS)) {
                mult *= 4f;
            }
            if (Mutacion.activa(Mutacion.ARQUEROS)
                    && source.getSource() instanceof PersistentProjectileEntity
                    && source.getAttacker() instanceof AbstractSkeletonEntity) {
                mult *= 2f;
            }
            if (Mutacion.activa(Mutacion.CRISTAL)
                    && !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.GENERIC_KILL)) {
                mult *= 1.5f;
            }
            if (mult == 1f) return true;

            // cancelamos el golpe original y lo repetimos con el daño aumentado
            reentrante = true;
            try {
                entity.damage(source, amount * mult);
            } finally {
                reentrante = false;
            }
            return false;
        });

        // ----- Mobs que aparecen: zombis, creepers, monstruos veloces -----
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (Mutacion.ACTIVAS.isEmpty()) return;

            if (entity instanceof ZombieEntity z && Mutacion.activa(Mutacion.ZOMBIS) && z.addCommandTag("dt_zombi")) {
                z.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1_000_000, 2, false, false));
                boolean diamante = world.random.nextFloat() < 0.25f;
                equipar(z, EquipmentSlot.HEAD, diamante ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
                equipar(z, EquipmentSlot.CHEST, diamante ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE);
                equipar(z, EquipmentSlot.LEGS, diamante ? Items.DIAMOND_LEGGINGS : Items.IRON_LEGGINGS);
                equipar(z, EquipmentSlot.FEET, diamante ? Items.DIAMOND_BOOTS : Items.IRON_BOOTS);
            }

            if (entity instanceof CreeperEntity c && Mutacion.activa(Mutacion.CREEPERS) && c.addCommandTag("dt_creeper")) {
                NbtCompound nbt = new NbtCompound();
                c.writeNbt(nbt);
                nbt.putBoolean("powered", true);
                c.readNbt(nbt);
            }

            if (entity instanceof HostileEntity h && Mutacion.activa(Mutacion.VELOZ)
                    && !(h instanceof WardenEntity) && !(h instanceof SombraEntity)
                    && h.addCommandTag("dt_veloz")) {
                h.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1_000_000, 1, false, false));
            }
        });
    }

    private static void equipar(MobEntity m, EquipmentSlot slot, Item item) {
        if (m.getEquippedStack(slot).isEmpty()) m.equipStack(slot, new ItemStack(item));
    }

    /** Se llama una vez por segundo. */
    public static void tick(MinecraftServer server) {
        // Heridas Profundas: apaga la regeneración natural (gamerule)
        GameRules.BooleanRule regen = server.getGameRules().get(GameRules.NATURAL_REGENERATION);
        boolean quiere = Mutacion.activa(Mutacion.HERIDAS);
        if (quiere && !heridasAplicado) {
            regen.set(false, server);
            heridasAplicado = true;
        } else if (!quiere && heridasAplicado) {
            regen.set(true, server);
            heridasAplicado = false;
        }

        // Hambre Voraz
        if (Mutacion.activa(Mutacion.HAMBRE)) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!p.isCreative() && !p.isSpectator()) p.addExhaustion(0.3f);
            }
        }

        // Despertar del Warden: cada 3 minutos aparece uno lejos de cada jugador
        if (Mutacion.activa(Mutacion.WARDEN) && server.getTicks() % 3600 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (p.isCreative() || p.isSpectator()) continue;
                ServerWorld w = p.getServerWorld();
                if (w.getRegistryKey() != World.OVERWORLD) continue;
                Box box = new Box(p.getBlockPos()).expand(96);
                if (!w.getEntitiesByType(EntityType.WARDEN, box, e -> true).isEmpty()) continue;
                BlockPos s = Punishments.buscar(w, p.getBlockPos(), 20, 30);
                if (s == null) continue;
                WardenEntity wd = EntityType.WARDEN.create(w);
                if (wd == null) continue;
                wd.refreshPositionAndAngles(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
                wd.increaseAngerAt(p);
                w.spawnEntity(wd);
                p.sendMessage(Text.literal("🌑 Algo ancestral despierta cerca...").formatted(Formatting.DARK_AQUA), true);
            }
        }
    }
}
