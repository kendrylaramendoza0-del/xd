package com.dailytrials;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DailyTrials implements ModInitializer {

    // ===== AJUSTES (cámbialos a tu gusto) =====
    /** Probabilidad (0.25 = 25%) de cada evento al terminar un día. Se tira por separado para cada uno. */
    private static final float PROBABILIDAD = 0.25f;
    /** Cuántas mutaciones se activan a la vez cuando cambia la dificultad. */
    private static final int MUTACIONES_A_LA_VEZ = 2;
    /** Probabilidad de que un cambio de dificultad sea volver a "normal" (sin mutaciones). */
    private static final float PROB_CALMA = 0.15f;
    /** Probabilidad de que un castigo sea doble (dos castigos seguidos). */
    private static final float PROB_CASTIGO_DOBLE = 0.20f;

    @Override
    public void onInitialize() {
        ModContent.init();
        Mutaciones.registrar();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Scheduler.tick();
            Limbo.tick(server);
            if (server.getTicks() % 20 != 0) return;

            ServerWorld ow = server.getOverworld();
            long day = ow.getTimeOfDay() / 24000L;
            DayState st = DayState.get(server);
            Mutacion.cargar(st.mutaciones);
            Mutaciones.tick(server);

            if (st.lastDay < 0) { // primera vez en este mundo
                st.lastDay = day;
                st.questIndex = ow.random.nextInt(Quests.ALL.size());
                st.markDirty();
                return;
            }
            if (day != st.lastDay) {
                finDeDia(server, st, day);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("mision").executes(ctx -> {
                MinecraftServer server = ctx.getSource().getServer();
                DayState st = DayState.get(server);
                Quests.Quest q = Quests.ALL.get(st.questIndex % Quests.ALL.size());
                ctx.getSource().sendFeedback(() -> Text.literal("📜 Misión de hoy (entrégala a " + EscribanoEntity.NOMBRE + " o en el Tablón):").formatted(Formatting.AQUA), false);
                ctx.getSource().sendFeedback(() -> q.describe(QuestLogic.multiplier(server)), false);
                return Command.SINGLE_SUCCESS;
            }));

            dispatcher.register(CommandManager.literal("modos").executes(ctx -> {
                if (Mutacion.ACTIVAS.isEmpty()) {
                    ctx.getSource().sendFeedback(() -> Text.literal("Dificultad normal: no hay mutaciones activas.").formatted(Formatting.GREEN), false);
                } else {
                    ctx.getSource().sendFeedback(() -> Text.literal("Mutaciones activas (recompensas ×2):").formatted(Formatting.RED, Formatting.BOLD), false);
                    for (Mutacion m : Mutacion.ACTIVAS) {
                        ctx.getSource().sendFeedback(() -> Text.literal("⚠ " + m.nombre).formatted(Formatting.RED)
                                .append(Text.literal(" - " + m.descripcion).formatted(Formatting.GRAY)), false);
                    }
                }
                return Command.SINGLE_SUCCESS;
            }));

            // Comandos de administrador para probar todo sin esperar
            List<String> ids = new ArrayList<>();
            for (Mutacion m : Mutacion.values()) ids.add(m.id());
            ids.add("ninguno");

            dispatcher.register(CommandManager.literal("dailytrials")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("nuevodia").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        finDeDia(server, DayState.get(server), server.getOverworld().getTimeOfDay() / 24000L);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("castigo")
                            .executes(ctx -> {
                                castigo(ctx.getSource().getServer(), -1);
                                return Command.SINGLE_SUCCESS;
                            })
                            .then(CommandManager.argument("numero", IntegerArgumentType.integer(0, Punishments.COUNT - 1))
                                    .executes(ctx -> {
                                        castigo(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "numero"));
                                        return Command.SINGLE_SUCCESS;
                                    })))
                    .then(CommandManager.literal("dificultad").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        cambiarMutaciones(server, DayState.get(server));
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("modo")
                            .then(CommandManager.argument("nombre", StringArgumentType.word())
                                    .suggests((c, b) -> CommandSource.suggestMatching(ids, b))
                                    .executes(ctx -> {
                                        MinecraftServer server = ctx.getSource().getServer();
                                        DayState st = DayState.get(server);
                                        String n = StringArgumentType.getString(ctx, "nombre");
                                        st.mutaciones.clear();
                                        Mutacion m = Mutacion.porId(n);
                                        if (m != null) st.mutaciones.add(m.id());
                                        st.markDirty();
                                        Mutacion.cargar(st.mutaciones);
                                        decir(server, Text.literal(m != null ? "⚠ Mutación activada: " + m.nombre : "Mutaciones desactivadas.")
                                                .formatted(Formatting.RED));
                                        return Command.SINGLE_SUCCESS;
                                    })))
                    .then(CommandManager.literal("limbo").executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        if (!Limbo.enviar(p)) {
                            ctx.getSource().sendFeedback(() -> Text.literal("El Limbo no existe en este mundo (prueba en un mundo nuevo).").formatted(Formatting.RED), false);
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("salir").executes(ctx -> {
                        Limbo.salir(ctx.getSource().getPlayerOrThrow(), false);
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }

    /** Se ejecuta cada vez que termina un día (al amanecer o al dormir). */
    private static void finDeDia(MinecraftServer server, DayState st, long day) {
        ServerWorld ow = server.getOverworld();
        st.lastDay = day;
        st.done.clear();
        st.questIndex = ow.random.nextInt(Quests.ALL.size());
        st.markDirty();

        sonido(server, SoundEvents.BLOCK_BELL_USE, 1f, 1f);
        decir(server, Text.literal("☀ Nuevo día").formatted(Formatting.YELLOW, Formatting.BOLD));

        // 1) Dificultad: 25% de que cambien las mutaciones
        if (ow.random.nextFloat() < PROBABILIDAD) {
            cambiarMutaciones(server, st);
        } else if (st.mutaciones.isEmpty()) {
            decir(server, Text.literal("Dificultad de hoy: Normal (sin mutaciones)").formatted(Formatting.GRAY));
        } else {
            decir(server, Text.literal("Mutaciones activas (sin cambios): ").formatted(Formatting.GRAY)
                    .append(Text.literal(nombresActivos(st)).formatted(Formatting.RED)));
        }

        // 2) Misión del día (siempre)
        Quests.Quest q = Quests.ALL.get(st.questIndex);
        decir(server, Text.literal("📜 Nueva misión: ").formatted(Formatting.AQUA)
                .append(q.describe(QuestLogic.multiplier(server))));

        // 3) Castigo: 25%
        if (ow.random.nextFloat() < PROBABILIDAD) {
            castigo(server, -1);
        } else {
            decir(server, Text.literal("🍀 Hoy no hay castigo. ¡Aprovecha!").formatted(Formatting.GREEN));
        }
    }

    private static void cambiarMutaciones(MinecraftServer server, DayState st) {
        ServerWorld ow = server.getOverworld();
        st.mutaciones.clear();
        sonido(server, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1f);
        decir(server, Text.literal("🎲 ¡Cambio de dificultad!").formatted(Formatting.DARK_RED, Formatting.BOLD));

        if (ow.random.nextFloat() < PROB_CALMA) {
            decir(server, Text.literal("Las mutaciones se disipan: el mundo vuelve a la normalidad.").formatted(Formatting.GREEN));
        } else {
            List<Mutacion> todas = new ArrayList<>(List.of(Mutacion.values()));
            Collections.shuffle(todas, new java.util.Random(ow.random.nextLong()));
            for (int i = 0; i < MUTACIONES_A_LA_VEZ && i < todas.size(); i++) {
                Mutacion m = todas.get(i);
                st.mutaciones.add(m.id());
                decir(server, Text.literal("⚠ " + m.nombre).formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal(" - " + m.descripcion).formatted(Formatting.GRAY)));
            }
            decir(server, Text.literal("Mientras haya mutaciones, las recompensas de las misiones se duplican.").formatted(Formatting.GOLD));
        }
        st.markDirty();
        Mutacion.cargar(st.mutaciones);
    }

    /** forzado = -1 → castigo al azar; si no, el número de castigo indicado. */
    private static void castigo(MinecraftServer server, int forzado) {
        ServerWorld ow = server.getOverworld();
        sonido(server, SoundEvents.ENTITY_WITHER_SPAWN, 0.5f, 1f);
        decir(server, Text.literal("🌙 ¡Es el momento castigo!").formatted(Formatting.DARK_RED, Formatting.BOLD));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.isSpectator()) continue;
            if (Punishments.protegido(p)) continue;

            int a = forzado >= 0 ? forzado : ow.random.nextInt(Punishments.COUNT);
            Punishments.apply(p, a);

            if (forzado < 0 && ow.random.nextFloat() < PROB_CASTIGO_DOBLE) {
                int b = (a + 1 + ow.random.nextInt(Punishments.COUNT - 1)) % Punishments.COUNT;
                p.sendMessage(Text.literal("💀 ¡Castigo doble! Prepárate...").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                Scheduler.later(120, () -> {
                    if (p.isAlive()) Punishments.apply(p, b);
                });
            }
        }
    }

    // ----- utilidades -----
    private static String nombresActivos(DayState st) {
        List<String> n = new ArrayList<>();
        for (String id : st.mutaciones) {
            Mutacion m = Mutacion.porId(id);
            if (m != null) n.add(m.nombre);
        }
        return String.join(", ", n);
    }

    private static void decir(MinecraftServer server, Text t) {
        server.getPlayerManager().broadcast(t, false);
    }

    private static void sonido(MinecraftServer server, SoundEvent s, float vol, float pitch) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.playSound(s, vol, pitch);
        }
    }
}
