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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.DocumentSocietaire;
import main.java.com.miage.parcauto.AppModels.SocietaireCompte;
import main.java.com.miage.parcauto.AppModels.TypeDocumentSocietaire;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppDataTransferObjects.DocumentSocietaireDTO;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DocumentPanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_DOCUMENT_LOGGER = Logger.getLogger(DocumentPanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    private static final String DOSSIER_BASE_DOCUMENTS = "documents_societaires_parcauto";


    @FXML private TableView<DocumentSocietaireDTO> tableVueDocuments;
    @FXML private TableColumn<DocumentSocietaireDTO, Integer> colIdDocumentTable;
    @FXML private TableColumn<DocumentSocietaireDTO, String> colNomSocietaireDocTable;
    @FXML private TableColumn<DocumentSocietaireDTO, String> colTypeDocumentTable;
    @FXML private TableColumn<DocumentSocietaireDTO, String> colCheminFichierDocTable;
    @FXML private TableColumn<DocumentSocietaireDTO, String> colDateUploadDocTable;

    @FXML private Button boutonUploaderDocument;
    @FXML private Button boutonTelechargerDocument;
    @FXML private Button boutonSupprimerDocument;
    @FXML private Button boutonActualiserListeDocuments;

    @FXML private ChoiceBox<SocietaireCompte> choiceBoxFiltreSocietaireDoc;
    @FXML private ChoiceBox<TypeDocumentSocietaire> choiceBoxFiltreTypeDoc;
    @FXML private Label labelSocietaireConcerne;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_DOCUMENT_LOGGER.fine("Dépendances services injectées dans DocumentPanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_DOCUMENT_LOGGER.info("Initialisation des données et de la configuration pour le panneau de gestion documentaire.");
        creerDossierStockageDocumentsSiInexistant();
        configurerColonnesTableDocuments();
        chargerOptionsFiltresDocuments();
        configurerPermissionsActionsDocuments();
        actionActualiserListeDocuments();

        tableVueDocuments.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            configurerEtatBoutonsContextuelsDocuments(newSelection);
        });
    }

    private void creerDossierStockageDocumentsSiInexistant() {
        try {
            Path cheminDossier = Paths.get(DOSSIER_BASE_DOCUMENTS);
            if (Files.notExists(cheminDossier)) {
                Files.createDirectories(cheminDossier);
                CONTROLEUR_DOCUMENT_LOGGER.info("Dossier de stockage des documents créé : " + cheminDossier.toAbsolutePath());
            }
        } catch (IOException e) {
            CONTROLEUR_DOCUMENT_LOGGER.log(Level.SEVERE, "Impossible de créer le dossier de stockage des documents : " + DOSSIER_BASE_DOCUMENTS, e);
            afficherNotificationAlerteInterface("Erreur Système Fichiers", "Le dossier de stockage des documents n'a pas pu être créé. L'upload sera impossible.", Alert.AlertType.ERROR);
        }
    }


    private void configurerColonnesTableDocuments() {
        colIdDocumentTable.setCellValueFactory(new PropertyValueFactory<>("idDoc"));
        colNomSocietaireDocTable.setCellValueFactory(new PropertyValueFactory<>("nomSocietaire"));
        colTypeDocumentTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTypeDoc() != null ? cellData.getValue().getTypeDoc().getDbValue() : "N/A"
        ));
        colCheminFichierDocTable.setCellValueFactory(new PropertyValueFactory<>("nomFichier"));
        colDateUploadDocTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateUpload() != null ? cellData.getValue().getDateUpload().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        CONTROLEUR_DOCUMENT_LOGGER.fine("Colonnes de la table des documents configurées.");
    }

    private void chargerOptionsFiltresDocuments() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean estAdminOuEquivalent = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS);

        if (estAdminOuEquivalent) {
            labelSocietaireConcerne.setText("");
            labelSocietaireConcerne.setVisible(false);
            labelSocietaireConcerne.setManaged(false);
            choiceBoxFiltreSocietaireDoc.setVisible(true);
            choiceBoxFiltreSocietaireDoc.setManaged(true);
            try {
                List<SocietaireCompte> societaires = servicePersistance.trouverTousLesSocietairesComptes();
                ObservableList<SocietaireCompte> societaireOptions = FXCollections.observableArrayList(societaires);
                societaireOptions.add(0, null);
                choiceBoxFiltreSocietaireDoc.setItems(societaireOptions);
                choiceBoxFiltreSocietaireDoc.setConverter(new StringConverter<SocietaireCompte>() {
                    @Override public String toString(SocietaireCompte sc) { return sc == null ? "Tous les sociétaires" : sc.getNom() + " (N°" + sc.getNumero() + ")"; }
                    @Override public SocietaireCompte fromString(String string) { return null; }
                });
                choiceBoxFiltreSocietaireDoc.setValue(null);
            } catch (Exception e) {
                CONTROLEUR_DOCUMENT_LOGGER.log(Level.WARNING, "Impossible de charger la liste des sociétaires pour le filtre.", e);
            }
        } else {
            choiceBoxFiltreSocietaireDoc.setVisible(false);
            choiceBoxFiltreSocietaireDoc.setManaged(false);
            labelSocietaireConcerne.setVisible(true);
            labelSocietaireConcerne.setManaged(true);
            Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
            if (idPersonnelConnecte != null && role == RoleUtilisateur.U3) {
                SocietaireCompte sc = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                if (sc != null) labelSocietaireConcerne.setText("Vos Documents (Sociétaire: " + sc.getNom() + ")");
                else labelSocietaireConcerne.setText("Aucun compte sociétaire associé à votre profil.");
            } else {
                labelSocietaireConcerne.setText("Accès aux documents non applicable.");
            }
        }

        ObservableList<TypeDocumentSocietaire> typesDoc = FXCollections.observableArrayList(TypeDocumentSocietaire.values());
        typesDoc.add(0, null);
        choiceBoxFiltreTypeDoc.setItems(typesDoc);
        choiceBoxFiltreTypeDoc.setConverter(new StringConverter<TypeDocumentSocietaire>() {
            @Override public String toString(TypeDocumentSocietaire td) { return td == null ? "Tous les types" : td.getDbValue(); }
            @Override public TypeDocumentSocietaire fromString(String string) { return "Tous les types".equals(string) ? null : TypeDocumentSocietaire.fromDbValue(string); }
        });
        choiceBoxFiltreTypeDoc.setValue(null);
        CONTROLEUR_DOCUMENT_LOGGER.fine("Options des filtres pour la liste des documents chargées.");
    }

    private void configurerPermissionsActionsDocuments() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        if (role == null) {
            Arrays.asList(boutonUploaderDocument, boutonTelechargerDocument, boutonSupprimerDocument)
                    .stream().filter(Objects::nonNull).forEach(b -> b.setDisable(true));
            return;
        }
        boolean peutUploader = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_UPLOAD_PROPRES) ||
                gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS);
        if (boutonUploaderDocument != null) boutonUploaderDocument.setDisable(!peutUploader);
    }

    private void configurerEtatBoutonsContextuelsDocuments(DocumentSocietaireDTO docSelectionne) {
        boolean aucuneSelection = docSelectionne == null;
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean peutTelecharger = !aucuneSelection;
        boolean peutSupprimer = false;

        if (!aucuneSelection && role != null) {
            if (gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS)) {
                peutSupprimer = true;
            } else if (gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_SUPPRIMER_PROPRES)) {
                Integer idPersonnelSession = SessionManager.obtenirIdPersonnelUtilisateurActuel();
                if (idPersonnelSession != null) {
                    SocietaireCompte scSession = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelSession);
                    if (scSession != null && scSession.getIdSocietaire() == docSelectionne.getIdSocietaire()) {
                        peutSupprimer = true;
                    }
                }
            }
        }

        if (boutonTelechargerDocument != null) boutonTelechargerDocument.setDisable(!peutTelecharger);
        if (boutonSupprimerDocument != null) boutonSupprimerDocument.setDisable(!peutSupprimer);
    }

    @FXML
    public void actionActualiserListeDocuments() {
        CONTROLEUR_DOCUMENT_LOGGER.info("Tentative d'actualisation de la liste des documents sociétaires.");
        try {
            Integer idSocietaireFiltre = null;
            RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
            boolean estAdminOuEquivalent = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS);

            if (estAdminOuEquivalent) {
                SocietaireCompte scFiltre = choiceBoxFiltreSocietaireDoc.getValue();
                if (scFiltre != null) idSocietaireFiltre = scFiltre.getIdSocietaire();
            } else {
                Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
                if (idPersonnelConnecte != null && role == RoleUtilisateur.U3) {
                    SocietaireCompte scConnecte = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                    if (scConnecte != null) idSocietaireFiltre = scConnecte.getIdSocietaire();
                    else {
                        tableVueDocuments.setItems(FXCollections.emptyObservableList());
                        return;
                    }
                } else {
                    tableVueDocuments.setItems(FXCollections.emptyObservableList());
                    return;
                }
            }

            TypeDocumentSocietaire typeDocFiltre = choiceBoxFiltreTypeDoc.getValue();
            List<DocumentSocietaire> documentsModel = serviceLogiqueMetier.rechercherDocumentsSocietaires(idSocietaireFiltre, typeDocFiltre);
            List<DocumentSocietaireDTO> documentsDto = DataMapper.convertirVersListeDeDocumentSocietaireDTO(documentsModel, servicePersistance);
            tableVueDocuments.setItems(FXCollections.observableArrayList(documentsDto));
            tableVueDocuments.refresh();
            configurerEtatBoutonsContextuelsDocuments(null);
            CONTROLEUR_DOCUMENT_LOGGER.info(documentsDto.size() + " documents chargés et affichés.");
        } catch (Exception e) {
            CONTROLEUR_DOCUMENT_LOGGER.log(Level.SEVERE, "Erreur majeure lors de l'actualisation des documents.", e);
            afficherNotificationAlerteInterface("Erreur de Données Documents", "Impossible de charger la liste des documents : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulaireUploadDocument() {
        CONTROLEUR_DOCUMENT_LOGGER.fine("Action utilisateur : ouvrir formulaire d'upload de document.");
        ouvrirDialogueFormulaireDocument();
    }

    private void ouvrirDialogueFormulaireDocument() {
        try {
            String titreDialogue = "Uploader un Nouveau Document Sociétaire";
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireDocumentView.fxml";
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire document introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            FormulaireDocumentController controleurFormulaire = chargeur.getController();
            controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            controleurFormulaire.preparerFormulaire();

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();
            actionActualiserListeDocuments();
            CONTROLEUR_DOCUMENT_LOGGER.info("Dialogue du formulaire document fermé.");

        } catch (IOException e) {
            CONTROLEUR_DOCUMENT_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture du formulaire document.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire d'upload : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionTelechargerOuVisualiserDocument() {
        DocumentSocietaireDTO docSelectionne = tableVueDocuments.getSelectionModel().getSelectedItem();
        if (docSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un document à télécharger ou visualiser.", Alert.AlertType.INFORMATION);
            return;
        }
        try {
            Path cheminFichierStocke = Paths.get(docSelectionne.getCheminFichierComplet());
            if (Files.exists(cheminFichierStocke) && Files.isReadable(cheminFichierStocke)) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Enregistrer le document sous...");
                fileChooser.setInitialFileName(docSelectionne.getNomFichier());

                // Suggest extension based on the original file or type
                String nomFichierOriginal = docSelectionne.getNomFichier();
                int lastDot = nomFichierOriginal.lastIndexOf('.');
                if (lastDot > 0 && lastDot < nomFichierOriginal.length() - 1) {
                    String extension = nomFichierOriginal.substring(lastDot + 1);
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase() + " Fichier", "*." + extension));
                }
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));


                File fichierDestination = fileChooser.showSaveDialog(MainApp.getPrimaryStage());

                if (fichierDestination != null) {
                    Files.copy(cheminFichierStocke, fichierDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    afficherNotificationAlerteInterface("Téléchargement Réussi", "Document '" + docSelectionne.getNomFichier() + "' enregistré.", Alert.AlertType.INFORMATION);

                    // Option pour ouvrir le fichier après sauvegarde
                    Optional<ButtonType> reponseOuverture = afficherDialogueConfirmationInterface("Ouvrir Fichier", "Voulez-vous ouvrir le fichier téléchargé '" + fichierDestination.getName() + "'?", ButtonType.YES, ButtonType.NO);
                    if (reponseOuverture.isPresent() && reponseOuverture.get() == ButtonType.YES) {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(fichierDestination);
                        } else {
                            afficherNotificationAlerteInterface("Ouverture Impossible", "L'ouverture automatique de fichiers n'est pas supportée sur ce système.", Alert.AlertType.INFORMATION);
                        }
                    }
                }
            } else {
                throw new IOException("Fichier document introuvable ou illisible sur le serveur : " + docSelectionne.getCheminFichierComplet());
            }
        } catch (IOException e) {
            CONTROLEUR_DOCUMENT_LOGGER.log(Level.SEVERE, "Erreur lors du téléchargement/visualisation du document ID: " + docSelectionne.getIdDoc(), e);
            afficherNotificationAlerteInterface("Erreur Fichier", "Impossible d'accéder au document : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionSupprimerDocumentSelectionne() {
        DocumentSocietaireDTO docSelectionne = tableVueDocuments.getSelectionModel().getSelectedItem();
        if (docSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un document à supprimer.", Alert.AlertType.INFORMATION);
            return;
        }

        Optional<ButtonType> reponse = afficherDialogueConfirmationInterface("Confirmation de Suppression",
                "Êtes-vous sûr de vouloir supprimer le document '" + docSelectionne.getNomFichier() + "' (Type: " + docSelectionne.getTypeDoc().getDbValue() + ") ?\nCette action est irréversible.", ButtonType.YES, ButtonType.CANCEL);
        if (reponse.isPresent() && reponse.get() == ButtonType.YES) {
            try {
                serviceLogiqueMetier.supprimerDocumentSocietaire(docSelectionne.getIdDoc(), docSelectionne.getCheminFichierComplet());
                actionActualiserListeDocuments();
                afficherNotificationAlerteInterface("Suppression Réussie", "Le document a été supprimé.", Alert.AlertType.INFORMATION);
            } catch (ErreurLogiqueMetier | IOException e) {
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

    private Optional<ButtonType> afficherDialogueConfirmationInterface(String titre, String message, ButtonType... buttonTypes) {
        Alert dialogue = new Alert(Alert.AlertType.CONFIRMATION);
        dialogue.setTitle(titre);
        dialogue.setHeaderText(null);
        dialogue.setContentText(message);
        if (buttonTypes != null && buttonTypes.length > 0) {
            dialogue.getButtonTypes().setAll(buttonTypes);
        } // Sinon, utilise OK et CANCEL par défaut
        Stage stagePrincipal = MainApp.getPrimaryStage();
        if (stagePrincipal != null && stagePrincipal.getScene() != null && stagePrincipal.isShowing()) {
            dialogue.initOwner(stagePrincipal);
        }
        return dialogue.showAndWait();
    }
}