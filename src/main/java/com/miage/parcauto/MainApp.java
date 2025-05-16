package main.java.com.miage.parcauto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static Stage primaryStageInstance;
    private static PersistenceService persistenceServiceInstance;
    private static BusinessLogicService businessLogicServiceInstance;
    private static SecurityManager securityManagerInstance;
    private static ReportingEngine reportingEngineInstance;
    private static Object controleurActif;

    private static final String PACKAGE_FXML_BASE = "/main/java/com/miage/parcauto/fxml/";
    private static final String DOSSIER_LOGS = "logs";
    private static final Logger APPLICATION_LOGGER = Logger.getLogger("main.java.com.miage.parcauto");

    static {
        try {
            Path cheminDossierLogs = Paths.get(DOSSIER_LOGS);
            if (Files.notExists(cheminDossierLogs)) {
                Files.createDirectories(cheminDossierLogs);
            }

            SimpleDateFormat formatNomFichier = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String nomFichierLog = cheminDossierLogs.resolve("ParcAutoApplication_" + formatNomFichier.format(new Date()) + ".log").toString();

            FileHandler fileHandler = new FileHandler(nomFichierLog, true);

            Formatter customFormatter = new Formatter() {
                private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                @Override
                public String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(LocalDateTime.now().format(dtf)).append("] ");
                    sb.append("[").append(String.format("%-7s", record.getLevel().getLocalizedName())).append("] ");

                    String loggerName = record.getLoggerName();
                    if (loggerName.startsWith("main.java.com.miage.parcauto.")) {
                        loggerName = loggerName.substring("main.java.com.miage.parcauto.".length());
                    }
                    sb.append("[").append(loggerName).append("] ");

                    sb.append(formatMessage(record));
                    sb.append(System.lineSeparator());
                    if (record.getThrown() != null) {
                        try (StringWriter sw = new StringWriter();
                             PrintWriter pw = new PrintWriter(sw)) {
                            record.getThrown().printStackTrace(pw);
                            sb.append(sw.toString());
                            sb.append(System.lineSeparator());
                        } catch (Exception ex) {
                            // Ne devrait pas arriver
                        }
                    }
                    return sb.toString();
                }
            };

            fileHandler.setFormatter(customFormatter);
            fileHandler.setLevel(Level.ALL);
            APPLICATION_LOGGER.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(customFormatter);
            consoleHandler.setLevel(Level.INFO);
            APPLICATION_LOGGER.addHandler(consoleHandler);

            APPLICATION_LOGGER.setLevel(Level.ALL);
            APPLICATION_LOGGER.setUseParentHandlers(false);

            APPLICATION_LOGGER.info("Système de journalisation initialisé. Logs vers: " + nomFichierLog);

        } catch (IOException e) {
            System.err.println("ERREUR CRITIQUE : Impossible d'initialiser le système de journalisation : " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }


    @Override
    public void start(Stage stage) {
        APPLICATION_LOGGER.info("Démarrage de l'application ParcAuto MIAGE Holding.");
        primaryStageInstance = stage;
        primaryStageInstance.setOnCloseRequest(event -> {
            APPLICATION_LOGGER.info("Signal de fermeture de l'application reçu. Arrêt.");
            Platform.exit();
            System.exit(0);
        });

        try {
            initialiserInfrastructureApplicative();
            chargerInterfaceConnexion();
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Erreur fatale lors du démarrage: " + e.getMessage(), e);
            afficherNotificationCritique("Erreur Fatale au Démarrage",
                    "Une défaillance majeure empêche le lancement.",
                    "Consultez les logs dans '" + DOSSIER_LOGS + "'.\nCause: " + e.getClass().getSimpleName() + " - " + e.getMessage() +
                            "\nL'application va se terminer.");
            Platform.exit();
            System.exit(1);
        }
    }

    private void initialiserInfrastructureApplicative() throws RuntimeException {
        APPLICATION_LOGGER.info("Initialisation de l'infrastructure applicative.");
        try {
            APPLICATION_LOGGER.fine("Validation de la connexion à la base de données.");
            try (Connection testConn = dbUtil.getConnection()) {
                if (testConn == null || testConn.isClosed()) {
                    throw new RuntimeException("Validation connexion BDD échouée : connexion invalide.");
                }
                APPLICATION_LOGGER.info("Connectivité à la base de données validée.");
            } catch (SQLException e) {
                throw new RuntimeException("Impossible d'établir/valider la connexion BDD initiale.", e);
            }

            persistenceServiceInstance = new PersistenceService();
            APPLICATION_LOGGER.fine("PersistenceService instancié.");

            securityManagerInstance = new SecurityManager(persistenceServiceInstance);
            APPLICATION_LOGGER.fine("SecurityManager instancié.");
            
            APPLICATION_LOGGER.fine("BusinessLogicService instancié.");

            reportingEngineInstance = new ReportingEngine(persistenceServiceInstance);
            APPLICATION_LOGGER.fine("ReportingEngine instancié.");

            APPLICATION_LOGGER.info("Tous les services principaux initialisés.");
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Échec initialisation services fondamentaux.", e);
            throw new RuntimeException("Impossible d'initialiser l'infrastructure applicative.", e);
        }
    }

    private static FXMLLoader preparerChargeurFXML(String nomFichierFxmlSimple) throws IOException {
        String cheminCompletFXML = PACKAGE_FXML_BASE + nomFichierFxmlSimple;
        URL fxmlUrl = MainApp.class.getResource(cheminCompletFXML);
        if (fxmlUrl == null) {
            String messageErreur = "Ressource FXML introuvable : " + cheminCompletFXML;
            APPLICATION_LOGGER.severe(messageErreur);
            throw new IOException(messageErreur);
        }
        APPLICATION_LOGGER.fine("Préparation chargement FXML : " + cheminCompletFXML);
        return new FXMLLoader(fxmlUrl);
    }

    public static void chargerInterfaceConnexion() throws IOException {
        APPLICATION_LOGGER.info("Chargement de LoginView.fxml.");
        FXMLLoader chargeurFXML = preparerChargeurFXML("LoginView.fxml");
        Parent racineInterface = chargeurFXML.load();

        Object controleurObtenu = chargeurFXML.getController();
        if (controleurObtenu instanceof ViewController) { // ViewController gère aussi LoginView
            ((ViewController) controleurObtenu).injecterDependancesServices(
                    businessLogicServiceInstance,
                    securityManagerInstance,
                    reportingEngineInstance,
                    persistenceServiceInstance
            );
            APPLICATION_LOGGER.fine("Services injectés dans le contrôleur de LoginView.");
        } else {
            APPLICATION_LOGGER.warning("Contrôleur de LoginView.fxml type incorrect: " + (controleurObtenu != null ? controleurObtenu.getClass().getName() : "null"));
        }
        controleurActif = controleurObtenu;

        primaryStageInstance.setTitle("ParcAuto MIAGE Holding - Authentification");
        Scene sceneConnexion = new Scene(racineInterface);
        primaryStageInstance.setScene(sceneConnexion);
        primaryStageInstance.setMinWidth(550);
        primaryStageInstance.setMinHeight(480);
        primaryStageInstance.setResizable(false);
        primaryStageInstance.centerOnScreen();
        primaryStageInstance.show();
        APPLICATION_LOGGER.info("Interface de connexion affichée.");
    }

    public static void chargerInterfacePrincipaleApplication() throws IOException {
        APPLICATION_LOGGER.info("Chargement de MainDashboardView.fxml.");
        FXMLLoader chargeurFXML = preparerChargeurFXML("MainDashboardView.fxml");
        Parent racineInterface = chargeurFXML.load();

        Object controleurObtenu = chargeurFXML.getController();
        if (controleurObtenu instanceof ViewController) {
            ViewController vc = (ViewController) controleurObtenu;
            vc.injecterDependancesServices(
                    businessLogicServiceInstance,
                    securityManagerInstance,
                    reportingEngineInstance,
                    persistenceServiceInstance
            );
            APPLICATION_LOGGER.fine("Services injectés dans le contrôleur de MainDashboardView.");
            Platform.runLater(() -> {
                vc.initialiserInterfacePrincipaleApresConnexion(); // Nom de méthode mis à jour
                APPLICATION_LOGGER.fine("initialiserInterfacePrincipaleApresConnexion appelée pour MainDashboardView.");
            });
        } else {
            String nomClasseControleur = (controleurObtenu != null) ? controleurObtenu.getClass().getName() : "null";
            APPLICATION_LOGGER.severe("Erreur: Contrôleur de MainDashboardView.fxml (" + nomClasseControleur + ") n'est pas instance de ViewController.");
            throw new IllegalStateException("Contrôleur principal de type incorrect: " + nomClasseControleur);
        }
        controleurActif = controleurObtenu;

        primaryStageInstance.setTitle("ParcAuto MIAGE Holding - Système de Gestion Intégré");
        Scene scenePrincipale = new Scene(racineInterface, 1440, 850);
        primaryStageInstance.setScene(scenePrincipale);
        primaryStageInstance.setMinWidth(1280);
        primaryStageInstance.setMinHeight(760);
        primaryStageInstance.setResizable(true);
        primaryStageInstance.setMaximized(false);
        primaryStageInstance.centerOnScreen();
        APPLICATION_LOGGER.info("Interface principale affichée.");
    }

    public static Stage getPrimaryStage(){
        return primaryStageInstance;
    }

    private static void afficherNotificationCritique(String titre, String entete, String contenu) {
        Runnable tacheAffichage = () -> {
            Alert alerte = new Alert(Alert.AlertType.ERROR);
            alerte.setTitle(titre);
            alerte.setHeaderText(entete);
            alerte.setContentText(contenu);
            if (primaryStageInstance != null && primaryStageInstance.isShowing()) {
                alerte.initOwner(primaryStageInstance);
            }
            alerte.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            tacheAffichage.run();
        } else {
            try {
                Platform.runLater(tacheAffichage);
            } catch (IllegalStateException e) {
                System.err.println("NOTIFICATION CRITIQUE (hors thread FX): " + titre + "\n" + entete + "\n" + contenu);
            }
        }
    }

    public static void main(String[] args) {
        APPLICATION_LOGGER.info("Méthode main. Lancement application JavaFX.");
        try {
            Application.launch(args);
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Exception non interceptée au lancement JavaFX.", e);
            System.err.println("Erreur fatale non gérée au lancement. Consultez les logs.");
            System.exit(1);
        }
    }
}