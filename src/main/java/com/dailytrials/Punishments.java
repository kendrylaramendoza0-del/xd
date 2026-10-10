package com.dailytrials;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Castigos / retos nocturnos. Añade más casos al switch si quieres. */
public final class Punishments {
    public static final int COUNT = 6;

    public static void apply(ServerPlayerEntity p, int type) {
        ServerWorld w = p.getServerWorld();
        BlockPos c = p.getBlockPos();
        String title;
        String sub;

        switch (type) {
            case 0 -> {
                title = "⚡ ¡Creepers eléctricos!";
                sub = "Corre... o prepárate para volar";
                for (int i = 0; i < 3; i++) {
                    CreeperEntity cr = EntityType.CREEPER.create(w);
                    if (cr == null) continue;
                    BlockPos s = spot(w, c);
                    cr.refreshPositionAndAngles(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
                    NbtCompound nbt = new NbtCompound();
                    cr.writeNbt(nbt);
                    nbt.putBoolean("powered", true);
                    cr.readNbt(nbt);
                    cr.setTarget(p);
                    w.spawnEntity(cr);
                }
            }
            case 1 -> {
                title = "🧟 ¡Horda de zombis!";
                sub = "Con casco de hierro, claro";
                for (int i = 0; i < 6; i++) {
                    ZombieEntity z = EntityType.ZOMBIE.create(w);
                    if (z == null) continue;
                    BlockPos s = spot(w, c);
                    z.refreshPositionAndAngles(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
                    z.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    z.setTarget(p);
                    w.spawnEntity(z);
                }
            }
            case 2 -> {
                title = "☠ Maldición nocturna";
                sub = "Lentitud, debilidad y hambre";
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 60, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 60, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 60, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 30, 1));
            }
            case 3 -> {
                title = "🌩 ¡Tormenta eléctrica!";
                sub = "Los rayos te buscan";
                w.setWeather(0, 6000, true, true);
                for (int i = 0; i < 4; i++) {
                    BlockPos s = spot(w, c);
                    LightningEntity l = EntityType.LIGHTNING_BOLT.create(w);
                    if (l == null) continue;
                    l.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(s));
                    w.spawnEntity(l);
                }
            }
            case 4 -> {
                title = "👁 ¡Las Sombras te cazan!";
                sub = "Ciegan al golpear";
                for (int i = 0; i < 2; i++) {
                    SombraEntity s = ModContent.SOMBRA.create(w);
                    if (s == null) continue;
                    BlockPos b = spot(w, c);
                    s.refreshPositionAndAngles(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
                    s.setTarget(p);
                    w.spawnEntity(s);
                }
            }
            default -> {
                title = "💔 Reto: poca vida";
                sub = "Sobrevive hasta el amanecer con 3 corazones";
                p.setHealth(6.0f);
                p.getHungerManager().setFoodLevel(8);
            }
        }

        p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub).formatted(Formatting.GRAY)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(Formatting.RED, Formatting.BOLD)));
    }

    /** Busca un sitio libre a 3-6 bloques del jugador. */
    private static BlockPos spot(ServerWorld w, BlockPos c) {
        for (int i = 0; i < 40; i++) {
            int dx = w.random.nextInt(13) - 6;
            int dz = w.random.nextInt(13) - 6;
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = c.add(dx, dy, dz);
                if (w.getBlockState(pos).isAir() && w.getBlockState(pos.up()).isAir()
                        && w.getBlockState(pos.down()).isSolidBlock(w, pos.down())) {
                    return pos;
                }
            }
        }
        return c;
    }
}
