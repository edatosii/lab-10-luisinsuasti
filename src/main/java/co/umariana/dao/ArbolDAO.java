package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArbolDAO {

    private static final int MAX_NOMBRE = 100;

    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.isBlank() || nombre.length() > MAX_NOMBRE || arbol == null) {
            return -1;
        }
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO arboles (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.executeUpdate();
                    try (ResultSet claves = ps.getGeneratedKeys()) {
                        if (!claves.next()) {
                            throw new SQLException("No se obtuvo el id del árbol");
                        }
                        id = claves.getLong(1);
                    }
                }
                insertarMiembros(con, id, arbol);
                con.commit();
                return id;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            informar(e);
            return -1;
        }
    }

    public List<ResumenArbol> listar() {
        List<ResumenArbol> resumenes = new ArrayList<>();
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total_miembros "
                + "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id "
                + "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp creado = rs.getTimestamp("creado");
                LocalDateTime fecha = creado == null ? null : creado.toLocalDateTime();
                resumenes.add(new ResumenArbol(rs.getLong("id"), rs.getString("nombre"),
                        rs.getInt("total_miembros"), fecha));
            }
        } catch (SQLException e) {
            informar(e);
            return new ArrayList<>();
        }
        return resumenes;
    }

    public ArbolGenealogico cargar(long id) {
        try (Connection con = ConexionBD.conectar()) {
            if (!existe(con, id)) {
                return null;
            }

            List<Persona> personas = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros WHERE arbol_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int fallecimiento = rs.getInt("fallecimiento");
                        Integer anioFallecimiento = rs.wasNull() ? null : fallecimiento;
                        personas.add(new Persona(rs.getString("posicion"), rs.getString("nombre"),
                                rs.getInt("nacimiento"), anioFallecimiento));
                    }
                }
            }

            personas.sort(Comparator.comparingInt(ArbolDAO::grupo).thenComparingInt(ArbolDAO::clave));
            if (personas.isEmpty() || !Posicion.esSujeto(personas.get(0).getPosicion())) {
                return null;
            }

            ArbolGenealogico arbol = new ArbolGenealogico(personas.get(0));
            for (int i = 1; i < personas.size(); i++) {
                arbol.agregar(personas.get(i));
            }
            return arbol;
        } catch (SQLException | IllegalArgumentException e) {
            informar(e);
            return null;
        }
    }

    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                if (!existe(con, id)) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                insertarMiembros(con, id, arbol);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            informar(e);
            return false;
        }
    }

    public boolean eliminar(long id) {
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                int filas;
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM arboles WHERE id = ?")) {
                    ps.setLong(1, id);
                    filas = ps.executeUpdate();
                }
                con.commit();
                return filas > 0;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            informar(e);
            return false;
        }
    }

    private boolean existe(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM arboles WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertarMiembros(Connection con, long arbolId, ArbolGenealogico arbol) throws SQLException {
        String sql = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Persona p : arbol.miembros()) {
                ps.setLong(1, arbolId);
                ps.setString(2, p.getPosicion());
                ps.setString(3, p.getNombre());
                ps.setInt(4, p.getNacimiento());
                if (p.getFallecimiento() == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, p.getFallecimiento());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static int grupo(Persona p) {
        if (Posicion.esSujeto(p.getPosicion())) {
            return 0;
        }
        return Posicion.esAscendiente(p.getPosicion()) ? 1 : 2;
    }

    private static int clave(Persona p) {
        if (Posicion.esAscendiente(p.getPosicion())) {
            return Integer.parseInt(p.getPosicion());
        }
        return Posicion.esDescendiente(p.getPosicion()) ? Posicion.generacion(p.getPosicion()) : 0;
    }

    private void informar(Exception e) {
        System.err.println("ArbolDAO: " + e.getMessage());
    }
}