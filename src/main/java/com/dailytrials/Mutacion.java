package com.dailytrials;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;

/** Las "mutaciones" de dificultad: cambian las reglas del juego. Se activan al terminar un día (25%). */
public enum Mutacion {
    CAIDAS("Caídas Mortales", "El daño por caída se multiplica ×4"),
    PERLAS("Perlas Traicioneras", "Las perlas de ender hacen 3 veces más daño"),
    ZOMBIS("Zombis Blindados", "Los zombis nacen con Fuerza III y armadura"),
    WARDEN("Despertar del Warden", "Los Warden aparecen de forma natural"),
    CREEPERS("Creepers Cargados", "Todos los creepers nacen cargados de electricidad"),
    ARQUEROS("Arqueros Letales", "Las flechas de los esqueletos hacen doble daño"),
    HAMBRE("Hambre Voraz", "Pierdes hambre mucho más rápido"),
    CRISTAL("Piel de Cristal", "Recibes un 50% más de daño de todo"),
    HERIDAS("Heridas Profundas", "No regeneras vida de forma natural"),
    VELOZ("Horda Veloz", "Los monstruos son mucho más rápidos");

    public final String nombre;
    public final String descripcion;

    Mutacion(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Mutaciones activas ahora mismo (se sincroniza con DayState cada segundo). */
    public static final EnumSet<Mutacion> ACTIVAS = EnumSet.noneOf(Mutacion.class);

    public static boolean activa(Mutacion m) { return ACTIVAS.contains(m); }

    public static Mutacion porId(String id) {
        for (Mutacion m : values()) if (m.id().equals(id)) return m;
        return null;
    }

    public static void cargar(Collection<String> ids) {
        ACTIVAS.clear();
        for (String s : ids) {
            Mutacion m = porId(s);
            if (m != null) ACTIVAS.add(m);
        }
    }
}
