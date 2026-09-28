# Base de conocimiento de la entrega 2

## Propósito y criterio de lectura

Esta base conecta el material de estudio con decisiones verificables del programa de ventas.
No es una copia de las lecturas. Se conservan sus archivos originales localmente; el repositorio
publica explicaciones propias, código y evidencias. Las páginas indicadas son las páginas físicas
del PDF, contando portada, pues la numeración impresa no siempre coincide.

## Inventario de fuentes

| Fuente local | Contenido y uso |
| --- | --- |
| Lectura 1.pdf (31 páginas) | Rafael Niquefa, Conceptos iniciales en lenguajes de programación. Vocabulario, Java, tipos, nombres, operadores, bits, precisión, cadenas y conversiones. Fundamenta long, String, boolean, nombres y cálculos. |
| Lectura 3.pdf (36 páginas) | Rafael Niquefa, Estructuras de control de flujo de programa. Decisiones, ciclos, break, continue, contador, acumulador, bandera, división de cadenas e indentación. Fundamenta menú, lectura y acumulación. |
| Lectura 2.pdf (16 páginas) | Rafael Niquefa, Funciones o métodos. Modularidad, parámetros, argumentos, retorno, ámbito, cadenas y recursión. Fundamenta la división en tareas pequeñas. |
| Lectura 4.pdf (24 páginas) | Rafael Niquefa, Arreglos de tamaño fijo. Índices, recorridos, utilidades, matrices, desviación estándar y memoización. Fundamenta arreglos del generador y campos separados. |
| Lectura 5.pdf (14 páginas) | Rafael Niquefa, Contenedores estándar de datos. ArrayList, LinkedList, Vector, Stack, Queue y PriorityQueue. Fundamenta listas de tamaño variable. |
| Prioridades y operadores.pdf (1 página) | Referencia de operadores; se revisó visualmente. Se usa multiplicación antes de suma y cortocircuito en validaciones. |
| Guia Entregables.pdf (6 páginas) | Requisitos de archivos, dos reportes, generador, dos main, entregas y reflexión. |
| Generación y clasificación de datos.pdf (6 páginas) | Segunda copia de las instrucciones; coincide en el alcance y formato del proyecto. No se cuenta como una exigencia adicional distinta. |
| Mejora Segun Profesor.png | Propuesta de ventas: Java 21, cruce por ID, validación, entradas TXT, salida CSV y menú de cinco opciones. |
| Entrega1_ConceptosFundamentalesProgramacion.docx | Antecedente del generador, autora, profesor e institución. El enlace antiguo se reemplaza por sotobit/cfpProyecto. |
| ExplicaciónProyectoOriginal.html | Guía NetBeans, generador, reportes y simulador. Se usa como antecedente, no como prueba de ejecución real. |
| Conceptos-Fundamentales-de-Programacion.html | Desarrollo interactivo de escenarios 1 a 3, glosario, ejercicios y erratas. Complementa los PDF; sus afirmaciones también necesitan revisión. |
| EjemploBuffer.java | nextInt seguido de nextLine con limpieza comentada. Motiva leer todas las opciones con nextLine. |
| cfpProyecto | Cuatro clases previas, CSV, README y metadatos. Se conserva el historial y se archivan los ejemplos originales. |

## Lectura 1 y selección de datos

Un programa contiene instrucciones; el compilador transforma el código fuente y la JVM ejecuta
el bytecode. El IDE organiza edición, compilación y depuración, pero no reemplaza al JDK.
Por eso se verifica con javac y java además de entregar configuración para NetBeans.

Los ocho tipos primitivos se distinguen de String, que es una clase. Se usa int para índices
y contadores pequeños, long para precios y cantidades, boolean para controlar el menú y
String para nombres e identificadores. El documento no se convierte a número: no se suma ni
multiplica y puede tener ceros iniciales. Los acumulados inician en cero.

El límite positivo de long es 9223372036854775807. Ser entero no significa ser ilimitado.
Antes de multiplicar precio por cantidad se compara el precio con Long.MAX_VALUE / cantidad,
solo cuando la cantidad es mayor que cero. Antes de sumar se compara el incremento con el
límite menos el acumulado. Así una venta inválida no modifica ni unidades ni recaudo.

BigInteger y BigDecimal sí aparecen en la lectura. Se retira BigDecimal por el alcance
acordado de pesos enteros, no porque sea incorrecto. No se sustituye por double: el proyecto
no necesita aproximación binaria ni redondeo. Los precios fraccionarios se rechazan, no se truncan.

String.equals compara contenido; compareTo define el desempate de IDs como texto; trim
elimina espacios externos; split separa campos. Long.parseLong transforma texto en entero y
puede fallar. Los operadores de bits, fechas y búsquedas avanzadas de cadenas se reconocen
como temas estudiados, pero no se introducen en una suma de ventas sin una necesidad concreta.

## Lectura 3 y control del programa

El while del menú continúa mientras la bandera continuar sea verdadera. switch elige una
opción y break termina cada caso; no termina por sí solo el while. La opción 5 cambia la
bandera. hasNextLine evita fallar o repetir infinitamente al cerrar la entrada.

Otro while lee líneas hasta readLine() == null. Un contador numero permite ubicar registros
incorrectos. cantidadVendida y totalRecaudado son acumuladores, no contadores de registros:
una venta de ocho unidades incrementa cantidadVendida en ocho, aunque sea una sola línea.

Los for recorren productos y vendedores. En el ordenamiento por inserción, el for toma un
registro y el while desplaza los que deben ir después. Se usan llaves y cuatro espacios de
sangría incluso para bloques pequeños. No se reproducen los ciclos exóticos del material.

El principio de leer todas las entradas como líneas, explicado en la sección de división de
cadenas de la lectura, también resuelve EjemploBuffer.java. Las opciones se comparan como
String y no necesitan convertir números ni mezclar nextInt y nextLine.

## Lectura 2 y organización en métodos

Un parámetro aparece en la declaración del método; un argumento es el valor que se entrega
al invocarlo. En createProductsFile(4), productsCount es el parámetro y 4 es el argumento.
void indica que no se devuelve un resultado; leerProductos devuelve una lista. return termina
el método y entrega el valor cuando corresponde. null indica una búsqueda sin coincidencias.

Main organiza lectura, búsqueda, cálculo, ordenamiento y exportación. Producto y Vendedor
son objetos simples que mantienen juntos los datos relacionados. Sus constructores asignan
los datos iniciales. Los campos final de identidad no se reasignan; los acumulados sí cambian.
No hay herencia propia, interfaces propias ni programación funcional.

Los métodos auxiliares son privados porque se usan dentro de la clase; los tres métodos del
generador conservan las firmas públicas exigidas. Las restricciones de ejercicios sobre
imprimir solo en main no se interpretan como prohibición general para el proyecto: los
métodos de presentación imprimen y los métodos de cálculo y búsqueda hacen su tarea concreta.

Recursión y casos base se revisan como parte de la lectura. No se usan porque procesar una
secuencia de ventas es iterativo. Esto evita complejidad sin beneficio para el problema.

## Lectura 4 y arreglos

Los índices comienzan en cero y llegan hasta length - 1. Los arreglos del generador contienen
nombres, apellidos y productos de ejemplo. El índice aleatorio siempre es menor que length.
El operador residuo permite reutilizar nombres de productos cuando se solicita un catálogo
más grande. Un nombre puede repetirse; lo que debe distinguir productos es su identificador.

split devuelve String[]. Se usa split(";", -1) para conservar campos vacíos finales y poder
validarlos. En una venta se comprueba primero el punto y coma final, luego se retira únicamente
ese separador y se exigen dos campos. No se permite que un separador sobrante esconda un dato.

Las matrices, desviación estándar y memoización son temas del material, pero no requisitos
funcionales del sistema de ventas. Las listas evitan imponer un máximo fijo a los catálogos.

## Lectura 5 y listas

ArrayList guarda una secuencia cuyo tamaño puede crecer. add incorpora un registro, get
consulta una posición, set reemplaza una posición y size informa el número de elementos.
ArrayList<Producto> solo admite elementos de ese tipo; ArrayList<Vendedor> mantiene a los
vendedores. La declaración genérica evita mezclar categorías sin necesidad de conversiones.

Se busca recorriendo la lista y comparando IDs. Es más fácil de seguir, aunque su costo crece
con la cantidad de registros. Inserción tiene costo cuadrático en el peor caso. La entrega
prioriza comprensión; HashMap y algoritmos más eficientes quedan como optimización opcional.

Stack es LIFO; Queue representa FIFO; PriorityQueue extrae según prioridad y no garantiza
que recorrerla produzca una lista ordenada. Ninguna se necesita para generar estos CSV.
Se revisaron los ejercicios de frecuencias: su idea de acumular y ordenar sí se traslada al
conteo de unidades vendidas. No se implementan ejercicios ajenos al proyecto dentro de src.

## Resolución de discrepancias

| Tema | Material anterior | Decisión de entrega 2 |
| --- | --- | --- |
| Java e IDE | Guía: Java 8 y Eclipse | Java 21 y NetBeans; metadatos Eclipse conservados y actualizados. |
| Interacción | Guía: dos programas sin preguntas | Menú y confirmación de reemplazo, siguiendo la indicación posterior priorizada. |
| Estructura | Imagen: HashMap | ArrayList, según nivel acordado; se conserva el cruce por ID. |
| Entrada | Proyecto anterior: CSV | TXT con punto y coma y cabecera del vendedor; CSV históricos separados. |
| Vendedor | Imagen ejemplifica ID en nombre del archivo | La cabecera conserva tipo y documento; evita perder la identidad de la guía. |
| Salida | Imagen destaca un consolidado de vendedores | Se conservan ambos reportes; no se cambia el esquema obligatorio de columnas. |
| Dinero | Proyecto anterior: BigDecimal | long en pesos enteros; no se aceptan centavos. |
| Corrupción | Proyecto anterior: detener ante una fila errónea | Omitir fila con advertencia; detener solo cuando no hay base válida o falla la lectura. |
| Clases main | PDF escribe main; HTML usa Main | Se conserva Main como nombre de clase y main como método Java. |

Las cinco opciones se adaptan al entorno local. Cargar significa leer los archivos de la raíz;
exportar significa escribir allí los CSV, no descargar desde Internet. La ilustración del
profesor no define todos los detalles de los formatos; se conserva la información necesaria
para ambos reportes.

## Revisión crítica y comprobaciones de los materiales

Se inspeccionaron visualmente el método max de Lectura 2 (página física 4), el ejemplo de
arreglos de Lectura 4 (página 4) y la tabla de operadores. La extracción de PDF puede desplazar
llaves, por lo que no se confunde un error de extracción con un error del algoritmo.

- El max con comparaciones estrictas devuelve 1 para (5, 5, 2, 1), aunque el mayor es 5.
  El proyecto no reutiliza ese método; sus empates tienen un criterio explícito.
- Reasignar arr en `for (int[] arr : b)` no asigna las filas de b. En cambio, escribir
  `b[i] = new int[m]` modifica el arreglo. No se copia el ejemplo incorrecto.
- El HTML indica que quitar paréntesis a i*j dentro de una concatenación cambia la
  multiplicación. En esa expresión, * ya tiene mayor precedencia que +; esa explicación
  no es correcta. Sí pueden requerirse paréntesis al sumar números junto con texto.
- El texto del curso llama a Python no tipado; técnicamente tiene tipado dinámico.
  No se usa esa clasificación imprecisa en el informe.
- Las tablas que muestran mínimos positivos de float/double no representan el valor más
  negativo del tipo. La precisión arbitraria tampoco equivale a memoria infinita.
- Los casos de Fibonacci, primos y comparaciones con tolerancia contienen inconsistencias
  entre algunos enunciados y salidas. No se usan como oráculos de las pruebas de ventas.
- El HTML del proyecto genera tipos de documento por separado en catálogos y ventas,
  lo que puede romper la correspondencia. El nuevo generador usa CC coherentemente.

Las pruebas del proyecto verifican resultados, no solo compilación: Ana vende 2 teclados de
100 y 4 mouse de 50, total 400; Luis vende 8 mouse de 50, total 400; Eva no vende, total 0.
Mouse acumula 12 unidades, Teclado 2 y Monitor 0. Los resultados esperados se guardan en
`ejemplos/caso_manual` y el script de pruebas compara los archivos producidos.

## Mapa de implementación para estudiar

| Responsabilidad | Métodos principales | Conceptos |
| --- | --- | --- |
| Elegir una acción | main | while, switch, boolean, Scanner.nextLine |
| Leer catálogos | leerProductos, leerVendedores | archivos, while, ArrayList.add |
| Separar y convertir | separar, enteroNoNegativo | String[], split, trim, parseLong |
| Encontrar registros | buscarProducto, buscarVendedor | for, equals, return, null |
| Procesar ventas | procesarVentas, acumularVenta | relaciones por ID, validación, multiplicación, acumuladores |
| Ordenar | ordenarProductos, ordenarVendedores | inserción, get, set, comparación y empates |
| Exportar | escribirReportes, filaProducto, filaVendedor | BufferedWriter, concatenación, CSV |
| Producir ejemplos | tres métodos create exigidos | arreglos, Random, for, IDs coherentes |

## Preguntas para la sustentación

1. ¿Por qué el precio no se guarda como String? Se convierte para poder multiplicarlo.
2. ¿Por qué el documento sí queda como String? Identifica, no se usa en operaciones aritméticas.
3. ¿Por qué se recalcula cada vez? Evita sumar nuevamente sobre resultados de una operación anterior.
4. ¿Qué ocurre con una venta corrupta? Se informa su ubicación y no se modifica ningún acumulado.
5. ¿Cómo se suman dos archivos del mismo vendedor? Ambos localizan el mismo objeto por su cabecera.
6. ¿Qué significa null? Que la búsqueda no encontró una coincidencia.
7. ¿Por qué el producto más caro no aparece primero? Se ordena por unidades, no por precio.
8. ¿Por qué long tampoco basta siempre? Tiene un máximo; se comprueba antes de operar.
9. ¿Por qué no hay HashMap? Se eligió practicar listas y búsqueda secuencial; se reconoce el costo.
10. ¿Qué falta para la entrega 3? Sustentación personal, validación en el IDE de la estudiante y
    revisión de robustez; las mejoras opcionales están separadas de las funciones ya completas.

## Fuentes externas comprobadas

- Oracle. Documentación de ArrayList para Java SE 21: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html
- Oracle. Documentación de Long para Java SE 21: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Long.html
- American Psychological Association. Orientación para trabajos estudiantiles:
  https://www.apa.org/ed/precollege/psn/2020/09/apa-style-student-papers

Las lecturas de Rafael Niquefa no muestran una fecha editorial inequívoca: se citan como
sin fecha, con letras para distinguir títulos. No se atribuyen al autor los años de los
libros que aparecen dentro de su bibliografía.
