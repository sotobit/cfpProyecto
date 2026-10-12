# Entrega final de ventas y vendedores

Aplicación educativa en **Java 21**, con interfaz Swing, archivos TXT y dos reportes CSV.
Permite crear, consultar, modificar y eliminar productos, vendedores y filas de ventas.
No usa bibliotecas externas. El repositorio contiene código, configuración, pruebas,
este README y la reflexión exigida en `conslusion.txt`.

## Ejecutar en NetBeans

1. Abrir `cfpProyecto` con File > Open Project.
2. Registrar JDK 21 en Tools > Java Platforms y seleccionarlo en Properties > Libraries.
3. Comprobar Java 21 y UTF-8 en Sources; la clase principal en Run es **`main`**, en minúsculas.
4. Mantener la raíz del proyecto como directorio de trabajo y ejecutar Run Project.

El arranque carga, valida, calcula y exporta automáticamente ambos reportes, y después
abre la ventana. No pide opciones ni información. Si todos los TXT de entrada están
ausentes, genera el ejemplo inicial. Si existe un conjunto incompleto, muestra un error
sin reemplazarlo. La interfaz permite cargar otra carpeta o generar ejemplos para recuperarse.

Run File sobre `GenerateInfoFiles.java` genera cuatro productos, tres vendedores y sus
ventas pseudoaleatorias, sin preguntas. Antes de sustituir un conjunto, guarda los
archivos anteriores en `respaldos/cambio_...`. Los archivos de ventas adicionales del
conjunto anterior se retiran de la raíz y quedan en el respaldo. Los CSV anteriores
también se respaldan y retiran: para obtener los nuevos, ejecutar `main`.

Hay **dos y solo dos clases con método main**: `GenerateInfoFiles` y `main`.
Los auxiliares de prueba se generan en carpetas temporales externas y no entran al JAR.

## Usar la ventana

| Control | Acción |
| --- | --- |
| Cargar datos | Relee la carpeta activa, valida y calcula; deja la exportación pendiente. |
| Abrir carpeta | Selecciona otra carpeta de entradas y muestra sus datos válidos. |
| Generar ejemplos | Tras confirmar la acción voluntaria, respalda y sustituye las entradas; calcula y exporta. |
| Recalcular | Relee los TXT y calcula desde cero. |
| Exportar CSV | Relee, calcula y escribe ambos reportes en la carpeta activa. |
| Nuevo | Limpia el formulario para crear un registro. |
| Guardar | Crea o modifica el registro, guarda los TXT y actualiza ambos CSV. |
| Eliminar | Confirma el borrado del registro seleccionado y aplica las restricciones de referencias. |

Las pestañas son Productos, Vendedores, Ventas y Reportes. Seleccionar una fila llena el
formulario. Los identificadores no se modifican al editar. Una venta conserva su vendedor
y archivo: se pueden cambiar producto y cantidad. Para asignarla a otro vendedor se elimina
y se crea una nueva. Las tablas se consultan; los formularios realizan los cambios.

Cambiar el precio recalcula todas las ventas con el precio actual del catálogo. No se
conserva un precio histórico por transacción. Los botones y formularios se bloquean
mientras `SwingWorker` realiza una operación de archivos.

## Formatos y cálculos

Archivos UTF-8, punto y coma, sin títulos de columnas. Los precios y cantidades son
enteros no negativos hasta `9223372036854775807`; no se admiten centavos ni separadores
de miles. El documento y los IDs son texto: `01` y `1` son diferentes.

| Archivo | Campos |
| --- | --- |
| productos.txt | `ID;Nombre;Precio` |
| vendedores.txt | `TipoDocumento;Documento;Nombres;Apellidos` |
| ventas_*.txt | Cabecera `TipoDocumento;Documento`; luego `IDProducto;Cantidad;` |
| reporte_vendedores.csv | `Nombre completo;Recaudo` |
| reporte_productos.csv | `Nombre;Precio unitario` |

Los archivos de ventas se leen solo en la raíz de la carpeta activa y en orden de nombre.
Su cabecera identifica al vendedor. Se suman varios archivos del mismo vendedor y se
incluyen registros sin ventas. El recaudo es la suma de precio por cantidad.

Vendedores: recaudo descendente, con empate por tipo y documento como texto.
Productos: unidades descendentes, con empate por ID como texto. Las unidades aparecen
en pantalla para verificar el orden y no se añaden al CSV de productos. Las comillas
en nombres se escapan en las salidas CSV.

## Validación y protección de archivos

Una fila inválida se omite con archivo, línea y motivo; una cabecera inválida descarta
ese archivo. Se conserva el primer identificador válido frente a duplicados. Las
comprobaciones de multiplicación, suma de unidades y suma de recaudo preceden a los
cambios de acumulados. Cada carga inicia desde cero.

Catálogos ausentes o sin registros válidos, falta de ventas y errores de lectura
detienen el procesamiento. Las advertencias permiten reportes de las filas válidas,
pero bloquean el CRUD hasta corregir y recargar las entradas, evitando perder las
filas originales. También se detectan cambios externos antes de guardar.

No se elimina un producto o vendedor con filas de ventas relacionadas, aunque su
cantidad sea cero; tampoco el último registro de un catálogo. Eliminar la última
venta conserva la cabecera. Un vendedor sin ventas puede retirarse junto con sus
archivos vacíos. Las nuevas ventas usan el primer archivo del vendedor o un nombre
seguro `ventas_ui_<n>.txt` cuando no tiene archivo.

Las escrituras se preparan en temporales, se respaldan los originales y se restauran
las sustituciones realizadas ante errores de E/S. El guardado CRUD incluye entradas
y los dos reportes. Los respaldos se conservan; no se borran automáticamente.
Esto protege frente a errores controlados, pero no garantiza una transacción atómica
ante apagados abruptos, fallos del disco o cambios concurrentes durante la sustitución.

Después de cargar sin exportar, la ventana identifica los CSV como pendientes.
Un fallo no anuncia exportación completada ni presenta CSV anteriores como actuales.

## Compilar y ejecutar

PowerShell desde la raíz, con JDK 21 en PATH:

```powershell
New-Item -ItemType Directory -Force build/classes
$fuentes = Get-ChildItem src/*.java | ForEach-Object FullName
javac -encoding UTF-8 --release 21 -d build/classes @fuentes
java -cp build/classes main
```

Para procesamiento automático sin ventana:

```powershell
java -cp build/classes main --sin-interfaz
```

Con Ant y JDK 21: `ant clean jar`; ejecutar `java -jar dist/cfpProyecto.jar`.
También se puede crear el JAR con el JDK:

```powershell
New-Item -ItemType Directory -Force dist
jar --create --file dist/cfpProyecto.jar --main-class main -C build/classes .
```

Eclipse conserva metadatos JavaSE-21. La verificación realizada usa JDK 21 y el JAR;
no se atribuye una comprobación visual al IDE cuando no se ha realizado.

## Pruebas y documentación

Python 3 se usa solo para verificar; no se necesita para ejecutar la aplicación.

```text
python pruebas/verificar.py --java-home "C:/ruta/al/jdk-21"
python pruebas/interfaz.py --java-home "C:/ruta/al/jdk-21"
```

Se verificaron **38 escenarios de integración y 12 de interfaz**, además del JAR con
el caso manual. Las pruebas trabajan en carpetas temporales; `--evidence-dir` permite
guardar resultados en una carpeta externa. La segunda suite acciona los controles
Swing reales y captura sus componentes en ejecución mediante `printAll`: estas vistas
no son fotografías del escritorio ni imágenes generadas con IA.

El informe APA 7, la base de conocimiento, capturas, resultados, caso manual y JAR se
conservan en la carpeta local hermana `EntregaFinal_Documentacion`. La entrega 2 se
mantiene como antecedente y las lecturas institucionales siguen fuera de GitHub.

La ejecución inicial automática cumple el criterio acordado de no solicitar información
al arrancar. El CRUD voluntario amplía el proyecto para su presentación final. Se mantiene
Java 21 y NetBeans conforme a la propuesta trabajada; se reconoce la diferencia respecto
a la guía anterior que menciona Java 8 y Eclipse.
