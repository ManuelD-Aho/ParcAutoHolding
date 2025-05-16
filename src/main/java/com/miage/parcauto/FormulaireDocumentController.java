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
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormulaireDocumentController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_FORM_DOC_LOGGER = Logger.getLogger(FormulaireDocumentController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private ChoiceBox<SocietaireCompte> choiceBoxSocietaireDocForm;
    @FXML private ChoiceBox<TypeDocumentSocietaire> choiceBoxTypeDocForm;
    @FXML private TextField champCheminFichierDocForm;
    @FXML private Button boutonParcourirFichierDocForm;
    @FXML private Button boutonEnregistrerDocForm;
    @FXML private Button boutonAnnulerDocForm;
    @FXML private Label labelTitreFormulaireDoc;

    private File fichierSelectionnePourUpload;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_FORM_DOC_LOGGER.fine("Dépendances services injectées.");
    }

    @Override
    public void initialiserDonneesVue() {
        // Cette méthode est appelée via l'interface InitializableServices mais n'est pas
        // pertinente ici car la préparation se fait dans preparerFormulaire.
    }

    public void preparerFormulaire() {
        labelTitreFormulaireDoc.setText("Téléverser un Nouveau Document");
        chargerSocietairesPourFormulaire();
        chargerTypesDocumentPourFormulaire();
        champCheminFichierDocForm.setEditable(false);
        fichierSelectionnePourUpload = null;
    }

    private void chargerSocietairesPourFormulaire() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        if (gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS)) { // Admin peut choisir sociétaire
            try {
                List<SocietaireCompte> societaires = servicePersistance.trouverTousLesSocietairesComptes();
                choiceBoxSocietaireDocForm.setItems(FXCollections.observableArrayList(societaires));
                choiceBoxSocietaireDocForm.setConverter(new StringConverter<SocietaireCompte>() {
                    @Override public String toString(SocietaireCompte sc) { return sc == null ? "Sélectionner..." : sc.getNom() + " (N°" + sc.getNumero() + ")";}
                    @Override public SocietaireCompte fromString(String string) { return null; }
                });
                if (!societaires.isEmpty()) choiceBoxSocietaireDocForm.setValue(societaires.get(0));
            } catch (Exception e) {
                CONTROLEUR_FORM_DOC_LOGGER.log(Level.WARNING, "Impossible de charger la liste des sociétaires pour le formulaire.", e);
                afficherNotificationAlerteInterface("Erreur Données", "Liste des sociétaires indisponible.", Alert.AlertType.WARNING);
            }
        } else { // U3 (Sociétaire) : le champ est pré-rempli et désactivé
            Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
            if (idPersonnelConnecte != null) {
                SocietaireCompte scConnecte = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                if (scConnecte != null) {
                    choiceBoxSocietaireDocForm.setItems(FXCollections.observableArrayList(scConnecte));
                    choiceBoxSocietaireDocForm.setValue(scConnecte);
                    choiceBoxSocietaireDocForm.setDisable(true);
                } else {
                    choiceBoxSocietaireDocForm.setDisable(true);
                    afficherNotificationAlerteInterface("Erreur Compte", "Aucun compte sociétaire lié à votre profil.", Alert.AlertType.ERROR);
                }
            } else {
                choiceBoxSocietaireDocForm.setDisable(true);
                afficherNotificationAlerteInterface("Erreur Utilisateur", "Profil utilisateur non sociétaire.", Alert.AlertType.ERROR);
            }
        }
    }

    private void chargerTypesDocumentPourFormulaire() {
        choiceBoxTypeDocForm.setItems(FXCollections.observableArrayList(TypeDocumentSocietaire.values()));
        choiceBoxTypeDocForm.setConverter(new StringConverter<TypeDocumentSocietaire>() {
            @Override public String toString(TypeDocumentSocietaire td) { return td == null ? "Sélectionner..." : td.getDbValue(); }
            @Override public TypeDocumentSocietaire fromString(String string) { return TypeDocumentSocietaire.fromDbValue(string); }
        });
        if (choiceBoxTypeDocForm.getItems().size() > 0) {
            choiceBoxTypeDocForm.setValue(choiceBoxTypeDocForm.getItems().get(0));
        }
    }

    @FXML
    private void actionParcourirFichier() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le document à uploader");
        // Ajouter des filtres d'extension si souhaité
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File fichier = fileChooser.showOpenDialog(boutonParcourirFichierDocForm.getScene().getWindow());
        if (fichier != null) {
            fichierSelectionnePourUpload = fichier;
            champCheminFichierDocForm.setText(fichier.getAbsolutePath());
        }
    }

    @FXML
    private void actionEnregistrerDocument() {
        SocietaireCompte societaireSelectionne = choiceBoxSocietaireDocForm.getValue();
        TypeDocumentSocietaire typeSelectionne = choiceBoxTypeDocForm.getValue();

        if (societaireSelectionne == null) {
            afficherNotificationAlerteInterface("Validation Formulaire", "Veuillez sélectionner un sociétaire.", Alert.AlertType.WARNING);
            return;
        }
        if (typeSelectionne == null) {
            afficherNotificationAlerteInterface("Validation Formulaire", "Veuillez sélectionner un type de document.", Alert.AlertType.WARNING);
            return;
        }
        if (fichierSelectionnePourUpload == null || !fichierSelectionnePourUpload.exists()) {
            afficherNotificationAlerteInterface("Validation Formulaire", "Veuillez sélectionner un fichier valide à uploader.", Alert.AlertType.WARNING);
            return;
        }

        try {
            serviceLogiqueMetier.televerserDocumentSocietaire(
                    societaireSelectionne.getIdSocietaire(),
                    typeSelectionne,
                    fichierSelectionnePourUpload.toPath(),
                    fichierSelectionnePourUpload.getName()
            );
            afficherNotificationAlerteInterface("Upload Réussi", "Le document '" + fichierSelectionnePourUpload.getName() + "' a été téléversé avec succès pour " + societaireSelectionne.getNom() + ".", Alert.AlertType.INFORMATION);
            actionAnnulerEtFermer();
        } catch (ErreurValidation | ErreurLogiqueMetier | IOException e) {
            CONTROLEUR_FORM_DOC_LOGGER.log(Level.SEVERE, "Échec de l'upload du document.", e);
            afficherNotificationAlerteInterface("Échec de l'Upload", "Impossible d'enregistrer le document : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionAnnulerEtFermer() {
        Stage stage = (Stage) boutonAnnulerDocForm.getScene().getWindow();
        stage.close();
    }

    private void afficherNotificationAlerteInterface(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        // Assurer que la boîte de dialogue est modale à la fenêtre du formulaire si possible
        Stage stageProprietaire = (Stage) champCheminFichierDocForm.getScene().getWindow();
        if (stageProprietaire != null && stageProprietaire.isShowing()) {
            alerte.initOwner(stageProprietaire);
        }
        alerte.showAndWait();
    }
}