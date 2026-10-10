# Daily Trials (Fabric 1.20.1)

Cada día de Minecraft:
1. Cambia la dificultad al azar (Fácil / Normal / Difícil). En Difícil las recompensas se duplican.
2. Aparece una misión diaria. Se entrega en el **Tablón de Misiones** (clic derecho). Receta: 3 palos arriba, 2 tablones + libro en medio, 3 tablones abajo.
3. Mob nuevo: **Sombra** (zombi rápido, fuego-inmune, ciega al golpear). Spawnea en el Overworld de noche y tiene huevo en el creativo.
4. **Momento castigo** al anochecer: creepers eléctricos, horda de zombis, maldición, tormenta con rayos, Sombras o reto de 3 corazones. Siempre te toca si no hiciste la misión; si la hiciste, 35% de probabilidad.

Comandos: `/mision` (ver misión), `/dailytrials nuevodia` y `/dailytrials castigo` (admin, para probar).

## Compilar
1. Entra a https://fabricmc.net/develop/template/ , genera un proyecto 1.20.1 y copia su carpeta `gradle/`, `gradlew` y `gradlew.bat` aquí
   (o ejecuta `gradle wrapper` si tienes Gradle instalado).
2. Con Java 17: `./gradlew build`
3. El .jar queda en `build/libs/` (el que NO dice `-sources`). Ponlo en tu carpeta `mods` junto con Fabric API 0.92.2+1.20.1.
