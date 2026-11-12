package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

public class ArbolGenealogico {

    private static class Nodo {
        Persona persona;
        Nodo padre;
        Nodo madre;
        List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int cantidad;

    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto es obligatorio");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1");
        }
        this.raiz = new Nodo(sujeto);
        this.cantidad = 1;
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return cantidad;
    }

    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    private Nodo buscarNodo(String posicion) {
        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            Nodo actual = raiz;
            for (int bit = 30 - Integer.numberOfLeadingZeros(n); bit >= 0 && actual != null; bit--) {
                actual = ((n >> bit) & 1) == 0 ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDAboville(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                actual = hijoConOrden(actual, Integer.parseInt(partes[i]));
            }
            return actual;
        }
        return null;
    }

    private Nodo hijoConOrden(Nodo nodo, int orden) {
        for (Nodo hijo : nodo.hijos) {
            if (Posicion.orden(hijo.persona.getPosicion()) == orden) {
                return hijo;
            }
        }
        return null;
    }

    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona es obligatoria");
        }
        persona.validar();
        String posicion = persona.getPosicion();

        if (Posicion.esSujeto(posicion) || buscarNodo(posicion) != null) {
            throw new ArbolInvalidoException("La posición " + posicion + " ya existe");
        }

        if (Posicion.esAscendiente(posicion)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + " porque no existe la posición " + posHijo);
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion
                        + " debe haber nacido antes que la posición " + posHijo);
            }
            Nodo nuevo = new Nodo(persona);
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            String posProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo progenitor = buscarNodo(posProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + " porque no existe la posición " + posProgenitor);
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion
                        + " debe haber nacido después que la posición " + posProgenitor);
            }
            Nodo nuevo = new Nodo(persona);
            int orden = Posicion.orden(posicion);
            int indice = 0;
            while (indice < progenitor.hijos.size()
                    && Posicion.orden(progenitor.hijos.get(indice).persona.getPosicion()) < orden) {
                indice++;
            }
            progenitor.hijos.add(indice, nuevo);
        }
        cantidad++;
        return persona;
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        verificarAscendiente(posicion);
        String nueva = Posicion.posicionDelPadre(posicion);
        return agregarAscendiente(nueva, nombre, nacimiento, fallecimiento);
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        verificarAscendiente(posicion);
        String nueva = Posicion.posicionDeLaMadre(posicion);
        return agregarAscendiente(nueva, nombre, nacimiento, fallecimiento);
    }

    private void verificarAscendiente(String posicion) {
        if (buscarNodo(posicion) == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes");
        }
    }

    private Persona agregarAscendiente(String nueva, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esAhnentafel(nueva)) {
            throw new ArbolInvalidoException("No se pueden agregar más de 10 generaciones de ascendientes");
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes");
        }
        int siguiente = 1;
        if (!nodo.hijos.isEmpty()) {
            Nodo ultimo = nodo.hijos.get(nodo.hijos.size() - 1);
            siguiente = Posicion.orden(ultimo.persona.getPosicion()) + 1;
        }
        String nueva = posicion + "." + siguiente;
        if (!Posicion.esDAboville(nueva)) {
            throw new ArbolInvalidoException("No se puede agregar otro hijo a la posición " + posicion);
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto no se puede eliminar");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe");
        }
        int eliminados;
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (hijo.padre == nodo) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
            eliminados = contarAscendentes(nodo);
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
            eliminados = contarDescendentes(nodo);
        }
        cantidad -= eliminados;
        return eliminados;
    }

    private int contarAscendentes(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + contarAscendentes(nodo.padre) + contarAscendentes(nodo.madre);
    }

    private int contarDescendentes(Nodo nodo) {
        int total = 1;
        for (Nodo hijo : nodo.hijos) {
            total += contarDescendentes(hijo);
        }
        return total;
    }

    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    public List<Persona> ascendientes() {
        List<Persona> lista = new ArrayList<>();
        List<Nodo> cola = new ArrayList<>();
        cola.add(raiz);
        for (int i = 0; i < cola.size(); i++) {
            Nodo actual = cola.get(i);
            if (actual.padre != null) {
                cola.add(actual.padre);
                lista.add(actual.padre.persona);
            }
            if (actual.madre != null) {
                cola.add(actual.madre);
                lista.add(actual.madre.persona);
            }
        }
        return lista;
    }

    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        recorrerDescendientes(raiz, lista);
        return lista;
    }

    private void recorrerDescendientes(Nodo nodo, List<Persona> lista) {
        for (Nodo hijo : nodo.hijos) {
            lista.add(hijo.persona);
            recorrerDescendientes(hijo, lista);
        }
    }
}