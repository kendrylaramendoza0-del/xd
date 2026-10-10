package com.dailytrials;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.Difficulty;

public class DailyTrials implements ModInitializer {

    // ===== AJUSTES (cámbialos a tu gusto) =====
    /** Dificultades que pueden tocar cada día. */
    private static final Difficulty[] POSIBLES = {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
    /** Probabilidad de castigo por la noche si SÍ completaste la misión (si no, siempre hay castigo). */
    private static final float CHANCE_CASTIGO_SI_CUMPLISTE = 0.35f;
    /** Hora del día en la que empieza el castigo (13000 = anochecer). */
    private static final long HORA_CASTIGO = 13000;

    @Override
    public void onInitialize() {
        ModContent.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            ServerWorld ow = server.getOverworld();
            long tod = ow.getTimeOfDay();
            long day = tod / 24000L;
            long inDay = tod % 24000L;
            DayState st = DayState.get(server);

            if (st.lastDay < 0) { // primera vez en este mundo
                st.lastDay = day;
                st.questIndex = ow.random.nextInt(Quests.ALL.size());
                st.markDirty();
            }
            if (day != st.lastDay) {
                newDay(server, st, day);
            }
            if (inDay >= HORA_CASTIGO && inDay < 23000 && st.lastPunishDay != day) {
                st.lastPunishDay = day;
                st.markDirty();
                punishTime(server, st, -1);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("mision").executes(ctx -> {
                MinecraftServer server = ctx.getSource().getServer();
                DayState st = DayState.get(server);
                Quests.Quest q = Quests.ALL.get(st.questIndex % Quests.ALL.size());
                ctx.getSource().sendFeedback(() -> Text.literal("📜 Misión de hoy (entrégala en el Tablón de Misiones):").formatted(Formatting.AQUA), false);
                ctx.getSource().sendFeedback(() -> q.describe(QuestBoardBlock.multiplier(server)), false);
                return Command.SINGLE_SUCCESS;
            }));

            dispatcher.register(CommandManager.literal("dailytrials")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("nuevodia").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        DayState st = DayState.get(server);
                        newDay(server, st, st.lastDay + 1);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(CommandManager.literal("castigo").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        punishTime(server, DayState.get(server), 0);
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }

    private static void newDay(MinecraftServer server, DayState st, long day) {
        ServerWorld ow = server.getOverworld();
        st.lastDay = day;
        st.done.clear();
        st.questIndex = ow.random.nextInt(Quests.ALL.size());
        st.markDirty();

        Difficulty d = POSIBLES[ow.random.nextInt(POSIBLES.length)];
        server.setDifficulty(d, true);

        Quests.Quest q = Quests.ALL.get(st.questIndex);
        server.getPlayerManager().broadcast(Text.literal("☀ Nuevo día").formatted(Formatting.YELLOW, Formatting.BOLD), false);
        server.getPlayerManager().broadcast(Text.literal("Dificultad de hoy: ").formatted(Formatting.GRAY)
                .append(Text.literal(nombre(d)).formatted(colorDe(d), Formatting.BOLD)), false);
        server.getPlayerManager().broadcast(Text.literal("📜 Nueva misión: ").formatted(Formatting.AQUA)
                .append(q.describe(QuestBoardBlock.multiplier(server))), false);
    }

    /** mode -1 = normal (según misión); 0 = forzado por comando. */
    private static void punishTime(MinecraftServer server, DayState st, int mode) {
        ServerWorld ow = server.getOverworld();
        server.getPlayerManager().broadcast(Text.literal("🌙 ¡Es el momento castigo!").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.isSpectator() || p.isCreative()) continue;
            boolean cumplio = st.done.contains(p.getUuid());
            boolean castigar = mode == 0 || !cumplio || ow.random.nextFloat() < CHANCE_CASTIGO_SI_CUMPLISTE;
            if (castigar) {
                Punishments.apply(p, ow.random.nextInt(Punishments.COUNT));
            } else {
                p.sendMessage(Text.literal("Esta noche te libras: cumpliste la misión. 😌").formatted(Formatting.GREEN), false);
            }
        }
    }

    private static String nombre(Difficulty d) {
        return switch (d) { case PEACEFUL -> "Pacífico"; case EASY -> "Fácil"; case NORMAL -> "Normal"; case HARD -> "Difícil"; };
    }

    private static Formatting colorDe(Difficulty d) {
        return switch (d) { case PEACEFUL, EASY -> Formatting.GREEN; case NORMAL -> Formatting.YELLOW; case HARD -> Formatting.RED; };
    }
}
