# Anexo 3 — Del Concepto al Diseño: de la Idea a la Pantalla

Objetivo: recorrer, paso a paso, el camino que convierte una idea vaga en un
diseño de app listo para implementar. No es un anexo de código, sino el proceso
que **precede** al código: entender el problema, definir el alcance, dibujar los
flujos y llegar a pantallas concretas que luego construirás con Compose y
Material 3 (Guías 03–14).

Para que no sea teoría abstracta, seguiremos un ejemplo real de principio a fin:

> 💡 **Ejemplo hilo conductor — "GastosJuntos".** Una app para repartir gastos
> compartidos entre un grupo (compañeros de piso, un viaje). La usaremos en cada
> fase para ver qué produce cada paso.

El marco general es el **Doble Diamante**: primero se **explora** el problema y
luego se **acota** una solución, dos veces.

```
   PROBLEMA                         SOLUCIÓN
  ◄─ divergir ─►  ◄─ converger ─►  ◄─ divergir ─►  ◄─ converger ─►
   Descubrir        Definir          Idear           Entregar
   (fases 1-2)      (fase 3)         (fases 4-6)      (fases 7-9)
```

---

## Fase 1 — De la idea al concepto

Una idea es una frase; un concepto responde **qué problema resuelves, para quién
y por qué importa**. Antes de diseñar nada, escríbelo en una sola frase.

- **Declaración de problema**: *"A los grupos que comparten gastos les cuesta
  saber quién debe qué a quién, y las cuentas a mano generan discusiones."*
- **Público objetivo**: grupos pequeños con gastos recurrentes (pisos, viajes).
- **Propuesta de valor**: *"Registra un gasto en segundos y ve al instante el
  saldo de cada persona, sin hojas de cálculo."*
- **Reformula como oportunidad** ("*How Might We*"): *"¿Cómo podríamos hacer que
  registrar y saldar un gasto compartido sea tan fácil como enviar un mensaje?"*

> ⚠️ **Enamórate del problema, no de la solución.** Si empiezas por "quiero hacer
> una app con IA / con gráficos 3D", estás fijando la solución antes de entender
> el problema. El concepto describe el **dolor**, no la tecnología.

---

## Fase 2 — Entender a las personas (investigación)

El objetivo es reemplazar tus suposiciones por evidencia. No hace falta un estudio
caro: 5 conversaciones bien hechas ya revelan patrones.

| Técnica | Para qué sirve |
|---|---|
| Entrevistas (5–8 personas) | Descubrir cómo resuelven hoy el problema y qué les frustra. |
| Análisis de competencia | Ver qué hacen apps similares y dónde fallan. |
| *Jobs To Be Done* | Enfocar la tarea real: *"cuando pago la cena del grupo, quiero registrar quién participó para que se reparta justo."* |

Con lo aprendido, condensa a los usuarios en **personas** (arquetipos) y un **mapa
de empatía** (qué dicen, piensan, hacen y sienten):

> **Persona — "Marta, 26, comparte piso".** Paga la compra común y persigue a sus
> compañeros para cobrar. Quiere apuntar el gasto en el momento, desde el súper,
> sin fricción, y que la app le diga a quién reclamar.

---

## Fase 3 — Definir el alcance y el MVP

Aquí se **converge**: de todo lo que *podrías* hacer, eliges lo mínimo que aporta
valor. Expresa las funciones como **historias de usuario**:

> *"Como miembro de un grupo, quiero añadir un gasto e indicar entre quiénes se
> reparte, para ver actualizado cuánto debo o me deben."*

Prioriza con **MoSCoW** (Must / Should / Could / Won't):

| Prioridad | Funciones |
|---|---|
| **Must** (MVP) | Crear grupo, añadir gasto, ver saldos por persona. |
| **Should** | Marcar deudas como saldadas, historial. |
| **Could** | Fotos de tickets, recordatorios, divisas. |
| **Won't (ahora)** | Chat integrado, integración bancaria. |

Define **métricas de éxito** desde ya (te dirán si el diseño funciona): *"el 80 %
de los usuarios registra su primer gasto en menos de 30 segundos."*

> ⚠️ **El enemigo del MVP es el "y ya que estamos…".** Cada función extra retrasa
> el aprendizaje. Envía lo mínimo, mide, y deja que los usuarios te digan qué
> añadir.

---

## Fase 4 — Arquitectura de información y flujos

Antes de dibujar pantallas, decide **qué contenido existe y cómo se conecta**.

- **Mapa del sitio (*sitemap*)**: la jerarquía de pantallas.
  `Grupos → Detalle de grupo → (Añadir gasto | Saldos | Historial)`
- **Flujo de usuario (*user flow*)**: la secuencia de pasos para completar una
  tarea, incluyendo decisiones. El *happy path* (camino ideal) primero; los
  errores y casos límite después.

```
[Lista de grupos] --tap "+"--> [Nuevo gasto]
        |                            |
        |                     ¿importe válido?
        |                       /        \
        |                     no          sí
        |                      |           |
        |                 [mostrar     [guardar] --> [Saldos actualizados]
        |                  error]
        └── tap grupo --> [Detalle: saldos + historial]
```

Un buen flujo revela pantallas y estados que no habías previsto (vacío, error,
cargando) **antes** de invertir tiempo en dibujarlos bonitos.

---

## Fase 5 — Bocetos y wireframes (baja fidelidad)

Ahora sí, pantallas: pero en **blanco y negro, sin estilo**. El objetivo es
resolver la **estructura y la jerarquía** (qué es lo más importante de cada
pantalla) sin distraerse con colores. Boceta rápido en papel y digitalízalo como
*wireframe*.

```
┌─────────────────────────────┐
│  ← Viaje a Lisboa       ⋮    │  Barra superior (TopAppBar)
├─────────────────────────────┤
│  Tu saldo:      +45,00 € 🟢  │  Dato más importante, arriba y grande
├─────────────────────────────┤
│  Últimos gastos             │
│  ┌─────────────────────────┐│
│  │ Cena        30 €  Marta ││  Lista (LazyColumn)
│  │ Taxi        12 €  Luis  ││
│  └─────────────────────────┘│
│                             │
│                       ( + ) │  Acción principal (FAB)
└─────────────────────────────┘
```

Principios que aplicas aquí (Guía 03 los materializa en Compose):

- **Jerarquía visual**: lo más importante, más grande y arriba.
- **Ley de proximidad**: agrupa lo relacionado; separa lo distinto.
- **Una acción principal por pantalla**: destacada (el FAB), el resto secundario.
- **Diseña los estados vacíos y de error**, no solo la pantalla "llena".

---

## Fase 6 — Prototipado

Sube la fidelidad y **conecta** las pantallas para simular la app real. Un
prototipo interactivo (en Figma, por ejemplo) permite "usar" la app antes de
programarla y detectar fricciones en el flujo.

- **Media fidelidad**: wireframes conectados con navegación; validas el flujo.
- **Alta fidelidad**: ya con estilo visual; se parece al producto final.
- Prototipa el *happy path* completo y los 2–3 puntos donde temes que el usuario
  se atasque.

---

## Fase 7 — Diseño visual y sistema de diseño

Con la estructura validada, aplicas la piel. En Android esto es **Material 3**, y
conviene pensar en **tokens** (decisiones reutilizables), no en valores sueltos.
Esta fase enlaza directamente con las Guías 10 y 11.

| Dimensión | Decisión de diseño | En el proyecto |
|---|---|---|
| Color | Paleta con roles semánticos (`primary`, `surface`, `error`…) | Guía 10 (`ColorScheme`, dark mode, dynamic color) |
| Tipografía | Escala de estilos (`headline`, `title`, `body`, `label`) | Guía 10 (`Typography`) |
| Forma y elevación | Esquinas y jerarquía por elevación tonal | Guía 11 (tokens, *tonal elevation*) |
| Espaciado | Rejilla base de 4/8 dp consistente | Guía 03 (`padding`, `Arrangement.spacedBy`) |
| Componentes | Botones, tarjetas, campos con sus **estados** | Guías 04–08 |

> 💡 **Diseña los estados, no solo el aspecto.** Cada componente tiene estados:
> normal, presionado, deshabilitado, error, foco. Y cada pantalla: con datos,
> vacía, cargando y con error. Un diseño "terminado" los contempla todos.

**Accesibilidad** desde el diseño, no como parche: contraste de texto suficiente
(WCAG AA), objetivos táctiles de al menos 48 dp, no comunicar solo con color, y
textos de contenido para lectores de pantalla (`contentDescription`).

---

## Fase 8 — Validación e iteración

Un diseño no está "bien" porque a ti te lo parezca, sino porque la gente lo usa
sin tropezar. Valídalo barato y pronto:

- **Test de usabilidad con 5 usuarios**: dales una tarea real ("registra la cena
  de anoche") y **observa sin ayudar**. Donde dudan, hay un problema de diseño.
- **Evaluación heurística** (heurísticas de Nielsen): visibilidad del estado,
  lenguaje del usuario, prevención de errores, consistencia, control y libertad…
- **Itera**: corrige, vuelve a probar. El diseño es un bucle, no una línea recta.

> ⚠️ **Observa lo que hacen, no lo que dicen.** La gente es amable ("está muy
> bien") pero su comportamiento no miente: si nadie encuentra el botón de saldar,
> el botón está mal, no el usuario.

---

## Fase 9 — Entrega a desarrollo (*handoff*)

El diseño se traduce a código. Un buen *handoff* evita idas y venidas:

- **Especificaciones**: medidas, espaciados, colores (como tokens), tipografías y
  comportamiento de cada estado.
- **Mapa de componentes**: cada elemento del diseño ↔ su composable de Material 3
  (una tarjeta → `Card`/`ElevatedCard` de la Guía 05; el `+` → `FloatingActionButton`
  de la Guía 04; la lista → `LazyColumn` de la Guía 05).
- **Tokens compartidos**: los colores/tipografías del diseño se vuelven el
  `MaterialTheme` de la app (Guía 10), de modo que diseño y código hablan el mismo
  idioma.

Así, la pantalla de "GastosJuntos" que bocetaste en la fase 5 aterriza como un
`Scaffold` con `TopAppBar`, un saldo destacado, un `LazyColumn` de gastos y un
`FloatingActionButton`, tal como se construye en las guías del Módulo II.

---

## Herramientas habituales

| Etapa | Herramientas |
|---|---|
| Ideación y notas | Papel, pizarra, FigJam, Miro |
| Wireframes y prototipos | Figma (estándar), Penpot (libre) |
| Sistemas de diseño | Material Theme Builder (genera el `ColorScheme` de M3) |
| Iconografía | Material Symbols |

---

## Resumen del recorrido

```
Idea → Concepto → Investigación → Alcance/MVP → Flujos → Wireframes
     → Prototipo → Diseño visual (M3) → Validación → Handoff → Código
```

Cada flecha reduce incertidumbre. Cuanto antes descubras un problema (en un boceto
de papel, no en código ya escrito), más barato es arreglarlo.

---

## Fuentes consultadas (19-07-2026)

- Proceso de diseño / Doble Diamante (Design Council): <https://www.designcouncil.org.uk/our-resources/the-double-diamond/>
- Material Design 3 — guías de diseño: <https://m3.material.io/>
- Material Theme Builder: <https://material-foundation.github.io/material-theme-builder/>
- 10 heurísticas de usabilidad (Nielsen Norman Group): <https://www.nngroup.com/articles/ten-usability-heuristics/>
- Accesibilidad en Android: <https://developer.android.com/guide/topics/ui/accessibility>
