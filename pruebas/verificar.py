"""Pruebas de integración: ejecutan Java en carpetas temporales sin cambiar los datos raíz."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--java-home', required=True)
parser.add_argument('--evidence-dir', type=Path,
                    help='Carpeta externa al repositorio para guardar resultados')
args = parser.parse_args()
repo = Path(__file__).resolve().parents[1]
if args.evidence_dir is not None:
 evidence = args.evidence_dir.resolve()
 if evidence == repo or repo in evidence.parents:
  parser.error('--evidence-dir debe estar fuera del repositorio')
java = Path(args.java_home).resolve() / 'bin' / 'java.exe'
javac = java.with_name('javac.exe')
classes = repo / 'build' / 'classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(javac), '-encoding', 'UTF-8', '--release', '21', '-Xlint:all', '-d', str(classes), *map(str, (repo/'src').glob('*.java'))], check=True)
version = subprocess.run([str(java), '-version'], capture_output=True, text=True).stderr.strip()
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

def run_case(name, changes=None, keys='4\n5\n', expected=(expected_v,expected_p), contains=(), mode='Main', pre=None):
 with tempfile.TemporaryDirectory(prefix='cfp-') as td:
  root = Path(td)
  files = dict(base)
  files.update(changes or {})
  for key,value in files.items():
   if value is not None: (root/key).write_text(value,encoding='utf-8')
  if pre: pre(root)
  proc = subprocess.run([str(java), '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-cp', str(classes), mode],cwd=root,input=keys,encoding='utf-8',capture_output=True,timeout=15)
  assert proc.returncode == 0, (name,proc.stderr)
  for part in contains: assert part in proc.stdout, (name,part,proc.stdout)
  if expected is not None:
   for fname,want in zip(['reporte_vendedores.csv','reporte_productos.csv'],expected):
    assert (root/fname).read_text(encoding='utf-8') == want, name
  results.append({'caso':name,'resultado':'APROBADO'})
  return proc.stdout

log=run_case('Cálculo manual, múltiples archivos, empate y registros sin ventas',contains=['Unidades: 12'])
run_case('Repetir cálculo y exportación no duplica acumulados',keys='3\n4\n3\n4\n5\n')
run_case('Menú inválido, consulta y exportación sin cálculo previo',keys='hola\n9\n\n2\n4\n5\n',contains=['Opción inválida','ARCHIVO: productos.txt'])
run_case('Datos corruptos se omiten y filas válidas se conservan',{'ventas_10.txt':base['ventas_10.txt']+'99;1;\n1;-2;\n1;2.5;\n1;dos;\n1;1\n1;;\n\n'},contains=['línea 4','Producto inexistente','ADVERTENCIA'])
run_case('Catálogos duplicados y precios inválidos',{'productos.txt':base['productos.txt']+'1;Otro;999\n4;X;-1\n5;X;1.5\n6;;1\n','vendedores.txt':base['vendedores.txt']+'CC;10;Otra;Persona\n'},contains=['duplicado'])
run_case('Cabecera inválida se omite',{'ventas_mala.txt':'CC;999\n1;100;\n'},contains=['Vendedor inexistente'])
run_case('Cabecera ausente se omite',{'ventas_vacia.txt':''},contains=['Falta la cabecera'])
run_case('Archivo con solo cabecera y cantidad cero',{'ventas_30.txt':'CC;30\n1;0;\n'})
run_case('Tipos de documento distintos con mismo número',{'vendedores.txt':base['vendedores.txt']+'CE;10;Sara;Díaz\n','ventas_ce.txt':'CE;10\n1;1;\n'},expected=('Ana Pérez;400\nLuis Gómez;400\nSara Díaz;100\nEva Ruiz;0\n',expected_p))
run_case('Desbordamiento de multiplicación no altera los totales',{'ventas_10.txt':base['ventas_10.txt']+'1;9223372036854775807;\n'},contains=['importe de la venta excede'])
run_case('Desbordamiento de recaudo no actualiza unidades',{'productos.txt':'1;Grande;9223372036854775807\n2;Uno;1\n','ventas_10.txt':'CC;10\n1;1;\n2;1;\n','ventas_10_extra.txt':None,'ventas_20.txt':'CC;20\n'},expected=('Ana Pérez;9223372036854775807\nLuis Gómez;0\nEva Ruiz;0\n','Grande;9223372036854775807\nUno;1\n'),contains=['acumulado excede','Uno;1 | Unidades: 0'])
run_case('Desbordamiento de unidades no altera recaudo',{'productos.txt':'1;Gratis;0\n','ventas_10.txt':'CC;10\n1;9223372036854775807;\n1;1;\n','ventas_10_extra.txt':None,'ventas_20.txt':'CC;20\n'},expected=('Ana Pérez;0\nLuis Gómez;0\nEva Ruiz;0\n','Gratis;0\n'),contains=['acumulado excede'])
for name,change,msg in [
 ('Catálogo ausente',{'productos.txt':None},'ERROR:'),
 ('Catálogo vacío',{'productos.txt':''},'no contiene productos válidos'),
 ('Vendedores sin registros válidos',{'vendedores.txt':'fila mala\n'},'no contiene vendedores válidos'),
 ('Sin archivos de ventas',{k:None for k in base if k.startswith('ventas_')},'No se encontraron'),
 ('Todas las cabeceras inválidas',{k:'CC;999\n' for k in base if k.startswith('ventas_')},'Ningún archivo'),
]:
 run_case(name,change,expected=('ANTERIOR\n','ANTERIOR\n'),contains=[msg,'no son resultados actuales'],pre=lambda r:[(r/f).write_text('ANTERIOR\n',encoding='utf-8') for f in ['reporte_vendedores.csv','reporte_productos.csv']])
run_case('Error de escritura conserva el menú',expected=None,contains=['ERROR:','Programa finalizado'],pre=lambda r:(r/'reporte_productos.csv').mkdir())
run_case('Cancelar generación protege entradas',keys='1\nNO\n4\n5\n',contains=['Generación cancelada'])
run_case('Fin de entrada cierra el menú',keys='',expected=None,contains=['Programa finalizado'])
run_case('Fin de entrada cancela sobrescritura',keys='1\n',expected=None,contains=['Generación cancelada'])
# Generador independiente en carpeta vacía, y confirmación negativa/positiva.
with tempfile.TemporaryDirectory(prefix='cfp-generador-') as td:
 root=Path(td)
 def generate(keys):
  p=subprocess.run([str(java),'-Dstdout.encoding=UTF-8','-cp',str(classes),'GenerateInfoFiles'],cwd=root,input=keys,encoding='utf-8',capture_output=True,timeout=15)
  assert p.returncode==0,p.stderr
  return p.stdout
 assert 'exitosamente' in generate('')
 before={p.name:p.read_bytes() for p in root.glob('*.txt')}
 assert len(before)==5
 assert 'cancelada' in generate('NO\n')
 assert before=={p.name:p.read_bytes() for p in root.glob('*.txt')}
 assert 'exitosamente' in generate('SI\n')
 p=subprocess.run([str(java),'-Dstdout.encoding=UTF-8','-cp',str(classes),'Main'],cwd=root,input='4\n5\n',encoding='utf-8',capture_output=True,timeout=15)
 assert 'ADVERTENCIA' not in p.stdout and 'Exportación completada' in p.stdout,p.stdout
 products={x.split(';')[0]:int(x.split(';')[2]) for x in (root/'productos.txt').read_text(encoding='utf-8').splitlines()}
 assert len(products)==4 and all(10000<=v<=2500000 for v in products.values())
 vendors=(root/'vendedores.txt').read_text(encoding='utf-8').splitlines()
 assert len(vendors)==3
 for row in vendors:
  t,ident,n,a=row.split(';'); lines=(root/f'ventas_{ident}.txt').read_text(encoding='utf-8').splitlines()
  assert lines[0]==f'{t};{ident}' and 2<=len(lines)-1<=5
  assert all(x.split(';')[0] in products and 1<=int(x.split(';')[1])<=10 for x in lines[1:])
 results.append({'caso':'Generador independiente, confirmación y coherencia','resultado':'APROBADO'})
if args.evidence_dir is not None:
 evidence.mkdir(parents=True, exist_ok=True)
 (evidence/'salida_caso_manual.txt').write_text(log, encoding='utf-8')
 (evidence/'resultados_pruebas.json').write_text(
  json.dumps({'java':version,'casos':results},ensure_ascii=False,indent=2),
  encoding='utf-8')
print(version)
print(str(len(results))+' escenarios APROBADOS')
