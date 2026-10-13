package com.dailytrials;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Datos guardados en el mundo: día, misión, mutaciones activas y dónde devolver a quien está en el Limbo. */
public class DayState extends PersistentState {
    public record LimboReturn(String dim, double x, double y, double z) {}

    public long lastDay = -1;
    public int questIndex = 0;
    public final Set<UUID> done = new HashSet<>();
    public final Set<String> mutaciones = new LinkedHashSet<>();
    public final Map<UUID, LimboReturn> limbo = new HashMap<>();

    public static DayState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(DayState::fromNbt, DayState::new, "dailytrials_state");
    }

    public static DayState fromNbt(NbtCompound nbt) {
        DayState s = new DayState();
        s.lastDay = nbt.getLong("lastDay");
        s.questIndex = nbt.getInt("questIndex");

        NbtList list = nbt.getList("done", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            try { s.done.add(UUID.fromString(list.getString(i))); } catch (Exception ignored) {}
        }
        NbtList ml = nbt.getList("mutaciones", NbtElement.STRING_TYPE);
        for (int i = 0; i < ml.size(); i++) s.mutaciones.add(ml.getString(i));

        NbtCompound lc = nbt.getCompound("limbo");
        for (String key : lc.getKeys()) {
            try {
                NbtCompound c = lc.getCompound(key);
                s.limbo.put(UUID.fromString(key),
                        new LimboReturn(c.getString("dim"), c.getDouble("x"), c.getDouble("y"), c.getDouble("z")));
            } catch (Exception ignored) {}
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("lastDay", lastDay);
        nbt.putInt("questIndex", questIndex);

        NbtList list = new NbtList();
        for (UUID u : done) list.add(NbtString.of(u.toString()));
        nbt.put("done", list);

        NbtList ml = new NbtList();
        for (String m : mutaciones) ml.add(NbtString.of(m));
        nbt.put("mutaciones", ml);

        NbtCompound lc = new NbtCompound();
        for (Map.Entry<UUID, LimboReturn> e : limbo.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putString("dim", e.getValue().dim());
            c.putDouble("x", e.getValue().x());
            c.putDouble("y", e.getValue().y());
            c.putDouble("z", e.getValue().z());
            lc.put(e.getKey().toString(), c);
        }
        nbt.put("limbo", lc);
        return nbt;
    }
}
