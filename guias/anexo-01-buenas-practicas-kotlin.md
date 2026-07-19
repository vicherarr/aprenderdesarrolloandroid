# Anexo 1 — Buenas Prácticas con Kotlin

Objetivo: reunir los hábitos de código que marcan la diferencia entre un Kotlin
"que funciona" y un Kotlin idiomático, seguro y mantenible. Cada punto incluye el
porqué y una comparación de la forma a evitar (❌) frente a la recomendada (✅).

---

## 1. Aprovecha el sistema de tipos nulos: nunca uses `!!`

La causa número uno de cierres inesperados (*crashes*) es el acceso a un valor
nulo. Kotlin distingue en el tipo si algo puede ser `null` (`String?`) o no
(`String`); apóyate en esa red de seguridad en lugar de saltártela con `!!`.

```kotlin
// ❌ !! lanza NullPointerException si el valor es nulo: anula la seguridad de tipos
val longitud = usuario!!.nombre!!.length

// ✅ Navegación segura (?.), valor por defecto (?:) y comprobaciones con smart-cast
val longitud = usuario?.nombre?.length ?: 0

fun saludar(usuario: Usuario?) {
    if (usuario == null) return          // early return
    // A partir de aquí, 'usuario' es no-nulo (smart cast) sin necesidad de ?.
    println("Hola, ${usuario.nombre}")
}
```

Reserva los tipos anulables para lo que realmente puede faltar. Si algo nunca
debería ser nulo, haz que su tipo lo refleje.

---

## 2. Prefiere la inmutabilidad: `val` sobre `var`

Un valor que no cambia no puede corromperse ni provocar condiciones de carrera.
Declara todo como `val` por defecto y cámbialo a `var` solo cuando de verdad
necesites reasignarlo. Lo mismo aplica a las colecciones: expón `List`, `Map` o
`Set` (solo lectura) en vez de sus variantes `Mutable`.

```kotlin
// ❌ Mutable sin necesidad; cualquiera puede reasignar o alterar la lista
var tareas: MutableList<String> = mutableListOf()

// ✅ Referencia fija y colección de solo lectura hacia fuera
val tareas: List<String> = listOf("Comprar", "Estudiar")

// Para "modificar", crea una copia nueva (los data class traen copy())
val usuarioActualizado = usuario.copy(nombre = "Ana")
```

---

## 3. Modela datos con `data class` y estados cerrados con `sealed`

Una `data class` genera `equals`, `hashCode`, `toString` y `copy` por ti. Una
jerarquía `sealed` (clase o interfaz) le dice al compilador que conoces **todos**
los casos posibles, de modo que un `when` sin `else` te obliga a tratarlos todos.

```kotlin
data class Usuario(val id: String, val nombre: String, val esPremium: Boolean)

// El estado de una pantalla como conjunto cerrado de posibilidades
sealed interface EstadoUi {
    data object Cargando : EstadoUi
    data class Exito(val usuarios: List<Usuario>) : EstadoUi
    data class Error(val mensaje: String) : EstadoUi
}

// when exhaustivo: si mañana añades un estado, esto deja de compilar hasta
// que lo contemples. Errores atrapados en compilación, no en producción.
fun render(estado: EstadoUi) = when (estado) {
    EstadoUi.Cargando -> mostrarSpinner()
    is EstadoUi.Exito -> mostrarLista(estado.usuarios)
    is EstadoUi.Error -> mostrarError(estado.mensaje)
}
```

---

## 4. Usa `if` y `when` como expresiones

En Kotlin la mayoría de las estructuras devuelven un valor. Aprovéchalo para
asignar directamente y para escribir funciones de una sola expresión: hay menos
variables mutables intermedias y el resultado se lee de un vistazo.

```kotlin
// ❌ Variable mutable que se rellena por ramas
var etiqueta: String
if (nota >= 5) etiqueta = "Aprobado" else etiqueta = "Suspenso"

// ✅ Expresión
val etiqueta = if (nota >= 5) "Aprobado" else "Suspenso"

// Función de una sola expresión
fun descripcion(nota: Int): String = when {
    nota >= 9 -> "Excelente"
    nota >= 5 -> "Aprobado"
    else -> "A mejorar"
}
```

---

## 5. Asincronía con corrutinas y *structured concurrency*

Lanza el trabajo asíncrono dentro de un *scope* con ciclo de vida
(`viewModelScope`, `lifecycleScope`), nunca en `GlobalScope`: así las tareas se
cancelan solas cuando la pantalla desaparece y no dejan fugas. Marca como
`suspend` las funciones que esperan, y mueve el trabajo pesado con `Dispatchers`.

```kotlin
// ❌ GlobalScope: vive para siempre, no se cancela, filtra memoria
GlobalScope.launch { repositorio.sincronizar() }

// ✅ Atado al ciclo de vida del ViewModel
class MiViewModel(private val repositorio: Repositorio) : ViewModel() {
    fun cargar() {
        viewModelScope.launch {                 // se cancela con el ViewModel
            val datos = withContext(Dispatchers.IO) { repositorio.leer() }
            _estado.value = EstadoUi.Exito(datos)
        }
    }
}

// Para flujos de datos que emiten con el tiempo, usa Flow en vez de callbacks
fun observarTareas(): Flow<List<Tarea>> = dao.observarTareas()
```

---

## 6. Haz explícito lo que puede fallar; no te tragues las excepciones

Un `catch` vacío esconde errores hasta que explotan en otro sitio. Devuelve el
fallo como parte del tipo de retorno (un `sealed` o `Result`) para que quien
llame esté obligado a considerarlo.

```kotlin
// ❌ La excepción desaparece; el llamador cree que todo fue bien
fun leer(): String {
    return try { archivo.readText() } catch (e: Exception) { "" }
}

// ✅ El resultado modela ambos caminos
sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>
    data class Fallo(val causa: Throwable) : Resultado<Nothing>
}

fun leer(): Resultado<String> =
    runCatching { archivo.readText() }
        .fold({ Resultado.Ok(it) }, { Resultado.Fallo(it) })
```

---

## 7. Argumentos con nombre y valores por defecto en vez de sobrecargas

Los parámetros por defecto eliminan la maraña de funciones sobrecargadas, y los
argumentos con nombre hacen que la llamada se entienda sin ir a mirar la firma.

```kotlin
// ❌ Telescoping: varias sobrecargas para las combinaciones
fun crearBoton(texto: String) { /* ... */ }
fun crearBoton(texto: String, habilitado: Boolean) { /* ... */ }

// ✅ Un único punto de entrada con defaults
fun crearBoton(
    texto: String,
    habilitado: Boolean = true,
    icono: Icono? = null,
) { /* ... */ }

// La llamada se autodocumenta y el orden deja de importar
crearBoton(texto = "Guardar", icono = Icono.Check)
```

---

## 8. Funciones de alcance (`let`, `apply`, `also`, `run`, `with`) con criterio

Son azúcar potente, pero anidarlas vuelve el código ilegible. Guía rápida:
`apply` para configurar y devolver el mismo objeto; `also` para efectos
secundarios (log); `let` para transformar o actuar sobre un valor no nulo; `run`
para calcular un resultado.

```kotlin
// ✅ apply: configurar un objeto y devolverlo
val intent = Intent(context, Detalle::class.java).apply {
    putExtra("id", id)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}

// ✅ let: ejecutar solo si no es nulo
usuario?.let { enviarCorreo(it.email) }

// ❌ Anidar varias funciones de alcance: difícil de seguir
config?.let { c -> c.run { with(c) { /* ¿quién es 'this' aquí? */ } } }
```

---

## 9. Añade legibilidad con funciones de extensión

En lugar de clases `Utils` con métodos estáticos, extiende los tipos que ya usas.
El código se lee como una frase y el autocompletado las descubre solas.

```kotlin
// ❌ TextUtils.esEmailValido(texto)
// ✅ texto.esEmailValido()
fun String.esEmailValido(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun Int.aDp(): Dp = this.dp
```

Evita extender tipos con lógica de negocio que dependa de un estado externo:
úsalas para transformaciones y ayudas puras.

---

## 10. Transforma colecciones con operadores funcionales

`map`, `filter`, `associateBy`, `groupBy`, `sumOf`… expresan la intención mucho
mejor que un bucle con acumuladores mutables.

```kotlin
// ❌ Bucle imperativo con estado mutable
val nombres = mutableListOf<String>()
for (u in usuarios) if (u.esPremium) nombres.add(u.nombre)

// ✅ Declarativo: qué queremos, no cómo recorrerlo
val nombres = usuarios.filter { it.esPremium }.map { it.nombre }

// Para colecciones muy grandes con varias etapas, asSequence() evita
// crear listas intermedias en cada paso
val total = numeros.asSequence().filter { it > 0 }.map { it * 2 }.sum()
```

---

## 11. Encapsula: mínima visibilidad y *backing properties*

Expón lo imprescindible. Marca como `private` o `internal` todo lo que no forme
parte de la API pública, y protege el estado mutable tras una propiedad de solo
lectura (patrón habitual en ViewModels con `StateFlow`).

```kotlin
class MiViewModel : ViewModel() {
    // Mutable y privado: solo el ViewModel puede emitir
    private val _estado = MutableStateFlow<EstadoUi>(EstadoUi.Cargando)
    // Público e inmutable: la UI solo observa
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()
}
```

---

## 12. `object` para singletons y `const val` para constantes

Usa `object` cuando necesites una única instancia, y `companion object` para
miembros ligados a la clase. Las constantes conocidas en compilación van con
`const val` (más eficientes que un `val` normal).

```kotlin
object Analytics {                 // singleton
    fun registrar(evento: String) { /* ... */ }
}

class ApiCliente {
    companion object {
        const val BASE_URL = "https://api.ejemplo.com/"   // constante en compilación
    }
}
```

---

## 13. Evita `lateinit`; prefiere `by lazy`

`lateinit` renuncia a la seguridad de nulos y lanza si accedes antes de tiempo.
Cuando el valor se puede calcular la primera vez que se usa, `by lazy` lo
inicializa bajo demanda y de forma segura.

```kotlin
// ❌ Acceder antes de asignar lanza UninitializedPropertyAccessException
private lateinit var formateador: DateFormat

// ✅ Se crea la primera vez que se lee, y se cachea
private val formateador: DateFormat by lazy {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
}
```

Reserva `lateinit` para casos donde el framework inyecta el valor después de
construir el objeto (p. ej. dependencias en tests).

---

## 14. Valida precondiciones con `require`, `check` y `requireNotNull`

Falla pronto y con un mensaje claro cuando los argumentos o el estado no son
válidos, en lugar de arrastrar datos corruptos.

```kotlin
fun transferir(cantidad: Int, saldo: Int) {
    require(cantidad > 0) { "La cantidad debe ser positiva, fue $cantidad" }  // IllegalArgumentException
    check(saldo >= cantidad) { "Saldo insuficiente" }                          // IllegalStateException
    // ...
}

val id = requireNotNull(argumentos["id"]) { "Falta el argumento 'id'" }
```

---

## 15. Sigue las convenciones idiomáticas y automatiza el estilo

Nombres en `camelCase` para funciones y propiedades, `PascalCase` para tipos,
`UPPER_SNAKE_CASE` para constantes. Usa *string templates* (`"Hola, $nombre"`) en
vez de concatenar, y expresa rangos e iteraciones de forma idiomática. Delega el
formato y las reglas a herramientas para no discutirlas en cada revisión:
**ktlint** o **detekt**.

```kotlin
// ❌ "Total: " + total + " €"
// ✅ Interpolación
val mensaje = "Total: $total €"

// ✅ Rangos idiomáticos
for (i in 1..10) { /* ... */ }
for (i in lista.indices) { /* ... */ }
repeat(3) { /* ... */ }
```

---

## Fuentes consultadas (19-07-2026)

- Convenciones de código de Kotlin (oficial): <https://kotlinlang.org/docs/coding-conventions.html>
- Guía de estilo de Kotlin para Android: <https://developer.android.com/kotlin/style-guide>
- Corrutinas y *structured concurrency*: <https://developer.android.com/kotlin/coroutines/coroutines-best-practices>
