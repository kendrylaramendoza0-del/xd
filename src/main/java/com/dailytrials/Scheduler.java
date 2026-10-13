package com.dailytrials;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Mini planificador: ejecuta tareas dentro de X ticks (para flechas, rayos escalonados, etc.). */
public final class Scheduler {
    private static final class Task {
        int delay;
        final Runnable action;
        Task(int delay, Runnable action) { this.delay = delay; this.action = action; }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(int ticks, Runnable action) {
        TASKS.add(new Task(ticks, action));
    }

    public static void tick() {
        if (TASKS.isEmpty()) return;
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            t.delay--;
            if (t.delay <= 0) {
                due.add(t.action);
                it.remove();
            }
        }
        for (Runnable r : due) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
}
