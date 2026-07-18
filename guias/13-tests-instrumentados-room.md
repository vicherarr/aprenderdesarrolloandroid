# Guía 13 — Tests instrumentados: Room de verdad y la migración con guardián

Objetivo: los primeros tests que corren **en el dispositivo** — el DAO
contra el SQLite real (el SQL de `@Query`, probado de verdad) y la
migración 1→2 de la guía 08 reproducida automáticamente con
`MigrationTestHelper` sobre los JSON de `app/schemas/`. Cuarta entrega de
la serie de tests (10: fundamentos, 11: ViewModel, 12: red).

---

## 1. Qué es un test instrumentado y cuándo lo necesitas

Vive en `app/src/androidTest/`, se compila en un APK de test y corre en un
dispositivo o emulador, orquestado por el runner (`AndroidJUnitRunner`, ya
configurado por la plantilla desde la guía 02). Es la segunda planta de la
pirámide: más lento que la JVM (~1 min con arranque de Gradle frente a
segundos), pero es el único sitio donde existen las cosas de verdad:
SQLite, el sistema de ficheros, el framework.

La regla de reparto quedó dibujada en las guías anteriores:

| Qué | Dónde se prueba | Por qué |
|---|---|---|
| Clientes del DAO (repositorio...) | JVM con `FakeFraseDao` (guías 10-12) | rápido; el contrato basta |
| **El DAO mismo** (su SQL) | **instrumentado** | `@Query` es SQL: lo ejecuta SQLite, no Kotlin |
| **Las migraciones** | **instrumentado** | son SQL contra esquemas históricos |

El fake y el test instrumentado son complementarios, no redundantes: el
fake *asume* que el DAO cumple su contrato; este test *demuestra* que lo
cumple.

## 2. Piezas y versiones

| Librería | Versión | Cómo se decidió |
|---|---|---|
| `androidx.room:room-testing` | **2.8.4** | trae `MigrationTestHelper`; misma versión que el runtime, siempre |
| `kotlinx-coroutines-test` | 1.8.1 | la misma de la guía 10, ahora también en `androidTestImplementation` |

(`androidx.test.ext:junit` y el runner ya estaban desde la plantilla.)

## 3. El DAO contra el SQLite real

```kotlin
@RunWith(AndroidJUnit4::class)
class FraseDaoTest {
    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        baseDatos = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        dao = baseDatos.fraseDao()
    }

    @After
    fun cerrar() = baseDatos.close()
}
```

`inMemoryDatabaseBuilder`: la base de datos vive en RAM y muere con
`close()` — cada test estrena una, sin ficheros que limpiar ni estado que
se filtre entre tests. Los tres tests apuntan al SQL que ningún fake puede
probar:

- **El upsert de verdad**: dos `insertar` con el mismo `id` dejan UNA fila
  con los datos del segundo (`OnConflictStrategy.REPLACE`, guía 07).
- **El `ORDER BY` de verdad**: con tres frases y la más antigua marcada
  favorita, el orden es `[1, 3, 2]` — favoritas primero, resto por
  recencia (la consulta de la guía 08).
- **El `NOT` de SQLite**: alternar favorita dos veces vuelve al origen.

Si mañana alguien toca el SQL de `observarTodas` y rompe el orden, el fake
de la JVM ni se entera; este test sí.

## 4. La migración, de prueba manual a guardián automático

En la guía 08 probamos la migración a mano: app v1 con datos en el
emulador, instalar v2, comprobar. `MigrationTestHelper` automatiza
exactamente eso:

```kotlin
@get:Rule
val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDatabase::class.java
)

@Test
fun migrar1a2ConservaLasFilasYFavoritaNaceEn0() {
    // 1. Base de datos EXACTAMENTE como era en la versión 1 (desde schemas/1.json)
    helper.createDatabase(nombreBd, 1).apply {
        execSQL("INSERT INTO frases (id, texto, autor, guardada_en) VALUES (42, 'texto', 'autora', 100)")
        close()
    }

    // 2. Ejecuta MIGRACION_1_2 y valida el esquema contra schemas/2.json
    val bd = helper.runMigrationsAndValidate(nombreBd, 2, true, MIGRACION_1_2)

    // 3. La fila sobrevivió y favorita nace en 0
    bd.query("SELECT id, autor, favorita FROM frases").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(42, cursor.getInt(0))
        assertEquals(0, cursor.getInt(2))
    }
}
```

Los detalles que hacen esto posible:

- **`createDatabase(nombre, 1)` construye la v1 desde `schemas/1.json`** —
  aunque `FraseEntity` ya no tenga esa forma en el código. Es LA razón por
  la que los JSON se versionan en git (guía 08 §4.1): son la única memoria
  de cómo eran los esquemas que tus usuarios aún tienen.
- El `INSERT` es SQL crudo **de la versión 1** (sin `favorita`): ni el DAO
  ni la entidad actuales sirven aquí, porque pertenecen al presente.
- **`runMigrationsAndValidate` valida, no solo ejecuta**: compara el
  esquema resultante contra `2.json`, columna a columna, `defaultValue`
  incluido.
- El plugin de Gradle de Room **expone `schemas/` a los tests
  automáticamente** — antes del plugin había que configurar
  `sourceSets.assets` a mano; verificado: sin configuración extra.

## 5. Calibración: el error de la guía 08, ahora en 10 segundos

Método de siempre. Se rompió la migración cambiando `DEFAULT 0` por
`DEFAULT 1` (esquema resultante ≠ esquema esperado) y:

```
MigracionesTest > migrar1a2ConservaLasFilasYFavoritaNaceEn0 FAILED
    java.lang.IllegalStateException: Migration didn't properly handle:  frases
```

Es **el mismo error** de la tabla de la guía 08 §6 — pero detectado por un
test en 10 segundos en vez de por un crash en el arranque de la app de un
usuario. Restaurado el `DEFAULT 0`, verde. A partir de ahora, cada
migración nueva (2→3, 3→4...) añade su test aquí antes de tocar producción.

## 6. Errores típicos con tests instrumentados de Room

| Hábito | Problema |
|---|---|
| Probar el DAO solo con fakes en la JVM | el SQL real queda sin ejecutar: los `ORDER BY`/`JOIN` rotos no se detectan |
| `databaseBuilder` (fichero) en tests | estado que se filtra entre tests; en memoria muere solo |
| Olvidar `close()` en `@After` | conexiones abiertas y avisos de fugas |
| Usar la entidad ACTUAL para poblar la BD vieja en un test de migración | la entidad pertenece al presente; la v1 se puebla con SQL crudo |
| No versionar `app/schemas/` en git | sin `1.json` no hay forma de recrear la BD que tienen tus usuarios |
| Probar migraciones solo con instalación limpia | las instalaciones limpias nunca migran (guía 08 §7): este helper existe para eso |

## 7. Verificar

```bash
cd HolaAndroid
./gradlew connectedDebugAndroidTest    # necesita emulador/dispositivo (guía 07 §7.1)
```

4 tests instrumentados (~1 min con el emulador ya arrancado). Informe en
`app/build/reports/androidTests/connected/debug/index.html`. La suite
completa del proyecto queda: **25 en la JVM + 4 en el dispositivo**.

## Fuentes consultadas (18-07-2026)

- Tests instrumentados (oficial): <https://developer.android.com/training/testing/instrumented-tests>
- Testear bases de datos Room (oficial): <https://developer.android.com/training/data-storage/room/testing-db>
- Testear migraciones con MigrationTestHelper (oficial): <https://developer.android.com/training/data-storage/room/migrating-db-versions#test>
- room-testing en el Maven de Google (versión = runtime): <https://dl.google.com/android/maven2/androidx/room/group-index.xml>

Siguiente de la serie (guía 14): **tests de UI de Compose** — el `when`
exhaustivo de `FraseSeccion`, el spinner de `Cargando` y el toque que marca
favoritas, probados con `createComposeRule` sobre el árbol de semántica.
