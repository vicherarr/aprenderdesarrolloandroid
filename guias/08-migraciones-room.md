# Guía 08 — Modificar el esquema de Room: migraciones

Objetivo: añadir una columna `favorita` a la tabla `frases` (marcar frases
con ★ tocándolas) **sin perder los datos que los usuarios ya tienen
guardados**. Ese "sin perder" es exactamente lo que resuelve una migración.

Toda la lección se ejecutó de verdad en el emulador de la guía 07, que
tenía la base de datos en versión 1 con 3 frases guardadas: el escenario
real de un usuario que actualiza la app.

---

## 1. Por qué existe esto: el contrato de `version`

SQLite guarda en cada fichero `.db` un número (`PRAGMA user_version`). Al
abrir la base de datos, Room lo compara con el `version` de `@Database`:

- **Coinciden** → todo sigue.
- **El fichero va por detrás** (app actualizada) → Room busca un camino de
  `Migration` registradas que lleve de una versión a otra, y lo ejecuta
  **en una transacción**. Con 1→2 y 2→3 registradas, un usuario que venga
  de la 1 pasa por las dos en orden.
- **No hay camino** → `IllegalStateException` y crash (lo veremos pasar).

La alternativa `fallbackToDestructiveMigration()` "resuelve" el caso 3
**borrando la base de datos entera**. En esta app perdería las frases; en
una app real, los datos del usuario. Es aceptable en una caché pura que se
puede reconstruir desde la red; como comodín general, no.

## 2. El cambio de esquema: los tres "default" de una columna nueva

`FraseEntity` gana el campo, y `@Database` sube a `version = 2`:

```kotlin
@Entity(tableName = "frases")
data class FraseEntity(
    @PrimaryKey val id: Int,
    val texto: String,
    val autor: String,
    @ColumnInfo(name = "guardada_en") val guardadaEn: Long,
    @ColumnInfo(defaultValue = "0") val favorita: Boolean = false
)
```

Hay **tres** valores por defecto en juego y conviene no confundirlos:

| Dónde | Qué es | Quién lo usa |
|---|---|---|
| `= false` (Kotlin) | default del constructor | tu código al crear instancias |
| `DEFAULT 0` (SQL de la migración) | default de la columna en SQLite | las filas YA existentes al añadir la columna |
| `@ColumnInfo(defaultValue = "0")` | le CUENTA a Room ese default de SQL | la validación de esquema de Room |

El tercero es el que se olvida: tras migrar, Room compara la tabla real con
la que esperaba (generada de la entidad). Si la migración crea la columna
con `DEFAULT 0` pero la entidad no declara `defaultValue`, los esquemas no
son idénticos y Room aborta con `Migration didn't properly handle: frases`.

## 3. Primero, el error (ejecutado a propósito)

Se compiló e instaló la app con el esquema nuevo y `version = 2` pero **sin
migración**, sobre la base de datos v1 del emulador. La instalación va
bien; el crash llega al abrir la app, en el momento exacto en que el Flow
del DAO abre la base de datos:

```
FATAL EXCEPTION: main
java.lang.IllegalStateException: A migration from 1 to 2 was required but not
found. Please provide the necessary Migration path via
RoomDatabase.Builder.addMigration(...) or allow for destructive migrations
via one of the RoomDatabase.Builder.fallbackToDestructiveMigration* functions.
    at androidx.room.BaseRoomConnectionManager.onMigrate(...)
    ...
    at androidx.room.TriggerBasedInvalidationTracker.syncTriggers(...)
```

Dos lecciones del stacktrace:

1. **El error es de runtime, no de compilación**: compila perfecto. Por eso
   una migración olvidada llega a producción si no se prueba la
   actualización (no la instalación limpia, que nunca migra).
2. **La base de datos queda intacta**: la migración corre en transacción y
   aquí ni empezó. Instalar después la versión arreglada funcionó sin
   pérdida — el crash no corrompió nada.

## 4. La solución: la `Migration` (`data/local/Migraciones.kt`)

```kotlin
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE frases ADD COLUMN favorita INTEGER NOT NULL DEFAULT 0")
    }
}
```

Y su registro en el builder (`di/BaseDatosModule.kt`):

```kotlin
Room.databaseBuilder(contexto, AppDatabase::class.java, "holaandroid.db")
    .addMigrations(MIGRACION_1_2)
    .build()
```

Detalles del SQL:

- `Boolean` en Room es `INTEGER` 0/1 en SQLite (no hay tipo booleano).
- `NOT NULL` obliga a dar `DEFAULT`: las filas existentes necesitan un
  valor para la columna que acaba de aparecer. Ese `DEFAULT 0` es el que la
  entidad declara en `@ColumnInfo(defaultValue = "0")` (§2).
- Una migración escribe **SQL contra el esquema viejo**: aquí no existen
  las clases de Room, solo `execSQL`. Para cambios que SQLite no sabe hacer
  con `ALTER TABLE` (borrar columna en versiones viejas, cambiar un tipo),
  el patrón es crear tabla nueva + `INSERT INTO ... SELECT` + renombrar.

### 4.1 Los JSON de `app/schemas/` son el contrato

Al compilar, el plugin exportó `2.json` junto al `1.json` de la guía 07.
El diff entre ambos es exactamente la migración que hay que escribir — y es
la razón por la que se versionan en git: sin el `1.json` no sabrías cómo
era el esquema que tus usuarios tienen en el móvil.

### 4.2 La alternativa moderna: `@AutoMigration`

Para cambios simples como este, Room puede **generar la migración él solo**
leyendo esos mismos JSON:

```kotlin
@Database(
    entities = [FraseEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
```

Sin `Migraciones.kt` ni `addMigrations`. Cubre añadir columna/tabla y, con
anotaciones de ayuda (`@RenameColumn`, `@DeleteColumn`...), renombrados y
borrados. Cuando hay que **transformar datos** (rellenar la columna nueva a
partir de otra, fusionar tablas), no llega: ahí la manual es obligatoria.
En esta guía se escribió la manual por ver el mecanismo completo; en el día
a día, empieza por la automática y baja a manual cuando no baste.

## 5. Dar uso a la columna (y una decisión de dominio honesta)

Cambios mínimos para que la columna viva de verdad:

- **DAO**: el `ORDER BY` pasa a `favorita DESC, guardada_en DESC` y se
  añade el conmutador — `NOT` sobre un INTEGER 0/1 es como se alterna un
  booleano en SQLite:

  ```kotlin
  @Query("UPDATE frases SET favorita = NOT favorita WHERE id = :id")
  suspend fun alternarFavorita(id: Int)
  ```

- **Dominio**: `Frase` gana `id` y `esFavorita`. La guía 06 presumía de que
  el id "no salía de la capa de datos" — y era verdad *entonces*: ningún
  caso de uso lo necesitaba. Marcar favoritas exige señalar QUÉ frase
  cambia, y eso es identidad. El modelo de dominio evoluciona cuando los
  casos de uso lo exigen, no antes.
- **Cadena completa**: `AlternarFavoritaUseCase` → `viewModelScope.launch`
  en el ViewModel → cada fila del `LazyColumn` es `clickable`. No hay
  código de "reordenar la lista": el `UPDATE` dispara el Flow de Room
  (guía 07) y la UI se recompone con el orden nuevo. El ordenado vive en el
  SQL del DAO, no en la UI.

## 6. Errores típicos con migraciones

| Síntoma | Causa |
|---|---|
| `A migration from X to Y was required but not found` | subiste `version` sin registrar la migración en `addMigrations` |
| `Migration didn't properly handle: frases` (con diff expected/found) | el SQL de la migración no deja la tabla EXACTAMENTE como la espera la entidad (el clásico: falta `defaultValue` en `@ColumnInfo`, §2) |
| `Room cannot verify the data integrity` | cambiaste la entidad SIN subir `version` (en desarrollo: desinstala o sube versión) |
| `Cannot add a NOT NULL column with default value NULL` | `ALTER TABLE ... NOT NULL` sin `DEFAULT` |
| Funciona en instalación limpia, crashea al actualizar | las instalaciones limpias nunca migran: prueba SIEMPRE el camino de actualización (§7) |

## 7. Verificar (ejecutado en el emulador de la guía 07)

La prueba de una migración es el **camino de actualización**: partir de una
instalación vieja con datos y actualizar encima, nunca instalar en limpio.

1. Estado inicial: app de la guía 07 con 3 frases guardadas (esquema v1).
2. `installDebug` de la versión sin migración → crash del §3 en Logcat.
3. `installDebug` de la versión con `MIGRACION_1_2` → la app abre y **las 3
   frases siguen ahí**. Datos supervivientes = migración correcta.
4. Un toque en una frase → gana la ★ y salta al primer puesto de la lista.
5. `force-stop` + relanzar → la ★ persiste.

Y por dentro (`run-as` + `sqlite3`, receta de la guía 07):

```
PRAGMA user_version;   → 2
SELECT sql FROM sqlite_master WHERE name='frases';
  → CREATE TABLE `frases` (..., `guardada_en` INTEGER NOT NULL,
                           favorita INTEGER NOT NULL DEFAULT 0, ...)
SELECT id, autor, favorita FROM frases ORDER BY favorita DESC;
  → 37|Marilyn Monroe|1        ← la marcada con ★
    1410|Walt Disney|0
    419|Salman Rushdie|0       ← ids intactos de la versión 1
```

Se ve hasta la firma del `ALTER TABLE`: las columnas originales llevan
comillas invertidas (las creó Room) y `favorita` no (la creó nuestro SQL).

## Fuentes consultadas (18-07-2026)

- Migraciones de Room (guía oficial): <https://developer.android.com/training/data-storage/room/migrating-db-versions>
- Referencia de `Migration` y `addMigrations`: <https://developer.android.com/reference/kotlin/androidx/room/migration/Migration>
- Auto-migraciones (`@AutoMigration`, anotaciones de ayuda): <https://developer.android.com/training/data-storage/room/migrating-db-versions#automated>
- `ALTER TABLE` en SQLite (qué puede y qué no): <https://www.sqlite.org/lang_altertable.html>
- Mensajes de error: capturados en vivo con `adb logcat` en este equipo

Próxima lección natural: **tests de la capa de datos** — `MigrationTestHelper`
usa los JSON de `app/schemas/` para probar migraciones sin emulador, y
MockWebServer (guía 06) hace lo propio con la red.
