package com.example.fleetIq;
import java.io.*;
import java.util.*;

public class CombinadorArchivos {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== COMBINADOR DE ARCHIVOS (INCLUYE SUBCARPETAS) ===");
        System.out.print("Ingrese la ruta de la carpeta: ");
        String rutaCarpeta = scanner.nextLine();

        System.out.print("Ingrese el nombre del archivo de salida (sin extensión): ");
        String nombreSalida = scanner.nextLine();

        try {
            combinarArchivosRecursivo(rutaCarpeta, nombreSalida + ".txt");
            System.out.println("¡Proceso completado con éxito!");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    public static void combinarArchivosRecursivo(String rutaCarpeta, String nombreArchivoSalida) throws IOException {
        File carpeta = new File(rutaCarpeta);

        // Verificar que la carpeta existe
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            throw new IOException("La carpeta no existe o no es válida: " + rutaCarpeta);
        }

        // Obtener lista de todos los archivos (recursivamente)
        List<File> todosArchivos = new ArrayList<>();
        listarArchivosRecursivamente(carpeta, todosArchivos);

        if (todosArchivos.isEmpty()) {
            throw new IOException("No se encontraron archivos en la carpeta y sus subcarpetas");
        }

        // Ordenar archivos por ruta completa
        todosArchivos.sort(Comparator.comparing(File::getAbsolutePath));

        // Crear archivo de salida
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivoSalida))) {

            for (File archivo : todosArchivos) {
                if (!archivo.getName().equals(nombreArchivoSalida)) {
                    escribirArchivoConRuta(archivo, carpeta, writer);
                }
            }
        }
    }

    private static void listarArchivosRecursivamente(File carpeta, List<File> listaArchivos) {
        File[] elementos = carpeta.listFiles();

        if (elementos != null) {
            for (File elemento : elementos) {
                if (elemento.isFile()) {
                    listaArchivos.add(elemento);
                } else if (elemento.isDirectory()) {
                    listarArchivosRecursivamente(elemento, listaArchivos);
                }
            }
        }
    }

    private static void escribirArchivoConRuta(File archivo, File carpetaRaiz, BufferedWriter writer) throws IOException {
        // Obtener ruta relativa para mostrar la estructura de carpetas
        String rutaRelativa = carpetaRaiz.toPath().relativize(archivo.toPath()).toString();

        // Escribir encabezado con la ruta del archivo
        writer.write("=== ARCHIVO: " + rutaRelativa + " ===");
        writer.newLine();
        writer.newLine();

        // Leer y escribir contenido del archivo
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                writer.write(linea);
                writer.newLine();
            }
        } catch (IOException e) {
            writer.write("ERROR: No se pudo leer este archivo - " + e.getMessage());
            writer.newLine();
        }

        // Escribir separador entre archivos
        writer.newLine();
        writer.write("=".repeat(60));
        writer.newLine();
        writer.newLine();
    }
}