package main.java.com.miage.parcauto;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane; // Ajouté pour les formulaires complexes
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppDataTransferObjects.*;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ViewController {

    protected BusinessLogicService businessLogicService;
    protected SecurityManager securityManager;
    protected ReportingEngine reportingEngine;
    protected PersistenceService persistenceService; // Ajouté pour accès direct si DataMapper en a besoin

    private static final Logger APPLICATION_LOGGER = Logger.getLogger(ViewController.class.getName());
    private static final String BASE_CHEMIN_FXML_RESSOURCES = "/main/java/com/miage/parcauto/fxml/";
    public static final DateTimeFormatter FORMATTEUR_DATE_STANDARD_VUE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMATTEUR_DATETIME_STANDARD_VUE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String DOSSIER_DOCUMENTS_SOCIETAIRES_UPLOAD = "documents_societaires";


    @FXML protected BorderPane conteneurPrincipalApplication;
    @FXML private TextField champLoginUtilisateur;
    @FXML private PasswordField champMotDePasseUtilisateur;
    @FXML private Label etiquetteUtilisateurConnecte;
    @FXML private Label etiquetteRoleUtilisateur;
    @FXML private MenuButton menuBoutonUtilisateur;

    @FXML private MenuItem menuItemTableauDeBord;
    @FXML private MenuItem menuItemGestionVehicules;
    @FXML private MenuItem menuItemGestionMissions;
    @FXML private MenuItem menuItemGestionEntretiens;
    @FXML private MenuItem menuItemGestionFinances;
    @FXML private MenuItem menuItemGestionDocuments;
    @FXML private MenuItem menuItemGestionUtilisateurs;
    @FXML private MenuItem menuItemRapportsEtStatistiques;
    @FXML private MenuItem menuItemParametresApplication;


    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.businessLogicService = Objects.requireNonNull(bls, "BusinessLogicService ne peut être nul.");
        this.securityManager = Objects.requireNonNull(sm, "SecurityManager ne peut être nul.");
        this.reportingEngine = Objects.requireNonNull(re, "ReportingEngine ne peut être nul.");
        this.persistenceService = Objects.requireNonNull(ps, "PersistenceService ne peut être nul.");
        APPLICATION_LOGGER.fine("Ensemble des dépendances services injectées avec succès dans ViewController.");
    }

    public void initialiserInterfacePrincipaleApresConnexion() {
        if (!SessionManager.estUtilisateurConnecte()) {
            APPLICATION_LOGGER.warning("Tentative d'initialisation de l'interface principale sans utilisateur authentifié. Redirection immédiate vers l'écran de connexion.");
            try {
                MainApp.chargerInterfaceConnexion();
            } catch (IOException e) {
                APPLICATION_LOGGER.log(Level.SEVERE, "Échec critique et irrécupérable de la redirection vers l'interface de connexion.", e);
                afficherNotificationAlerte("Erreur Fatale de Session", "Session utilisateur invalide ou corrompue. Redirection vers l'écran de connexion impossible.", Alert.AlertType.ERROR);
            }
            return;
        }

        Utilisateur utilisateurCourant = SessionManager.obtenirUtilisateurActuel();
        String loginAffiche = utilisateurCourant.getLogin();
        String roleAffiche = utilisateurCourant.getRole().getLibelleInterface(); // Utiliser un libellé plus convivial

        if (etiquetteUtilisateurConnecte != null) etiquetteUtilisateurConnecte.setText("Connecté : " + loginAffiche);
        if (etiquetteRoleUtilisateur != null) etiquetteRoleUtilisateur.setText("Rôle : " + roleAffiche);
        if (menuBoutonUtilisateur != null) menuBoutonUtilisateur.setText(loginAffiche);

        configurerPermissionsMenusInterfacePrincipale();
        chargerVueInitialePostConnexion();
        APPLICATION_LOGGER.info("ViewController initialisé pour l'utilisateur '" + loginAffiche + "' (Rôle: " + roleAffiche + "). Interface principale configurée et prête.");
    }

    private void configurerPermissionsMenusInterfacePrincipale() {
        RoleUtilisateur roleActuel = SessionManager.obtenirRoleUtilisateurActuel();
        if (roleActuel == null) {
            APPLICATION_LOGGER.severe("Impossible de configurer les permissions des menus : rôle utilisateur actuel est indéfini (null).");
            // Potentiellement désactiver tous les menus sauf déconnexion
            Arrays.asList(menuItemTableauDeBord, menuItemGestionVehicules, menuItemGestionMissions, menuItemGestionEntretiens, menuItemGestionFinances, menuItemGestionDocuments, menuItemGestionUtilisateurs, menuItemRapportsEtStatistiques, menuItemParametresApplication)
                    .stream().filter(Objects::nonNull).forEach(item -> item.setVisible(false));
            return;
        }

        boolean estAdminSysteme = securityManager.estAutorise(roleActuel, Permissions.ACCES_ADMINISTRATION_SYSTEME); // Permission générique pour U4

        if (menuItemTableauDeBord != null) menuItemTableauDeBord.setVisible(true); // Visible pour tous

        if (menuItemGestionVehicules != null) menuItemGestionVehicules.setVisible(
                estAdminSysteme || securityManager.estAutorise(roleActuel, Permissions.VEHICULE_CONSULTER_TOUS) || securityManager.estAutorise(roleActuel, Permissions.VEHICULE_CONSULTER_PROPRES)
        );
        if (menuItemGestionMissions != null) menuItemGestionMissions.setVisible(
                estAdminSysteme || securityManager.estAutorise(roleActuel, Permissions.MISSION_GERER_TOUTES) || securityManager.estAutorise(roleActuel, Permissions.MISSION_CONSULTER_PROPRES)
        );
        if (menuItemGestionEntretiens != null) menuItemGestionEntretiens.setVisible(
                estAdminSysteme || securityManager.estAutorise(roleActuel, Permissions.ENTRETIEN_GERER_TOUS) || securityManager.estAutorise(roleActuel, Permissions.ENTRETIEN_CONSULTER_PROPRES)
        );
        if (menuItemGestionFinances != null) menuItemGestionFinances.setVisible(
                estAdminSysteme || securityManager.estAutorise(roleActuel, Permissions.FINANCE_CONSULTER_COMPTES_SOCIETAIRES)
        );
        if (menuItemGestionDocuments != null) menuItemGestionDocuments.setVisible(
                estAdminSysteme || securityManager.estAutorise(roleActuel, Permissions.DOCUMENT_GERER_PROPRES) // U3 pour ses docs, U4 pour plus?
        );
        if (menuItemGestionUtilisateurs != null) menuItemGestionUtilisateurs.setVisible(estAdminSysteme);

        if (menuItemRapportsEtStatistiques != null) menuItemRapportsEtStatistiques.setVisible(true); // Accès de base, filtrage dans le module

        if (menuItemParametresApplication != null) menuItemParametresApplication.setVisible(estAdminSysteme);

        APPLICATION_LOGGER.fine("Permissions des menus de l'interface principale configurées pour le rôle : " + roleActuel.getDbValue());
    }

    private void chargerVueInitialePostConnexion() {
        APPLICATION_LOGGER.fine("Chargement de la vue initiale par défaut après la connexion de l'utilisateur.");
        try {
            if (menuItemTableauDeBord != null && menuItemTableauDeBord.isVisible()){
                actionNaviguerVersTableauDeBordPrincipal();
            } else if (menuItemGestionVehicules != null && menuItemGestionVehicules.isVisible()) {
                actionNaviguerVersGestionVehicules();
            } else {
                chargerContenuDansPanneauCentral("BienvenueView.fxml"); // Une vue d'accueil générique
                APPLICATION_LOGGER.info("Vue d'accueil générique chargée, aucune vue par défaut spécifique au rôle n'a été identifiée.");
            }
        } catch (IOException e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Échec critique lors du chargement de la vue initiale post-connexion.", e);
            afficherNotificationAlerte("Erreur de Chargement d'Interface", "Impossible de charger la vue initiale de l'application : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void initialize() {
        APPLICATION_LOGGER.fine("Méthode initialize() appelée par JavaFX pour le contrôleur : " + this.getClass().getName() + ". Ce FXML est : " + (champLoginUtilisateur != null ? "LoginView" : "MainDashboardView ou autre panel"));
        if (champLoginUtilisateur != null) { // Spécifique à LoginView.fxml
            Platform.runLater(() -> champLoginUtilisateur.requestFocus());
            APPLICATION_LOGGER.fine("Focus initial positionné sur le champ de login utilisateur (LoginView).");
        }
        // Les initialisations spécifiques aux panneaux (VehiculePanelView, etc.) se feront dans leurs contrôleurs dédiés via `initialiserDonneesVue`.
    }

    @FXML
    public void actionAuthentificationUtilisateur() {
        if (champLoginUtilisateur == null || champMotDePasseUtilisateur == null) {
            APPLICATION_LOGGER.warning("Tentative d'authentification depuis une interface non prévue (champs login/mot de passe FXML absents). Opération ignorée.");
            return;
        }

        String loginSaisi = champLoginUtilisateur.getText();
        String motDePasseSaisi = champMotDePasseUtilisateur.getText();

        if (loginSaisi.trim().isEmpty() || motDePasseSaisi.isEmpty()) {
            afficherNotificationAlerte("Champs Obligatoires Non Renseignés", "Le nom d'utilisateur et le mot de passe sont indispensables pour procéder à la connexion.", Alert.AlertType.WARNING);
            return;
        }

        APPLICATION_LOGGER.info("Tentative d'authentification pour l'utilisateur : '" + loginSaisi + "'.");
        try {
            Utilisateur utilisateurAuthentifie = securityManager.authentifierUtilisateur(loginSaisi, motDePasseSaisi);
            SessionManager.definirUtilisateurActuel(utilisateurAuthentifie);
            APPLICATION_LOGGER.info("Authentification confirmée pour l'utilisateur '" + loginSaisi + "'. Rôle attribué : " + utilisateurAuthentifie.getRole().getLibelleInterface());
            MainApp.chargerInterfacePrincipaleApplication();
        } catch (ErreurAuthentification e) {
            APPLICATION_LOGGER.warning("Échec de l'authentification pour l'utilisateur '" + loginSaisi + "' : " + e.getMessage());
            afficherNotificationAlerte("Échec de l'Authentification Utilisateur", e.getMessage(), Alert.AlertType.ERROR);
        } catch (IOException e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Erreur d'Entrée/Sortie critique lors du chargement de l'interface principale après l'authentification.", e);
            afficherNotificationAlerte("Erreur Système Critique et Majeure", "Une défaillance majeure est survenue lors du chargement de l'application principale : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Erreur système inattendue et non gérée durant le processus d'authentification de l'utilisateur.", e);
            afficherNotificationAlerte("Erreur Système Inattendue", "Une erreur imprévue et non cataloguée s'est produite. Veuillez réessayer ultérieurement ou contacter le support technique.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void actionDeconnexionUtilisateur() {
        String utilisateurAvantDeconnexion = SessionManager.estUtilisateurConnecte() ? SessionManager.obtenirUtilisateurActuel().getLogin() : "Utilisateur non identifié";
        APPLICATION_LOGGER.info("Requête de déconnexion initiée pour l'utilisateur : " + utilisateurAvantDeconnexion);
        SessionManager.deconnecterUtilisateur();
        try {
            MainApp.chargerInterfaceConnexion();
            APPLICATION_LOGGER.info("Déconnexion de l'utilisateur effectuée avec succès. Retour à l'interface d'authentification.");
        } catch (IOException e) {
            APPLICATION_LOGGER.log(Level.SEVERE, "Erreur d'Entrée/Sortie lors du rechargement de l'interface de connexion après la déconnexion.", e);
            afficherNotificationAlerte("Erreur Critique de Déconnexion", "Une erreur est survenue lors de la tentative de redirection vers l'écran de connexion : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    protected void chargerContenuDansPanneauCentral(String nomFichierFxmlSimple) throws IOException {
        if (conteneurPrincipalApplication == null) {
            String messageErreurTechnique = "Le conteneur d'affichage principal (BorderPane 'conteneurPrincipalApplication') est introuvable (null) ou non correctement injecté depuis le FXML. Impossible de charger la vue demandée : " + nomFichierFxmlSimple;
            APPLICATION_LOGGER.severe(messageErreurTechnique);
            afficherNotificationAlerte("Erreur Technique Interne d'Affichage", messageErreurTechnique, Alert.AlertType.ERROR);
            return;
        }
        String cheminFxmlCompletRessource = BASE_CHEMIN_FXML_RESSOURCES + nomFichierFxmlSimple;
        URL urlFxml = getClass().getResource(cheminFxmlCompletRessource);
        if (urlFxml == null) {
            APPLICATION_LOGGER.severe("Ressource FXML non trouvée à l'emplacement : " + cheminFxmlCompletRessource + ". Vérification du chemin et de la présence du fichier dans les ressources requise.");
            throw new IOException("Fichier FXML introuvable : " + cheminFxmlCompletRessource);
        }

        FXMLLoader chargeurFXML = new FXMLLoader(urlFxml);
        Parent vueChargee = chargeurFXML.load();
        APPLICATION_LOGGER.fine("Fichier FXML '" + nomFichierFxmlSimple + "' chargé en mémoire.");

        Object controleurDeLaVueChargee = chargeurFXML.getController();
        if (controleurDeLaVueChargee instanceof InitializableServices) {
            ((InitializableServices) controleurDeLaVueChargee).injecterDependancesServices(this.businessLogicService, this.securityManager, this.reportingEngine, this.persistenceService);
            ((InitializableServices) controleurDeLaVueChargee).initialiserDonneesVue();
            APPLICATION_LOGGER.fine("Services injectés et données initialisées pour le contrôleur de '" + nomFichierFxmlSimple + "' (implémente InitializableServices).");
        } else if (controleurDeLaVueChargee != null) {
            APPLICATION_LOGGER.info("Le contrôleur pour '" + nomFichierFxmlSimple + "' (" + controleurDeLaVueChargee.getClass().getName() + ") a été chargé mais n'implémente pas l'interface 'InitializableServices'. L'injection complète des services et l'initialisation des données spécifiques pourraient ne pas avoir eu lieu.");
        } else {
            APPLICATION_LOGGER.warning("Aucun contrôleur n'a été associé au fichier FXML '" + nomFichierFxmlSimple + "'. Vérifiez la directive 'fx:controller' dans le fichier FXML.");
        }
        conteneurPrincipalApplication.setCenter(vueChargee);
        APPLICATION_LOGGER.info("Contenu du fichier FXML '" + nomFichierFxmlSimple + "' affiché avec succès dans le panneau central de l'application.");
    }

    public interface InitializableServices {
        void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps);
        void initialiserDonneesVue();
    }

    @FXML public void actionNaviguerVersTableauDeBordPrincipal() throws IOException {
        // Le tableau de bord principal pourrait être une vue spécifique ou simplement la réinitialisation.
        // Pour l'instant, on peut imaginer qu'il charge une vue "DashboardView.fxml" si elle existe.
        // Ou, si MainDashboardView.fxml *est* le tableau de bord, on ne fait rien ou on rafraîchit.
        // Pour cet exemple, on charge une vue fictive, à adapter.
        verifierAutorisationEtChargerVue(Permissions.ACCES_TABLEAU_DE_BORD, null, "DashboardApercuView.fxml", "Tableau de Bord Principal");
    }
    @FXML public void actionNaviguerVersGestionVehicules() throws IOException {
        verifierAutorisationEtChargerVue(Permissions.VEHICULE_CONSULTER_TOUS, Permissions.VEHICULE_CONSULTER_PROPRES, "VehiculePanelView.fxml", "Module de Gestion des Véhicules");
    }
    @FXML public void actionNaviguerVersGestionMissions() throws IOException {
        verifierAutorisationEtChargerVue(Permissions.MISSION_GERER_TOUTES, Permissions.MISSION_CONSULTER_PROPRES, "MissionPanelView.fxml", "Module de Gestion des Missions");
    }
    @FXML public void actionNaviguerVersGestionEntretiens() throws IOException {
        verifierAutorisationEtChargerVue(Permissions.ENTRETIEN_GERER_TOUS, Permissions.ENTRETIEN_CONSULTER_PROPRES, "EntretienPanelView.fxml", "Module de Gestion des Entretiens et Maintenances");
    }
    @FXML public void actionNaviguerVersGestionFinanciere() throws IOException {
        verifierAutorisationEtChargerVue(Permissions.FINANCE_CONSULTER_COMPTES_SOCIETAIRES, null, "FinancePanelView.fxml", "Module de Gestion Financière des Sociétaires");
    }
    @FXML public void actionNaviguerVersGestionDocuments() throws IOException {
        // La permission principale pourrait être pour un admin, l'alternative pour un utilisateur sur ses propres documents.
        verifierAutorisationEtChargerVue(Permissions.DOCUMENT_CONSULTER_TOUS, Permissions.DOCUMENT_GERER_PROPRES, "DocumentPanelView.fxml", "Module de Gestion Électronique des Documents");
    }
    @FXML public void actionNaviguerVersGestionUtilisateurs() throws IOException {
        if (securityManager.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.UTILISATEUR_GERER_COMPTES)) { // Permission spécifique pour admin
            chargerContenuDansPanneauCentral("UserManagementPanelView.fxml");
        } else {
            afficherNotificationNonAutorise("Module de Gestion des Comptes Utilisateurs");
        }
    }
    @FXML public void actionNaviguerVersRapportsEtStatistiques() throws IOException {
        verifierAutorisationEtChargerVue(Permissions.ACCES_RAPPORTS_STATISTIQUES, null, "ReportPanelView.fxml", "Module de Rapports et Statistiques");
    }
    @FXML public void actionNaviguerVersParametresApplication() throws IOException {
        if (securityManager.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.APPLICATION_CONFIGURER_PARAMETRES)) {
            chargerContenuDansPanneauCentral("SettingsManagerView.fxml");
        } else {
            afficherNotificationNonAutorise("Module de Paramétrage de l'Application");
        }
    }

    private void verifierAutorisationEtChargerVue(String permissionPrincipale, String permissionAlternative, String nomFichierFxml, String nomModuleConvivial) throws IOException {
        RoleUtilisateur roleCourantUtilisateur = SessionManager.obtenirRoleUtilisateurActuel();
        boolean accesPermis = securityManager.estAutorise(roleCourantUtilisateur, Permissions.ACCES_ADMINISTRATION_SYSTEME) || // L'admin a accès à tout
                securityManager.estAutorise(roleCourantUtilisateur, permissionPrincipale) ||
                (permissionAlternative != null && securityManager.estAutorise(roleCourantUtilisateur, permissionAlternative));

        if (accesPermis) {
            chargerContenuDansPanneauCentral(nomFichierFxml);
            APPLICATION_LOGGER.info("Accès autorisé et chargement de la vue pour le module : " + nomModuleConvivial);
        } else {
            afficherNotificationNonAutorise(nomModuleConvivial);
        }
    }

    private void afficherNotificationNonAutorise(String nomModuleConvivial) {
        String utilisateurActuelLogin = SessionManager.estUtilisateurConnecte() ? SessionManager.obtenirUtilisateurActuel().getLogin() : "Utilisateur non identifié";
        APPLICATION_LOGGER.warning("Tentative d'accès non autorisé au module '" + nomModuleConvivial + "' par l'utilisateur '" + utilisateurActuelLogin + "'.");
        afficherNotificationAlerte("Accès Non Autorisé", "Vos privilèges actuels ne vous octroient pas l'autorisation nécessaire pour accéder au " + nomModuleConvivial + ".", Alert.AlertType.WARNING);
    }

    protected void afficherNotificationAlerte(String titre, String message, Alert.AlertType typeAlerteGraphique) {
        Alert alerteGraphique = new Alert(typeAlerteGraphique);
        alerteGraphique.setTitle(titre);
        alerteGraphique.setHeaderText(null);
        alerteGraphique.setContentText(message);

        Stage stageProprietaireActuel = MainApp.getPrimaryStage();
        if (stageProprietaireActuel != null && stageProprietaireActuel.getScene() != null && stageProprietaireActuel.isShowing()) {
            alerteGraphique.initOwner(stageProprietaireActuel);
            APPLICATION_LOGGER.fine("Alerte '" + titre + "' affichée, modale à la fenêtre principale.");
        } else {
            APPLICATION_LOGGER.fine("Alerte '" + titre + "' affichée sans fenêtre propriétaire (primaryStage non pleinement initialisé ou non visible).");
        }
        alerteGraphique.showAndWait();
    }

    protected Optional<ButtonType> afficherDialogueConfirmationUtilisateur(String titre, String message) {
        Alert dialogueConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
        dialogueConfirmation.setTitle(titre);
        dialogueConfirmation.setHeaderText(null);
        dialogueConfirmation.setContentText(message);
        Stage stageProprietaireActuel = MainApp.getPrimaryStage();
        if (stageProprietaireActuel != null && stageProprietaireActuel.getScene() != null && stageProprietaireActuel.isShowing()) {
            dialogueConfirmation.initOwner(stageProprietaireActuel);
        }
        return dialogueConfirmation.showAndWait();
    }

    public static StringConverter<LocalDate> obtenirConvertisseurDateStandard() {
        return new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? FORMATTEUR_DATE_STANDARD_VUE.format(date) : "";
            }
            @Override
            public LocalDate fromString(String chaine) {
                try {
                    return (chaine != null && !chaine.trim().isEmpty()) ? LocalDate.parse(chaine, FORMATTEUR_DATE_STANDARD_VUE) : null;
                } catch (DateTimeParseException e) {
                    APPLICATION_LOGGER.warning("Échec de la conversion de la chaîne '" + chaine + "' en LocalDate avec le formatteur standard : " + e.getMessage());
                    return null; // Retourner null en cas d'échec de parsing
                }
            }
        };
    }

    public static StringConverter<LocalDateTime> obtenirConvertisseurDateTimeStandard() {
        return new StringConverter<LocalDateTime>() {
            @Override
            public String toString(LocalDateTime dateTime) {
                return (dateTime != null) ? FORMATTEUR_DATETIME_STANDARD_VUE.format(dateTime) : "";
            }
            @Override
            public LocalDateTime fromString(String chaine) {
                try {
                    return (chaine != null && !chaine.trim().isEmpty()) ? LocalDateTime.parse(chaine, FORMATTEUR_DATETIME_STANDARD_VUE) : null;
                } catch (DateTimeParseException e) {
                    APPLICATION_LOGGER.warning("Échec de la conversion de la chaîne '" + chaine + "' en LocalDateTime avec le formatteur standard : " + e.getMessage());
                    return null;
                }
            }
        };
    }

    // Les champs FXML et la logique pour les panneaux spécifiques (Vehicule, Mission, etc.)
    // DOIVENT être dans leurs propres classes de contrôleur dédiées (ex: VehiculePanelController)
    // qui implémenteront ViewController.InitializableServices.
    // ViewController ne fait que charger ces panneaux dans son conteneurPrincipalApplication.
    // Ci-dessous, un exemple COMMENTÉ de ce que contiendrait un VehiculePanelController.java

    /* Exemple de ce qui irait dans VehiculePanelController.java
    public class VehiculePanelController implements ViewController.InitializableServices {
        @FXML private TableView<VehiculeDTO> tableViewVehicules;
        // ... autres @FXML pour les colonnes, boutons du panneau véhicule ...
        private BusinessLogicService businessLogicService;
        private SecurityManager securityManager;
        private ReportingEngine reportingEngine;
        private PersistenceService persistenceService;

        @Override
        public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
            this.businessLogicService = bls;
            this.securityManager = sm;
            this.reportingEngine = re;
            this.persistenceService = ps;
        }

        @Override
        public void initialiserDonneesVue() {
            // Configurer les colonnes de tableViewVehicules
            // colIdVehicule.setCellValueFactory(new PropertyValueFactory<>("idVehicule"));
            // ...
            // Charger les données initiales
            // actionRafraichirTableVehicules();
        }

        @FXML
        private void actionRafraichirTableVehicules() {
            // List<Vehicule> modelList = businessLogicService.recupererTousLesVehicules();
            // List<VehiculeDTO> dtoList = DataMapper.convertirVersListeDeVehiculeDTO(modelList, persistenceService);
            // tableViewVehicules.setItems(FXCollections.observableArrayList(dtoList));
        }

        @FXML
        private void actionOuvrirFormulaireAjoutVehicule() {
            // Logique d'ouverture du dialogue/formulaire pour ajouter un véhicule
            // Similaire à ce qui était esquissé dans la version précédente de ViewController
            // Mais ici, il utiliserait this.businessLogicService, etc.
            // Et appellerait actionRafraichirTableVehicules() à la fin.
        }
        // ... autres méthodes pour modifier, supprimer, filtrer véhicules ...
    }
    */
}