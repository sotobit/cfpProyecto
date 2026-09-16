# Entrega 1: generación, lectura y reportes CSV

Aplicación de consola para Java 8, sin dependencias externas. Integra la generación
indicada en el PDF con la lectura y los reportes del HTML. Puede abrirse en NetBeans
(Ant) y en Eclipse.

## NetBeans

1. Tener un JDK instalado, preferiblemente JDK 8.
2. Abrir `cfpProyecto` con **File > Open Project**.
3. Ejecutar `GenerateInfoFiles.java` mediante **Run File**.
4. Ejecutar `Main.java` mediante **Run File** o usar **Run Project**.
5. Revisar el mensaje de éxito y los CSV en la raíz del proyecto.

## Eclipse

1. Usar **File > Import > General > Existing Projects into Workspace** y seleccionar `cfpProyecto`.
2. Configurar un JDK compatible con el entorno **JavaSE-1.8**.
3. Ejecutar `GenerateInfoFiles.java` con **Run As > Java Application**.
4. Ejecutar `Main.java` de la misma manera. Usar la raíz del proyecto como directorio de trabajo.

## Consola

Desde la raíz de `cfpProyecto`, crear la carpeta `build/classes` y ejecutar con JDK 8:

```text
javac -encoding UTF-8 -d build/classes src/GenerateInfoFiles.java src/Main.java src/Producto.java src/Vendedor.java
java -cp build/classes GenerateInfoFiles
java -cp build/classes Main
```

Con Ant y un JDK disponibles en el PATH:

```text
ant -Dmain.class=GenerateInfoFiles run
ant run
```

`ant jar` crea el ejecutable. Desde la raíz, `java -jar dist/cfpProyecto.jar` procesa
los archivos existentes. `Main` no genera ni modifica los datos de entrada.

## Archivos

Todos usan UTF-8 y punto y coma, sin encabezados. Estos ejemplos se pueden ver los formatos:

| Archivo | Ejemplo |
| --- | --- |
| `productos.csv` | `1;Laptop;1500000.50` |
| `vendedores.csv` | `CC;100000001;Ana;Perez` |
| `vendedor_100000001.csv` | Primera línea: `CC;100000001`. Después: `1;5;` |
| `reporte_vendedores.csv` | `Ana Perez;7500002.50` |
| `reporte_productos.csv` | `Laptop;1500000.50` |

El generador crea cuatro productos y tres vendedores (documentos 100000001 a
100000003, tipo CC), con dos a cinco registros de venta cada uno. Genera precios
enteros entre 10000 y 2500000 y cantidades entre 1 y 10. Los nombres, apellidos,
precios y ventas son pseudoaleatorios. Los documentos y nombres de archivo son
estables: repetir la generación sobrescribe los mismos cinco archivos. Si se reduce
la cantidad de vendedores en el código, los archivos sobrantes no se borran solos.

`Main` lee ambos catálogos y todos los archivos `vendedor_*.csv` de la raíz, sin fijar
la cantidad de vendedores. Se pueden agregar archivos como `vendedor_100000001_extra.csv`:
la primera línea identifica al vendedor y todas sus ventas se suman. Un vendedor
puede no tener archivo; un archivo con solo la primera línea representa cero ventas.
Debe existir al menos un archivo de ventas y ambos catálogos deben contener datos.

El reporte de vendedores incluye nombre completo y recaudo, ordenados de mayor a
menor recaudo. El de productos incluye nombre y precio unitario, ordenados por
unidades vendidas de mayor a menor. La cantidad sirve para ordenar, pero no se añade
como columna. Ambos incluyen registros sin ventas. Los empates se resuelven por
orden de texto del identificador (tipo y documento para vendedores).

Los importes se calculan con `BigDecimal` y se escriben con punto y dos decimales,
redondeando la salida con `HALF_UP`. Se admiten precios decimales en las entradas.

## Clases y validaciones

- `GenerateInfoFiles`: primer `main` y los métodos `createProductsFile(int)`,
  `createSalesManInfoFile(int)` y `createSalesMenFile(int, String, long)`.
- `Main`: segundo `main`, lectura línea por línea, procesamiento y reportes.
- `Producto` y `Vendedor`: datos y acumulados, sin método `main`.

Se usan ciclos, listas, mapas y cierre automático de archivos. Ningún programa pide
datos al usuario. Los archivos faltantes, formatos incorrectos, campos vacíos,
identificadores duplicados en catálogos, referencias inexistentes y valores negativos
producen un mensaje de error y código de salida 1. Los precios y cantidades cero
son válidos en entradas externas. Las ventas requieren el punto y coma final.

Los reportes se escriben después de validar todas las entradas. Si falla la validación,
los reportes anteriores permanecen sin cambios y no representan los datos actuales.
Un error de escritura puede dejar un reporte incompleto: revisar siempre el mensaje
final de éxito. Cada ejecución calcula desde cero, sin acumular resultados anteriores.
