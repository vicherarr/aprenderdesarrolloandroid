# Guía 07 — Persistencia con Room: el repositorio coordina dos fuentes

Objetivo: guardar en el dispositivo cada frase que llega de la red y verlas
sin conexión. Es la continuación natural de la lección 06: el repositorio
deja de ser un simple traductor de la API y pasa a **coordinar dos fuentes
de datos** (Retrofit y Room), que es su verdadera razón de ser.

---

## 1. Elegir las piezas y sus versiones

### 1.1 Qué es Room y qué piezas lo forman

Room es la capa oficial de persistencia de Android sobre SQLite: en vez de
escribir SQL a mano contra `SQLiteOpenHelper`, se declaran clases anotadas y
Room **genera el código en compilación** (con KSP, como Hilt). Tres piezas:

| Pieza | Papel |
|---|---|
| `@Entity` | una clase = una tabla; una instancia = una fila |
| `@Dao` | interfaz con las operaciones; Room implementa |
| `@Database` | registra entidades y expone los DAOs |

El paralelismo con Retrofit es exacto y no es casualidad: se declara la
interfaz y la librería la implementa. La diferencia: Retrofit crea un proxy
en *runtime*; Room genera código en *compilación*, y por eso **valida el SQL
de cada `@Query` al compilar** — una consulta mal escrita no llega al APK.

### 1.2 Versiones verificadas (18-07-2026)

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `androidx.room:room-runtime` | **2.8.4** | última estable del Maven de Google |
| `androidx.room:room-compiler` | **2.8.4** | el procesador KSP; misma versión que runtime, siempre |
| plugin `androidx.room` | **2.8.4** | plugin de Gradle de Room; misma versión |

Dos lecciones de versionado nuevas:

1. **androidx NO está en Maven Central**: la búsqueda de `search.maven.org`
   que usamos en las guías 03 y 06 devuelve vacío. Las librerías de Google
   viven en su propio repositorio (el `google()` de `settings.gradle.kts`);
   el índice de versiones se consulta en
   `https://dl.google.com/android/maven2/androidx/room/group-index.xml`.
2. **`room-ktx` ya no existe como dependencia necesaria**: desde Room 2.7
   (reescritura KMP) las APIs de corrutinas y `Flow` viven en
   `room-runtime`. Los tutoriales antiguos que añaden `room-ktx` están
   desactualizados.

No hace falta tocar la versión de KSP: Room usa el mismo procesador que ya
configuró la guía 03 para Hilt (y aplican sus mismas reglas: el prefijo de
KSP = versión de Kotlin).

## 2. Configuración del proyecto

`libs.versions.toml`: versión `room`, las dos librerías y el plugin
`androidx.room`. El plugin se declara en el raíz con `apply false` (como
Hilt) y se aplica en `app`, donde además se le indica dónde exportar el
esquema:

```kotlin
// app/build.gradle.kts
plugins {
    // ...
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
}
```

**Qué es eso del esquema**: en cada compilación, Room escribe un JSON con la
foto exacta de la base de datos de cada `version` (tablas, columnas,
índices) en `app/schemas/`. Se versiona en git — es la referencia contra la
que se escribirán las **migraciones** cuando el esquema cambie. Antes del
plugin esto se configuraba con argumentos crudos de KSP; el plugin es la
forma oficial actual.

## 3. Las tres piezas en la capa de datos

### 3.1 La entidad: el modelo de la persistencia (`data/local/FraseEntity.kt`)

```kotlin
@Entity(tableName = "frases")
data class FraseEntity(
    @PrimaryKey val id: Int,
    val texto: String,
    val autor: String,
    @ColumnInfo(name = "guardada_en") val guardadaEn: Long
)
```

Con esto la app maneja **tres modelos de la misma frase**, y es lo correcto
(modelo por capa, guías 05 y 06):

| Modelo | Capa | Sabe de |
|---|---|---|
| `FraseDto` | datos (red) | JSON, `@SerialName` |
| `FraseEntity` | datos (local) | tabla, columnas, clave primaria |
| `Frase` | dominio | nada: `texto` y `autor` |

Decisiones de diseño de la tabla:

- **La clave primaria es el `id` de la API** (que el dominio ignora, pero la
  capa de datos conoce): identifica la frase de forma natural y evita
  duplicados.
- `guardadaEn` (epoch millis) permite ordenar por recencia. `@ColumnInfo`
  desacopla el nombre de columna del nombre Kotlin, igual que `@SerialName`
  en el DTO.

### 3.2 El DAO: operaciones declaradas, no escritas (`data/local/FraseDao.kt`)

```kotlin
@Dao
interface FraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(frase: FraseEntity)

    @Query("SELECT * FROM frases ORDER BY guardada_en DESC")
    fun observarTodas(): Flow<List<FraseEntity>>
}
```

Los dos tipos de operación de un DAO, y su regla:

- **Un disparo** (insertar, borrar, leer una vez) → función `suspend`. Room
  la ejecuta fuera del hilo principal (main-safe), como Retrofit.
- **Consulta observable** → devuelve `Flow` y **no** es `suspend`: llamarla
  no hace nada hasta que alguien colecciona, y a partir de ahí Room emite la
  lista actualizada **cada vez que la tabla cambia**. Es la pieza que hace
  reactiva toda la cadena hasta la UI.

`OnConflictStrategy.REPLACE` convierte el insert en un *upsert*: si llega la
frase con un `id` ya existente (el endpoint aleatorio repite), la fila se
sustituye — se actualiza su `guardada_en` — en vez de fallar con
`SQLiteConstraintException` o duplicarse.

### 3.3 La base de datos (`data/local/AppDatabase.kt`)

```kotlin
@Database(entities = [FraseEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fraseDao(): FraseDao
}
```

`version = 1` es un contrato serio: cuando el esquema cambie (una columna
nueva, otra tabla) habrá que subirla **y** decirle a Room cómo transformar
los datos existentes (una migración). Subir la versión sin migración =
excepción al arrancar. Es la próxima lección.

### 3.4 El módulo de Hilt (`di/BaseDatosModule.kt`)

```kotlin
@Provides @Singleton
fun proveeBaseDatos(@ApplicationContext contexto: Context): AppDatabase =
    Room.databaseBuilder(contexto, AppDatabase::class.java, "holaandroid.db")
        .build()

@Provides
fun proveeFraseDao(baseDatos: AppDatabase): FraseDao = baseDatos.fraseDao()
```

Mismo patrón que `RedModule` (guía 06): clases construidas con builder de
terceros → `@Provides`; una sola instancia (`@Singleton`) porque abrir
SQLite es caro y dos instancias sobre el mismo fichero acaban en bloqueos.
El DAO **se pide a la base de datos**, no se construye: su `@Provides` no
necesita `@Singleton` porque `AppDatabase` ya devuelve siempre el mismo.

## 4. El repositorio, por fin, coordina

Hasta ahora `DefaultFraseRepository` solo traducía DTO → dominio. Ahora:

```kotlin
@Singleton
class DefaultFraseRepository @Inject constructor(
    private val api: FrasesApi,      // fuente remota (guía 06)
    private val fraseDao: FraseDao   // fuente local (esta guía)
) : FraseRepository {

    override suspend fun obtenerFraseAleatoria(): Frase {
        val dto = api.obtenerFraseAleatoria()
        fraseDao.insertar(dto.aEntidad())   // la red alimenta la caché local
        return dto.aDominio()
    }

    override fun observarFrasesGuardadas(): Flow<List<Frase>> =
        fraseDao.observarTodas().map { entidades -> entidades.map { it.aDominio() } }
}
```

Los detalles que importan:

- La interfaz `FraseRepository` gana `observarFrasesGuardadas():
  Flow<List<Frase>>` — **de dominio**. Ni `FraseEntity` ni `FraseDto` cruzan
  la frontera de la capa de datos; el `map` del Flow traduce en el camino.
- Nadie por encima decide "guárdalo": persistir lo que llega de la red es
  **política de la capa de datos**. El ViewModel pide una frase y observa
  las guardadas; no sabe que existe una base de datos.
- El caso de uso nuevo (`ObservarFrasesGuardadasUseCase`) no tiene
  `try/catch` ni `Result`, a diferencia del de red: leer de la base local no
  falla por las razones por las que falla la red. Cada fuente tiene su
  contrato de errores.

## 5. La reactividad recorre todas las capas

En el ViewModel, el `combine` de la guía 05 gana un cuarto flujo:

```kotlin
combine(esFormal, observarContador(), fraseState, observarFrasesGuardadas()) {
    formal, contador, frase, guardadas -> SaludoUiState(...)
}
```

Y ese es todo el código de actualización que existe. No hay ningún
"refrescar lista": pulsar el botón inserta en la tabla → Room emite la lista
nueva → `combine` recalcula el `SaludoUiState` → Compose recompone la
sección. La UI la pinta un `LazyColumn` (compone solo las filas visibles;
la lista crece sin límite):

```
DATA    FraseDao.observarTodas()  ──Flow<List<FraseEntity>>──┐
          ▲ (Room emite al cambiar la tabla)                 │ map → dominio
        DefaultFraseRepository ──Flow<List<Frase>>───────────┘
              │
DOMAIN  ObservarFrasesGuardadasUseCase()
              │
UI      combine(...) ─► uiState.frasesGuardadas ─► LazyColumn recompone
```

Compárese con el contador de la guía 05: mismo patrón (fuente observable →
Flow → combine → estado inmutable), distinta tecnología debajo. Esa es la
gracia de la arquitectura: **añadir Room no ha tocado ni una línea del
dominio existente ni de la lógica de UI** — solo se añadió una fuente, un
caso de uso y un campo más en el estado.

## 6. Errores típicos con Room

| Síntoma | Causa |
|---|---|
| `Cannot find implementation for ...AppDatabase` (en runtime) | falta `ksp(libs.androidx.room.compiler)`: sin procesador no hay código generado |
| error de compilación citando tu SQL | `@Query` mal escrita — Room valida el SQL al compilar (es una ventaja, no un fastidio) |
| `Cannot access database on the main thread` | operación de un disparo sin `suspend` llamada desde el hilo principal (no uses `allowMainThreadQueries()`: oculta el problema) |
| `SQLiteConstraintException: UNIQUE constraint failed` | insert de una clave primaria repetida sin `onConflict` |
| `IllegalStateException: A migration from 1 to 2 was necessary` | subiste `version` sin proporcionar la migración |
| `Room cannot verify the data integrity` | cambiaste el esquema SIN subir `version` (típico en desarrollo: desinstala la app o sube la versión) |
| La lista no se actualiza sola | la consulta devuelve `List` en vez de `Flow<List>` (una foto, no un flujo) |

## 7. Verificar

### 7.1 Sin dispositivo físico: emulador desde la CLI

Esta lección se verificó en un emulador creado y manejado íntegramente por
línea de comandos (18-07-2026, este equipo). Receta y sus dos trampas:

```bash
# 1. Imagen de sistema ESTABLE (ver trampa 2) + AVD
sdkmanager "system-images;android-36;google_apis;x86_64"
avdmanager create avd -n pruebas-room \
    -k "system-images;android-36;google_apis;x86_64" -d medium_phone

# 2. Arrancar sin ventana (ver trampa 1) y esperar el boot
ANDROID_AVD_HOME=~/.config/.android/avd \
    emulator -avd pruebas-room -no-window -no-audio -no-boot-anim \
             -gpu swiftshader_indirect -no-snapshot &
adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
```

- **Trampa 1 — el emulador no encuentra los AVD**: en este equipo
  `avdmanager` los crea en `~/.config/.android/avd` (ruta XDG) pero
  `emulator -list-avds` mira en `~/.android/avd` y devuelve vacío. Se
  resuelve exportando `ANDROID_AVD_HOME=~/.config/.android/avd`.
- **Trampa 2 — las imágenes pre-release no arrancan**: con la imagen
  `google_apis_playstore_ps16k` (16 KB page size, *Pre-Release*) el
  arranque se quedó colgado más de 25 minutos con la CPU al 100%. Con la
  estable `android-36;google_apis` arranca en ~40 segundos. Misma lección
  que con las librerías (guía 06): lo último no es lo estable. Bonus: las
  imágenes `google_apis` (sin Play Store) permiten `adb root` y `run-as`
  sin trabas.

Interacción sin tocar pantalla: `adb shell input tap X Y` pulsa botones
(las coordenadas se sacan de un `adb exec-out screencap -p > captura.png`),
y el modo avión se conmuta con `adb shell cmd connectivity airplane-mode
enable|disable`.

### 7.2 La prueba de la persistencia

```bash
cd HolaAndroid && ./gradlew installDebug
```

La prueba completa, en tres pasos (ejecutada y superada en el emulador):

1. Pulsa **"Frase del día"** varias veces → la sección "Guardadas en el
   dispositivo (N)" crece con cada frase, ordenada por recencia.
2. **Modo avión** y pulsa otra vez → error de red (guía 06)… pero la lista
   guardada sigue ahí: esa es la diferencia entre memoria y persistencia.
3. **Cierra la app del todo** (deslízala de recientes) y ábrela → las
   frases siguen. Con el contador de SharedPreferences (guía 04) ya vimos
   persistencia clave-valor; esto es persistencia estructurada y consultable.

Para ver la base de datos por dentro hay dos caminos:

- Android Studio → **App Inspection → Database Inspector** con la app en
  marcha (muestra la tabla `frases` en vivo y admite consultas).
- Por CLI, con `run-as` (funciona porque el build debug es depurable):

  ```bash
  adb shell run-as com.aprender.holaandroid \
      sqlite3 databases/holaandroid.db "SELECT * FROM frases;"
  # 1410|I Try To Build A Full Personality...|Walt Disney|1784388466456
  # 37|What Do I Wear In Bed?...|Marilyn Monroe|1784388462435
  ```

  Ahí se ven las decisiones del §3.1 funcionando: la clave primaria es el
  id real de la API y `guardada_en` ordena por recencia. Junto al `.db`
  aparecen `-wal` y `-shm`: el *write-ahead log* de SQLite que Room activa
  por defecto.

El esquema exportado queda en
`app/schemas/com.aprender.holaandroid.data.local.AppDatabase/1.json`.

## Fuentes consultadas (18-07-2026)

- Room (guía oficial): <https://developer.android.com/training/data-storage/room>
- Room y Flow (consultas observables): <https://developer.android.com/training/data-storage/room/async-queries>
- Plugin de Gradle y exportación de esquemas: <https://developer.android.com/training/data-storage/room/migrating-db-versions>
- Notas de versión de Room (2.7: APIs de ktx movidas a runtime; 2.8.4 estable): <https://developer.android.com/jetpack/androidx/releases/room>
- Índice de versiones del Maven de Google: <https://dl.google.com/android/maven2/androidx/room/group-index.xml>
- Capa de datos y "fuente de verdad": <https://developer.android.com/topic/architecture/data-layer>

Próxima lección natural: **migraciones de esquema** (añadir una columna
"favorita" subiendo `version` a 2 con su `Migration`), o **tests** de la
capa de datos — Room en un test instrumentado y la API con MockWebServer
(apuntado en la guía 06).
