package com.dailytrials;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Datos guardados en el mundo: día actual, misión del día y quién ya la entregó. */
public class DayState extends PersistentState {
    public long lastDay = -1;
    public long lastPunishDay = -1;
    public int questIndex = 0;
    public final Set<UUID> done = new HashSet<>();

    public static DayState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(DayState::fromNbt, DayState::new, "dailytrials_state");
    }

    public static DayState fromNbt(NbtCompound nbt) {
        DayState s = new DayState();
        s.lastDay = nbt.getLong("lastDay");
        s.lastPunishDay = nbt.getLong("lastPunishDay");
        s.questIndex = nbt.getInt("questIndex");
        NbtList list = nbt.getList("done", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            try { s.done.add(UUID.fromString(list.getString(i))); } catch (Exception ignored) {}
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("lastDay", lastDay);
        nbt.putLong("lastPunishDay", lastPunishDay);
        nbt.putInt("questIndex", questIndex);
        NbtList list = new NbtList();
        for (UUID u : done) list.add(NbtString.of(u.toString()));
        nbt.put("done", list);
        return nbt;
    }
}
