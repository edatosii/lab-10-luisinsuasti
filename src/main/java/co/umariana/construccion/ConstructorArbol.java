package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class Registro {
        final int linea;
        final Persona persona;

        Registro(int linea, Persona persona) {
            this.linea = linea;
            this.persona = persona;
        }
    }

    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos la línea del sujeto (posición 1).");
        }

        List<Registro> registros = leerRegistros(texto);

        boolean haySujeto = false;
        for (Registro r : registros) {
            if (Posicion.esSujeto(r.persona.getPosicion())) {
                haySujeto = true;
            }
        }
        if (!haySujeto) {
            throw new ArbolInvalidoException("Falta el sujeto: debe existir una persona en la posición 1.");
        }

        registros.sort(Comparator.comparingInt(ConstructorArbol::grupo)
                .thenComparingInt(ConstructorArbol::clave));

        ArbolGenealogico arbol = null;
        for (Registro r : registros) {
            try {
                if (arbol == null) {
                    arbol = new ArbolGenealogico(r.persona);
                } else {
                    arbol.agregar(r.persona);
                }
            } catch (IllegalArgumentException e) {
                throw conLinea(r.linea, e.getMessage());
            }
        }
        return arbol;
    }

    private static List<Registro> leerRegistros(String texto) {
        List<Registro> registros = new ArrayList<>();
        String[] lineas = texto.split("\r?\n", -1);

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            if (linea.isBlank()) {
                continue;
            }
            int numero = i + 1;

            String[] campos = linea.split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw conLinea(numero, "debe tener 3 o 4 campos separados por ';' (posicion;nombre;nacimiento;fallecimiento).");
            }
            for (int c = 0; c < campos.length; c++) {
                campos[c] = campos[c].trim();
            }

            int nacimiento = leerAnio(campos[2], "nacimiento", numero);
            Integer fallecimiento = null;
            if (campos.length == 4 && !campos[3].isEmpty()) {
                fallecimiento = leerAnio(campos[3], "fallecimiento", numero);
            }

            Persona persona = new Persona(campos[0], campos[1], nacimiento, fallecimiento);
            try {
                persona.validar();
            } catch (IllegalArgumentException e) {
                throw conLinea(numero, e.getMessage());
            }

            for (Registro previo : registros) {
                if (previo.persona.getPosicion().equals(persona.getPosicion())) {
                    throw conLinea(numero, "la posición " + persona.getPosicion() + " está repetida.");
                }
            }
            registros.add(new Registro(numero, persona));
        }
        return registros;
    }

    private static int leerAnio(String valor, String campo, int numeroLinea) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw conLinea(numeroLinea, "el año de " + campo + " debe ser un número entero.");
        }
    }

    private static int grupo(Registro r) {
        String posicion = r.persona.getPosicion();
        if (Posicion.esSujeto(posicion)) {
            return 0;
        }
        return Posicion.esAscendiente(posicion) ? 1 : 2;
    }

    private static int clave(Registro r) {
        String posicion = r.persona.getPosicion();
        if (Posicion.esAscendiente(posicion)) {
            return Integer.parseInt(posicion);
        }
        return Posicion.esDescendiente(posicion) ? Posicion.generacion(posicion) : 0;
    }

    private static ArbolInvalidoException conLinea(int numero, String mensaje) {
        return new ArbolInvalidoException("Línea " + numero + ": " + mensaje);
    }
}