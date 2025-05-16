package main.java.com.miage.parcauto;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.DocumentSocietaire;
import main.java.com.miage.parcauto.AppModels.SocietaireCompte;
import main.java.com.miage.parcauto.AppModels.TypeDocumentSocietaire;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormulaireDocumentController implements ViewController.InitializableServices {
    private static final Logger FORM_DOC_LOGGER = Logger.getLogger(FormulaireDocumentController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private PersistenceService servicePersistance;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;

    private File fichierSelectionnePourUpload;

    @FXML private ChoiceBox<SocietaireCompte> choiceBoxSocietairePourDocument; // Visible si admin
    @FXML private Label labelSocietairePourDocument; // Visible si U3 (non admin)
    @FXML private ChoiceBox<TypeDocumentSocietaire> choiceBoxTypeDocumentUpload;
    @FXML private TextField champCheminFichierSelectionne;
    @FXML private Button boutonParcourirFichier;
    @FXML private Button boutonConfirmerUploadDocument;
    @FXML private Button boutonAnnulerUpload;
    @FXML private Label etiquetteTitreFormulaireUpload;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_DOC_LOGGER.fine("Dépendances services injectées dans FormulaireDocumentController.");
    }

    @Override
    public void initialiserDonneesVue() {
        // La préparation se fait dans preparerFormulaire
    }

    public void preparerFormulaire() {
        FORM_DOC_LOGGER.info("Initialisation des données pour le formulaire d'upload de document.");
        chargerOptionsTypesDocument();
        configurerVisibiliteChoixSocietaire();
    }

    private void chargerOptionsTypesDocument() {
        choiceBoxTypeDocumentUpload.setItems(FXCollections.observableArrayList(TypeDocumentSocietaire.values()));
        choiceBoxTypeDocumentUpload.setConverter(new StringConverter<TypeDocumentSocietaire>() {
            @Override public String toString(TypeDocumentSocietaire type) { return type == null ? "" : type.getDbValue(); }
            @Override public TypeDocumentSocietaire fromString(String string) { return TypeDocumentSocietaire.fromDbValue(string); }
        });
        if (choiceBoxTypeDocumentUpload.getItems().size() > 0) {
            choiceBoxTypeDocumentUpload.setValue(choiceBoxTypeDocumentUpload.getItems().get(0));
        }
        FORM_DOC_LOGGER.fine("Types de documents chargés pour la sélection.");
    }

    private void configurerVisibiliteChoixSocietaire() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean estAdminOuEquivalent = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS); // Admin peut uploader pour d'autres

        if (estAdminOuEquivalent) {
            choiceBoxSocietairePourDocument.setVisible(true);
            choiceBoxSocietairePourDocument.setManaged(true);
            labelSocietairePourDocument.setVisible(false);
            labelSocietairePourDocument.setManaged(false);
            try {
                List<SocietaireCompte> societaires = servicePersistance.trouverTousLesSocietairesComptes();
                choiceBoxSocietairePourDocument.setItems(FXCollections.observableArrayList(societaires));
                choiceBoxSocietairePourDocument.setConverter(new StringConverter<SocietaireCompte>() {
                    @Override public String toString(SocietaireCompte sc) { return sc == null ? "" : sc.getNom() + " (N°" + sc.getNumero() + ")"; }
                    @Override public SocietaireCompte fromString(String string) { return null; }
                });
            } catch (Exception e) {
                FORM_DOC_LOGGER.log(Level.WARNING, "Impossible de charger la liste des sociétaires pour l'upload (admin).", e);
                afficherNotificationAlerteFormulaire("Erreur Chargement", "Liste des sociétaires indisponible.", Alert.AlertType.ERROR);
            }
        } else { // Cas U3 qui upload pour lui-même
            choiceBoxSocietairePourDocument.setVisible(false);
            choiceBoxSocietairePourDocument.setManaged(false);
            labelSocietairePourDocument.setVisible(true);
            labelSocietairePourDocument.setManaged(true);
            Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
            if (idPersonnelConnecte != null) {
                SocietaireCompte sc = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                if (sc != null) labelSocietairePourDocument.setText("Sociétaire : " + sc.getNom());
                else {
                    labelSocietairePourDocument.setText("Aucun compte sociétaire associé à votre profil.");
                    boutonConfirmerUploadDocument.setDisable(true); // Empêcher l'upload
                }
            } else {
                labelSocietairePourDocument.setText("Profil utilisateur non lié à un personnel.");
                boutonConfirmerUploadDocument.setDisable(true);
            }
        }
    }

    @FXML
    private void actionParcourirFichierPourUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un document à uploader");
        // Ajouter des filtres d'extension si nécessaire
        // fileChooser.getExtensionFilters().addAll(
        //    new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
        //    new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg")
        // );
        File fichier = fileChooser.showOpenDialog(MainApp.getPrimaryStage());
        if (fichier != null) {
            this.fichierSelectionnePourUpload = fichier;
            champCheminFichierSelectionne.setText(fichier.getAbsolutePath());
            FORM_DOC_LOGGER.info("Fichier sélectionné pour upload : " + fichier.getAbsolutePath());
        } else {
            this.fichierSelectionnePourUpload = null;
            champCheminFichierSelectionne.clear();
            FORM_DOC_LOGGER.info("Sélection de fichier annulée par l'utilisateur.");
        }
    }

    @FXML
    private void actionConfirmerUploadDocument() {
        FORM_DOC_LOGGER.fine("Tentative de confirmation de l'upload du document.");
        if (!validerSaisiesFormulaireUpload()) {
            FORM_DOC_LOGGER.warning("Validation des saisies du formulaire d'upload échouée.");
            return;
        }

        try {
            int idSocietaireConcerne;
            RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
            boolean estAdminOuEquivalent = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS);

            if (estAdminOuEquivalent) {
                if (choiceBoxSocietairePourDocument.getValue() == null) throw new ErreurValidation("Un sociétaire doit être sélectionné par l'administrateur.");
                idSocietaireConcerne = choiceBoxSocietairePourDocument.getValue().getIdSocietaire();
            } else {
                Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
                if (idPersonnelConnecte == null) throw new ErreurLogiqueMetier("Utilisateur non lié à un personnel.");
                SocietaireCompte sc = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                if (sc == null) throw new ErreurLogiqueMetier("Aucun compte sociétaire associé à cet utilisateur.");
                idSocietaireConcerne = sc.getIdSocietaire();
            }

            TypeDocumentSocietaire typeDoc = choiceBoxTypeDocumentUpload.getValue();

            serviceLogiqueMetier.enregistrerNouveauDocumentSocietaire(
                    idSocietaireConcerne,
                    typeDoc,
                    fichierSelectionnePourUpload
            );

            FORM_DOC_LOGGER.info("Document uploadé avec succès pour sociétaire ID: " + idSocietaireConcerne);
            afficherNotificationAlerteFormulaire("Upload Réussi", "Le document a été uploadé et enregistré avec succès.", Alert.AlertType.INFORMATION);
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier | IOException e) {
            FORM_DOC_LOGGER.log(Level.WARNING, "Échec de l'upload du document : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec de l'Upload", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            FORM_DOC_LOGGER.log(Level.SEVERE, "Erreur système inattendue lors de l'upload du document.", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Une erreur imprévue est survenue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validerSaisiesFormulaireUpload() {
        StringBuilder messagesErreur = new StringBuilder();
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean estAdminOuEquivalent = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS);

        if (estAdminOuEquivalent && choiceBoxSocietairePourDocument.getValue() == null) {
            messagesErreur.append("Veuillez sélectionner un sociétaire pour ce document.\n");
        }
        if (choiceBoxTypeDocumentUpload.getValue() == null) {
            messagesErreur.append("Le type de document est requis.\n");
        }
        if (fichierSelectionnePourUpload == null || !fichierSelectionnePourUpload.exists()) {
            messagesErreur.append("Veuillez sélectionner un fichier valide à uploader.\n");
        }

        if (messagesErreur.length() > 0) {
            afficherNotificationAlerteFormulaire("Erreurs de Validation", messagesErreur.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void actionAnnulerUpload() {
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerUpload.getScene().getWindow();
        stage.close();
        FORM_DOC_LOGGER.info("Formulaire d'upload de document fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) boutonAnnulerUpload.getScene().getWindow();
        if (stageProprietaire != null) alerte.initOwner(stageProprietaire);
        alerte.showAndWait();
    }
}