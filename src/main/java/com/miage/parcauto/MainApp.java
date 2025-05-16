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
                            // Ne devrait pas arriver avec StringWriter/PrintWriter
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

            APPLICATION_LOGGER.setLevel(Level.ALL); // Le logger principal capture tout, les handlers filtrent.
            APPLICATION_LOGGER.setUseParentHandlers(false);

            APPLICATION_LOGGER.info("Système de journalisation initialisé. Les logs sont dirigés vers: " + nomFichierLog);

        } catch (IOException e) {
            System.err.println("ERREUR CRITIQUE : Impossible d'initialiser le système de journalisation des événements : " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }


    @Override
    public void start(Stage stage) {
        APPLICATION_LOGGER.info("Démarrage de l'application ParcAuto MIAGE Holding, version 1.0.");
        primaryStageInstance = stage;
        primaryStageInstance.setOnCloseRequest(event -> {
            APPLICATION_LOGGER.info("Signal de fermeture de l'application reçu. Arrêt en cours.");
            Platform.exit();
            System.exit(0);
        });

        try {
            initialiserInfrastructureApplicative();
            chargerInterfaceConnexion();
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Erreur irrécupérable et fatale lors de la phase de démarrage de l'application: " + e.getMessage(), e);
            afficherNotificationCritique("Erreur Fatale au Démarrage",
                    "Une défaillance majeure est survenue, empêchant le lancement de l'application.",
                    "Veuillez consulter les journaux d'événements (logs) dans le répertoire '" + DOSSIER_LOGS + "' pour une analyse détaillée.\nCause: " + e.getClass().getSimpleName() + " - " + e.getMessage() +
                            "\nL'application va maintenant se terminer de manière abrupte.");
            Platform.exit();
            System.exit(1);
        }
    }

    private void initialiserInfrastructureApplicative() throws RuntimeException {
        APPLICATION_LOGGER.info("Initialisation de l'infrastructure applicative et des services principaux.");
        try {
            // L'initialisation de dbUtil (et donc le chargement de db.properties)
            // se fait via son bloc static. Si dbUtil échoue, une RuntimeException sera levée.
            // Nous validons explicitement la connectivité.
            APPLICATION_LOGGER.fine("Tentative de validation de la connexion à la base de données via dbUtil.");
            try (Connection testConn = dbUtil.getConnection()) {
                if (testConn == null || testConn.isClosed()) {
                    APPLICATION_LOGGER.severe("Test de connexion à la base de données a échoué : connexion nulle ou déjà fermée après obtention.");
                    throw new RuntimeException("Validation de la connexion à la base de données a échoué : connexion invalide retournée par dbUtil.");
                }
                APPLICATION_LOGGER.info("Validation de la connectivité à la base de données réussie.");
            } catch (SQLException e) {
                APPLICATION_LOGGER.log(Level.SEVERE, "Échec critique du test de connexion initial à la base de données. Vérifiez db.properties et l'état du serveur MySQL.", e);
                throw new RuntimeException("Impossible d'établir ou de valider la connexion initiale à la base de données.", e);
            } catch (Exception e) {
                APPLICATION_LOGGER.log(Level.SEVERE, "Erreur inattendue lors de l'initialisation statique de dbUtil ou du test de connexion.", e);
                throw new RuntimeException("Problème majeur avec l'utilitaire de base de données dbUtil.", e);
            }

            persistenceServiceInstance = new PersistenceService();
            APPLICATION_LOGGER.fine("Service de persistance (PersistenceService) instancié.");

            securityManagerInstance = new SecurityManager(persistenceServiceInstance);
            APPLICATION_LOGGER.fine("Gestionnaire de sécurité (SecurityManager) instancié.");

            businessLogicServiceInstance = new BusinessLogicService(persistenceServiceInstance);
            APPLICATION_LOGGER.fine("Service de logique métier (BusinessLogicService) instancié.");

            reportingEngineInstance = new ReportingEngine(persistenceServiceInstance);
            APPLICATION_LOGGER.fine("Moteur de reporting (ReportingEngine) instancié.");

            APPLICATION_LOGGER.info("Tous les services principaux de l'infrastructure applicative ont été initialisés avec succès.");
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Échec catastrophique de l'initialisation d'un ou plusieurs services fondamentaux de l'application.", e);
            throw new RuntimeException("Impossible d'initialiser l'infrastructure applicative.", e);
        }
    }

    private static FXMLLoader preparerChargeurFXML(String nomFichierFxmlSimple) throws IOException {
        String cheminCompletFXML = PACKAGE_FXML_BASE + nomFichierFxmlSimple;
        URL fxmlUrl = MainApp.class.getResource(cheminCompletFXML);
        if (fxmlUrl == null) {
            String messageErreurTechnique = "Ressource FXML introuvable : " + cheminCompletFXML + ". Vérifiez que le fichier existe bien à l'emplacement attendu dans les ressources du projet (src/main/resources" + PACKAGE_FXML_BASE + nomFichierFxmlSimple + ").";
            APPLICATION_LOGGER.severe(messageErreurTechnique);
            throw new IOException(messageErreurTechnique);
        }
        APPLICATION_LOGGER.fine("Préparation du chargement pour le fichier FXML : " + cheminCompletFXML);
        return new FXMLLoader(fxmlUrl);
    }

    public static void chargerInterfaceConnexion() throws IOException {
        APPLICATION_LOGGER.info("Chargement de l'interface de connexion utilisateur (LoginView.fxml).");
        FXMLLoader chargeurFXML = preparerChargeurFXML("LoginView.fxml");
        Parent racineInterface = chargeurFXML.load();

        Object controleurObtenu = chargeurFXML.getController();
        if (controleurObtenu instanceof ViewController.InitializableServices) {
            ((ViewController.InitializableServices) controleurObtenu).setServices(businessLogicServiceInstance, securityManagerInstance);
            APPLICATION_LOGGER.fine("Services injectés dans le contrôleur de LoginView via InitializableServices.");
        } else if (controleurObtenu instanceof ViewController) {
            ((ViewController) controleurObtenu).setServices(businessLogicServiceInstance, securityManagerInstance);
            APPLICATION_LOGGER.fine("Services injectés dans le contrôleur de LoginView (instance de ViewController).");
        } else {
            APPLICATION_LOGGER.warning("Le contrôleur de LoginView.fxml (" + (controleurObtenu != null ? controleurObtenu.getClass().getName() : "null") + ") n'est pas compatible avec l'injection de services (ni ViewController, ni InitializableServices).");
        }
        controleurActif = controleurObtenu;

        primaryStageInstance.setTitle("ParcAuto MIAGE Holding - Authentification Requise");
        Scene sceneConnexion = new Scene(racineInterface);
        // Exemple d'ajout de CSS:
        // URL cssLoginUrl = MainApp.class.getResource("/main/java/com/miage/parcauto/css/login-theme.css");
        // if (cssLoginUrl != null) sceneConnexion.getStylesheets().add(cssLoginUrl.toExternalForm());
        primaryStageInstance.setScene(sceneConnexion);
        primaryStageInstance.setMinWidth(550);
        primaryStageInstance.setMinHeight(480);
        primaryStageInstance.setResizable(false);
        primaryStageInstance.centerOnScreen();
        primaryStageInstance.show();
        APPLICATION_LOGGER.info("Interface de connexion (LoginView.fxml) chargée et affichée à l'utilisateur.");
    }

    public static void chargerInterfacePrincipaleApplication() throws IOException {
        APPLICATION_LOGGER.info("Chargement de l'interface principale de l'application (MainDashboardView.fxml).");
        FXMLLoader chargeurFXML = preparerChargeurFXML("MainDashboardView.fxml");
        Parent racineInterface = chargeurFXML.load();

        Object controleurObtenu = chargeurFXML.getController();
        if (controleurObtenu instanceof ViewController) {
            ViewController vc = (ViewController) controleurObtenu;
            vc.setServices(businessLogicServiceInstance, securityManagerInstance);
            APPLICATION_LOGGER.fine("Services injectés dans le contrôleur de MainDashboardView.");
            Platform.runLater(() -> {
                vc.initialiserApresLogin();
                APPLICATION_LOGGER.fine("Méthode initialiserApresLogin appelée pour MainDashboardView.");
            });
        } else {
            String nomClasseControleur = (controleurObtenu != null) ? controleurObtenu.getClass().getName() : "null";
            APPLICATION_LOGGER.severe("Erreur critique: Le contrôleur de MainDashboardView.fxml (" + nomClasseControleur + ") n'est pas une instance de ViewController. Fonctionnalités principales compromises.");
            throw new IllegalStateException("Contrôleur principal de type incorrect: " + nomClasseControleur);
        }
        controleurActif = controleurObtenu;

        primaryStageInstance.setTitle("ParcAuto MIAGE Holding - Système de Gestion Intégré du Parc Automobile");
        Scene scenePrincipale = new Scene(racineInterface, 1440, 850);
        // URL cssDashboardUrl = MainApp.class.getResource("/main/java/com/miage/parcauto/css/dashboard.css");
        // if (cssDashboardUrl != null) scenePrincipale.getStylesheets().add(cssDashboardUrl.toExternalForm());
        primaryStageInstance.setScene(scenePrincipale);
        primaryStageInstance.setMinWidth(1280);
        primaryStageInstance.setMinHeight(760);
        primaryStageInstance.setResizable(true);
        primaryStageInstance.setMaximized(false); // Peut être true si vous préférez
        primaryStageInstance.centerOnScreen();
        APPLICATION_LOGGER.info("Interface principale (MainDashboardView.fxml) chargée et affichée à l'utilisateur.");
    }

    public static BusinessLogicService getBusinessLogicService() {
        return businessLogicServiceInstance;
    }

    public static SecurityManager getSecurityManager() {
        return securityManagerInstance;
    }

    public static ReportingEngine getReportingEngine() {
        return reportingEngineInstance;
    }

    public static Stage getPrimaryStage(){
        return primaryStageInstance;
    }

    public static Object getActiveController() {
        return controleurActif;
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
            // Si nous ne sommes pas sur le thread FX et que la plateforme FX est initialisée, utiliser Platform.runLater.
            // Si la plateforme n'est pas encore initialisée (très tôt dans start()), cela pourrait ne pas fonctionner.
            try {
                Platform.runLater(tacheAffichage);
            } catch (IllegalStateException e) {
                // La plateforme FX n'est pas disponible (par exemple, erreur avant même que `launch` ne soit complètement actif)
                System.err.println("NOTIFICATION CRITIQUE (hors thread FX et plateforme FX non prête): " + titre + "\n" + entete + "\n" + contenu);
            }
        }
    }

    public static void main(String[] args) {
        APPLICATION_LOGGER.info("Exécution de la méthode main de MainApp. Lancement de l'application JavaFX.");
        try {
            Application.launch(args);
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Exception non interceptée au niveau du lancement de l'application JavaFX (Application.launch).", e);
            System.err.println("Une erreur fatale non gérée est survenue lors du lancement de l'application. Consultez les logs.");
            System.exit(1); // Quitter en cas d'erreur fatale au lancement même de JavaFX.
        }
    }
}