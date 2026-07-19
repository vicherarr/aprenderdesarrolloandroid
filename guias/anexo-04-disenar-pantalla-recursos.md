# Anexo 4 — Diseñar una Pantalla Bonita: Consejos y Recursos

Objetivo: bajar del proceso general (Anexo 3) al detalle práctico de **diseñar
una pantalla concreta** para que se vea cuidada y agradable, y ofrecer una lista
curada de **recursos de inspiración** para nutrir tu gusto y resolver dudas de
color, tipografía, iconos e ilustraciones.

---

## Parte A — Consejos para diseñar una pantalla

### 1. Empieza por el objetivo: una acción principal

Antes de colocar nada, responde: *"¿qué viene a hacer aquí el usuario?"*. Esa
tarea manda. Habrá **una** acción principal destacada (un botón, un FAB) y el
resto será secundario. Si todo grita, nada se oye.

### 2. Construye una jerarquía visual clara

El ojo debe recorrer la pantalla en el orden correcto. Tienes cuatro palancas
para decir "esto importa más":

| Palanca | Cómo la usas |
|---|---|
| **Tamaño** | Lo importante, más grande (el saldo, el titular). |
| **Peso/color** | Negrita o color de acento para lo principal; gris para lo secundario. |
| **Posición** | Arriba y a la izquierda se lee primero (en culturas LTR). |
| **Espacio** | Rodear algo de aire lo hace destacar más que agrandarlo. |

> 💡 No uses el color como única jerarquía. Combina tamaño + peso + espacio; el
> color es el último toque, no el primero.

### 3. Respeta una rejilla y un espaciado consistentes

Elige una unidad base (en Material, múltiplos de **4/8 dp**) y sé fiel a ella en
márgenes y separaciones. La consistencia del espaciado es lo que, sin saber por
qué, hace que un diseño se sienta "profesional". En Compose esto es `padding` y
`Arrangement.spacedBy` (Guía 03).

### 4. Deja respirar: el espacio en blanco no es espacio perdido

El *whitespace* agrupa, separa y da calma. Amontonar elementos para "aprovechar"
la pantalla produce sensación de estrés. Duplica el aire alrededor del contenido
importante.

### 5. Agrupa por proximidad (principios de Gestalt)

Los elementos cercanos se perciben como relacionados. Junta lo que va junto
(etiqueta + su campo) y separa los bloques distintos. La proximidad comunica
estructura sin dibujar una sola línea.

### 6. Tipografía: pocas fuentes, escala clara

- **Una o dos familias** como mucho (una para títulos, otra para texto, o una
  sola con distintos pesos).
- Una **escala** definida (`headline`, `title`, `body`, `label` en M3, Guía 10):
  no inventes tamaños a ojo.
- Longitud de línea cómoda y suficiente contraste con el fondo.

### 7. Color con propósito y roles semánticos

- Regla **60-30-10**: 60 % color dominante (normalmente neutro), 30 % secundario,
  10 % acento para las acciones.
- Usa **roles semánticos** de Material 3 (`primary`, `surface`, `error`…), no
  colores sueltos (Guías 10 y 11): así el modo oscuro y el contraste salen gratis.
- El color de acento se reserva para lo accionable. Si todo es de color, nada
  llama a la acción.

### 8. Reutiliza componentes y patrones conocidos

No reinventes controles: la gente ya sabe usar un `Switch`, un `Slider`, una
`Card` o un `BottomSheet` (Guías 04–08). La familiaridad es usabilidad. La
originalidad va en el contenido y la marca, no en romper convenciones básicas.

### 9. Diseña TODOS los estados de la pantalla

Una pantalla no es solo su versión "con datos bonitos". Diseña también:

- **Vacío** (aún no hay datos): explica qué es y ofrece la primera acción.
- **Cargando**: *skeletons* o indicador (Guía 08); nunca una pantalla congelada.
- **Error**: mensaje humano ("¿Hay conexión?") y forma de reintentar (Guía 07).
- **Éxito/lleno**: el caso feliz.

Saltarse los estados vacío y de error es el error de diseño más común en apps de
principiante.

### 10. Da feedback: microinteracciones

Toda acción merece respuesta: un *ripple* al pulsar, una transición al navegar,
un `Snackbar` al guardar (Guías 07 y 12). El feedback confirma que la app "te
escuchó" y hace la experiencia agradable.

### 11. Accesibilidad = diseño para más gente

- **Contraste** de texto suficiente (WCAG AA: 4.5:1 en texto normal).
- **Objetivos táctiles** de al menos **48 dp**.
- **No comuniques solo con color** (añade icono o texto al estado).
- Texto que **escala** con los ajustes del sistema; `contentDescription` en
  iconos con significado.

### 12. Pensado para el pulgar (móvil)

Las acciones frecuentes van en la **zona del pulgar** (mitad inferior); lo
peligroso (borrar), lejos de ahí. Aprovecha *edge-to-edge* con cuidado de no
tapar contenido con las barras del sistema.

---

### Antes / Después: una pantalla de perfil

Los mismos elementos, ordenados con estos principios:

```
   ❌ ANTES                          ✅ DESPUÉS
┌───────────────────┐          ┌───────────────────┐
│Nombre Apellido    │          │       ( 👤 )       │  avatar centrado, aire
│correo@mail.com    │          │                   │
│[Editar][Salir][+] │          │   Ana Martín      │  jerarquía: nombre grande
│Ajustes            │          │  ana@mail.com     │  correo secundario (gris)
│Notificaciones ON  │          │                   │
│Tema oscuro OFF    │          │  ┌─────────────┐  │
│Idioma español     │          │  │ Ajustes     │  │  agrupados por proximidad
│Privacidad         │          │  │  Notificac. ⌄│  │
│v1.0.0             │          │  │  Tema      ⌄│  │
└───────────────────┘          │  └─────────────┘  │
   todo apelmazado,            │                   │
   sin jerarquía,              │  [  Editar perfil ]│  UNA acción principal
   3 botones compitiendo       │   Cerrar sesión    │  secundaria, discreta
                               └───────────────────┘
```

Qué cambió: una acción principal clara, jerarquía por tamaño/peso/color, grupos
por proximidad, y aire alrededor de lo importante.

---

### Lista rápida para auditar una pantalla

- [ ] ¿Se entiende de un vistazo cuál es la acción principal?
- [ ] ¿La jerarquía guía el ojo en el orden correcto?
- [ ] ¿El espaciado es consistente (múltiplos de 4/8 dp)?
- [ ] ¿Hay suficiente aire; no está apelmazada?
- [ ] ¿Uso ≤ 2 fuentes y una escala tipográfica definida?
- [ ] ¿El acento de color se reserva para lo accionable?
- [ ] ¿Están diseñados los estados vacío, cargando y error?
- [ ] ¿Contraste AA y objetivos táctiles ≥ 48 dp?
- [ ] ¿Es coherente con el resto de la app?

---

## Parte B — Recursos de inspiración

> ⚠️ **Inspírate, no copies.** Guarda referencias en un *mood board* (un tablero
> con capturas que te gustan), identifica **por qué** funcionan y adapta el
> principio a tu caso. Copiar una pantalla entera rara vez encaja con tu problema.

### Galerías e inspiración de UI

| Recurso | Para qué |
|---|---|
| **Mobbin** — <https://mobbin.com> | Capturas reales de apps por pantallas y patrones (lo mejor para móvil). |
| **Dribbble** — <https://dribbble.com> | Conceptos visuales; ideas de estilo (a veces poco realistas). |
| **Behance** — <https://www.behance.net> | Casos de estudio completos de diseño. |
| **Screenlane** — <https://screenlane.com> | Ideas de UI móvil/web filtrables por patrón. |
| **Godly** — <https://godly.website> | Diseño web de referencia, útil para estética. |
| **Collect UI** — <https://collectui.com> | Inspiración diaria por tipo de componente. |

### Específicos de Android / Material

| Recurso | Para qué |
|---|---|
| **Material Design 3** — <https://m3.material.io> | La fuente de verdad de los componentes y las guías (Guías 10–11). |
| **Now in Android** — <https://github.com/android/nowinandroid> | App de Google, ejemplo real de M3 + Compose bien hechos. |
| **Material Symbols** — <https://fonts.google.com/icons> | Catálogo oficial de iconos. |

### Color

| Recurso | Para qué |
|---|---|
| **Material Theme Builder** — <https://material-foundation.github.io/material-theme-builder/> | Genera el `ColorScheme` de M3 desde un color base (exporta a Compose). |
| **Coolors** — <https://coolors.co> | Generador rápido de paletas. |
| **Color Hunt** — <https://colorhunt.co> | Paletas ya combinadas por la comunidad. |
| **Realtime Colors** — <https://www.realtimecolors.com> | Prueba una paleta sobre una UI real al instante. |
| **Adobe Color** — <https://color.adobe.com> | Rueda cromática y comprobador de contraste. |

### Tipografía

| Recurso | Para qué |
|---|---|
| **Google Fonts** — <https://fonts.google.com> | Fuentes gratis listas para Android. |
| **Fontpair** — <https://www.fontpair.co> | Combinaciones de fuentes que funcionan juntas. |
| **Typescale** — <https://typescale.com> | Genera una escala tipográfica coherente. |

### Iconos e ilustraciones

| Recurso | Para qué |
|---|---|
| **Lucide** — <https://lucide.dev> · **Phosphor** — <https://phosphoricons.com> | Sets de iconos alternativos y consistentes. |
| **unDraw** — <https://undraw.co> | Ilustraciones libres recoloreables a tu paleta. |
| **Storyset** — <https://storyset.com> | Ilustraciones (algunas animables) para estados vacíos. |
| **Blush / Open Peeps** — <https://blush.design> · <https://www.openpeeps.com> | Ilustraciones de personas componibles. |

### Fotografía libre

| Recurso | Para qué |
|---|---|
| **Unsplash** — <https://unsplash.com> · **Pexels** — <https://www.pexels.com> | Fotos gratuitas de alta calidad. |

### Para aprender el "porqué" del buen diseño

| Recurso | Para qué |
|---|---|
| **Refactoring UI** — <https://www.refactoringui.com> | Consejos prácticos de diseño para quien programa (muy recomendable). |
| **Laws of UX** — <https://lawsofux.com> | Principios psicológicos del diseño de interfaces, explicados. |
| **Nielsen Norman Group** — <https://www.nngroup.com> | Artículos de referencia sobre usabilidad. |

---

## Fuentes consultadas (19-07-2026)

- Material Design 3 — Fundamentos de diseño: <https://m3.material.io/foundations>
- Refactoring UI (principios de jerarquía, espaciado y color): <https://www.refactoringui.com>
- Laws of UX (Gestalt, ley de Fitts, carga cognitiva): <https://lawsofux.com>
- Accesibilidad en Android: <https://developer.android.com/guide/topics/ui/accessibility>
