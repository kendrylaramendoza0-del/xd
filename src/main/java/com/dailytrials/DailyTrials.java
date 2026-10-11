package com.dailytrials;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.Difficulty;

import java.util.ArrayList;
import java.util.List;

public class DailyTrials implements ModInitializer {

    // ===== AJUSTES (cámbialos a tu gusto) =====
    /** Probabilidad (0.25 = 25%) de que ocurra cada evento al terminar un día. Se tira por separado para cada uno. */
    private static final float PROBABILIDAD = 0.25f;
    /** Dificultades que pueden tocar. */
    private static final Difficulty[] POSIBLES = {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
    /** Cuántas Sombras aparecen junto a cada jugador cuando llega el evento "nuevo mob". */
    private static final int SOMBRAS_POR_JUGADOR = 2;

    @Override
    public void onInitialize() {
        ModContent.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            ServerWorld ow = server.getOverworld();
            long day = ow.getTimeOfDay() / 24000L;
            DayState st = DayState.get(server);
            SombraEntity.activas = st.sombrasActivas;

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

            // Comandos de administrador para probar los eventos sin esperar
            dispatcher.register(CommandManager.literal("dailytrials")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("nuevodia").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        DayState st = DayState.get(server);
                        finDeDia(server, st, server.getOverworld().getTimeOfDay() / 24000L);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("castigo").executes(ctx -> {
                        castigo(ctx.getSource().getServer());
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("dificultad").executes(ctx -> {
                        cambiarDificultad(ctx.getSource().getServer());
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("sombras").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        activarSombras(server, DayState.get(server));
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
        st.sombrasActivas = false;
        st.markDirty();
        SombraEntity.activas = false;

        sonido(server, SoundEvents.BLOCK_BELL_USE, 1f, 1f);
        decir(server, Text.literal("☀ Nuevo día").formatted(Formatting.YELLOW, Formatting.BOLD));

        // 1) Dificultad: 25% de cambiar
        if (ow.random.nextFloat() < PROBABILIDAD) {
            cambiarDificultad(server);
        } else {
            decir(server, Text.literal("Dificultad de hoy: ").formatted(Formatting.GRAY)
                    .append(textoDificultad(ow.getDifficulty()))
                    .append(Text.literal(" (sin cambios)").formatted(Formatting.GRAY)));
        }

        // 2) Misión del día (siempre)
        Quests.Quest q = Quests.ALL.get(st.questIndex);
        decir(server, Text.literal("📜 Nueva misión: ").formatted(Formatting.AQUA)
                .append(q.describe(QuestLogic.multiplier(server))));

        // 3) Nuevo mob: 25% de que las Sombras salgan hoy
        if (ow.random.nextFloat() < PROBABILIDAD) {
            activarSombras(server, st);
        }

        // 4) Castigo: 25%
        if (ow.random.nextFloat() < PROBABILIDAD) {
            castigo(server);
        } else {
            decir(server, Text.literal("🍀 Hoy no hay castigo. ¡Aprovecha!").formatted(Formatting.GREEN));
        }
    }

    private static void cambiarDificultad(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        Difficulty actual = ow.getDifficulty();
        List<Difficulty> opciones = new ArrayList<>();
        for (Difficulty d : POSIBLES) if (d != actual) opciones.add(d);
        Difficulty nueva = opciones.get(ow.random.nextInt(opciones.size()));
        server.setDifficulty(nueva, true);
        sonido(server, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1f);
        decir(server, Text.literal("🎲 ¡La dificultad cambió! Ahora: ").formatted(Formatting.WHITE)
                .append(textoDificultad(nueva)));
    }

    private static void activarSombras(MinecraftServer server, DayState st) {
        st.sombrasActivas = true;
        st.markDirty();
        SombraEntity.activas = true;
        sonido(server, SoundEvents.ENTITY_ENDERMAN_STARE, 1f, 0.6f);
        decir(server, Text.literal("👁 ¡Las Sombras caminan hoy por el mundo!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        decir(server, Text.literal("Nuevo mob: Sombra, un zombi veloz que ciega al golpear.").formatted(Formatting.LIGHT_PURPLE));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.isSpectator()) continue;
            Punishments.spawnSombras(p, SOMBRAS_POR_JUGADOR);
        }
    }

    private static void castigo(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        sonido(server, SoundEvents.ENTITY_WITHER_SPAWN, 0.5f, 1f);
        decir(server, Text.literal("🌙 ¡Es el momento castigo!").formatted(Formatting.DARK_RED, Formatting.BOLD));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.isSpectator()) continue;
            if (Punishments.protegido(p)) continue;
            Punishments.apply(p, ow.random.nextInt(Punishments.COUNT));
        }
    }

    // ----- utilidades -----
    private static void decir(MinecraftServer server, Text t) {
        server.getPlayerManager().broadcast(t, false);
    }

    private static void sonido(MinecraftServer server, SoundEvent s, float vol, float pitch) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.playSound(s, vol, pitch);
        }
    }

    private static MutableText textoDificultad(Difficulty d) {
        String n = switch (d) { case PEACEFUL -> "Pacífico"; case EASY -> "Fácil"; case NORMAL -> "Normal"; case HARD -> "Difícil"; };
        Formatting c = switch (d) { case PEACEFUL, EASY -> Formatting.GREEN; case NORMAL -> Formatting.YELLOW; case HARD -> Formatting.RED; };
        return Text.literal(n).formatted(c, Formatting.BOLD);
    }
}
