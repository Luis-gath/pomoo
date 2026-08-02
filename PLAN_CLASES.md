# Plan — Áreas: organizar cursos, habilidades y proyectos

> **La idea, en una frase:** el Pomodoro controla *cuánto* estudias; esta segunda mitad controla
> *sobre qué*. Un sitio donde los archivos de cada curso dejen de estar regados por el móvil,
> con lo importante y las entregas marcadas, listo para mandar a NotebookLM y para recibir de
> vuelta lo que genere.
>
> **Veredicto:** muy viable, y bastante más barato de lo que parecía. Como NotebookLM se encarga
> del análisis, **no hace falta OCR propio ni modelos en la GPU**. El trabajo real está en la
> captura, la organización y el almacenamiento.

---

## 1. El concepto que lo une todo: Área

Un curso, una habilidad que quieres aprender y un proyecto **son la misma cosa** desde el punto
de vista de los datos: un contenedor con nombre, con materiales dentro, con tareas y con fechas.

Esto es la decisión de diseño más importante del plan. Si modelas la tabla como `Curso`, el día
que quieras organizar proyectos tendrás que renombrarlo todo o crear una segunda estructura
paralela. Si la llamas **`Area`** desde el principio, con un campo de tipo:

```
Area    id · nombre · tipo(CURSO | HABILIDAD | PROYECTO) · color · activa · creadaEn
```

…entonces pasar a algo tipo Trello más adelante **no es una migración, es una vista nueva**.

Ese único cambio de nombre es lo que separa "tener que rehacerlo" de "solo añadir una pantalla".

---

## 2. El flujo que de verdad resuelve tu problema

Dijiste que el problema es que **los archivos están regados por el celular**. Eso no se arregla
con un importador dentro de la app, porque exige acordarse de entrar a buscarlos. Se arregla
capturándolos **en el momento en que llegan**.

La pieza clave es registrar la app como **destino para compartir**. Con un `intent-filter` de
`ACTION_SEND` en el manifest, desde WhatsApp, Drive, Chrome o el navegador aparece tu app en el
menú de compartir:

```
Te llega un PDF por WhatsApp
   → Compartir → Pomodoro
   → "¿A qué área? Cálculo II"  ·  "¿Marcar? Entrega, 12 de agosto"
   → guardado y clasificado, sin salir de la conversación
```

Esto invierte el flujo: en vez de ir a buscar archivos dispersos, los archivos se archivan solos
en el momento en que aparecen. **Es la funcionalidad de mayor valor de todo el plan** y es de las
más sencillas de implementar.

Hoy la app no tiene nada de esto: no hay `intent-filter` de compartir ni `FileProvider`.

---

## 3. NotebookLM: lo que se puede y lo que no

### No hay API pública

NotebookLM **no ofrece una API** para subir fuentes desde otra aplicación. No se puede
automatizar del todo. Cualquiera que te diga lo contrario te está vendiendo humo.

### Lo que sí funciona: el menú de compartir de Android

En sentido de ida, la app puede **enviar** archivos hacia fuera con `ACTION_SEND_MULTIPLE`. Si
la app de NotebookLM está instalada y acepta ese tipo de archivo, aparecerá en la lista. El
gesto sería:

```
Área "Cálculo II" → seleccionar 8 fotos y 2 PDFs → Compartir → NotebookLM
```

Para poder compartir tus propios archivos hace falta declarar un **`FileProvider`** en el
manifest: sin él, Android no deja exponer ficheros internos a otras apps. Es una configuración
de unas 15 líneas.

**Ruta alternativa, más fiable:** exportar a **Google Drive** y desde NotebookLM añadir las
fuentes desde Drive. Es un paso más, pero no depende de que NotebookLM acepte compartición
directa.

> **Verifícalo tú en dos minutos**, porque no lo puedo comprobar desde aquí: coge un PDF
> cualquiera en el móvil, pulsa Compartir y mira si NotebookLM sale en la lista. Si sale, la ruta
> directa funciona; si no, se va por Drive. Esa comprobación decide cómo se implementa la fase 4.

### La vuelta: traer lo que NotebookLM genere

NotebookLM permite exportar sus resultados (resúmenes, notas, el audio del "resumen hablado").
Esos archivos acaban en Descargas o en Drive, y desde la app se recogen con el selector de
documentos del sistema y se archivan bajo el área correspondiente, marcados como material
generado.

Así se cierra el círculo: **material en bruto → NotebookLM → resultado, todo guardado junto.**

---

## 4. Cómo se organiza: sin subsecciones

Pediste pocas subsecciones, con áreas y fechas. La estructura mínima que lo consigue:

```
Area  ──┬── Item (foto · PDF · vídeo · enlace · nota · generado)
        └── Task  (las tareas que ya existen)

Item    id · areaId · tipo · titulo · uri · rutaLocal · fecha ·
        marca(NINGUNA | IMPORTANTE | ENTREGA) · fechaEntrega · taskId?
```

**Una sola marca por elemento**, no un sistema de etiquetas libre. Es una decisión deliberada:
las etiquetas libres se convierten en un cajón desordenado con el tiempo, y tú pediste
explícitamente no complicarlo. Tres estados bastan para lo que describes.

Dentro de un área, el contenido se ve como una **línea temporal**: lo más reciente arriba,
agrupado por mes. Con dos filtros rápidos en la cabecera: *Importantes* y *Entregas*. Sin
carpetas, sin subcarpetas, sin árbol que mantener.

**Enganche con lo que ya existe:** `TaskEntity` tiene hoy un campo de texto libre
`courseOrProject`. Se le añade un `areaId` opcional que apunte a `Area`, se migran los valores
existentes emparejando por nombre, y el campo viejo se retira más adelante.

---

## 5. Almacenamiento: la regla que evita el desastre

| Tipo | Estrategia | Por qué |
|---|---|---|
| Fotos tomadas en la app | **Copiar** a `filesDir` | Las crea la app, debe poseerlas |
| PDFs | **Referenciar** por URI persistido | Suelen vivir en Descargas; copiar duplica |
| Vídeos | **Referenciar** siempre | 5 min en 1080p ≈ 500 MB |
| Enlaces y notas | Solo texto en la base de datos | — |

Referenciar es guardar el URI y pedir permiso permanente con `takePersistableUriPermission`,
exactamente el patrón que **ya usas** para las pistas de audio importadas.

**Contrapartida que hay que asumir:** si el usuario borra el original desde su galería, el
elemento queda roto. Hay que detectarlo al abrirlo y mostrarlo como "archivo no disponible" en
vez de fallar en silencio.

**Permisos:** ninguno peligroso nuevo. El selector de fotos y el de documentos no requieren
permiso, y el escáner de ML Kit gestiona la cámara por su cuenta.

---

## 6. Fotos de cuaderno

Aunque el análisis lo haga NotebookLM, sigue mereciendo la pena **ML Kit Document Scanner**
(`play-services-mlkit-document-scanner`) para capturar: da la pantalla de escaneo completa con
detección de bordes, corrección de perspectiva, recorte, varias páginas y exportación a PDF.

Eso importa porque **NotebookLM digiere mucho mejor un PDF limpio y enderezado que una foto
torcida con la mesa de fondo**. El escáner mejora el resultado del análisis sin que tú escribas
nada de visión por computador.

Al distribuirse por los servicios de Google Play, apenas engorda el APK.

---

## 7. El camino a Trello o Asana

Buena noticia: **ya tienes la mitad construida.** `TaskDashboardScreen` incluye un tablero
kanban funcional (`TaskBoardView`) con columnas Por hacer / Haciendo / Hecho y movimiento entre
ellas. Existe hoy, funciona hoy.

Lo que falta para que sea un gestor de proyectos de verdad:

| Pieza | Estado |
|---|---|
| Tablero con columnas y arrastrar | **Ya existe** |
| Estados de tarea | **Ya existe** (TODO / DOING / DONE) |
| Prioridades | **Ya existe** |
| Fechas de entrega | **Ya existe** |
| Agrupar por proyecto | Falta: es exactamente el `areaId` de este plan |
| Subtareas | Falta |
| Columnas configurables | Falta |
| Varias personas | No aplica: la app es local, sin servidor |

O sea: si modelas `Area` bien desde el principio, llegar a algo tipo Trello es **filtrar el
tablero que ya tienes por área** y poco más. Por eso insisto tanto en el punto 1.

Lo que **nunca** será sin un backend es la colaboración entre personas. Para uso propio no
importa; conviene tenerlo claro por si algún día cambia la ambición.

---

## 8. Fases

**Fase 1 — Áreas.** Tabla `Area`, migración 7→8, pantalla de lista y detalle. Enlazar
`TaskEntity.areaId`. Al terminar ya puedes agrupar tus tareas por curso.

**Fase 2 — Capturar lo que llega.** `intent-filter` de compartir + `FileProvider` + pantalla de
"guardar en área". Aquí es donde el problema de los archivos regados desaparece de verdad.

**Fase 3 — Materiales dentro del área.** Tabla `Item`, línea temporal, marcas de Importante y
Entrega, filtros. PDFs con `PdfRenderer` (viene en Android) y vídeos con ExoPlayer (**ya está en
el proyecto**).

**Fase 4 — Ida y vuelta con NotebookLM.** Selección múltiple y compartir hacia fuera; importar
lo generado y marcarlo como tal.

**Fase 5 — Fotos de cuaderno.** ML Kit Document Scanner, con exportación a PDF pensada para que
NotebookLM lo lea bien.

**Fase 6 — Proyectos.** Filtrar el tablero existente por área, subtareas y columnas
configurables.

---

## 9. Riesgos

| Riesgo | Gravedad | Mitigación |
|---|---|---|
| Los vídeos llenan el móvil | Alta | No copiarlos nunca; mostrar el espacio por área |
| Archivos referenciados que desaparecen | Media | Verificar el URI al abrir y marcarlo |
| Fotos en `filesDir` se pierden al desinstalar | Alta | Exportar por área, o sincronizar con Drive |
| NotebookLM no acepta compartir directo | Media | Ruta alternativa por Drive; verifícalo antes |
| La app se vuelve dos apps sin relación | Media | Que el temporizador arranque *desde* un área |

Sobre el último: la unión natural entre las dos mitades es que estudiar sea siempre *para* algo.
Si al abrir un área puedes pulsar "estudiar esto" y el Pomodoro arranca ya asociado a ese curso,
y las estadísticas te dicen cuántas horas metiste en cada área, entonces las dos mitades se
justifican mutuamente. **Sin ese puente, son dos apps compartiendo icono.**

---

## 10. Recomendación

Empieza por las **fases 1 y 2**. Juntas son poco código y resuelven tu queja concreta: las áreas
te dan dónde poner las cosas, y el destino de compartir hace que ponerlas cueste dos toques
desde cualquier app.

Deja NotebookLM para la fase 4, pero **haz la comprobación del menú de compartir esta misma
semana**, porque decide cómo se implementa.

Y descarta desde ya cualquier idea de reconocimiento de imágenes propio: NotebookLM ya lo hace
mejor de lo que lo harías tú en el móvil, y gratis.

> Los nombres exactos de dependencias y versiones conviene confirmarlos en la documentación
> oficial antes de escribirlos en el `build.gradle.kts`.
