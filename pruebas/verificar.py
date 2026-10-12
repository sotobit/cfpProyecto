"""Pruebas reales de Java 21 en carpetas temporales, sin modificar las entradas del proyecto."""
import argparse
import ctypes
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--java-home', required=True)
parser.add_argument('--evidence-dir', type=Path, help='Carpeta externa para los registros de prueba')
args = parser.parse_args()
repo = Path(__file__).resolve().parents[1]
java_home = Path(args.java_home).resolve()
java = java_home / 'bin/java.exe'
javac = java_home / 'bin/javac.exe'
classes = repo / 'build/classes'
if classes.exists():
    assert classes.resolve().is_relative_to((repo / 'build').resolve()) and not classes.is_symlink()
    shutil.rmtree(classes)
classes.mkdir(parents=True)
compilation = subprocess.run([str(javac), '-encoding', 'UTF-8', '--release', '21', '-Xlint:all', '-d', str(classes),
                              *map(str, (repo / 'src').glob('*.java'))], encoding='utf-8', capture_output=True)
assert compilation.returncode == 0 and not compilation.stderr.strip(), compilation.stderr
version = subprocess.run([str(java), '-version'], capture_output=True, encoding='utf-8').stderr.strip()
base = {
    'productos.txt': '1;Teclado;100\n2;Mouse;50\n3;Monitor;200\n',
    'vendedores.txt': 'CC;10;Ana;Pérez\nCC;20;Luis;Gómez\nCC;30;Eva;Ruiz\n',
    'ventas_10.txt': 'CC;10\n1;2;\n2;1;\n',
    'ventas_10_extra.txt': 'CC;10\n2;3;\n',
    'ventas_20.txt': 'CC;20\n2;8;\n',
}
expected_v = 'Ana Pérez;400\nLuis Gómez;400\nEva Ruiz;0\n'
expected_p = 'Mouse;50\nTeclado;100\nMonitor;200\n'
results = []


def prepare(root, changes=None):
    files = dict(base)
    files.update(changes or {})
    for name, value in files.items():
        if value is not None:
            (root / name).write_text(value, encoding='utf-8', newline='\n')


def execute(root, mode='main', arguments=('--sin-interfaz',), extra_classes=None):
    classpath = str(classes) if extra_classes is None else str(classes) + os.pathsep + str(extra_classes)
    return subprocess.run([str(java), '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-cp', classpath,
                           mode, *arguments], cwd=root, input='', encoding='utf-8', capture_output=True, timeout=20)


def verify_outputs(root, expected):
    if expected is not None:
        for name, want in zip(['reporte_vendedores.csv', 'reporte_productos.csv'], expected):
            assert (root / name).read_text(encoding='utf-8') == want, (name, root)


def passed(name, group='Procesamiento'):
    results.append({'caso': name, 'grupo': group, 'resultado': 'APROBADO'})


def run_case(name, changes=None, expected=(expected_v, expected_p), contains=(), code=0, pre=None,
             mode='main', arguments=('--sin-interfaz',), repeat=1):
    with tempfile.TemporaryDirectory(prefix='cfp-final-') as td:
        root = Path(td)
        prepare(root, changes)
        if pre:
            pre(root)
        for _ in range(repeat):
            proc = execute(root, mode, arguments)
            output = proc.stdout + proc.stderr
            assert proc.returncode == code, (name, proc.returncode, output)
            for part in contains:
                assert part in output, (name, part, output)
            verify_outputs(root, expected)
        passed(name)
        return output


log = run_case('Cálculo manual: 400/400/0, 12/2/0 unidades, empate y múltiples archivos', contains=['Unidades: 12'])
run_case('Repetir el procesamiento no duplica acumulados', repeat=3)
run_case('Filas corruptas se omiten sin alterar las válidas',
         {'ventas_10.txt': base['ventas_10.txt'] + '99;1;\n1;-2;\n1;2.5;\n1;dos;\n1;1\n1;;\n\n'}, contains=['línea 4', 'Producto inexistente'])
run_case('Primer identificador válido, duplicados y precios inválidos',
         {'productos.txt': base['productos.txt'] + '1;Otro;999\n4;X;-1\n5;X;1.5\n6;;1\n',
          'vendedores.txt': base['vendedores.txt'] + 'CC;10;Otra;Persona\n'}, contains=['duplicado'])
run_case('Cabecera inexistente se omite', {'ventas_mala.txt': 'CC;999\n1;100;\n'}, contains=['Vendedor inexistente'])
run_case('Cabecera ausente se omite', {'ventas_vacia.txt': ''}, contains=['Falta la cabecera'])
run_case('Archivo solo con cabecera y cantidad cero', {'ventas_30.txt': 'CC;30\n1;0;\n'})
run_case('Mismo documento con tipos distintos',
         {'vendedores.txt': base['vendedores.txt'] + 'CE;10;Sara;Díaz\n', 'ventas_ce.txt': 'CE;10\n1;1;\n'},
         expected=('Ana Pérez;400\nLuis Gómez;400\nSara Díaz;100\nEva Ruiz;0\n', expected_p))
run_case('Ceros iniciales y desempate de identificadores como texto',
         {'productos.txt': '01;Uno;10\n1;Otro;10\n', 'ventas_10.txt': 'CC;10\n01;1;\n1;1;\n',
          'ventas_10_extra.txt': None, 'ventas_20.txt': 'CC;20\n'},
         expected=('Ana Pérez;20\nLuis Gómez;0\nEva Ruiz;0\n', 'Uno;10\nOtro;10\n'))
run_case('Desbordamiento de multiplicación sin cambios parciales',
         {'ventas_10.txt': base['ventas_10.txt'] + '1;9223372036854775807;\n'}, contains=['importe de la venta excede'])
run_case('Desbordamiento de recaudo no suma unidades',
         {'productos.txt': '1;Grande;9223372036854775807\n2;Uno;1\n', 'ventas_10.txt': 'CC;10\n1;1;\n2;1;\n',
          'ventas_10_extra.txt': None, 'ventas_20.txt': 'CC;20\n'},
         expected=('Ana Pérez;9223372036854775807\nLuis Gómez;0\nEva Ruiz;0\n', 'Grande;9223372036854775807\nUno;1\n'),
         contains=['acumulado excede', 'Uno;1 | Unidades: 0'])
run_case('Desbordamiento de unidades en producto gratuito',
         {'productos.txt': '1;Gratis;0\n', 'ventas_10.txt': 'CC;10\n1;9223372036854775807;\n1;1;\n',
          'ventas_10_extra.txt': None, 'ventas_20.txt': 'CC;20\n'},
         expected=('Ana Pérez;0\nLuis Gómez;0\nEva Ruiz;0\n', 'Gratis;0\n'), contains=['acumulado excede'])


def previous_reports(root):
    for name in ['reporte_vendedores.csv', 'reporte_productos.csv']:
        (root / name).write_text('ANTERIOR\n', encoding='utf-8')


for name, change, message in [
    ('Catálogo ausente sin regeneración silenciosa', {'productos.txt': None}, 'Falta un archivo regular'),
    ('Catálogo vacío', {'productos.txt': ''}, 'no contiene productos válidos'),
    ('Vendedores sin registros válidos', {'vendedores.txt': 'fila mala\n'}, 'no contiene vendedores válidos'),
    ('Sin archivos de ventas', {k: None for k in base if k.startswith('ventas_')}, 'No se encontraron'),
    ('Todas las cabeceras inválidas', {k: 'CC;999\n' for k in base if k.startswith('ventas_')}, 'Ningún archivo'),
]:
    run_case(name, change, expected=('ANTERIOR\n', 'ANTERIOR\n'), contains=[message, 'no son resultados actuales'], code=1, pre=previous_reports)


def directory_output(root):
    (root / 'reporte_vendedores.csv').write_text('ANTERIOR\n', encoding='utf-8')
    (root / 'reporte_productos.csv').mkdir()


with tempfile.TemporaryDirectory(prefix='cfp-error-') as td:
    root = Path(td); prepare(root); directory_output(root)
    proc = execute(root)
    assert proc.returncode == 1 and 'ERROR' in proc.stderr
    assert (root / 'reporte_vendedores.csv').read_text(encoding='utf-8') == 'ANTERIOR\n'
    assert (root / 'reporte_productos.csv').is_dir()
    passed('Destino CSV inválido conserva el otro reporte anterior', 'Escritura')

run_case('Argumento desconocido: error sin esperar entrada', expected=None, code=1,
         arguments=('--desconocido',), contains=['argumento desconocido'])

with tempfile.TemporaryDirectory(prefix='cfp-inicial-') as td:
    root = Path(td); proc = execute(root)
    assert proc.returncode == 0 and 'Ejemplo inicial generado automáticamente' in proc.stdout
    assert len(list(root.glob('*.txt'))) == 5 and len(list(root.glob('*.csv'))) == 2
    assert 'ADVERTENCIA' not in proc.stdout and '¿' not in proc.stdout
    passed('Primer arranque vacío genera y exporta sin preguntas', 'Generador')

with tempfile.TemporaryDirectory(prefix='cfp-generar-') as td:
    root = Path(td); prepare(root); previous_reports(root)
    before = {p.name: p.read_bytes() for p in root.iterdir() if p.is_file()}
    proc = execute(root, 'GenerateInfoFiles', ())
    assert proc.returncode == 0 and 'exitosamente' in proc.stdout and '¿' not in proc.stdout
    backups = list((root / 'respaldos').iterdir()); assert len(backups) == 1
    assert all((backups[0] / name).read_bytes() == data for name, data in before.items())
    assert not (root / 'ventas_10_extra.txt').exists() and len(list(root.glob('*.txt'))) == 5
    assert not list(root.glob('*.csv'))
    products = {row.split(';')[0]: int(row.split(';')[2]) for row in (root / 'productos.txt').read_text(encoding='utf-8').splitlines()}
    assert len(products) == 4 and all(10000 <= x <= 2500000 for x in products.values())
    for row in (root / 'vendedores.txt').read_text(encoding='utf-8').splitlines():
        kind, doc, _, _ = row.split(';'); lines = (root / f'ventas_{doc}.txt').read_text(encoding='utf-8').splitlines()
        assert lines[0] == f'{kind};{doc}' and 2 <= len(lines) - 1 <= 5
        assert all(line.endswith(';') and line.split(';')[0] in products and 1 <= int(line.split(';')[1]) <= 10 for line in lines[1:])
    proc = execute(root); assert proc.returncode == 0 and 'ADVERTENCIA' not in proc.stdout
    passed('Generador independiente: coherencia, respaldo y sustitución completa', 'Generador')

# Este auxiliar se crea fuera del repositorio y se elimina al terminar la suite.
driver = r'''
import java.nio.file.*;
public class PruebaTemporal {
    private static void exige(boolean condicion) { if (!condicion) throw new AssertionError("Condición no cumplida"); }
    public static void main(String[] args) throws Exception {
        GestorVentas g = new GestorVentas(Path.of(".")); g.cargar();
        if (!args[0].equals("fallo_guardado")) g.exportar();
        switch (args[0]) {
            case "productos":
                g.agregarProducto("4", "Prueba", 123); exige(g.buscarProducto("4").precio == 123);
                g.modificarProducto("4", "Cambio", 456); exige(g.buscarProducto("4").nombre.equals("Cambio"));
                g.eliminarProducto("4"); exige(g.buscarProducto("4") == null); break;
            case "precio": g.modificarProducto("1", "Teclado", 150); break;
            case "vendedores":
                g.agregarVendedor("CE", "10", "Sara", "Díaz");
                g.modificarVendedor("CE", "10", "Sara", "López");
                exige(g.buscarVendedor("CE", "10").apellidos.equals("López"));
                g.eliminarVendedor("CE", "10"); exige(g.buscarVendedor("CE", "10") == null); break;
            case "venta_nueva": g.agregarVenta("CC", "30", "3", 2); break;
            case "venta_editar": g.modificarVenta(2, "2", 5); break;
            case "venta_eliminar": g.eliminarVenta(2); exige(Files.readString(Path.of("ventas_10_extra.txt")).equals("CC;10\n")); break;
            case "vendedor_vacio": g.eliminarVendedor("CC", "30"); exige(!Files.exists(Path.of("ventas_30.txt"))); break;
            case "referencias":
                try { g.eliminarProducto("1"); throw new AssertionError(); } catch (IllegalArgumentException e) { exige(e.getMessage().contains("ventas asociadas")); }
                try { g.eliminarVendedor("CC", "10"); throw new AssertionError(); } catch (IllegalArgumentException e) { exige(e.getMessage().contains("ventas asociadas")); }
                break;
            case "ultimo":
                try { g.eliminarProducto("1"); throw new AssertionError(); } catch (IllegalArgumentException e) { exige(e.getMessage().contains("al menos un")); }
                try { g.eliminarVendedor("CC", "10"); throw new AssertionError(); } catch (IllegalArgumentException e) { exige(e.getMessage().contains("al menos un")); }
                break;
            case "invalidos":
                try { g.agregarProducto("1", "Duplicado", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.agregarVendedor("CC", "10", "Otra", "Persona"); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.agregarProducto("4", " ", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.agregarProducto("4", "X;Y", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.agregarProducto("4", "Nombre\n", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.agregarProducto("4", "X", -1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { GestorVentas.entero("1.5"); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.agregarVenta("CC", "999", "1", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.modificarVenta(0, "999", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                break;
            case "overflow":
                try { g.agregarVenta("CC", "10", "1", Long.MAX_VALUE); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                try { g.modificarProducto("1", "Grande", Long.MAX_VALUE); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                break;
            case "corruptos":
                exige(!g.puedeEditar());
                try { g.agregarProducto("4", "Prueba", 1); throw new AssertionError(); } catch (IllegalArgumentException e) { }
                break;
            case "externos":
                Files.writeString(Path.of("productos.txt"), "4;Externo;10\n", StandardOpenOption.APPEND);
                try { g.agregarProducto("5", "Prueba", 1); throw new AssertionError(); } catch (java.io.IOException e) { exige(e.getMessage().contains("fuera de la aplicación")); }
                break;
            case "comillas": g.agregarProducto("4", "Monitor \"Pro\"", 200); break;
            case "fallo_guardado":
                try { g.agregarProducto("4", "Prueba", 123); throw new AssertionError(); }
                catch (java.io.IOException e) { exige(e.getMessage().contains("restauraron")); }
                exige(g.buscarProducto("4") == null); break;
            default: throw new AssertionError("Caso desconocido");
        }
        System.out.println("CRUD APROBADO");
    }
}
'''

with tempfile.TemporaryDirectory(prefix='cfp-driver-') as driver_dir:
    helper = Path(driver_dir); source = helper / 'PruebaTemporal.java'; source.write_text(driver, encoding='utf-8')
    subprocess.run([str(javac), '-encoding', 'UTF-8', '--release', '21', '-cp', str(classes), '-d', str(helper), str(source)], check=True)

    def crud(name, scenario, changes=None, expected=(expected_v, expected_p), unchanged=False, check=None):
        with tempfile.TemporaryDirectory(prefix='cfp-crud-') as td:
            root = Path(td); prepare(root, changes)
            before = {p.name: p.read_bytes() for p in root.glob('*.txt')}
            proc = execute(root, 'PruebaTemporal', (scenario,), helper)
            assert proc.returncode == 0 and 'CRUD APROBADO' in proc.stdout, (name, proc.stdout, proc.stderr)
            verify_outputs(root, expected)
            if unchanged:
                assert before == {p.name: p.read_bytes() for p in root.glob('*.txt')}, name
            if check:
                check(root)
            # Otro proceso relee los TXT: no se depende de objetos que quedaron en memoria.
            if scenario != 'externos':
                assert execute(root).returncode == 0
                verify_outputs(root, expected)
            passed(name, 'CRUD')

    crud('CRUD de productos y persistencia tras reiniciar', 'productos', unchanged=True)
    crud('Modificar precio recalcula ventas existentes', 'precio',
         expected=('Ana Pérez;500\nLuis Gómez;400\nEva Ruiz;0\n', 'Mouse;50\nTeclado;150\nMonitor;200\n'))
    crud('CRUD de vendedores y mismo número con distinto tipo', 'vendedores', unchanged=True)
    def verify_new_sale(root):
        assert (root/'ventas_ui_1.txt').read_text(encoding='utf-8') == 'CC;30\n3;2;\n'

    def verify_external(root):
        assert '5;Prueba' not in (root/'productos.txt').read_text(encoding='utf-8')

    crud('Crear venta para vendedor sin archivo y guardar cabecera', 'venta_nueva',
         expected=('Ana Pérez;400\nLuis Gómez;400\nEva Ruiz;400\n', expected_p),
         check=verify_new_sale)
    crud('Editar una venta conserva vendedor y archivo de origen', 'venta_editar',
         expected=('Ana Pérez;500\nLuis Gómez;400\nEva Ruiz;0\n', expected_p))
    crud('Eliminar última venta conserva archivo con cabecera', 'venta_eliminar',
         expected=('Luis Gómez;400\nAna Pérez;250\nEva Ruiz;0\n', expected_p))
    crud('Eliminar vendedor sin ventas retira su archivo vacío', 'vendedor_vacio', {'ventas_30.txt': 'CC;30\n'},
         expected=('Ana Pérez;400\nLuis Gómez;400\n', expected_p))
    crud('Borrados referenciados se rechazan sin cambiar datos', 'referencias', unchanged=True)
    crud('Último producto y vendedor quedan protegidos', 'ultimo',
         {'productos.txt': '1;Uno;10\n', 'vendedores.txt': 'CC;10;Ana;Pérez\n', 'ventas_10.txt': 'CC;10\n',
          'ventas_10_extra.txt': None, 'ventas_20.txt': None}, expected=('Ana Pérez;0\n', 'Uno;10\n'), unchanged=True)
    crud('CRUD rechaza duplicados, campos inválidos y referencias inexistentes', 'invalidos', unchanged=True)
    crud('CRUD con desbordamiento se rechaza sin modificar entradas', 'overflow', unchanged=True)
    crud('Datos corruptos permiten reportes pero bloquean CRUD', 'corruptos',
         {'ventas_10.txt': base['ventas_10.txt'] + '99;1;\n'}, unchanged=True)
    crud('Cambios externos requieren recarga y no se sobrescriben', 'externos', expected=None, check=verify_external)
    crud('Comillas en nombres se escapan en CSV', 'comillas',
         expected=(expected_v, expected_p + '"Monitor ""Pro""";200\n'))

    if os.name == 'nt':
        lock_api = ctypes.WinDLL('kernel32', use_last_error=True)
        lock_api.CreateFileW.argtypes = [ctypes.c_wchar_p, ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p,
                                        ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p]
        lock_api.CreateFileW.restype = ctypes.c_void_p
        lock_api.CloseHandle.argtypes = [ctypes.c_void_p]
        with tempfile.TemporaryDirectory(prefix='cfp-crud-rollback-') as td:
            root = Path(td); prepare(root); previous_reports(root)
            before = {p.name: p.read_bytes() for p in root.iterdir() if p.is_file()}
            handle = lock_api.CreateFileW(str(root/'reporte_productos.csv'), 0x80000000, 3, None, 3, 0x80, None)
            assert handle not in (None, ctypes.c_void_p(-1).value)
            try:
                proc = execute(root, 'PruebaTemporal', ('fallo_guardado',), helper)
                assert proc.returncode == 0, proc.stderr
                assert before == {p.name: p.read_bytes() for p in root.iterdir() if p.is_file()}
            finally:
                lock_api.CloseHandle(handle)
            passed('Fallo de CSV durante CRUD restaura TXT y reportes y conserva el modelo', 'Escritura')

if os.name == 'nt':
    # Se bloquea solo la sustitución del segundo CSV; su lectura y respaldo siguen permitidos.
    kernel = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel.CreateFileW.argtypes = [ctypes.c_wchar_p, ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p,
                                  ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p]
    kernel.CreateFileW.restype = ctypes.c_void_p
    kernel.CloseHandle.argtypes = [ctypes.c_void_p]
    with tempfile.TemporaryDirectory(prefix='cfp-rollback-') as td:
        root = Path(td); prepare(root); previous_reports(root)
        handle = kernel.CreateFileW(str(root/'reporte_productos.csv'), 0x80000000, 3, None, 3, 0x80, None)
        assert handle not in (None, ctypes.c_void_p(-1).value), ctypes.get_last_error()
        try:
            proc = execute(root)
            assert proc.returncode == 1 and 'restauraron' in proc.stderr, proc.stderr
            verify_outputs(root, ('ANTERIOR\n', 'ANTERIOR\n'))
        finally:
            kernel.CloseHandle(handle)
        passed('Fallo al sustituir segundo CSV restaura el primero', 'Escritura')

entrypoints = []
for source in (repo/'src').glob('*.java'):
    if re.search(r'public\s+static\s+void\s+main\s*\(', source.read_text(encoding='utf-8')):
        entrypoints.append(source.stem)
assert sorted(entrypoints) == ['GenerateInfoFiles', 'main'], entrypoints
signatures = subprocess.run([str(java_home/'bin/javap.exe'), '-cp', str(classes), 'GenerateInfoFiles'],
                            capture_output=True, encoding='utf-8', check=True).stdout
assert all(x in signatures for x in ['createProductsFile(int)', 'createSalesManInfoFile(int)',
                                     'createSalesMenFile(int, java.lang.String, long)'])
passed('Dos clases main con nombres exactos y tres firmas obligatorias', 'Estructura')

if args.evidence_dir:
    evidence = args.evidence_dir.resolve()
    if evidence == repo or repo in evidence.parents:
        parser.error('Las evidencias deben guardarse fuera del repositorio')
    evidence.mkdir(parents=True, exist_ok=True)
    (evidence/'salida_caso_manual.txt').write_text(log, encoding='utf-8')
    (evidence/'resultados_pruebas.json').write_text(json.dumps({'java': version, 'casos': results}, ensure_ascii=False, indent=2), encoding='utf-8')
print(version)
print(str(len(results)) + ' escenarios APROBADOS')
