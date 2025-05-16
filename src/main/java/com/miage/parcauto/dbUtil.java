package main.java.com.miage.parcauto;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class dbUtil {
    private static final Properties properties = new Properties();
    // Le fichier est maintenant attendu dans le même package que dbUtil.class
    private static final String DB_PROPERTIES_FILE_NAME = "db.properties";
    private static final Logger DB_UTIL_LOGGER = Logger.getLogger(dbUtil.class.getName());

    static {
        // Utilisation d'un chemin relatif au package de la classe dbUtil
        try (InputStream input = dbUtil.class.getResourceAsStream(DB_PROPERTIES_FILE_NAME)) {
            if (input == null) {
                DB_UTIL_LOGGER.log(Level.SEVERE, "Impossible de trouver le fichier '" + DB_PROPERTIES_FILE_NAME + "' dans le package 'main.java.com.miage.parcauto'.");
                DB_UTIL_LOGGER.log(Level.SEVERE, "Vérifiez que '" + DB_PROPERTIES_FILE_NAME + "' est présent dans 'src/main/java/com/miage/parcauto/' et que les fichiers .properties sont copiés dans le classpath lors de la compilation.");
                throw new IOException("Fichier de configuration '" + DB_PROPERTIES_FILE_NAME + "' non trouvé dans le package de la classe dbUtil.");
            }
            properties.load(input);
            DB_UTIL_LOGGER.info("Fichier de configuration de la base de données '" + DB_PROPERTIES_FILE_NAME + "' chargé avec succès depuis le package.");

            Class.forName(properties.getProperty("db.driver"));
            DB_UTIL_LOGGER.info("Driver JDBC chargé : " + properties.getProperty("db.driver"));
        } catch (IOException e) {
            DB_UTIL_LOGGER.log(Level.SEVERE, "Erreur lors du chargement du fichier '" + DB_PROPERTIES_FILE_NAME + "': " + e.getMessage());
            throw new RuntimeException("Impossible de charger la configuration de la base de données.", e);
        } catch (ClassNotFoundException e) {
            DB_UTIL_LOGGER.log(Level.SEVERE, "Driver JDBC non trouvé: " + properties.getProperty("db.driver"), e);
            throw new RuntimeException("Driver JDBC introuvable.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(
                    properties.getProperty("db.url"),
                    properties.getProperty("db.username"),
                    properties.getProperty("db.password")
            );
            DB_UTIL_LOGGER.fine("Connexion à la base de données établie.");
            return conn;
        } catch (SQLException e) {
            DB_UTIL_LOGGER.log(Level.SEVERE, "Échec de la connexion à la base de données. URL: " + properties.getProperty("db.url") + ", Utilisateur: " + properties.getProperty("db.username"), e);
            throw new SQLException("Impossible d'établir la connexion à la base de données : " + e.getMessage(), e);
        }
    }

    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                DB_UTIL_LOGGER.fine("Connexion à la base de données fermée.");
            } catch (SQLException e) {
                DB_UTIL_LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion.", e);
            }
        }
    }

    public static void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
                DB_UTIL_LOGGER.finest("Statement fermé.");
            } catch (SQLException e) {
                DB_UTIL_LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du Statement.", e);
            }
        }
    }

    public static void close(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
                DB_UTIL_LOGGER.finest("PreparedStatement fermé.");
            } catch (SQLException e) {
                DB_UTIL_LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du PreparedStatement.", e);
            }
        }
    }

    public static void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
                DB_UTIL_LOGGER.finest("ResultSet fermé.");
            } catch (SQLException e) {
                DB_UTIL_LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du ResultSet.", e);
            }
        }
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        close(rs);
        close(stmt);
        close(conn);
    }

    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        close(rs);
        close(pstmt);
        close(conn);
    }
    public static void close(Connection conn, PreparedStatement pstmt) {
        close(pstmt);
        close(conn);
    }
}