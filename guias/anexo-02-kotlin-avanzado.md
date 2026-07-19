# Anexo 2 — Kotlin Avanzado

Objetivo: dar el salto de "escribir Kotlin correcto" a "aprovechar sus
mecanismos avanzados": funciones de orden superior, lambdas con receptor, `inline`
y `reified`, genéricos con varianza, delegación y sobrecarga de operadores. Cada
apartado explica el mecanismo y muestra un uso real.

---

## 1. Funciones de orden superior y tipos función

En Kotlin las funciones son ciudadanos de primera clase: una función puede recibir
otra como parámetro o devolverla. El tipo de una función se escribe
`(Entrada) -> Salida`.

```kotlin
// 'operacion' es una función que recibe dos Int y devuelve Int
fun calcular(a: Int, b: Int, operacion: (Int, Int) -> Int): Int = operacion(a, b)

val suma = calcular(2, 3) { x, y -> x + y }        // 5
val producto = calcular(2, 3) { x, y -> x * y }    // 6
```

Reglas idiomáticas que ya usas sin darte cuenta:

- **Trailing lambda**: si el último parámetro es una función, la lambda va fuera
  de los paréntesis (`items.forEach { ... }`).
- **`it`**: cuando la lambda tiene un solo parámetro, puedes omitir su nombre.
- Una función puede **devolver** otra función:

```kotlin
fun multiplicadorPor(factor: Int): (Int) -> Int = { numero -> numero * factor }

val triple = multiplicadorPor(3)
triple(10) // 30
```

---

## 2. Referencias a funciones y propiedades (`::`)

Cuando ya existe una función que hace justo lo que necesita una lambda, pásala por
referencia con `::` en lugar de envolverla.

```kotlin
val nombres = listOf("ana", "luis")

// En vez de: nombres.map { it.uppercase() }
val enMayus = nombres.map(String::uppercase)          // referencia a método

fun esPar(n: Int) = n % 2 == 0
val pares = (1..10).filter(::esPar)                    // referencia a función top-level

val longitudes = nombres.map(String::length)          // referencia a propiedad

// Referencia ligada a una instancia concreta
val saludador = "Hola"::plus
saludador(" mundo")                                    // "Hola mundo"
```

---

## 3. Lambdas con receptor: la base de los DSL

Un tipo función puede tener un **receptor**: `T.() -> Unit`. Dentro de la lambda,
`this` es una instancia de `T`, así que puedes llamar a sus miembros sin
prefijo. Es el mecanismo detrás de `apply`, `buildString` y de los *builders* de
Compose o Gradle.

```kotlin
// buildString ya usa este patrón por dentro
val texto = buildString {
    append("Hola")      // this es un StringBuilder
    append(", mundo")
}

// Construir un DSL propio
class MenuBuilder {
    val platos = mutableListOf<String>()
    fun plato(nombre: String) { platos.add(nombre) }
}

fun menu(bloque: MenuBuilder.() -> Unit): List<String> =
    MenuBuilder().apply(bloque).platos

val carta = menu {
    plato("Ensalada")   // llamamos a plato() sin escribir 'builder.'
    plato("Pasta")
}
```

---

## 4. `inline`, `noinline` y `crossinline`

Cada lambda que pasas a una función normal crea, por debajo, un objeto. En
funciones de orden superior muy usadas eso tiene coste. `inline` le pide al
compilador que **incruste** el cuerpo de la función (y de sus lambdas) en el
punto de llamada, eliminando ese objeto y habilitando los *non-local returns*.

```kotlin
inline fun medirTiempo(bloque: () -> Unit): Long {
    val inicio = System.nanoTime()
    bloque()
    return System.nanoTime() - inicio
}

fun buscar(usuarios: List<Usuario>, id: String): Usuario? {
    usuarios.forEach { u ->
        if (u.id == id) return u   // 'return' sale de buscar(): posible porque forEach es inline
    }
    return null
}
```

- **`noinline`**: marca una lambda concreta para que NO se incruste (p. ej. si
  necesitas guardarla en una variable).
- **`crossinline`**: permite incrustar una lambda pero prohíbe los *non-local
  returns* dentro de ella (necesario cuando la lambda se invoca desde otro
  contexto, como un `Runnable`).

> Usa `inline` sobre todo en funciones pequeñas con parámetros lambda. No lo
> apliques a funciones grandes: duplicarías mucho código en cada llamada.

---

## 5. Parámetros de tipo `reified`

Por el *type erasure* de la JVM, normalmente no puedes consultar `T` en tiempo de
ejecución. Dentro de una función `inline`, un parámetro `reified` conserva el
tipo real y te deja usar `is T`, `T::class` o `filterIsInstance<T>()`.

```kotlin
inline fun <reified T> List<*>.soloDeTipo(): List<T> = filterIsInstance<T>()

val mezcla: List<Any> = listOf(1, "dos", 3, "cuatro")
val soloTextos: List<String> = mezcla.soloDeTipo()   // ["dos", "cuatro"]

// Patrón habitual: navegar a una Activity sin repetir la clase
inline fun <reified T : Activity> Context.abrir() {
    startActivity(Intent(this, T::class.java))
}
// uso: context.abrir<DetalleActivity>()
```

---

## 6. Genéricos con varianza (`out`, `in`) y límites

La varianza indica cómo se relacionan los genéricos al heredar los tipos:

- **`out T`** (covariante): la clase solo **produce** `T` (lo devuelve). Permite
  que `Repositorio<Perro>` sea un `Repositorio<Animal>`.
- **`in T`** (contravariante): la clase solo **consume** `T` (lo recibe).

```kotlin
interface Fuente<out T> {          // solo produce T
    fun obtener(): T
}
interface Sumidero<in T> {         // solo consume T
    fun aceptar(valor: T)
}

// Límite de tipo: T debe ser Comparable
fun <T : Comparable<T>> maximo(a: T, b: T): T = if (a >= b) a else b

// Varios límites con 'where'
fun <T> copiarSiCabe(origen: T) where T : CharSequence, T : Appendable { /* ... */ }
```

`List<out E>` es covariante por eso: puedes tratar un `List<String>` como
`List<Any>`; `MutableList` no, porque también consume elementos.

---

## 7. Funciones y propiedades de extensión avanzadas

Más allá de lo básico, las extensiones pueden ser **propiedades**, actuar sobre
tipos **anulables** o **genéricos**, y colgarse de un `companion object`.

```kotlin
// Propiedad de extensión (sin backing field: se calcula al vuelo)
val String.esPalindromo: Boolean
    get() = this == this.reversed()

// Extensión sobre tipo anulable: puede manejar el null internamente
fun String?.oVacio(): String = this ?: ""

// Extensión genérica
fun <T> T.enLista(): List<T> = listOf(this)

// Extensión sobre companion → se lee como método "estático"
class Color(val hex: String) {
    companion object
}
fun Color.Companion.desdeNombre(nombre: String): Color = Color("#000000")
val negro = Color.desdeNombre("negro")
```

> La resolución de extensiones es **estática**: se decide por el tipo declarado,
> no por el tipo real en runtime. No sobreescriben métodos miembro (si existe un
> método con la misma firma, gana el miembro).

---

## 8. Delegación de propiedades (`by`)

`by` delega el `get`/`set` de una propiedad en otro objeto. La biblioteca estándar
trae varios delegados listos, y puedes escribir el tuyo.

```kotlin
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

// Lazy: se calcula la primera vez que se lee
val config: Config by lazy { cargarConfig() }

// Observable: reacciona a cada cambio
var contador: Int by Delegates.observable(0) { _, viejo, nuevo ->
    println("cambió de $viejo a $nuevo")
}

// vetoable: puede rechazar el cambio
var edad: Int by Delegates.vetoable(0) { _, _, nuevo -> nuevo >= 0 }

// Delegado propio: implementa getValue/setValue con 'operator'
class Mayusculas {
    private var valor: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = valor
    operator fun setValue(thisRef: Any?, property: KProperty<*>, nuevo: String) {
        valor = nuevo.uppercase()
    }
}
var titulo: String by Mayusculas()   // titulo = "hola" → guarda "HOLA"
```

---

## 9. Delegación de clase: composición sobre herencia

Con `by` una clase puede implementar una interfaz **delegando** en otra instancia,
y sobreescribir solo lo que necesite. Es composición sin escribir código
repetitivo de reenvío.

```kotlin
interface Repositorio {
    fun leer(id: String): String
    fun guardar(id: String, dato: String)
}

class RepositorioReal : Repositorio {
    override fun leer(id: String) = "dato-$id"
    override fun guardar(id: String, dato: String) { /* ... */ }
}

// Añade logging reutilizando toda la implementación real salvo lo que redefine
class RepositorioConLog(
    private val delegado: Repositorio
) : Repositorio by delegado {
    override fun leer(id: String): String {
        println("Leyendo $id")
        return delegado.leer(id)
    }
    // guardar() se hereda del delegado sin escribir nada
}
```

---

## 10. `value class` y `typealias`: seguridad de tipos sin coste

Una `value class` (`@JvmInline`) envuelve un único valor para dar significado y
evitar confundir parámetros del mismo tipo primitivo; en tiempo de ejecución se
representa como el valor interno, sin *overhead*.

```kotlin
@JvmInline value class UserId(val valor: String)
@JvmInline value class Email(val valor: String)

fun enviar(a: UserId, correo: Email) { /* ... */ }

// enviar(email, userId) NO compila: los tipos no se pueden intercambiar

// typealias: solo un alias de nombre (NO da seguridad de tipos, solo legibilidad)
typealias Manejador = (Evento) -> Unit
```

---

## 11. Operadores, `infix` e `invoke`

Kotlin permite dar sintaxis natural a tus tipos sobrecargando operadores con
`operator fun`, definiendo funciones `infix` y haciendo un objeto "invocable".

```kotlin
data class Vector(val x: Int, val y: Int) {
    operator fun plus(otro: Vector) = Vector(x + otro.x, y + otro.y)   // a + b
    operator fun get(indice: Int) = if (indice == 0) x else y          // v[0]
}

val v = Vector(1, 2) + Vector(3, 4)   // Vector(4, 6)
val primera = v[0]                    // 4

// infix: se llama sin punto ni paréntesis
infix fun Int.elevadoA(exp: Int): Int {
    var r = 1; repeat(exp) { r *= this }; return r
}
val ocho = 2 elevadoA 3               // 8

// operator invoke: el objeto se usa como si fuera una función
class Descuento(val porcentaje: Int) {
    operator fun invoke(precio: Double) = precio * (1 - porcentaje / 100.0)
}
val rebaja = Descuento(20)
val finalPrice = rebaja(50.0)         // 40.0
```

---

## 12. Detalles avanzados que conviene conocer

**Destructuring**: cualquier tipo con funciones `componentN()` (las `data class`
las generan) se puede desestructurar.

```kotlin
val (id, nombre) = usuario
for ((clave, valor) in mapa) { /* ... */ }
```

**El tipo `Nothing`**: representa "no hay valor" / "nunca retorna". Permite que
`throw` y funciones como `error()` encajen en cualquier expresión, y hace
covariar bien los `sealed` genéricos (`Fallo : Resultado<Nothing>`).

```kotlin
fun fallar(msg: String): Nothing = throw IllegalStateException(msg)
val nombre = usuario.nombre ?: fallar("Usuario sin nombre")
```

**`tailrec`**: convierte una recursión de cola en un bucle, evitando el
desbordamiento de pila.

```kotlin
tailrec fun mcd(a: Int, b: Int): Int = if (b == 0) a else mcd(b, a % b)
```

**Contratos (`contract`)**: informan al compilador de garantías que él no puede
deducir solo, mejorando el *smart cast* (API experimental).

```kotlin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Any?.esTextoNoVacio(): Boolean {
    contract { returns(true) implies (this@esTextoNoVacio is String) }
    return this is String && this.isNotEmpty()
}
```

---

## Fuentes consultadas (19-07-2026)

- Funciones de orden superior y lambdas: <https://kotlinlang.org/docs/lambdas.html>
- Funciones `inline`: <https://kotlinlang.org/docs/inline-functions.html>
- Genéricos y varianza: <https://kotlinlang.org/docs/generics.html>
- Propiedades delegadas: <https://kotlinlang.org/docs/delegated-properties.html>
- Sobrecarga de operadores: <https://kotlinlang.org/docs/operator-overloading.html>
