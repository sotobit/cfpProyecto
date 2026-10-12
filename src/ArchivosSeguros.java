import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/** Prepara archivos completos y respalda los originales antes de sustituirlos. */
public class ArchivosSeguros {
    private ArchivosSeguros() { }

    /**
     * Restaura las sustituciones realizadas si ocurre un error de escritura.
     * @param carpeta directorio de datos, sin rutas de archivos externas
     * @param nombres archivos por escribir, en el mismo orden que contenidos
     * @param contenidos texto completo de cada archivo
     * @param eliminar archivos administrados que se retiran después de escribir
     * @return carpeta que conserva los originales
     * @throws IOException si no se completa la preparación o sustitución
     */
    public static Path guardar(Path carpeta, ArrayList<String> nombres,
            ArrayList<String> contenidos, ArrayList<String> eliminar) throws IOException {
        if (nombres.size() != contenidos.size()) {
            throw new IllegalArgumentException("Cada archivo debe tener un contenido.");
        }
        carpeta = carpeta.toAbsolutePath().normalize();
        ArrayList<String> destinos = new ArrayList<>(nombres);
        for (String nombre : eliminar) {
            if (!destinos.contains(nombre)) {
                destinos.add(nombre);
            }
        }
        for (String nombre : destinos) {
            validarDestino(carpeta, nombre);
        }
        Path temporal = Files.createTempDirectory(carpeta, ".cfp-temporal-");
        Path respaldo = null;
        ArrayList<Boolean> existian = new ArrayList<>();
        ArrayList<Integer> cambiados = new ArrayList<>();
        try {
            for (int i = 0; i < nombres.size(); i++) {
                Files.writeString(temporal.resolve(nombres.get(i)), contenidos.get(i), StandardCharsets.UTF_8);
            }
            Path baseRespaldos = carpeta.resolve("respaldos");
            if (Files.isSymbolicLink(baseRespaldos)) {
                throw new IOException("La carpeta de respaldos no puede ser un enlace.");
            }
            Files.createDirectories(baseRespaldos);
            respaldo = Files.createTempDirectory(baseRespaldos, "cambio_");
            for (String nombre : destinos) {
                Path original = carpeta.resolve(nombre);
                boolean existe = Files.exists(original, LinkOption.NOFOLLOW_LINKS);
                existian.add(existe);
                if (existe) {
                    Files.copy(original, respaldo.resolve(nombre));
                }
            }
            for (int i = 0; i < destinos.size(); i++) {
                Path destino = carpeta.resolve(destinos.get(i));
                if (i < nombres.size()) {
                    Files.move(temporal.resolve(destinos.get(i)), destino, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.deleteIfExists(destino);
                }
                cambiados.add(i);
            }
            return respaldo;
        } catch (IOException error) {
            boolean restaurado = true;
            for (int j = cambiados.size() - 1; j >= 0; j--) {
                int i = cambiados.get(j);
                Path destino = carpeta.resolve(destinos.get(i));
                try {
                    if (existian.get(i)) {
                        Files.copy(respaldo.resolve(destinos.get(i)), destino, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(destino);
                    }
                } catch (IOException falloRestauracion) {
                    restaurado = false;
                    error.addSuppressed(falloRestauracion);
                }
            }
            String mensaje = restaurado ? "Los archivos anteriores se conservaron o restauraron."
                    : "Revise el respaldo para recuperar los archivos: " + respaldo;
            throw new IOException("No se pudo completar la escritura. " + mensaje + " Causa: " + error.getMessage(), error);
        } finally {
            // La carpeta temporal solo contiene los nombres verificados por este método.
            for (String nombre : nombres) {
                try { Files.deleteIfExists(temporal.resolve(nombre)); } catch (IOException ignorado) { }
            }
            try { Files.deleteIfExists(temporal); } catch (IOException ignorado) { }
        }
    }

    private static void validarDestino(Path carpeta, String nombre) throws IOException {
        boolean permitido = nombre.equals("productos.txt") || nombre.equals("vendedores.txt")
                || nombre.equals("reporte_vendedores.csv") || nombre.equals("reporte_productos.csv")
                || (nombre.startsWith("ventas_") && nombre.endsWith(".txt"));
        Path destino = carpeta.resolve(nombre).normalize();
        if (!permitido || !destino.getParent().equals(carpeta)
                || nombre.contains("/") || nombre.contains("\\")) {
            throw new IOException("Nombre de archivo no permitido: " + nombre);
        }
        if (Files.exists(destino, LinkOption.NOFOLLOW_LINKS)
                && (!Files.isRegularFile(destino, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(destino))) {
            throw new IOException("El destino debe ser un archivo regular: " + nombre);
        }
    }
}
