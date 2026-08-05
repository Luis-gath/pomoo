# Plan: Áreas como espacio de estudio

Objetivo: que organizar material sea rápido y evidente, y que ese material desemboque en
estudio efectivo en lugar de quedarse en un cajón ordenado.

---

## Parte 0 — De qué partimos

Conviene ser preciso, porque hay más construido de lo que parece y varias piezas están
**modeladas pero sin conectar**. Eso cambia el orden de trabajo: no hay que diseñar de cero,
hay que terminar de cablear.

### Ya funciona

| Pieza | Estado |
|---|---|
| Modelo `Area` (curso / habilidad / proyecto) | Completo |
| Modelo `Item` con tipo, marca, `dueAt`, `taskId` | Completo |
| Importar archivos con el selector | Funciona |
| Adoptar una carpeta entera con permiso persistente | Funciona |
| Recibir desde otras apps (compartir) | Funciona, `ACTION_SEND` y `SEND_MULTIPLE` |
| Miniaturas de foto, vídeo y PDF | Funciona, con caché |
| Enlaces y notas | Funciona |
| Filtros Todo / Importantes / Entregas | Funciona |

La detección de tipo (`ItemKindResolver`) ya cubre el caso real de que el MIME que llega al
compartir sea basura, cayendo en la extensión. Está bien pensado.

### Modelado pero muerto

Esto es lo importante, y donde el plan debe empezar:

1. **`dueAt` no sirve de nada todavía.** Se puede fijar al compartir, pero en el detalle del
   área `cycleMark` reenvía el valor que ya había: no hay forma de elegir ni cambiar la
   fecha. Y **nada programa una notificación**. La marca `ENTREGA` es decorativa.
2. **`taskId` no lo usa nadie.** El campo existe en la tabla y ni un solo archivo lo lee o
   lo escribe. Es el puente entre material y temporizador, y está sin construir.
3. **`ItemKind.GENERADO` no lo produce nada.** Está documentado como «resultado traído de
   vuelta desde NotebookLM», pero no hay ningún camino que cree un item así.
4. **No hay cámara**, aunque `localPath` se diseñó explícitamente para eso («las fotos de
   cuaderno: si las hace la app, la app debe poseerlas»).
5. **No hay lector.** Solo miniaturas: para leer un PDF hay que salir a otra app, que es
   justo lo que rompe la concentración.

---

## Parte 1 — La idea que ordena todo lo demás

Una app de archivos ordenados no mejora el aprendizaje. Lo que lo mejora es cerrar el ciclo:

```
material  →  entregable con fecha  →  tarea  →  sesión Pomodoro  →  repaso espaciado
```

Ese ciclo **ya casi existe** en tu app: tienes tareas, horarios semanales, hábitos de
estudio, temporizador y estadísticas. Lo que falta es el eslabón que une el material con
todo eso, y resulta que el campo que lo une (`taskId`) ya está en la tabla sin usar.

Por eso el criterio para priorizar no es «qué función es más vistosa» sino **qué elimina un
paso manual**. Cada fase de abajo quita fricción de un punto concreto.

---

## Parte 2 — Fases

### Fase 1 — Que las entregas avisen de verdad

La de mayor valor y menor trabajo, porque la infraestructura ya está hecha.

- Al marcar un material como `ENTREGA`, abrir un selector de fecha y hora. Hoy `cycleMark`
  cicla la marca pero nunca deja elegir cuándo.
- Programar el aviso **reutilizando `TaskAlarmScheduler` y `TaskReminderNotifier`**, que ya
  usan las tareas. No montar un sistema de alarmas paralelo.
- Avisos escalonados: una semana antes, un día antes y el mismo día. Un único aviso llega
  tarde para un trabajo largo.
- Pantalla **«Próximas entregas»** que cruce todas las áreas. Hoy el filtro `ENTREGAS` es
  por área, así que no existe la pregunta más importante: *¿qué tengo que entregar esta
  semana?*
- Al completar la entrega, archivarla en vez de borrarla.

**Por qué primero:** es lo único de la lista que evita un daño real (perder una entrega), y
se apoya entero en código que ya funciona.

### Fase 2 — Capturar sin fricción: cámara dentro del área

- Botón de cámara en el área que guarda directo en `localPath`, sin pasar por la galería.
- **Modo varias páginas**: disparar tres fotos del cuaderno y que queden como un solo
  material, no como tres sueltos. Es lo que uno hace de verdad al fotografiar apuntes.
- **OCR en el dispositivo con ML Kit**: es gratuito, funciona sin conexión y convierte la
  foto en texto buscable. Esto es lo que evita que las fotos se conviertan en un cementerio
  de imágenes que nadie vuelve a abrir, y además alimenta la Fase 5.
- Buscador por texto sobre notas y sobre el texto extraído de las fotos.

**Dos caminos para la cámara:**

| Opción | A favor | En contra |
|---|---|---|
| `ACTION_IMAGE_CAPTURE` (cámara del sistema) | Muy poco código, ya tienes FileProvider configurado | Salir y volver de la app en cada foto; incómodo para varias |
| CameraX dentro de la app | Ráfaga de fotos sin salir, recorte y encuadre propios | Bastante más trabajo y permisos de cámara |

Recomiendo **empezar por el intent** y pasar a CameraX solo si el modo varias páginas se
queda corto. Así la funcionalidad está en manos del usuario en días, no en semanas.

### Fase 3 — Leer dentro de la app

Hoy solo hay miniaturas. Para leer hay que salir, y salir rompe el pomodoro.

- Visor a pantalla completa: PDF paginado (ya usas `PdfRenderer`, falta la navegación),
  imagen con zoom, editor para las notas.
- **Leer con el temporizador corriendo**, con el tiempo restante visible en una esquina.
  Esta es la diferencia real frente a abrir Drive: el material y la sesión de estudio viven
  en el mismo sitio.
- Recordar la última página de cada PDF.
- Subrayar o marcar una página y que ese fragmento pueda convertirse en pregunta después.

### Fase 4 — Estudio activo, no solo lectura

Aquí es donde «optimizar tiempo» deja de ser una frase.

- Entidad nueva `StudyCard`: pregunta, respuesta, área, material de origen opcional.
- **Repetición espaciada** (SM-2 es suficiente y sencillo; FSRS es mejor pero más complejo).
  Este es el mecanismo con más respaldo para retener, y encaja de forma natural con tus
  estadísticas y rachas.
- Nuevo tipo de sesión en el temporizador: **repaso**, que en vez de un bloque de trabajo
  presenta las tarjetas que tocan hoy.
- Agrupar tarjetas en **kits** por tema, para poder repasar «solo integrales».

### Fase 5 — Generar las preguntas

Ver la Parte 3: es la que tiene una restricción externa importante.

---

## Parte 3 — Lo de NotebookLM: la respuesta honesta

**No se puede hacer lo que imaginas.** Lo verifiqué: NotebookLM (renombrado a *Gemini
Notebook*) **no tiene API pública**. Google solo documenta API para clientes **Enterprise**.
No hay forma de que una app de consumo pida las preguntas a un cuaderno y las reciba.

Hay proyectos de la comunidad que automatizan NotebookLM haciendo de navegador simulado.
No los recomiendo para un producto: dependen de que la web no cambie, incumplen los términos
de uso y se romperán sin aviso.

### Las tres alternativas reales

**A. Compartir desde NotebookLM (funciona hoy, sin escribir casi nada)**

Tu app **ya recibe** contenido compartido de cualquier otra: el `intent-filter` con
`ACTION_SEND` y `*/*` está puesto y `ReceiveShareScreen` funciona. Desde NotebookLM el
usuario copia o comparte el texto generado y lo manda a tu app.

Lo único que falta es un **analizador** que convierta ese texto en tarjetas: detectar
patrones de pregunta y respuesta (líneas `P:` / `R:`, numeradas, con guiones) y proponer las
tarjetas para que el usuario confirme antes de guardarlas.

- **A favor:** disponible ya, sin coste, sin depender de nadie. Aprovecha el `GENERADO` que
  dejaste preparado.
- **En contra:** el usuario da un rodeo manual por otra app.

**B. Generar las preguntas dentro de la app con un modelo (el destino real)**

El usuario pulsa «generar preguntas» sobre un PDF o unos apuntes y las obtiene ahí mismo.
Sin rodeo, sin copiar y pegar. Es mejor producto que lo que pediste.

**Aviso importante de arquitectura:** la clave de API **no puede ir dentro del APK**;
cualquiera la extrae y te gasta el saldo. Hace falta una función en servidor que reciba el
texto, llame al modelo y devuelva las preguntas.

Y aquí aparece una coincidencia útil: **ese servidor es el mismo que ya necesitas** para
validar las compras premium y para los contadores y notificaciones del foro. La decisión del
plan Blaze que tienes pendiente pasa a tener **tres motivos** en vez de uno.

- **A favor:** cero fricción; es el argumento de venta natural del premium.
- **En contra:** cuesta dinero por uso, necesita conexión y exige el backend.

**C. Escribirlas a mano**

Suena pobre, pero formular la pregunta uno mismo es de las cosas que más ayudan a retener.
Debe existir siempre, y además es lo que garantiza que la Fase 4 funcione desde el día uno
sin depender de A ni de B.

### Recomendación

Construir **C primero** (es la base de datos y la interfaz que necesitan las otras dos),
**A después** porque es barato y desbloquea NotebookLM ya, y **B cuando exista el backend**.
Así ninguna fase queda bloqueada esperando a otra.

---

## Parte 4 — Otras ideas que encajan

- **Exportar a Anki** (`.apkg`). Mucha gente ya vive ahí; poder sacar tus tarjetas reduce el
  miedo a quedarse encerrado y cuesta poco.
- **Plantillas de área.** Al crear «Cálculo», que ya venga con estructura de temas. Enlaza
  con las plantillas de horario que ya tienes.
- **Repaso desde la pantalla de inicio**, como un widget: la fricción de abrir la app es
  justo lo que rompe el hábito diario.
- **Detectar la fecha de entrega en el texto** de un PDF o del nombre del archivo y
  proponerla, en vez de que el usuario la teclee.
- **Enlazar material con la tarea** usando el `taskId` que ya existe: al empezar un pomodoro
  de «Cálculo», que el material de esa área esté a un toque.

---

## Parte 5 — Qué hay que decidir antes de empezar

1. **¿Backend sí o no?** Es la misma decisión del plan Blaze. Condiciona si la Fase 5B
   existe, y también la validación de compras y el foro. Mientras no se decida, todo lo
   demás del plan sigue siendo viable.
2. **¿Repetición espaciada propia o exportar a Anki?** Hacerla propia da una experiencia
   integrada con tu temporizador y tus rachas; exportar es mucho menos trabajo.
3. **¿Cámara por intent o CameraX?** Recomiendo intent primero.
4. **¿Las tarjetas cuelgan del área o del material?** Sugiero **del área, con referencia
   opcional al material**: si borras un PDF no deberías perder lo que aprendiste de él.

---

## Orden sugerido

1. Fecha y hora en las entregas, con avisos escalonados reutilizando las alarmas de tareas.
2. Pantalla «Próximas entregas» cruzando todas las áreas.
3. Cámara por intent, con modo varias páginas.
4. OCR con ML Kit y buscador sobre notas y fotos.
5. Visor a pantalla completa con el temporizador visible.
6. `StudyCard` y repaso espaciado, con creación manual.
7. Analizador de texto compartido, para traer preguntas de NotebookLM.
8. Generación con modelo, cuando exista el backend.

Los puntos 1 y 2 son los que más daño evitan y los que menos código nuevo requieren, porque
se apoyan en el sistema de alarmas que ya funciona.
