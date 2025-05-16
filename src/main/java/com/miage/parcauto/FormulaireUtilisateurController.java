package main.java.com.miage.parcauto;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.Utilisateur;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppModels.Personnel;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormulaireUtilisateurController implements ViewController.InitializableServices {
    private static final Logger FORM_USER_LOGGER = Logger.getLogger(FormulaireUtilisateurController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private PersistenceService servicePersistance;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;

    private Utilisateur utilisateurEnCoursEdition;
    private boolean modeCreation;

    @FXML private TextField champLoginCreationUtilisateur;
    @FXML private PasswordField champMotDePasseCreationUtilisateur; // Pour la création
    @FXML private PasswordField champConfirmationMotDePasseUtilisateur; // Pour la création
    @FXML private ChoiceBox<RoleUtilisateur> choiceBoxRoleCreationUtilisateur;
    @FXML private ChoiceBox<Personnel> choiceBoxPersonnelAssocieUtilisateur;

    @FXML private Button boutonSauvegarderUtilisateur;
    @FXML private Button boutonAnnulerFormulaireUtilisateur;
    @FXML private Label etiquetteTitreFormulaireUtilisateur;
    @FXML private Label labelMotDePasseUtilisateur;
    @FXML private Label labelConfirmationMotDePasseUtilisateur;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_USER_LOGGER.fine("Dépendances services injectées dans FormulaireUtilisateurController.");
    }

    @Override
    public void initialiserDonneesVue() {
        FORM_USER_LOGGER.info("Initialisation des données pour le formulaire utilisateur.");
        chargerOptionsRolesEtPersonnel();

        // Gérer la visibilité des champs de mot de passe
        champMotDePasseCreationUtilisateur.setVisible(modeCreation);
        champMotDePasseCreationUtilisateur.setManaged(modeCreation);
        labelMotDePasseUtilisateur.setVisible(modeCreation);
        labelMotDePasseUtilisateur.setManaged(modeCreation);
        champConfirmationMotDePasseUtilisateur.setVisible(modeCreation);
        champConfirmationMotDePasseUtilisateur.setManaged(modeCreation);
        labelConfirmationMotDePasseUtilisateur.setVisible(modeCreation);
        labelConfirmationMotDePasseUtilisateur.setManaged(modeCreation);


        if (modeCreation) {
            etiquetteTitreFormulaireUtilisateur.setText("Création d'un Nouveau Compte Utilisateur");
            this.utilisateurEnCoursEdition = new Utilisateur();
        } else if (this.utilisateurEnCoursEdition != null) {
            etiquetteTitreFormulaireUtilisateur.setText("Modification de l'Utilisateur : " + utilisateurEnCoursEdition.getLogin());
            preRemplirChampsFormulaireUtilisateur();
        } else {
            FORM_USER_LOGGER.severe("Formulaire utilisateur ouvert en mode édition sans utilisateur fourni.");
            afficherNotificationAlerteFormulaire("Erreur Critique", "Aucun utilisateur spécifié pour modification.", Alert.AlertType.ERROR);
            actionAnnulerFormulaire();
        }
    }

    public void preparerFormulairePourEdition(Utilisateur utilisateur) {
        this.utilisateurEnCoursEdition = utilisateur;
        this.modeCreation = (utilisateur == null);
    }

    private void chargerOptionsRolesEtPersonnel() {
        choiceBoxRoleCreationUtilisateur.setItems(FXCollections.observableArrayList(RoleUtilisateur.values()));
        choiceBoxRoleCreationUtilisateur.setConverter(new StringConverter<RoleUtilisateur>() {
            @Override public String toString(RoleUtilisateur role) { return role == null ? "" : role.getDbValue() + " (" + role.getLibelleInterface() + ")"; }
            @Override public RoleUtilisateur fromString(String string) { return RoleUtilisateur.fromDbValue(string.split(" ")[0]); } // Simple parse
        });

        try {
            List<Personnel> personnelList = servicePersistance.trouverToutLePersonnel();
            choiceBoxPersonnelAssocieUtilisateur.setItems(FXCollections.observableArrayList(personnelList));
            choiceBoxPersonnelAssocieUtilisateur.setConverter(new StringConverter<Personnel>() {
                @Override public String toString(Personnel p) { return p == null ? "Aucun (Compte Système/Générique)" : p.getNomPersonnel() + " " + p.getPrenomPersonnel() + " (Mat: " + p.getMatricule() + ")"; }
                @Override public Personnel fromString(String string) { return null; }
            });
            choiceBoxPersonnelAssocieUtilisateur.getItems().add(0, null); // Option pour "Aucun"
            choiceBoxPersonnelAssocieUtilisateur.setValue(null);
        } catch (Exception e) {
            FORM_USER_LOGGER.log(Level.WARNING, "Impossible de charger la liste du personnel.", e);
        }
        FORM_USER_LOGGER.fine("Options des ChoiceBox (Rôle, Personnel) chargées.");
    }

    private void preRemplirChampsFormulaireUtilisateur() {
        if (utilisateurEnCoursEdition == null) return;
        champLoginCreationUtilisateur.setText(utilisateurEnCoursEdition.getLogin());
        champLoginCreationUtilisateur.setDisable(!modeCreation); // Login non modifiable après création

        choiceBoxRoleCreationUtilisateur.setValue(utilisateurEnCoursEdition.getRole());

        if (utilisateurEnCoursEdition.getIdPersonnel() != null) {
            Personnel pAssocie = servicePersistance.trouverPersonnelParId(utilisateurEnCoursEdition.getIdPersonnel());
            choiceBoxPersonnelAssocieUtilisateur.setValue(pAssocie);
        }
        FORM_USER_LOGGER.info("Champs du formulaire pré-remplis pour utilisateur ID: " + utilisateurEnCoursEdition.getId());
    }

    @FXML
    private void actionSauvegarderUtilisateur() {
        FORM_USER_LOGGER.fine("Tentative de sauvegarde de l'utilisateur.");
        if (!validerSaisiesFormulaireUtilisateur()) {
            FORM_USER_LOGGER.warning("Validation des saisies du formulaire utilisateur échouée.");
            return;
        }

        try {
            utilisateurEnCoursEdition.setLogin(champLoginCreationUtilisateur.getText().trim());
            utilisateurEnCoursEdition.setRole(choiceBoxRoleCreationUtilisateur.getValue());
            Personnel pSelectionne = choiceBoxPersonnelAssocieUtilisateur.getValue();
            utilisateurEnCoursEdition.setIdPersonnel(pSelectionne != null ? pSelectionne.getIdPersonnel() : null);
            // MFA Secret non géré dans ce formulaire simple

            if (modeCreation) {
                String motDePasse = champMotDePasseCreationUtilisateur.getText();
                // Le hashage se fait dans BusinessLogicService.creerNouvelUtilisateur
                serviceLogiqueMetier.creerNouvelUtilisateur(utilisateurEnCoursEdition, motDePasse);
                FORM_USER_LOGGER.info("Nouvel utilisateur créé : " + utilisateurEnCoursEdition.getLogin());
                afficherNotificationAlerteFormulaire("Création Réussie", "Le compte utilisateur a été créé.", Alert.AlertType.INFORMATION);
            } else {
                serviceLogiqueMetier.modifierUtilisateur(utilisateurEnCoursEdition); // Méthode à créer pour modifier rôle/personnel
                FORM_USER_LOGGER.info("Utilisateur ID " + utilisateurEnCoursEdition.getId() + " modifié.");
                afficherNotificationAlerteFormulaire("Modification Réussie", "Les informations de l'utilisateur ont été mises à jour.", Alert.AlertType.INFORMATION);
            }
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            FORM_USER_LOGGER.log(Level.WARNING, "Échec de la sauvegarde de l'utilisateur : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec Sauvegarde", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            FORM_USER_LOGGER.log(Level.SEVERE, "Erreur système lors de la sauvegarde de l'utilisateur.", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Erreur imprévue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validerSaisiesFormulaireUtilisateur() {
        StringBuilder messagesErreur = new StringBuilder();
        if (champLoginCreationUtilisateur.getText() == null || champLoginCreationUtilisateur.getText().trim().isEmpty()) {
            messagesErreur.append("Le login est requis.\n");
        }
        if (choiceBoxRoleCreationUtilisateur.getValue() == null) {
            messagesErreur.append("Le rôle est requis.\n");
        }
        if (modeCreation) {
            String mdp = champMotDePasseCreationUtilisateur.getText();
            String confirmMdp = champConfirmationMotDePasseUtilisateur.getText();
            if (mdp == null || mdp.isEmpty()) {
                messagesErreur.append("Le mot de passe est requis pour un nouveau compte.\n");
            } else if (!mdp.equals(confirmMdp)) {
                messagesErreur.append("Les mots de passe ne correspondent pas.\n");
            }
            // Ajouter des règles de complexité pour le mot de passe si nécessaire
        }

        if (messagesErreur.length() > 0) {
            afficherNotificationAlerteFormulaire("Erreurs de Validation", messagesErreur.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void actionAnnulerFormulaire() {
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerFormulaireUtilisateur.getScene().getWindow();
        stage.close();
        FORM_USER_LOGGER.info("Formulaire utilisateur fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) boutonAnnulerFormulaireUtilisateur.getScene().getWindow();
        if (stageProprietaire != null) alerte.initOwner(stageProprietaire);
        alerte.showAndWait();
    }
}