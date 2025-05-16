package main.java.com.miage.parcauto;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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

    private static final Logger CONTROLEUR_UTILISATEUR_LOGGER = Logger.getLogger(UserManagementPanelController.class.getName());
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
    @FXML private Button boutonModifierUtilisateur;
    @FXML private Button boutonSupprimerUtilisateur;
    @FXML private Button boutonReinitialiserMotDePasse;
    @FXML private Button boutonActualiserListeUtilisateurs;

    @FXML private TextField champRechercheLoginUtilisateur;
    @FXML private ChoiceBox<RoleUtilisateur> choiceBoxFiltreRoleUtilisateur;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_UTILISATEUR_LOGGER.fine("Dépendances services injectées dans UserManagementPanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_UTILISATEUR_LOGGER.info("Initialisation des données et de la configuration pour le panneau de gestion des utilisateurs.");
        configurerColonnesTableUtilisateurs();
        chargerOptionsFiltresUtilisateurs();
        configurerPermissionsActionsUtilisateurs();
        actionActualiserListeUtilisateurs();

        tableVueUtilisateurs.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            configurerEtatBoutonsContextuelsUtilisateurs(newSelection);
        });
    }

    private void configurerColonnesTableUtilisateurs() {
        colIdUtilisateurTable.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLoginUtilisateurTable.setCellValueFactory(new PropertyValueFactory<>("login"));
        colRoleUtilisateurTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getRole() != null ? cellData.getValue().getRole().getLibelleInterface() : "N/A"
        ));
        colPersonnelAssocieTable.setCellValueFactory(new PropertyValueFactory<>("nomPersonnelAssocie"));
        CONTROLEUR_UTILISATEUR_LOGGER.fine("Colonnes de la table des utilisateurs configurées.");
    }

    private void chargerOptionsFiltresUtilisateurs() {
        ObservableList<RoleUtilisateur> roles = FXCollections.observableArrayList(RoleUtilisateur.values());
        roles.add(0, null);
        choiceBoxFiltreRoleUtilisateur.setItems(roles);
        choiceBoxFiltreRoleUtilisateur.setConverter(new StringConverter<RoleUtilisateur>() {
            @Override public String toString(RoleUtilisateur role) { return role == null ? "Tous les rôles" : role.getLibelleInterface(); }
            @Override public RoleUtilisateur fromString(String string) {
                if ("Tous les rôles".equals(string) || string == null) return null;
                return Arrays.stream(RoleUtilisateur.values()).filter(r -> r.getLibelleInterface().equals(string)).findFirst().orElse(null);
            }
        });
        choiceBoxFiltreRoleUtilisateur.setValue(null);
        CONTROLEUR_UTILISATEUR_LOGGER.fine("Options des filtres pour la liste des utilisateurs chargées.");
    }

    private void configurerPermissionsActionsUtilisateurs() {
        RoleUtilisateur roleConnecte = SessionManager.obtenirRoleUtilisateurActuel();
        boolean estAdmin = roleConnecte == RoleUtilisateur.U4 && gestionnaireSecurite.estAutorise(roleConnecte, Permissions.UTILISATEUR_GERER_COMPTES);
        if (boutonCreerUtilisateur != null) boutonCreerUtilisateur.setDisable(!estAdmin);
        configurerEtatBoutonsContextuelsUtilisateurs(null);
    }

    private void configurerEtatBoutonsContextuelsUtilisateurs(UtilisateurDTO utilisateurSelectionne) {
        boolean aucuneSelection = utilisateurSelectionne == null;
        RoleUtilisateur roleConnecte = SessionManager.obtenirRoleUtilisateurActuel();
        boolean estAdmin = roleConnecte == RoleUtilisateur.U4 && gestionnaireSecurite.estAutorise(roleConnecte, Permissions.UTILISATEUR_GERER_COMPTES);

        if (boutonModifierUtilisateur != null) boutonModifierUtilisateur.setDisable(aucuneSelection || !estAdmin);
        if (boutonSupprimerUtilisateur != null) {
            boolean peutSupprimer = estAdmin && !aucuneSelection &&
                    (utilisateurSelectionne.getId() != SessionManager.obtenirUtilisateurActuel().getId());
            boutonSupprimerUtilisateur.setDisable(!peutSupprimer);
        }
        if (boutonReinitialiserMotDePasse != null) boutonReinitialiserMotDePasse.setDisable(aucuneSelection || !estAdmin);
    }

    @FXML
    public void actionActualiserListeUtilisateurs() {
        CONTROLEUR_UTILISATEUR_LOGGER.info("Tentative d'actualisation de la liste des utilisateurs.");
        try {
            String rechercheLogin = champRechercheLoginUtilisateur.getText();
            RoleUtilisateur filtreRole = choiceBoxFiltreRoleUtilisateur.getValue();

            List<Utilisateur> utilisateursModel = serviceLogiqueMetier.rechercherUtilisateursFiltres(rechercheLogin, filtreRole);
            List<UtilisateurDTO> utilisateursDto = DataMapper.convertirVersListeDeUtilisateurDTO(utilisateursModel, servicePersistance);
            tableVueUtilisateurs.setItems(FXCollections.observableArrayList(utilisateursDto));
            tableVueUtilisateurs.refresh();
            configurerEtatBoutonsContextuelsUtilisateurs(null);
            CONTROLEUR_UTILISATEUR_LOGGER.info(utilisateursDto.size() + " utilisateurs chargés et affichés.");
        } catch (Exception e) {
            CONTROLEUR_UTILISATEUR_LOGGER.log(Level.SEVERE, "Erreur lors de l'actualisation des utilisateurs.", e);
            afficherNotificationAlerteInterface("Erreur de Données", "Impossible de charger la liste des utilisateurs : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulaireCreationUtilisateur() {
        CONTROLEUR_UTILISATEUR_LOGGER.fine("Action utilisateur : ouvrir formulaire de création d'utilisateur.");
        ouvrirDialogueFormulaireUtilisateur(null);
    }

    @FXML
    private void actionOuvrirFormulaireModificationUtilisateur() {
        UtilisateurDTO utilisateurSelectionneDto = tableVueUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurSelectionneDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un utilisateur à modifier.", Alert.AlertType.INFORMATION);
            return;
        }
        CONTROLEUR_UTILISATEUR_LOGGER.fine("Action utilisateur : ouvrir formulaire de modification pour utilisateur ID: " + utilisateurSelectionneDto.getId());
        Utilisateur utilisateurModel = servicePersistance.trouverUtilisateurParId(utilisateurSelectionneDto.getId());
        if (utilisateurModel == null){
            afficherNotificationAlerteInterface("Erreur Donnée Introuvable", "L'utilisateur sélectionné (ID: "+utilisateurSelectionneDto.getId()+") n'a pas pu être retrouvé.", Alert.AlertType.ERROR);
            actionActualiserListeUtilisateurs();
            return;
        }
        ouvrirDialogueFormulaireUtilisateur(utilisateurModel);
    }

    private void ouvrirDialogueFormulaireUtilisateur(Utilisateur utilisateurAEditer) {
        try {
            String titreDialogue = (utilisateurAEditer == null) ? "Créer un Nouvel Utilisateur" : "Modifier l'Utilisateur : " + utilisateurAEditer.getLogin();
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireUtilisateurView.fxml";
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
            CONTROLEUR_UTILISATEUR_LOGGER.info("Dialogue du formulaire utilisateur fermé.");

        } catch (IOException e) {
            CONTROLEUR_UTILISATEUR_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture du formulaire utilisateur.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionSupprimerUtilisateurSelectionne() {
        UtilisateurDTO utilisateurSelectionne = tableVueUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un utilisateur à supprimer.", Alert.AlertType.INFORMATION);
            return;
        }
        if (utilisateurSelectionne.getId() == SessionManager.obtenirUtilisateurActuel().getId()) {
            afficherNotificationAlerteInterface("Action Interdite", "Vous ne pouvez pas supprimer votre propre compte utilisateur.", Alert.AlertType.WARNING);
            return;
        }

        Optional<ButtonType> reponse = afficherDialogueConfirmationInterface("Confirmation de Suppression",
                "Êtes-vous sûr de vouloir supprimer l'utilisateur '" + utilisateurSelectionne.getLogin() + "' ? Cette action est irréversible.", ButtonType.YES, ButtonType.NO);
        if (reponse.isPresent() && reponse.get() == ButtonType.YES) {
            try {
                serviceLogiqueMetier.supprimerUtilisateur(utilisateurSelectionne.getId());
                actionActualiserListeUtilisateurs();
                afficherNotificationAlerteInterface("Suppression Réussie", "L'utilisateur a été supprimé.", Alert.AlertType.INFORMATION);
            } catch (ErreurLogiqueMetier | ErreurBaseDeDonnees e) {
                afficherNotificationAlerteInterface("Échec de Suppression", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void actionReinitialiserMotDePasseUtilisateur() {
        UtilisateurDTO utilisateurSelectionne = tableVueUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un utilisateur pour réinitialiser son mot de passe.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<String> dialogueNouveauMdp = new Dialog<>();
        dialogueNouveauMdp.setTitle("Réinitialiser Mot de Passe");
        dialogueNouveauMdp.setHeaderText("Nouveau mot de passe pour l'utilisateur : " + utilisateurSelectionne.getLogin());
        Stage stageProprietaire = MainApp.getPrimaryStage();
        if (stageProprietaire != null) dialogueNouveauMdp.initOwner(stageProprietaire);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        PasswordField champNouveauMdp = new PasswordField();
        champNouveauMdp.setPromptText("Nouveau mot de passe");
        PasswordField champConfirmationMdp = new PasswordField();
        champConfirmationMdp.setPromptText("Confirmer le mot de passe");

        grille.add(new Label("Nouveau mot de passe:"), 0, 0);
        grille.add(champNouveauMdp, 1, 0);
        grille.add(new Label("Confirmer:"), 0, 1);
        grille.add(champConfirmationMdp, 1, 1);
        dialogueNouveauMdp.getDialogPane().setContent(grille);

        ButtonType boutonConfirmerReinit = new ButtonType("Réinitialiser", ButtonBar.ButtonData.OK_DONE);
        dialogueNouveauMdp.getDialogPane().getButtonTypes().addAll(boutonConfirmerReinit, ButtonType.CANCEL);

        final Button boutonOk = (Button) dialogueNouveauMdp.getDialogPane().lookupButton(boutonConfirmerReinit);
        boutonOk.setDisable(true);

        Runnable validerEtActiverBouton = () -> {
            String mdp1 = champNouveauMdp.getText();
            String mdp2 = champConfirmationMdp.getText();
            boutonOk.setDisable(mdp1.trim().isEmpty() || !mdp1.equals(mdp2));
        };
        champNouveauMdp.textProperty().addListener((observable, oldValue, newValue) -> validerEtActiverBouton.run());
        champConfirmationMdp.textProperty().addListener((observable, oldValue, newValue) -> validerEtActiverBouton.run());


        dialogueNouveauMdp.setResultConverter(typeBouton -> {
            if (typeBouton == boutonConfirmerReinit) {
                String mdp1 = champNouveauMdp.getText();
                // La validation est déjà faite par le listener du bouton, mais on peut la redoubler ici par sécurité
                if (mdp1.isEmpty() || !mdp1.equals(champConfirmationMdp.getText())) {
                    afficherNotificationAlerteInterface("Erreur de Saisie", "Les mots de passe ne correspondent pas ou sont vides.", Alert.AlertType.WARNING);
                    return null;
                }
                return mdp1;
            }
            return null;
        });

        Optional<String> resultat = dialogueNouveauMdp.showAndWait();
        resultat.ifPresent(nouveauMdp -> {
            try {
                serviceLogiqueMetier.reinitialiserMotDePasseUtilisateur(utilisateurSelectionne.getId(), nouveauMdp);
                afficherNotificationAlerteInterface("Mot de Passe Réinitialisé", "Le mot de passe pour " + utilisateurSelectionne.getLogin() + " a été mis à jour.", Alert.AlertType.INFORMATION);
            } catch (ErreurValidation | ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec Réinitialisation", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
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

    private Optional<ButtonType> afficherDialogueConfirmationInterface(String titre, String message, ButtonType... buttonTypes) {
        Alert dialogue = new Alert(Alert.AlertType.CONFIRMATION);
        dialogue.setTitle(titre);
        dialogue.setHeaderText(null);
        dialogue.setContentText(message);
        if (buttonTypes != null && buttonTypes.length > 0) {
            dialogue.getButtonTypes().setAll(buttonTypes);
        }
        Stage stagePrincipal = MainApp.getPrimaryStage();
        if (stagePrincipal != null && stagePrincipal.getScene() != null && stagePrincipal.isShowing()) {
            dialogue.initOwner(stagePrincipal);
        }
        return dialogue.showAndWait();
    }
}