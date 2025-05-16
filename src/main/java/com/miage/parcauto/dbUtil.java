package main.java.com.miage.parcauto;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class dbUtil {

    private static final Logger LOGGER = Logger.getLogger(dbUtil.class.getName());
    private static final Properties dbProperties = new Properties();
    private static final String DB_PROPERTIES_FILE = "db.properties";

    static {
        try (InputStream input = dbUtil.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE)) {
            if (input == null) {
                LOGGER.log(Level.SEVERE, "Impossible de trouver le fichier " + DB_PROPERTIES_FILE + " dans le classpath (src/main/resources).");
                throw new IOException("Fichier de configuration " + DB_PROPERTIES_FILE + " non trouvé.");
            }
            dbProperties.load(input);

            String driver = dbProperties.getProperty("db.driver");
            if (driver == null || driver.trim().isEmpty()) {
                LOGGER.log(Level.SEVERE, "La propriété 'db.driver' est manquante dans " + DB_PROPERTIES_FILE);
                throw new RuntimeException("Configuration du driver JDBC manquante.");
            }
            Class.forName(driver);
            LOGGER.info("Driver JDBC MySQL chargé avec succès: " + driver);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement du fichier " + DB_PROPERTIES_FILE + ": " + e.getMessage(), e);
            throw new RuntimeException("Impossible de charger la configuration de la base de données.", e);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver JDBC MySQL non trouvé: " + e.getMessage(), e);
            throw new RuntimeException("Driver JDBC MySQL non trouvé.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inattendue lors de l'initialisation de DbUtil: " + e.getMessage(), e);
            throw new RuntimeException("Erreur d'initialisation de DbUtil.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = dbProperties.getProperty("db.url");
        String user = dbProperties.getProperty("db.username");
        String pass = dbProperties.getProperty("db.password");

        if (url == null || url.trim().isEmpty() ||
                user == null || user.trim().isEmpty() ||
                pass == null) { // Le mot de passe peut être vide, mais la clé doit exister
            LOGGER.log(Level.SEVERE, "Les propriétés de connexion (db.url, db.username, db.password) sont manquantes ou incomplètes dans " + DB_PROPERTIES_FILE);
            throw new SQLException("Configuration de la base de données incomplète.");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    public static void close(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion JDBC: " + e.getMessage(), e);
            }
        }
    }

    public static void close(Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du Statement JDBC: " + e.getMessage(), e);
            }
        }
    }

    public static void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                if (!resultSet.isClosed()) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du ResultSet JDBC: " + e.getMessage(), e);
            }
        }
    }

    public static void closeQuietly(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null && !rs.isClosed()) rs.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur silencieuse lors de la fermeture du ResultSet: " + e.getMessage());
        }
        try {
            if (stmt != null && !stmt.isClosed()) stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur silencieuse lors de la fermeture du Statement: " + e.getMessage());
        }
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur silencieuse lors de la fermeture de la Connexion: " + e.getMessage());
        }
    }

    public static void closeQuietly(Connection conn, Statement stmt) {
        closeQuietly(conn, stmt, null);
    }
}