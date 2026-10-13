package com.dailytrials;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ArrowEntity;
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

/** Castigos y retos. Para añadir uno: nuevo "case" en apply() y sube COUNT. */
public final class Punishments {
    public static final int COUNT = 15;

    /** Si el jugador lleva un Amuleto de Protección, lo gasta y se salva del castigo. */
    public static boolean protegido(ServerPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(ModContent.AMULETO)) {
                s.decrement(1);
                p.sendMessage(Text.literal("🛡 Tu Amuleto de Protección te salvó del castigo (se consumió).")
                        .formatted(Formatting.AQUA, Formatting.BOLD), false);
                return true;
            }
        }
        return false;
    }

    public static void apply(ServerPlayerEntity p, int type) {
        ServerWorld w = p.getServerWorld();
        // ajustes para que no se junten castigos absurdos
        if (type == 11 && Mutacion.activa(Mutacion.CAIDAS)) type = 7;
        if (type == 6 && !Limbo.disponible(p.getServer())) type = 4;

        String title;
        String sub;

        switch (type) {
            case 0 -> {
                title = "⚡ ¡Creepers eléctricos!";
                sub = "Corre... o prepárate para volar";
                for (int i = 0; i < 4; i++) {
                    CreeperEntity cr = crear(p, EntityType.CREEPER);
                    if (cr == null) continue;
                    NbtCompound nbt = new NbtCompound();
                    cr.writeNbt(nbt);
                    nbt.putBoolean("powered", true);
                    cr.readNbt(nbt);
                    soltar(p, cr);
                }
            }
            case 1 -> {
                title = "🧟 ¡Horda de zombis!";
                sub = "Con casco de hierro, claro";
                for (int i = 0; i < 6; i++) {
                    ZombieEntity z = crear(p, EntityType.ZOMBIE);
                    if (z == null) continue;
                    z.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    soltar(p, z);
                }
            }
            case 2 -> {
                title = "☠ Maldición";
                sub = "Lentitud, debilidad y hambre";
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 60, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 60, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 60, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 30, 1));
            }
            case 3 -> {
                title = "🌩 ¡Tormenta eléctrica!";
                sub = "Esta vez los rayos van por ti";
                w.setWeather(0, 6000, true, true);
                int[] retardos = {10, 35, 60, 90};
                for (int i = 0; i < retardos.length; i++) {
                    final boolean directo = (i % 2 == 0); // 1.º y 3.er rayo caen justo sobre ti
                    Scheduler.later(retardos[i], () -> rayo(p, directo));
                }
            }
            case 4 -> {
                title = "👁 ¡Las Sombras te cazan!";
                sub = "Vuelan, ciegan y no se rinden";
                spawnSombras(p, 3);
            }
            case 5 -> {
                title = "💔 Reto: poca vida";
                sub = "Sobrevive con 3 corazones";
                p.setHealth(6.0f);
                p.getHungerManager().setFoodLevel(8);
            }
            case 6 -> {
                title = "🌑 ¡Arrastrado al Limbo!";
                sub = "Escapa del laberinto de las Sombras";
                Limbo.enviar(p);
            }
            case 7 -> {
                title = "🏹 ¡Lluvia de flechas!";
                sub = "Mira hacia arriba... mejor no";
                for (int i = 0; i < 24; i++) {
                    Scheduler.later(10 + i * 5, () -> flecha(p));
                }
            }
            case 8 -> {
                title = "🔥 ¡Ataque del Nether!";
                sub = "Blazes y fuego";
                p.setOnFireFor(5);
                for (int i = 0; i < 2; i++) {
                    MobEntity b = crear(p, EntityType.BLAZE);
                    if (b != null) soltar(p, b);
                }
            }
            case 9 -> {
                title = "🕷 ¡Plaga!";
                sub = "Arañas venenosas y lepismas";
                for (int i = 0; i < 6; i++) {
                    MobEntity a = crear(p, EntityType.CAVE_SPIDER);
                    if (a != null) soltar(p, a);
                }
                for (int i = 0; i < 4; i++) {
                    MobEntity s = crear(p, EntityType.SILVERFISH);
                    if (s != null) soltar(p, s);
                }
            }
            case 10 -> {
                title = "🌫 Oscuridad total";
                sub = "No ves nada... y no estás solo";
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20 * 15, 0));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * 8, 0));
                spawnSombras(p, 2);
            }
            case 11 -> {
                title = "🎈 Caída libre";
                sub = "Vas a flotar... y luego a caer";
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20 * 10, 0));
            }
            case 12 -> {
                title = "🌑 ¡Noche forzada!";
                sub = "El sol se apagó";
                ServerWorld ow = p.getServer().getOverworld();
                long dia = ow.getTimeOfDay() / 24000L;
                ow.setTimeOfDay(dia * 24000L + 14000L);
                spawnSombras(p, 2);
            }
            case 13 -> {
                title = "💸 Saqueo";
                sub = "Pierdes experiencia y hambre";
                p.addExperienceLevels(-5);
                p.getHungerManager().setFoodLevel(4);
            }
            default -> {
                title = "👑 ¡Sombra Alfa!";
                sub = "Una gran Sombra viene por ti";
                SombraEntity alfa = crear(p, ModContent.SOMBRA);
                if (alfa != null) {
                    hacerAlfa(alfa);
                    soltar(p, alfa);
                }
            }
        }

        p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub).formatted(Formatting.GRAY)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(Formatting.RED, Formatting.BOLD)));
    }

    // ------------------------------------------------------------------ utilidades

    public static void spawnSombras(ServerPlayerEntity p, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            SombraEntity s = crear(p, ModContent.SOMBRA);
            if (s != null) soltar(p, s);
        }
    }

    public static void hacerAlfa(SombraEntity s) {
        EntityAttributeInstance vida = s.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (vida != null) vida.setBaseValue(100.0);
        s.setHealth(100.0f);
        s.setCustomName(Text.literal("Sombra Alfa").formatted(Formatting.DARK_PURPLE));
        s.setGlowing(true);
    }

    private static void rayo(ServerPlayerEntity p, boolean directo) {
        if (!p.isAlive()) return;
        ServerWorld w = p.getServerWorld();
        BlockPos pos = directo ? p.getBlockPos() : spot(w, p.getBlockPos());
        LightningEntity l = EntityType.LIGHTNING_BOLT.create(w);
        if (l == null) return;
        l.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos));
        w.spawnEntity(l);
    }

    private static void flecha(ServerPlayerEntity p) {
        if (!p.isAlive()) return;
        ServerWorld w = p.getServerWorld();
        double x = p.getX() + (w.random.nextDouble() - 0.5) * 7.0;
        double z = p.getZ() + (w.random.nextDouble() - 0.5) * 7.0;
        int h = 14;
        while (h > 3 && !w.getBlockState(BlockPos.ofFloored(x, p.getY() + h, z)).isAir()) h--;
        ArrowEntity a = new ArrowEntity(w, x, p.getY() + h, z);
        a.setVelocity(0.0, -1.6, 0.0);
        a.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        w.spawnEntity(a);
    }

    /** Crea el mob cerca del jugador, sin soltarlo todavía. */
    private static <T extends MobEntity> T crear(ServerPlayerEntity p, EntityType<T> tipo) {
        ServerWorld w = p.getServerWorld();
        T m = tipo.create(w);
        if (m == null) return null;
        BlockPos s = spot(w, p.getBlockPos());
        m.refreshPositionAndAngles(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
        return m;
    }

    private static void soltar(ServerPlayerEntity p, MobEntity m) {
        m.setTarget(p);
        p.getServerWorld().spawnEntity(m);
    }

    /** Sitio libre a 3-6 bloques del jugador (si no hay, su propia posición). */
    private static BlockPos spot(ServerWorld w, BlockPos c) {
        BlockPos r = buscar(w, c, 3, 6);
        return r != null ? r : c;
    }

    /** Busca un sitio libre entre min y max bloques de distancia en horizontal. Null si no encuentra. */
    public static BlockPos buscar(ServerWorld w, BlockPos c, int min, int max) {
        for (int i = 0; i < 60; i++) {
            int dx = w.random.nextInt(max * 2 + 1) - max;
            int dz = w.random.nextInt(max * 2 + 1) - max;
            if (Math.max(Math.abs(dx), Math.abs(dz)) < min) continue;
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos pos = c.add(dx, dy, dz);
                if (w.getBlockState(pos).isAir() && w.getBlockState(pos.up()).isAir()
                        && w.getBlockState(pos.down()).isSolidBlock(w, pos.down())) {
                    return pos;
                }
            }
        }
        return null;
    }
}
