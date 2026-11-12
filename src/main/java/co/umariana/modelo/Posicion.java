package co.umariana.modelo;

public final class Posicion {

    public static final String SUJETO = "1";
    public static final int MAX_AHNENTAFEL = 2047;
    public static final int MAX_ORDEN = 9999;
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    public static boolean esSujeto(String posicion) {
        return SUJETO.equals(posicion);
    }

    public static boolean esAhnentafel(String posicion) {
        if (posicion == null || !posicion.matches("[1-9][0-9]{0,3}")) {
            return false;
        }
        return Integer.parseInt(posicion) <= MAX_AHNENTAFEL;
    }

    public static boolean esDAboville(String posicion) {
        return posicion != null
                && posicion.length() <= MAX_LONGITUD
                && posicion.matches("1(\\.[1-9][0-9]{0,3})+");
    }

    public static boolean esValida(String posicion) {
        return esAhnentafel(posicion) || esDAboville(posicion);
    }

    public static boolean esAscendiente(String posicion) {
        return esAhnentafel(posicion) && !esSujeto(posicion);
    }

    public static boolean esDescendiente(String posicion) {
        return esDAboville(posicion);
    }

    public static String posicionDelPadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("Posición Ahnentafel inválida: " + ahnentafel);
        }
        return String.valueOf(Integer.parseInt(ahnentafel) * 2);
    }

    public static String posicionDeLaMadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("Posición Ahnentafel inválida: " + ahnentafel);
        }
        return String.valueOf(Integer.parseInt(ahnentafel) * 2 + 1);
    }

    public static String posicionDelHijoEnLinea(String ahnentafel) {
        if (!esAscendiente(ahnentafel)) {
            throw new IllegalArgumentException("No es un ascendiente: " + ahnentafel);
        }
        return String.valueOf(Integer.parseInt(ahnentafel) / 2);
    }

    public static String posicionDelProgenitor(String daboville) {
        if (!esDAboville(daboville)) {
            throw new IllegalArgumentException("Posición d'Aboville inválida: " + daboville);
        }
        return daboville.substring(0, daboville.lastIndexOf('.'));
    }

    public static int orden(String daboville) {
        if (!esDAboville(daboville)) {
            throw new IllegalArgumentException("Posición d'Aboville inválida: " + daboville);
        }
        return Integer.parseInt(daboville.substring(daboville.lastIndexOf('.') + 1));
    }

    public static int generacion(String posicion) {
        if (esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            int generaciones = 0;
            while (n > 1) {
                n /= 2;
                generaciones++;
            }
            return generaciones;
        }
        if (esDAboville(posicion)) {
            return posicion.split("\\.").length - 1;
        }
        throw new IllegalArgumentException("Posición inválida: " + posicion);
    }
}