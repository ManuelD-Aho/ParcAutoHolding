package main.java.com.miage.parcauto;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.Utilisateur;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppModels.Personnel;
import main.java.com.miage.parcauto.AppDataTransferObjects.UtilisateurDTO;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserManagementPanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_USER_LOGGER = Logger.getLogger(UserManagementPanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private TableView<UtilisateurDTO> tableVueUtilisateurs;
    @FXML private TableColumn<UtilisateurDTO, Integer> colIdUtilisateurTable;
    @FXML private TableColumn<UtilisateurDTO, String> colLoginUtilisateurTable;
    @FXML private TableColumn<UtilisateurDTO, String> colRoleUtilisateurTable;
    @FXML private TableColumn<UtilisateurDTO, String> colPersonnelAssocieTable;

    @FXML private Button boutonCreerUtilisateur;
    @FXML private Button boutonModifierUtilisateur; // Pour changer rôle, personnel associé
    @FXML private Button boutonReinitialiserMotDePasse;
    @FXML private Button boutonSupprimerUtilisateur;
    @FXML private Button boutonActualiserListeUtilisateurs;

    @FXML private TextField champRechercheLoginUtilisateur;
    @FXML private ChoiceBox<RoleUtilisateur> choiceBoxFiltreRoleUtilisateur;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_USER_LOGGER.fine("Dépendances services injectées dans UserManagementPanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_USER_LOGGER.info("Initialisation du panneau de gestion des utilisateurs.");
        // Vérification de sécurité pour l'accès même à ce panneau
        if (!gestionnaireSecurite.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.UTILISATEUR_GERER_COMPTES)) {
            afficherNotificationAlerteInterface("Accès Interdit", "Vous n'avez pas les droits nécessaires pour accéder à la gestion des utilisateurs.", Alert.AlertType.ERROR);
            // Masquer le contenu ou le désactiver complètement
            if (tableVueUtilisateurs != null && tableVueUtilisateurs.getParent() != null) {
                tableVueUtilisateurs.getParent().setVisible(false);
            }
            return;
        }

        configurerColonnesTableUtilisateurs();
        chargerOptionsFiltresUtilisateurs();
        configurerPermissionsActionsUtilisateurs(); // Les boutons sont déjà limités par l'accès au panel
        actionActualiserListeUtilisateurs();

        tableVueUtilisateurs.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            configurerEtatBoutonsContextuelsUtilisateurs(newSelection);
        });
    }

    private void configurerColonnesTableUtilisateurs() {
        colIdUtilisateurTable.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLoginUtilisateurTable.setCellValueFactory(new PropertyValueFactory<>("login"));
        colRoleUtilisateurTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getRole() != null ? cellData.getValue().getRole().getDbValue() : "N/A"
        ));
        colPersonnelAssocieTable.setCellValueFactory(new PropertyValueFactory<>("nomPersonnelAssocie"));
        CONTROLEUR_USER_LOGGER.fine("Colonnes de la table des utilisateurs configurées.");
    }

    private void chargerOptionsFiltresUtilisateurs() {
        ObservableList<RoleUtilisateur> roles = FXCollections.observableArrayList(RoleUtilisateur.values());
        roles.add(0, null); // Option "Tous les rôles"
        choiceBoxFiltreRoleUtilisateur.setItems(roles);
        choiceBoxFiltreRoleUtilisateur.setConverter(new StringConverter<RoleUtilisateur>() {
            @Override public String toString(RoleUtilisateur role) { return role == null ? "Tous les rôles" : role.getDbValue(); }
            @Override public RoleUtilisateur fromString(String string) { return "Tous les rôles".equals(string) ? null : RoleUtilisateur.fromDbValue(string); }
        });
        choiceBoxFiltreRoleUtilisateur.setValue(null);
        CONTROLEUR_USER_LOGGER.fine("Options du filtre par rôle utilisateur chargées.");
    }

    private void configurerPermissionsActionsUtilisateurs() {
        // Les permissions granulaires (créer, modifier, supprimer user) sont vérifiées ici pour activer/désactiver les boutons
        RoleUtilisateur roleActuel = SessionManager.obtenirRoleUtilisateurActuel();
        if (boutonCreerUtilisateur != null) boutonCreerUtilisateur.setDisable(!gestionnaireSecurite.estAutorise(roleActuel, Permissions.UTILISATEUR_CREER_COMPTE));
        // Les autres sont contextuels et vérifiés dans configurerEtatBoutonsContextuelsUtilisateurs
    }

    private void configurerEtatBoutonsContextuelsUtilisateurs(UtilisateurDTO utilisateurSelectionne) {
        boolean aucuneSelection = utilisateurSelectionne == null;
        RoleUtilisateur roleActuel = SessionManager.obtenirRoleUtilisateurActuel();

        if (boutonModifierUtilisateur != null) {
            boutonModifierUtilisateur.setDisable(aucuneSelection || !gestionnaireSecurite.estAutorise(roleActuel, Permissions.UTILISATEUR_MODIFIER_COMPTE));
        }
        if (boutonReinitialiserMotDePasse != null) {
            boutonReinitialiserMotDePasse.setDisable(aucuneSelection || !gestionnaireSecurite.estAutorise(roleActuel, Permissions.UTILISATEUR_MODIFIER_COMPTE)); // Même permission que modifier pour simplifier
        }
        if (boutonSupprimerUtilisateur != null) {
            boolean peutSupprimer = !aucuneSelection && gestionnaireSecurite.estAutorise(roleActuel, Permissions.UTILISATEUR_SUPPRIMER_COMPTE);
            // Empêcher la suppression de son propre compte admin
            if (peutSupprimer && utilisateurSelectionne.getId() == SessionManager.obtenirUtilisateurActuel().getId()) {
                peutSupprimer = false;
            }
            boutonSupprimerUtilisateur.setDisable(!peutSupprimer);
        }
    }

    @FXML
    public void actionActualiserListeUtilisateurs() {
        CONTROLEUR_USER_LOGGER.info("Actualisation de la liste des utilisateurs.");
        try {
            String loginRecherche = champRechercheLoginUtilisateur.getText();
            RoleUtilisateur roleFiltre = choiceBoxFiltreRoleUtilisateur.getValue();

            List<Utilisateur> utilisateursModel = serviceLogiqueMetier.rechercherUtilisateurs(loginRecherche, roleFiltre);
            List<UtilisateurDTO> utilisateursDto = DataMapper.convertirVersListeDeUtilisateurDTO(utilisateursModel, servicePersistance);
            tableVueUtilisateurs.setItems(FXCollections.observableArrayList(utilisateursDto));
            tableVueUtilisateurs.refresh();
            configurerEtatBoutonsContextuelsUtilisateurs(null);
            CONTROLEUR_USER_LOGGER.info(utilisateursDto.size() + " utilisateurs chargés.");
        } catch (Exception e) {
            CONTROLEUR_USER_LOGGER.log(Level.SEVERE, "Erreur lors de l'actualisation des utilisateurs.", e);
            afficherNotificationAlerteInterface("Erreur Données Utilisateurs", "Impossible de charger la liste : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulaireCreationUtilisateur() {
        CONTROLEUR_USER_LOGGER.fine("Action : ouvrir formulaire de création d'utilisateur.");
        ouvrirDialogueFormulaireUtilisateur(null);
    }

    @FXML
    private void actionOuvrirFormulaireModificationUtilisateur() {
        UtilisateurDTO utilisateurSelectionneDto = tableVueUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurSelectionneDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un utilisateur à modifier.", Alert.AlertType.INFORMATION);
            return;
        }
        CONTROLEUR_USER_LOGGER.fine("Action : ouvrir formulaire de modification pour utilisateur ID: " + utilisateurSelectionneDto.getId());
        Utilisateur utilisateurModel = servicePersistance.trouverUtilisateurParId(utilisateurSelectionneDto.getId());
        if (utilisateurModel == null){
            afficherNotificationAlerteInterface("Donnée Introuvable", "L'utilisateur sélectionné (ID: "+utilisateurSelectionneDto.getId()+") n'a pas été retrouvé.", Alert.AlertType.ERROR);
            actionActualiserListeUtilisateurs();
            return;
        }
        ouvrirDialogueFormulaireUtilisateur(utilisateurModel);
    }

    private void ouvrirDialogueFormulaireUtilisateur(Utilisateur utilisateurAEditer) {
        try {
            String titreDialogue = (utilisateurAEditer == null) ? "Créer un Nouveau Compte Utilisateur" : "Modifier l'Utilisateur : " + utilisateurAEditer.getLogin();
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireUtilisateurView.fxml"; // FXML à créer
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire utilisateur introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            FormulaireUtilisateurController controleurFormulaire = chargeur.getController();
            controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            controleurFormulaire.preparerFormulairePourEdition(utilisateurAEditer);

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();
            actionActualiserListeUtilisateurs();
            CONTROLEUR_USER_LOGGER.info("Dialogue du formulaire utilisateur fermé.");

        } catch (IOException e) {
            CONTROLEUR_USER_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture du formulaire utilisateur.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionReinitialiserMotDePasseUtilisateur() {
        UtilisateurDTO utilisateurSelectionne = tableVueUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un utilisateur pour réinitialiser son mot de passe.", Alert.AlertType.INFORMATION);
            return;
        }

        TextInputDialog dialogueNouveauMotDePasse = new TextInputDialog();
        dialogueNouveauMotDePasse.setTitle("Réinitialisation du Mot de Passe");
        dialogueNouveauMotDePasse.setHeaderText("Entrez le nouveau mot de passe pour l'utilisateur : " + utilisateurSelectionne.getLogin());
        dialogueNouveauMotDePasse.setContentText("Nouveau mot de passe :");
        Stage stageProprietaire = MainApp.getPrimaryStage();
        if (stageProprietaire != null) dialogueNouveauMotDePasse.initOwner(stageProprietaire);

        Optional<String> resultat = dialogueNouveauMotDePasse.showAndWait();
        resultat.ifPresent(nouveauMdp -> {
            if (nouveauMdp.trim().isEmpty()) {
                afficherNotificationAlerteInterface("Mot de Passe Vide", "Le nouveau mot de passe ne peut pas être vide.", Alert.AlertType.WARNING);
                return;
            }
            try {
                serviceLogiqueMetier.modifierMotDePasseUtilisateur(utilisateurSelectionne.getId(), nouveauMdp);
                afficherNotificationAlerteInterface("Mot de Passe Réinitialisé", "Le mot de passe pour " + utilisateurSelectionne.getLogin() + " a été mis à jour.", Alert.AlertType.INFORMATION);
            } catch (ErreurValidation | ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec Réinitialisation", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void actionSupprimerUtilisateurSelectionne() {
        UtilisateurDTO utilisateurSelectionne = tableVueUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un utilisateur à supprimer.", Alert.AlertType.INFORMATION);
            return;
        }
        if (utilisateurSelectionne.getId() == SessionManager.obtenirUtilisateurActuel().getId()) {
            afficherNotificationAlerteInterface("Action Interdite", "Vous ne pouvez pas supprimer votre propre compte administrateur.", Alert.AlertType.WARNING);
            return;
        }

        Optional<ButtonType> reponse = afficherDialogueConfirmationInterface("Confirmation de Suppression",
                "Êtes-vous sûr de vouloir supprimer l'utilisateur '" + utilisateurSelectionne.getLogin() + "' ? Cette action est irréversible.");
        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            try {
                serviceLogiqueMetier.supprimerUtilisateur(utilisateurSelectionne.getId()); // Méthode à créer
                actionActualiserListeUtilisateurs();
                afficherNotificationAlerteInterface("Suppression Réussie", "L'utilisateur a été supprimé.", Alert.AlertType.INFORMATION);
            } catch (ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec de Suppression", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void afficherNotificationAlerteInterface(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stagePrincipal = MainApp.getPrimaryStage();
        if (stagePrincipal != null && stagePrincipal.getScene() != null && stagePrincipal.isShowing()) {
            alerte.initOwner(stagePrincipal);
        }
        alerte.showAndWait();
    }

    private Optional<ButtonType> afficherDialogueConfirmationInterface(String titre, String message) {
        Alert dialogue = new Alert(Alert.AlertType.CONFIRMATION);
        dialogue.setTitle(titre);
        dialogue.setHeaderText(null);
        dialogue.setContentText(message);
        Stage stagePrincipal = MainApp.getPrimaryStage();
        if (stagePrincipal != null && stagePrincipal.getScene() != null && stagePrincipal.isShowing()) {
            dialogue.initOwner(stagePrincipal);
        }
        return dialogue.showAndWait();
    }

    // Classe interne TextInputDialog si non disponible ou pour personnalisation
    // JavaFX fournit javafx.scene.control.TextInputDialog
    private static class TextInputDialog extends Dialog<String> {
        private PasswordField champMotDePasse; // Utiliser PasswordField pour la saisie de MDP

        public TextInputDialog() {
            this("");
        }

        public TextInputDialog(String defaultValue) {
            final DialogPane dialogPane = getDialogPane();

            this.champMotDePasse = new PasswordField();
            this.champMotDePasse.setText(defaultValue);

            dialogPane.setContentText("Nouveau mot de passe:"); // Ce texte n'est pas utilisé si setContent est appelé

            GridPane content = new GridPane();
            content.setHgap(10);
            content.setVgap(5);
            content.add(new Label("Nouveau mot de passe:"),0,0);
            content.add(this.champMotDePasse,1,0);
            dialogPane.setContent(content);


            setTitle("Saisie");
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Platform.runLater(() -> champMotDePasse.requestFocus());

            setResultConverter(dialogButton -> {
                ButtonType. όταν(dialogButton.getButtonData()).isEqualTo(ButtonBar.ButtonData.OK_DONE);
                if (dialogButton == ButtonType.OK) {
                    return champMotDePasse.getText();
                }
                return null;
            });
        }
    }
}