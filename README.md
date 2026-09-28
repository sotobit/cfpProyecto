# Entrega 2 de ventas y vendedores

Aplicación educativa de consola en **Java 21**, sin dependencias externas.
Lee catálogos y ventas TXT, valida registros y produce dos reportes CSV.
El repositorio contiene solo código, configuración y este README. Los datos e informes
de la entrega se conservan en la carpeta local hermana `Entrega2_Documentacion`.

## Ejecutar en NetBeans

1. Abrir la carpeta `cfpProyecto` con File > Open Project.
2. Registrar un JDK 21 en Tools > Java Platforms si aún no aparece.
3. En Properties > Libraries seleccionar ese JDK. En Sources comprobar Java 21 y UTF-8.
4. En Run mantener `Main` como clase principal y la raíz del proyecto como directorio de trabajo.
5. Ejecutar Run Project. En el primer uso, elegir **1** para generar datos de ejemplo;
   después usar **3** para calcular o **4** para exportar.

El generador también se ejecuta con Run File sobre `GenerateInfoFiles.java`.
No es necesario regenerar datos cada vez. Si existen datos de ejemplo, solo `SI`
autoriza reemplazarlos. Los TXT y CSV generados se ignoran en Git.

## Menú

| Opción | Acción |
| --- | --- |
| 1 | Genera cuatro productos, tres vendedores y sus ventas pseudoaleatorias. |
| 2 | Lee y muestra el texto de los archivos; no valida aún cada registro. |
| 3 | Relee, valida, calcula y muestra los dos reportes. |
| 4 | Relee, valida, calcula y exporta los CSV; funciona sin usar antes 2 o 3. |
| 5 | Finaliza. El fin de entrada también cierra correctamente. |

## Compilación y ejecución por consola

Desde la raíz del proyecto, con JDK 21 en PATH:

```powershell
New-Item -ItemType Directory -Force build/classes
javac -encoding UTF-8 --release 21 -d build/classes src/GenerateInfoFiles.java src/Main.java src/Producto.java src/Vendedor.java
java -cp build/classes Main
```

Con Ant y JAVA_HOME configurados: `ant jar` y `java -jar dist/cfpProyecto.jar`.
Eclipse: importar como proyecto existente y seleccionar JavaSE-21.
La validación incluida se ejecutó con `javac` y `java`; no constituye una prueba visual de NetBeans o Eclipse.

## Formatos

Todos los archivos son UTF-8, sin fila de títulos, con punto y coma. Precios y cantidades
son enteros entre 0 y 9223372036854775807. No se admiten centavos ni separadores de miles.
Identificadores y documentos se conservan como texto; `01` es distinto de `1`.

| Archivo | Formato |
| --- | --- |
| productos.txt | `ID;Nombre;Precio` |
| vendedores.txt | `TipoDocumento;Documento;Nombres;Apellidos` |
| ventas_*.txt | Primera línea `TipoDocumento;Documento`; siguientes `IDProducto;Cantidad;` |
| reporte_vendedores.csv | `Nombre completo;Recaudo` |
| reporte_productos.csv | `Nombre;Precio unitario` |

Los archivos de ventas se exploran solo en la raíz y en orden de nombre. La cabecera,
no el nombre del archivo, identifica al vendedor. Se permiten varios archivos por vendedor,
por ejemplo `ventas_100000001_extra.txt`. Se incluyen vendedores y productos sin ventas.

Vendedores: recaudo descendente; empate por tipo y documento en orden de texto.
Productos: unidades descendentes; empate por ID en orden de texto. Las unidades se
muestran en consola para comprobar el orden, pero no se agregan como columna al CSV.

## Errores y límites

Una fila incorrecta se omite con advertencia de archivo, línea y motivo. Se conserva
el primer ID válido ante duplicados. Una cabecera inválida descarta todo ese archivo.
Precios fraccionarios, negativos, campos vacíos, referencias inexistentes y desbordamientos
no se suman. Las tres comprobaciones numéricas se hacen antes de cambiar acumulados.

Catálogos ausentes o sin registros válidos, falta de archivos de ventas, todas las cabeceras
inválidas y errores de lectura detienen la operación y devuelven el control al menú.
Cada cálculo comienza desde cero. Los resultados pueden ser parciales respecto a las
entradas si hubo filas descartadas: siempre deben revisarse las advertencias.

Si falla la lectura, los CSV anteriores permanecen y no representan la ejecución actual.
Un fallo durante la exportación puede dejar uno de los reportes escrito o incompleto;
la aplicación avisa y no anuncia exportación completada. La publicación conjunta de los
dos archivos es una mejora pendiente de robustez.

El generador reemplaza solo `productos.txt`, `vendedores.txt` y sus tres archivos
`ventas_100000001.txt` a `ventas_100000003.txt`. Otros archivos de ventas permanecen:
revisarlos antes de generar otro conjunto. Los métodos públicos del generador escriben
archivos; la confirmación pertenece a los dos puntos de entrada interactivos.

Se eligen listas y búsqueda secuencial para facilitar la explicación. La búsqueda cuesta
O(n) y el ordenamiento por inserción O(n²) en el peor caso. No hay un límite fijo de
vendedores en el código, pero no se promete rendimiento para volúmenes masivos.

## Pruebas y documentación local

El informe APA 7, el documento de pendientes, la base de conocimiento, las fuentes
consultadas y los datos de ejemplo se entregan en `Entrega2_Documentacion`, fuera del
repositorio. El historial Git conserva la entrega anterior.

Pruebas de integración (Python 3 solo para verificar; no es necesario para usar Java):

```text
python pruebas/verificar.py --java-home "C:/ruta/al/jdk-21"
```

Las pruebas usan carpetas temporales y no reemplazan los datos de la raíz. Por defecto
no escriben archivos adicionales en el repositorio. Para guardar su registro se puede
pasar `--evidence-dir` con una carpeta externa.
Solo hay cuatro clases de aplicación y dos clases con `main`.

## Criterio académico

Se priorizó la indicación posterior del profesor y el criterio acordado: Java 21,
menú, TXT y descarte informado. Se conservan los dos reportes y los tres métodos exigidos
por la guía. Se usa ArrayList en lugar del HashMap sugerido para mantener el nivel
acordado. Esta versión no afirma cumplir simultáneamente la restricción anterior de
Java 8 y ejecución sin interacción.
