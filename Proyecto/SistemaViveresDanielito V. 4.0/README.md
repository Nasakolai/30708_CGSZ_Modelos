# Sistema Víveres Danielito — v4.0

Reporte de cambios realizados sobre la versión 3.0. Este documento explica, con el mayor detalle posible, **qué estaba roto, qué se arregló, qué se eliminó, qué se añadió y por qué**, para que cualquiera que retome el proyecto entienda las decisiones tomadas sin tener que adivinarlas leyendo el código.

> El código fuente está comentado en español, en minúsculas y con un tono informal a propósito (así se pidió). El objetivo de los comentarios no es la corrección gramatical, sino que cualquier persona que abra el archivo entienda rápido el "por qué" de una decisión, no solo el "qué".

---

## 1. Resumen ejecutivo

| | |
|---|---|
| **Bugs corregidos** | 20+ (de navegación, de datos, de seguridad, de UI, de validación) |
| **Clases / archivos eliminados** | 7 (código muerto o sin uso real) |
| **Métodos muertos eliminados** | 6 |
| **Pantallas nuevas** | 1 (Ver Movimientos, con eliminación de registros) |
| **Controladores nuevos** | 1 (`ControladorGestionMovimientos`) |
| **Librerías nuevas añadidas** | 0 (no hizo falta ninguna, ver sección 7) |
| **Librerías eliminadas** | 1 (`jcalendar-1.4.jar`, no se usaba en ningún lado) |
| **Compilador usado para verificar** | Ninguno disponible en este entorno (ver sección 9) — se hizo una revisión manual exhaustiva en su lugar |

---

## 2. Bugs corregidos

### 2.1 Bug grave de navegación — ventanas "fantasma" del menú principal

**Sintoma:** cada vez que se navegaba desde el menú principal (`FrmSistema`) hacia cualquier otra pantalla (Agregar Producto, Gestión de Productos, Gestión de Inventario, Generar Reportes) y luego se volvía a la pantalla principal usando el logo, **se creaban dos ventanas del menú principal al mismo tiempo**, una visible (correcta, creada por el clic en el logo) y otra invisible que quedaba flotando en memoria para siempre (un *memory leak* silencioso). En algunos casos (Gestión de Inventario, Agregar Productos, Generar Reportes) la ventana fantasma se creaba con `setVisible(false)`, y en otro caso (Gestión de Productos) se creaba con `setVisible(true)`, lo que producía **dos menús principales superpuestos exactamente en el mismo lugar**.

**Causa:** cada pantalla, al abrirse desde el menú, le agregaba un `addWindowListener` extra al `FrmSistema` que ya no hacía falta, porque cada pantalla destino **ya tiene su propio mecanismo correcto** para volver al menú (el clic sobre el logo, método `logoMouseClicked`, que crea el `FrmSistema` y cierra la ventana actual). El listener duplicado disparaba una segunda creación del menú cada vez que la ventana se cerraba.

**Arreglo:** se eliminaron los 13 bloques `addWindowListener` redundantes (repartidos entre `FrmSistema`, `FrmGestionProductos`, `FrmGestionInventarioo`, `FrmProducto` y `FrmGenerarReporte`). La navegación de vuelta al menú ahora pasa siempre por el mismo camino (el logo), sin duplicados.

### 2.2 Bug de empaquetado — las imágenes no cargaban fuera de NetBeans

**Síntoma:** todos los iconos (`SetImageLabel(... , "src/imagenes/algo.png")`) se cargaban con una ruta de archivo relativa (`src/imagenes/...`). Esto **solo funciona si se ejecuta el programa parado justo en la carpeta raíz del proyecto** dentro de NetBeans. Si el `.jar` final se movía a otra carpeta, se ejecutaba desde `dist/`, o se compartía con otra persona, **todos los iconos desaparecían** (quedaban en blanco) sin ningún mensaje de error.

**Arreglo:** se cambiaron todas las llamadas a `getClass().getResource("/imagenes/algo.png")`, que carga la imagen desde el classpath (la carpeta `imagenes` ya viaja empaquetada dentro del `.jar` final). Esto funciona igual de bien en NetBeans, en el `.jar` exportado, o en cualquier máquina. Se corrigió en `FrmSistema`, `FrmLogin`, `FrmProducto`, `FrmGestionProductos`, `FrmGestionInventarioo` y `FrmGenerarReporte`. Como medida extra, si una imagen no se encuentra, ya no se cae el programa: se imprime un aviso en consola y se sigue funcionando sin ese ícono.

### 2.3 Contraseñas guardadas en texto plano

**Síntoma:** `LoginDAO` guardaba y comparaba la contraseña del usuario tal cual, sin ningún cifrado. Cualquiera con acceso a la base de datos (o a un respaldo) podía leer la contraseña de un vistazo.

**Arreglo:** ahora se guarda un hash **SHA-256** de la contraseña (usando `java.security.MessageDigest`, que ya viene incluido en el JDK, no se necesita ninguna librería externa). Para no romper instalaciones que ya tuvieran el usuario `admin` guardado con el formato viejo, `iniciarSesion()` primero intenta comparar contra el hash; si no encuentra coincidencia, prueba también el formato viejo (texto plano) y, si coincide, **actualiza ese registro al nuevo formato automáticamente**, de forma transparente para quien ya tenía el sistema instalado.

### 2.4 Inyección de regex / caída del buscador de productos

**Síntoma:** `buscarEspecie()` y la búsqueda de duplicados (`existeProducto`, antes llamada con un bucle manual sobre toda la colección) armaban una expresión regular de MongoDB concatenando directamente el texto que escribía el usuario. Si el usuario escribía un producto con caracteres como `.`, `*`, `(`, `[`, la consulta podía lanzar una excepción de patrón inválido y tronar la pantalla de **Gestión de Productos**, o devolver resultados que no tenían nada que ver con la búsqueda.

**Arreglo:** el texto se escapa con `Pattern.quote(...)` antes de meterlo en la consulta `$regex`, así un carácter especial se trata como texto literal, no como sintaxis de regex.

### 2.5 `existeProducto` podía tronar con `NullPointerException`

**Síntoma:** el método original recorría **toda** la colección de productos a mano y llamaba `obj.get("nombre").toString()` sin comprobar si `"nombre"` venía nulo. Un solo documento mal guardado (sin ese campo) hacía caer la validación de "producto duplicado" para absolutamente todos los registros.

**Arreglo:** se reescribió usando una consulta directa de Mongo (`$regex` insensible a mayúsculas, escapada como en el punto 2.4) en vez de traer todo a memoria y comparar a mano. Es más rápido y ya no puede explotar por un documento con datos faltantes.

### 2.6 Combo de "tipo de producto" con un valor fantasma

**Síntoma:** en la pantalla de **Agregar Productos**, el combo de tipo intentaba seleccionar por defecto el texto `"Seleccione"`, pero ese texto **no existía** en la lista de opciones del combo. Como resultado, `setSelectedItem("Seleccione")` no hacía nada y el combo se quedaba mostrando el primer tipo real de la lista (`Lácteos`) ya seleccionado, sin que el usuario se diera cuenta. Era muy fácil registrar un producto con el tipo equivocado sin querer.

**Arreglo:** se agregó `"Seleccione un tipo"` como una opción real (la primera) del combo, y se añadió validación en `ControladorProducto` y en `FrmEditaPrduct` para **rechazar el guardado** si ese tipo "vacío" sigue seleccionado.

### 2.7 Buscador de productos invisible (función completa pero inalcanzable)

**Síntoma:** en `FrmGestionProductos`, el campo de búsqueda (`txtBuscar`) y su botón (`btnBuscar`) estaban **perfectamente conectados y funcionando** en el controlador (`ControladorGestionP.buscarPorNombre()`), pero la propia pantalla los ocultaba con `setVisible(false)` apenas se abría. Es decir: la función de búsqueda existía, funcionaba, pero **nadie podía usarla nunca** porque ni se veía.

**Arreglo:** se quitó el `setVisible(false)`. Adicionalmente el campo y el botón tenían un tamaño casi inútil (10px y 20px de alto, ancho automático) que se corrigió a un tamaño legible (32px de alto, anchos fijos) y se reubicaron prolijamente junto al título de la pantalla.

### 2.8 El título de "Gestión de Productos" estaba oculto

**Síntoma:** justo debajo del bug anterior había otro: la pantalla ocultaba (`jLabel6.setVisible(false)`) su propio título ("Gestión de Productos"). Todo indica que esa línea se copió y pegó de `FrmProducto.java` (donde `jLabel6` es una etiqueta totalmente distinta, "Código de Producto", que sí debe estar oculta) sin darse cuenta de que en esta otra pantalla `jLabel6` es el título visible.

**Arreglo:** se eliminó esa línea. El título ahora se ve como debería.

### 2.9 Botón "Cancelar" / botón fantasma dejaban la aplicación sin ninguna ventana visible

**Síntoma:** en `FrmGestionInventarioo`, el botón **Cancelar** solo hacía `vista.dispose()`. Como esta pantalla se abre con `DISPOSE_ON_CLOSE` (no `EXIT_ON_CLOSE`), cerrar la ventana sin volver a abrir el menú principal dejaba al usuario **sin ninguna ventana visible**, como si el programa se hubiera cerrado solo, aunque el proceso de Java seguía corriendo en segundo plano. Existía además un segundo botón (`jButtonMenuPrincipal`) escondido (`setVisible(false)`), con el texto de relleno por defecto de NetBeans ("jButton1") nunca reemplazado, que tenía exactamente el mismo problema y que nunca se podía ni pulsar.

**Arreglo:** "Cancelar" ahora vuelve correctamente al menú principal (igual que el logo) antes de cerrar la ventana actual. El botón fantasma sin terminar (`jButtonMenuPrincipal`) se eliminó por completo, ya que no aportaba nada que el logo no hiciera ya correctamente.

### 2.10 Botón de guardar con la etiqueta equivocada, y un botón "Guardar" duplicado que no guardaba nada

**Síntoma:** el botón que en verdad registra el movimiento de inventario decía **"Gestionar Registros"** (un texto que no explica lo que hace). Al lado había otro botón, literalmente llamado **"Guardar"**, que parecía el botón principal pero que **no estaba conectado a ninguna lógica real** — solo tenía efectos visuales de "hover" y no hacía nada al hacer clic.

**Arreglo:** el botón que sí guarda ahora se llama **"Guardar Movimiento"** (`btnGuardarMovimiento`, antes `jButton3Guardar`). El botón decorativo sin función (`jButton3Guardar1`) se **reaprovechó** en vez de simplemente borrarlo: ahora es el botón **"Ver Movimientos"** que abre la nueva pantalla (ver sección 4).

### 2.11 Mensajes de confirmación duplicados al registrar un producto

**Síntoma:** al registrar un producto nuevo aparecían **dos** cuadros de diálogo seguidos ("Se añadió X" y luego "Producto registrado correctamente..."), uno detrás del otro, por una llamada a `JOptionPane.showMessageDialog` que sobraba.

**Arreglo:** un solo mensaje, claro y con el código generado incluido.

### 2.12 Placeholder del login solo se limpiaba con el mouse

**Síntoma:** los campos de usuario y contraseña mostraban un texto de ejemplo ("Ingrese su nombre de usuario", `*********`) que solo se borraba si el usuario **hacía clic con el mouse** dentro del campo (`MouseListener`). Si alguien llegaba al campo con la tecla `Tab` (navegación por teclado) y presionaba Enter sin tocar el mouse, el programa intentaba iniciar sesión usando el texto de ejemplo como si fuera el usuario/contraseña real.

**Arreglo:** se cambió de `MouseListener` a `FocusListener`, que se activa sin importar cómo se llegue al campo (mouse o teclado). Además, por seguridad extra, `ControladorLogin` ahora también revisa explícitamente si el texto sigue siendo el de relleno antes de intentar loguear.

### 2.13 Filtro de fechas en reportes con lógica imposible de leer (y parcialmente muerta)

**Síntoma:** el filtro de fecha "desde" en `ControladorGenerarReporte` tenía esta condición:
```java
if (desdeText != null && !desdeText.isEmpty() && !desdeText.contains(".") && !desdeText.contains("/") == false) { ... }
```
La doble negación al final (`!x == false`) equivale simplemente a `x`, así que toda la condición es innecesariamente retorcida, y el bloque `else` repetía exactamente el mismo `try/parse` que el bloque `if`. Costaba mucho entender qué hacía esto en realidad (nada distinto de "intentar parsear la fecha, y si falla, ignorarla").

**Arreglo:** se reemplazó por un método auxiliar (`parsearFechaSegura`) de cuatro líneas, que hace lo mismo pero se entiende a la primera leída.

### 2.14 Campos de fecha de reportes con un texto de ejemplo que no tenía sentido

**Síntoma:** los campos de fecha del reporte mostraban el texto `"DD/MM/AA"` (2 dígitos de año) como ejemplo, pero el formato que en verdad se usa para comparar fechas es `dd/MM/yyyy` (4 dígitos). Si alguien copiaba ese ejemplo literalmente, jamás iba a coincidir con el formato real.

**Arreglo:** se les puso una máscara de entrada (`MaskFormatter`, igual que ya se usaba en la pantalla de inventario) con el formato correcto `##/##/####`, y se dejaron vacíos por defecto (son filtros opcionales).

### 2.15 `código` y otros campos numéricos podían tronar con `NullPointerException`

**Síntoma:** varios métodos de `ProductoDAO` hacían `((Number) doc.get("precio unitario")).doubleValue()` directo, sin comprobar si el campo existía. Un solo documento de Mongo con ese campo faltante (por ejemplo, importado a mano, o de una versión vieja de la base) tronaba toda la pantalla de Gestión de Productos.

**Arreglo:** se centralizó en un método `numeroSeguro(Object)` que devuelve `0.0` si el valor no es un número válido, en vez de lanzar una excepción.

### 2.16 Etiqueta duplicada flotando encima del logo

**Síntoma:** en `FrmProducto`, además del logo real (`logo`), existía una segunda etiqueta (`logo1`) agregada **exactamente en las mismas coordenadas**, sin ícono y sin ninguna función. No hacía daño visible, pero era peso muerto.

**Arreglo:** eliminada.

### 2.17 Falta el ícono del logo en la pantalla de edición de productos

**Síntoma:** `FrmEditaPrduct` nunca llamaba a `SetImageLabel(logo, ...)`, así que el logo se veía en blanco en esa pantalla, a diferencia de todas las demás.

**Arreglo:** se agregó la llamada correspondiente.

### 2.18 Reaprovechamiento de objetos fantasma en el formulario de edición

**Síntoma:** el constructor sin argumentos de `FrmEditaPrduct` volvía a crear con `new` una etiqueta y un campo de texto que `initComponents()` **ya había creado y agregado a los paneles**. Los objetos nuevos quedaban sueltos (nunca se agregaban a ningún panel), así que llamarles `setVisible(false)` no tenía ningún efecto real. Código muerto y confuso.

**Arreglo:** eliminado.

### 2.19 Edición de productos sin validar precio, stock ni nombres duplicados

**Síntoma:** al modificar un producto desde `FrmEditaPrduct`, se podía guardar un precio negativo o cero, un stock negativo, o renombrar un producto para que quedara con el **mismo nombre que otro producto ya existente** (Mongo terminaría con dos documentos distintos compartiendo el mismo nombre, rompiendo la lógica de búsqueda por nombre en el resto de la app).

**Arreglo:** ahora se valida que el precio sea mayor a cero, que el stock no sea negativo, y que si se cambia el nombre, ese nombre nuevo no choque con otro producto ya existente.

### 2.20 Generación de reportes con rutas de Windows fijas a una persona, y de otro proyecto

**Síntoma:** `ProductoDAO` tenía un método `generarReporteProductos()` con una ruta fija `C:\Users\Personal\Documents\CamaroneraReportes\reporte_especies.txt` — ni siquiera mencionaba víveres: "Camaronera" es claramente sobrante de otro proyecto (una camaronera/finca de camarones) que se copió por error. Tampoco se usaba en ningún lado. Lo mismo con `migrarLista()`, que apuntaba a `C:\Users\HP\Desktop\migrar.txt`, una ruta que solo existía en la computadora de quien escribió el código originalmente.

**Arreglo:** ambos métodos (y la clase `Archivo.java`, que solo existía para darle servicio a `migrarLista()`) se eliminaron — ver sección 3.

---

## 3. Código eliminado (módulos que no hacían nada)

| Archivo eliminado | Por qué |
|---|---|
| `Modelo/Proveedor.java` | Clase nunca instanciada en ningún lado del proyecto. |
| `Modelo/ProveedorDAO.java` | Clase completamente vacía (sin un solo método). |
| `Controlador/ControladorProveedor.java` | Clase completamente vacía (sin un solo método). |
| `Modelo/Login.java` | POJO nunca usado; `LoginDAO` trabaja directo con `BasicDBObject`. |
| `Modelo/Archivo.java` | Solo le daba servicio a `migrarLista()` (ver abajo), que era código muerto con una ruta de Windows fija a una sola computadora. Además tenía un bug real: revisaba `partes.length >= 3` pero después leía `partes[3]`, lo que hubiera lanzado `ArrayIndexOutOfBoundsException` si alguna vez se hubiera llegado a usar. |
| `Util/TestModificarProducto.java` | Era un script de prueba manual (con su propio `main`), no una prueba automatizada real, y no formaba parte del flujo de la aplicación. |
| `mvcviveresdanielito/MVCViveresDanielito.java` | La clase principal real de la app es `Vista.FrmSistema` (así está configurado en `manifest.mf` y en `project.properties`). Esta otra clase tenía un `main()` completamente vacío, sobrante de la plantilla por defecto que genera NetBeans al crear el proyecto. |
| `ProductoDAO.mostrarTabla(...)` | Método nunca llamado desde ningún lado, y además roto: agregaba columnas llamadas "Tamaño", "Precio/Kg" y "Dureza" (otra vez restos de un proyecto de pescado/camarón) que no coinciden ni en cantidad ni en sentido con los datos reales de un producto de víveres. |
| `ProductoDAO.migrarLista()` y `generarReporteProductos()` | Ver fila de `Archivo.java` arriba; nunca se llamaban desde ningún lado. |
| `ProductoDAO.productoExiste(...)` | Hacía exactamente lo mismo que `existeProducto(...)` pero **tampoco se llamaba desde ningún lado**. Se conservó únicamente `existeProducto`, ya reescrito para ser eficiente y seguro (ver punto 2.5). |
| `FrmSistema.menuGenerarReporteActionPerformed(...)` | Método huérfano: hacía exactamente lo mismo que `txtReporteMouseClicked`, pero no estaba conectado a ningún botón ni menú real. |
| `FrmGenerarReporte` → botón `jButton3` ("Menú Principal") | Estaba oculto (`setVisible(false)`) y además medía 10px de ancho, así que ni puesto visible a la fuerza se podría haber usado. El logo ya cumple esa función. |
| `FrmProducto` → etiqueta `logo1` | Ver punto 2.16. |
| `FrmGestionInventarioo` → botón `jButtonMenuPrincipal` | Ver punto 2.9. |
| `lib/jcalendar-1.4.jar` | Dependencia incluida en el proyecto pero **nunca importada ni usada en ningún archivo** (`grep` no encontró ni una sola referencia a `com.toedter` o `JCalendar` en todo el código fuente). Se quitó del proyecto y de `project.properties`. |

---

## 4. Funcionalidad nueva: pantalla "Ver Movimientos"

Se agregó una pantalla nueva, **`Vista/FrmGestionMovimientos.java`** (con su `Controlador/ControladorGestionMovimientos.java` y su archivo `.form` correspondiente), que muestra el historial completo de movimientos de inventario (entradas y salidas) guardados en Mongo, con un botón debajo de la tabla para eliminar el movimiento seleccionado.

**Cómo se usa:**
1. Desde el menú principal entrar a **"Gestión Inventario"**.
2. Abajo, junto a "Limpiar" y "Cancelar", hay un botón nuevo: **"Ver Movimientos"**.
3. Se abre la pantalla nueva con la tabla de movimientos (fecha, tipo, producto, código, cantidad, precio unitario y total), ordenados del más reciente al más antiguo.
4. Para borrar uno: seleccionar la fila haciendo clic sobre ella y presionar el botón **"Eliminar Movimiento"** debajo de la tabla. El sistema pide confirmación antes de borrar (mostrando tipo, producto y fecha del movimiento que se va a eliminar) y no se puede deshacer.

**Decisiones de diseño:**

- **Misma estructura de menú que las demás pantallas:** la nueva ventana tiene el mismo encabezado, el mismo menú lateral (Agregar Productos / Gestión Productos / Gestión Inventario / Generar Reportes / Salir del Sistema) y el mismo logo que el resto de la aplicación, para que se sienta igual de familiar al usarla.
- **Identificación del movimiento a borrar:** cada movimiento que se guarda en Mongo recibe automáticamente un identificador único (`_id`). Antes, el modelo `Movimiento` no guardaba ese dato en ningún lado, así que no había forma confiable de decir "borra justo este y no otro" si dos movimientos tenían exactamente los mismos datos (mismo producto, misma fecha, misma cantidad — algo que puede pasar fácilmente). Se agregó el campo `id` al modelo `Movimiento` y un método `eliminarMovimiento(String id)` en `MovimientoDAO`. La tabla de la pantalla nueva guarda ese id en una primera columna que **existe en los datos pero se mantiene con ancho cero**, así nunca se lo ve el usuario pero el programa sabe siempre, sin ambigüedad, cuál registro hay que borrar.
- **Punto de entrada:** se decidió no agregar un sexto ícono al menú lateral de las cuatro pantallas existentes (eso hubiera significado tocar el layout de cuatro formularios distintos, con más riesgo de romper algo visualmente sin poder probarlo). En su lugar, se reaprovechó un botón que ya existía en la pantalla de Gestión de Inventario pero que no servía para nada (ver punto 2.10), ya que es el lugar más natural: es la pantalla donde se generan los movimientos.

---

## 5. Validaciones agregadas (para que sea "inquebrantable")

- **Registrar producto:** nombre, precio y proveedor obligatorios; tipo debe ser uno real (no el texto de "seleccione"); precio debe ser mayor a cero; no se permite un nombre duplicado.
- **Editar producto:** mismas validaciones que registrar, más la verificación de nombre duplicado al renombrar (ver punto 2.19), y stock no negativo.
- **Movimiento de inventario:** cantidad debe ser mayor a cero; no se permite una salida que deje el stock en negativo (ya existía, se mantuvo).
- **Login:** usuario y contraseña no pueden quedar en blanco ni en el texto de relleno; las contraseñas ahora se comparan por hash, no en texto plano.
- **Búsqueda de productos:** cualquier texto que el usuario escriba (incluyendo caracteres especiales de regex) se escapa antes de usarse en una consulta, así nunca puede romper la búsqueda ni la validación de duplicados.
- **Conexión a la base de datos:** antes de mostrar cualquier ventana, el programa verifica que pueda hablar con MongoDB. Si no puede, se muestra un mensaje claro ("No se pudo conectar a la base de datos... verifique que MongoDB esté encendido") en vez de que el programa truene más adelante con un error técnico que un usuario normal no va a entender.
- **Eliminar movimiento:** pide confirmación explícita antes de borrar, mostrando los datos del movimiento, y avisa si por alguna razón ya no se pudo encontrar (por ejemplo, si alguien más lo borró un instante antes).

---

## 6. Nombres de cosas que se mejoraron

| Antes | Ahora | Por qué |
|---|---|---|
| `jButton3Guardar` (decía "Gestionar Registros") | `btnGuardarMovimiento` (dice "Guardar Movimiento") | El nombre y el texto no explicaban que este botón es el que de verdad guarda el movimiento. |
| `jButton3Guardar1` (decía "Guardar" pero no hacía nada) | `btnVerMovimientos` (dice "Ver Movimientos") | Reaprovechado para la función nueva, ver sección 4. |
| `Conexion.coleccionProv` | *(eliminado)* | Apuntaba a una colección de Mongo ("Proveedor") que ningún DAO real usaba. |
| Comentarios de `ProductoDAO`/`Archivo` mencionando "Camaronera", "Tamaño", "Dureza", "Especies" | Comentarios y nombres acordes a víveres | Eran restos copiados de otro proyecto (una camaronera) que no tenían nada que ver con este sistema. |

---

## 7. Librerías / dependencias

**No se agregó ninguna librería nueva.** Todo lo necesario (hash de contraseñas con `MessageDigest`, máscaras de fecha con `MaskFormatter`) ya viene incluido en el JDK estándar, sin necesidad de descargar nada adicional.

Sí se **quitó** una dependencia que no se usaba (`jcalendar-1.4.jar`, ver sección 3). Las dos que quedan son las mismas que ya traía el proyecto, ambas gratuitas y de descarga libre desde Maven Central (el repositorio oficial de librerías Java, sin pago ni registro):

- **MongoDB Java Driver 3.12.8** (acceso a la base de datos):
  `https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.12.8/mongo-java-driver-3.12.8.jar`
- **AbsoluteLayout RELEASE270** (el layout de posicionamiento absoluto que usa el editor visual de NetBeans):
  `https://repo1.maven.org/maven2/org/netbeans/external/AbsoluteLayout/RELEASE270/AbsoluteLayout-RELEASE270.jar`

Si por algún motivo hace falta volver a descargar cualquiera de los dos `.jar` (por ejemplo, si se clona el proyecto en otra máquina sin la carpeta `lib/`), esos enlaces los descargan directo, sin intermediarios.

---

## 8. Requisitos para ejecutar el proyecto

1. **JDK 24** instalado (el proyecto está configurado con `javac.source=24` / `javac.target=24` en `nbproject/project.properties`). Si se quiere usar una versión distinta del JDK, hay que bajar ese número en esa misma propiedad.
2. **MongoDB** corriendo en `localhost:27017` (el nombre de la base de datos es `ViveresDanielito`, se crea sola la primera vez que el programa se conecta e inserta algo). Si Mongo no está corriendo, el programa ahora lo avisa con un mensaje claro al abrir, en vez de tronar feo más adelante (ver punto 5).
3. **NetBeans** (o cualquier IDE que entienda proyectos Ant/NetBeans) para abrir el proyecto tal cual está, o simplemente compilarlo a mano con `javac` usando los dos `.jar` de `lib/` en el classpath.
4. La clase principal a ejecutar es **`Vista.FrmSistema`** (así está configurado en `manifest.mf`).
5. Usuario por defecto: **`admin`** / **`admin1234`** (se crea automáticamente la primera vez que se abre el programa, si todavía no existe ningún usuario `admin`). Se recomienda cambiar esa contraseña por una propia.

---

## 9. Sobre la verificación del código

Este trabajo se hizo en un entorno sin acceso a un compilador de Java (`javac`) ni a una conexión de red para instalarlo, así que **no fue posible compilar el proyecto de punta a punta para confirmarlo automáticamente**. En su lugar se hizo una revisión manual exhaustiva, archivo por archivo:

- Se verificó el balance de llaves/paréntesis en cada `.java` con un script de apoyo.
- Se rastrearon, uno por uno, todos los campos, métodos y clases tocados, para confirmar que cada referencia sigue apuntando a algo que existe.
- Se revisaron a mano los nombres de variables que el controlador de cada pantalla espera encontrar (`public` vs `private`) contra lo que cada formulario expone.

Aun así, **se recomienda compilar el proyecto una vez en NetBeans (Clean and Build) antes de darlo por definitivo**, por si algún detalle se escapó en la revisión manual.

---

## 10. Sobre los archivos `.form`

NetBeans guarda el diseño visual de cada ventana en un archivo `.form` que va de la mano con el `.java`. Se actualizaron los `.form` correspondientes en todos los casos donde se agregó, quitó o reposicionó un componente visual (por ejemplo, al quitar el botón fantasma `jButtonMenuPrincipal`, al revelar el buscador oculto de Gestión de Productos, o al crear el `.form` nuevo de `FrmGestionMovimientos`). Los cambios que **no** tocan componentes visuales (por ejemplo, quitar un `addWindowListener` que vive dentro del cuerpo de un método, o cambiar la lógica de un botón que ya existía) no afectan al `.form`, porque ese archivo solo describe la parte que genera el editor visual, no el código que cada quien escribe a mano dentro de los métodos de eventos.

Si al abrir el editor visual de NetBeans en alguna de estas pantallas se llegara a notar alguna inconsistencia menor, basta con guardar el formulario una vez desde el editor visual para que NetBeans lo vuelva a sincronizar; el comportamiento real del programa no depende del `.form`, sino del `.java`.

---

## 11. Cosas que se dejaron tal cual, a propósito

- El driver de MongoDB sigue usando la **API "legacy"** (`com.mongodb.DB`, `DBCollection`, etc., en vez de la API moderna `MongoDatabase`/`MongoCollection`). Migrar a la API nueva es un cambio grande que tocaría los seis DAOs del proyecto sin corregir ningún bug adicional, así que se dejó fuera del alcance de esta limpieza.
- Las tarjetas decorativas del panel principal ("Productos Registrados", "Proveedores Registrados", "Categorías") siguen mostrando solo iconos estáticos, sin datos en vivo ni función al hacer clic. No están rotas (ahora cargan bien su ícono gracias al arreglo de rutas de imagen), pero tampoco hacen nada interactivo. Convertirlas en tarjetas funcionales (por ejemplo, mostrando el conteo real de productos) es una mejora válida a futuro, pero se prefirió no tocar el layout del menú principal más de lo necesario para no arriesgar romper algo que no se puede revisar visualmente en este entorno.
- Los métodos `main()` que NetBeans genera automáticamente en cada formulario (para poder previsualizar la ventana sola, fuera del flujo normal de la app) se dejaron como están: no forman parte del flujo real del programa (la app siempre arranca desde `Vista.FrmSistema`, ver sección 8) y es una convención estándar de NetBeans, no un bug.

---

¡Gracias por la paciencia con un proyecto de este tamaño! Cualquier duda sobre por qué se tomó alguna decisión específica, ya quedó explicada arriba con el número de sección correspondiente.
