package com.dailytrials;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * El Limbo: dimensión oscura con un laberinto lleno de Sombras.
 * La dimensión se define en data/dailytrials/dimension/limbo.json; el laberinto se construye por código
 * cada vez que alguien entra (y no hay nadie dentro).
 */
public final class Limbo {
    public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, ModContent.id("limbo"));

    private static final int N = 10;            // celdas por lado
    private static final int G = 2 * N + 1;     // casillas de la cuadrícula
    private static final int S = 2;             // bloques por casilla (pasillos de 2 de ancho)
    private static final int Y0 = 60;           // altura del suelo
    private static final int ALTO = 4;          // altura de los pasillos
    private static final int CANT_SOMBRAS = 10;

    private static final double START_X = 3.0, START_Z = 3.0;
    private static final double EXIT_X = (G - 2) * S + 1.0, EXIT_Z = (G - 2) * S + 1.0;

    public static boolean disponible(MinecraftServer server) {
        return server != null && server.getWorld(KEY) != null;
    }

    public static boolean enviar(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        ServerWorld limbo = server.getWorld(KEY);
        if (limbo == null) return false;
        if (p.getServerWorld() == limbo) return true;

        DayState st = DayState.get(server);
        ServerWorld actual = p.getServerWorld();
        st.limbo.put(p.getUuid(), new DayState.LimboReturn(
                actual.getRegistryKey().getValue().toString(), p.getX(), p.getY(), p.getZ()));
        st.markDirty();

        if (limbo.getPlayers().isEmpty()) construir(limbo);
        p.teleport(limbo, START_X, Y0 + 1.0, START_Z, -45f, 0f);
        p.sendMessage(Text.literal("🌑 Estás en el Limbo. Encuentra la salida: la luz blanca del otro extremo del laberinto.")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        return true;
    }

    public static void salir(ServerPlayerEntity p, boolean premio) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        DayState st = DayState.get(server);
        DayState.LimboReturn r = st.limbo.remove(p.getUuid());
        st.markDirty();

        ServerWorld dest = null;
        double x = 0, y = 0, z = 0;
        if (r != null) {
            dest = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(r.dim())));
            x = r.x();
            y = r.y();
            z = r.z();
        }
        if (dest == null) {
            dest = server.getOverworld();
            BlockPos sp = dest.getSpawnPos();
            x = sp.getX() + 0.5;
            y = sp.getY();
            z = sp.getZ() + 0.5;
        }
        p.teleport(dest, x, y, z, p.getYaw(), p.getPitch());
        if (premio) {
            p.getInventory().offerOrDrop(new ItemStack(ModContent.ALMA, 2));
            p.addExperienceLevels(5);
            p.sendMessage(Text.literal("🌅 ¡Escapaste del Limbo! Recompensa: 2 Almas Oscuras y 5 niveles.")
                    .formatted(Formatting.GOLD, Formatting.BOLD), false);
        }
    }

    /** Se llama cada tick: si alguien llega a la salida, lo devuelve. */
    public static void tick(MinecraftServer server) {
        ServerWorld limbo = server.getWorld(KEY);
        if (limbo == null) return;
        List<ServerPlayerEntity> jugadores = limbo.getPlayers();
        if (jugadores.isEmpty()) return;
        for (ServerPlayerEntity p : new ArrayList<>(jugadores)) {
            if (Math.abs(p.getX() - EXIT_X) <= 1.6 && Math.abs(p.getZ() - EXIT_Z) <= 1.6 && p.getY() < Y0 + 3) {
                salir(p, true);
            }
        }
    }

    // ------------------------------------------------------------------ laberinto

    private static void construir(ServerWorld w) {
        Random r = new Random();

        // Sombras de visitas anteriores fuera
        Box area = new Box(-4, Y0 - 2, -4, G * S + 4, Y0 + ALTO + 6, G * S + 4);
        for (SombraEntity e : w.getEntitiesByType(ModContent.SOMBRA, area, x -> true)) e.discard();

        // Generar laberinto (DFS)
        boolean[][] muro = new boolean[G][G];
        for (boolean[] fila : muro) Arrays.fill(fila, true);
        Deque<int[]> pila = new ArrayDeque<>();
        muro[1][1] = false;
        pila.push(new int[]{1, 1});
        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        while (!pila.isEmpty()) {
            int[] cur = pila.peek();
            List<int[]> opciones = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], nz = cur[1] + d[1];
                if (nx >= 1 && nx <= G - 2 && nz >= 1 && nz <= G - 2 && muro[nx][nz]) opciones.add(d);
            }
            if (opciones.isEmpty()) {
                pila.pop();
                continue;
            }
            int[] d = opciones.get(r.nextInt(opciones.size()));
            muro[cur[0] + d[0] / 2][cur[1] + d[1] / 2] = false;
            muro[cur[0] + d[0]][cur[1] + d[1]] = false;
            pila.push(new int[]{cur[0] + d[0], cur[1] + d[1]});
        }

        // Bloques
        BlockState suelo = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState techo = Blocks.BLACKSTONE.getDefaultState();
        BlockState ladrillo = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState agrietado = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState aire = Blocks.AIR.getDefaultState();

        for (int gx = 0; gx < G; gx++) {
            for (int gz = 0; gz < G; gz++) {
                for (int dx = 0; dx < S; dx++) {
                    for (int dz = 0; dz < S; dz++) {
                        int x = gx * S + dx, z = gz * S + dz;
                        poner(w, x, Y0, z, suelo);
                        poner(w, x, Y0 + ALTO + 1, z, techo);
                        for (int y = 1; y <= ALTO; y++) {
                            BlockState b = aire;
                            if (muro[gx][gz]) b = r.nextInt(5) == 0 ? agrietado : ladrillo;
                            poner(w, x, Y0 + y, z, b);
                        }
                    }
                }
            }
        }

        // Faroles de almas (luz tenue)
        for (int gx = 1; gx <= G - 2; gx += 2) {
            for (int gz = 1; gz <= G - 2; gz += 2) {
                boolean inicio = gx == 1 && gz == 1;
                boolean salida = gx == G - 2 && gz == G - 2;
                if (!inicio && !salida && r.nextFloat() < 0.35f) {
                    poner(w, gx * S + r.nextInt(S), Y0 + 1, gz * S + r.nextInt(S), Blocks.SOUL_LANTERN.getDefaultState());
                }
            }
        }

        // Salida: plataforma de luz blanca
        for (int dx = 0; dx < S; dx++) {
            for (int dz = 0; dz < S; dz++) {
                poner(w, (G - 2) * S + dx, Y0, (G - 2) * S + dz, Blocks.SEA_LANTERN.getDefaultState());
            }
        }

        // Sombras repartidas lejos del inicio
        List<int[]> celdas = new ArrayList<>();
        for (int gx = 1; gx <= G - 2; gx += 2) {
            for (int gz = 1; gz <= G - 2; gz += 2) {
                if (Math.abs(gx - 1) + Math.abs(gz - 1) >= 10) celdas.add(new int[]{gx, gz});
            }
        }
        Collections.shuffle(celdas, r);
        for (int i = 0; i < Math.min(CANT_SOMBRAS, celdas.size()); i++) {
            int[] c = celdas.get(i);
            soltarSombra(w, c[0] * S + 1.0, Y0 + 1.2, c[1] * S + 1.0, r, false);
        }
        // Sombra Alfa cerca de la salida
        soltarSombra(w, (G - 2) * S + 1.0, Y0 + 1.2, (G - 4) * S + 1.0, r, true);
    }

    private static void soltarSombra(ServerWorld w, double x, double y, double z, Random r, boolean alfa) {
        SombraEntity s = ModContent.SOMBRA.create(w);
        if (s == null) return;
        s.refreshPositionAndAngles(x, y, z, r.nextFloat() * 360f, 0f);
        s.setPersistent();
        if (alfa) Punishments.hacerAlfa(s);
        w.spawnEntity(s);
    }

    private static void poner(ServerWorld w, int x, int y, int z, BlockState state) {
        w.setBlockState(new BlockPos(x, y, z), state, Block.NOTIFY_LISTENERS);
    }
}
