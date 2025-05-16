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
import main.java.com.miage.parcauto.AppDataTransferObjects.DocumentSocietaireDTO; // Supposons un DTO
import main.java.com.miage.parcauto.AppExceptions.*;

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
    @FXML private TableColumn<DocumentSocietaireDTO, String> colNomSocietaireDocTable; // DTO devra avoir nom sociétaire
    @FXML private TableColumn<DocumentSocietaireDTO, String> colTypeDocumentTable;
    @FXML private TableColumn<DocumentSocietaireDTO, String> colCheminFichierDocTable; // Ou juste nom du fichier
    @FXML private TableColumn<DocumentSocietaireDTO, String> colDateUploadDocTable;

    @FXML private Button boutonUploaderDocument;
    @FXML private Button boutonTelechargerDocument; // Ou Visualiser
    @FXML private Button boutonSupprimerDocument;
    @FXML private Button boutonActualiserListeDocuments;

    @FXML private ChoiceBox<SocietaireCompte> choiceBoxFiltreSocietaireDoc; // Pour filtrer par sociétaire (si admin)
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
        colNomSocietaireDocTable.setCellValueFactory(new PropertyValueFactory<>("nomSocietaire")); // DTO doit avoir ce champ
        colTypeDocumentTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTypeDoc() != null ? cellData.getValue().getTypeDoc().getDbValue() : "N/A"
        ));
        colCheminFichierDocTable.setCellValueFactory(new PropertyValueFactory<>("nomFichier")); // DTO: juste le nom, pas le chemin complet
        colDateUploadDocTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateUpload() != null ? cellData.getValue().getDateUpload().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        CONTROLEUR_DOCUMENT_LOGGER.fine("Colonnes de la table des documents configurées.");
    }

    private void chargerOptionsFiltresDocuments() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean estAdminOuEquivalent = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS);

        if (estAdminOuEquivalent) {
            try {
                List<SocietaireCompte> societaires = servicePersistance.trouverTousLesSocietairesComptes();
                ObservableList<SocietaireCompte> societaireOptions = FXCollections.observableArrayList(societaires);
                societaireOptions.add(0, null); // Option "Tous les sociétaires"
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
            // Pour un U3, on affiche ses propres documents par défaut
            Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
            if (idPersonnelConnecte != null) {
                SocietaireCompte sc = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                if (sc != null) labelSocietaireConcerne.setText("Documents pour : " + sc.getNom());
                else labelSocietaireConcerne.setText("Aucun compte sociétaire associé.");
            } else {
                labelSocietaireConcerne.setText("Utilisateur non sociétaire.");
            }
        }

        ObservableList<TypeDocumentSocietaire> typesDoc = FXCollections.observableArrayList(TypeDocumentSocietaire.values());
        typesDoc.add(0, null); // Option "Tous les types"
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
        // L'upload est permis si l'utilisateur peut gérer ses propres documents OU s'il est admin.
        // Le choix du sociétaire pour l'upload se fera dans le formulaire d'upload si admin.
        boolean peutUploader = gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_UPLOAD_PROPRES) ||
                gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS); // Admin peut uploader pour d'autres
        if (boutonUploaderDocument != null) boutonUploaderDocument.setDisable(!peutUploader);
    }

    private void configurerEtatBoutonsContextuelsDocuments(DocumentSocietaireDTO docSelectionne) {
        boolean aucuneSelection = docSelectionne == null;
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean peutTelecharger = !aucuneSelection; // Tous ceux qui voient peuvent télécharger/visualiser
        boolean peutSupprimer = false;

        if (!aucuneSelection) {
            if (gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_CONSULTER_TOUS)) { // Admin peut tout supprimer
                peutSupprimer = true;
            } else if (gestionnaireSecurite.estAutorise(role, Permissions.DOCUMENT_SUPPRIMER_PROPRES)) {
                // Vérifier si le document appartient à l'utilisateur connecté (U3)
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
            } else { // Pour U3, on filtre sur son propre idSocietaire
                Integer idPersonnelConnecte = SessionManager.obtenirIdPersonnelUtilisateurActuel();
                if (idPersonnelConnecte != null) {
                    SocietaireCompte scConnecte = servicePersistance.trouverSocietaireCompteParIdPersonnel(idPersonnelConnecte);
                    if (scConnecte != null) idSocietaireFiltre = scConnecte.getIdSocietaire();
                    else { // U3 non lié à un compte sociétaire ne voit rien
                        tableVueDocuments.setItems(FXCollections.emptyObservableList());
                        return;
                    }
                } else { // Non U3 et non admin ne voit rien (ou logique à affiner)
                    tableVueDocuments.setItems(FXCollections.emptyObservableList());
                    return;
                }
            }

            TypeDocumentSocietaire typeDocFiltre = choiceBoxFiltreTypeDoc.getValue();
            List<DocumentSocietaire> documentsModel = serviceLogiqueMetier.rechercherDocumentsSocietaires(idSocietaireFiltre, typeDocFiltre);
            // DataMapper.convertirVersListeDeDocumentSocietaireDTO(documentsModel, servicePersistance) à créer
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
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireDocumentView.fxml"; // FXML à créer
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
            Path cheminFichierStocke = Paths.get(docSelectionne.getCheminFichierComplet()); // DTO doit fournir le chemin complet
            if (Files.exists(cheminFichierStocke) && Files.isReadable(cheminFichierStocke)) {
                // Pour une visualisation simple, on pourrait utiliser Desktop.getDesktop().open(file)
                // Pour un téléchargement, on utilise FileChooser pour "Enregistrer sous"
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Enregistrer le document sous...");
                fileChooser.setInitialFileName(docSelectionne.getNomFichier()); // DTO doit avoir le nom original
                File fichierDestination = fileChooser.showSaveDialog(MainApp.getPrimaryStage());

                if (fichierDestination != null) {
                    Files.copy(cheminFichierStocke, fichierDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    afficherNotificationAlerteInterface("Téléchargement Réussi", "Document '" + docSelectionne.getNomFichier() + "' enregistré.", Alert.AlertType.INFORMATION);
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
                "Êtes-vous sûr de vouloir supprimer le document '" + docSelectionne.getNomFichier() + "' (Type: " + docSelectionne.getTypeDoc().getDbValue() + ") ?");
        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            try {
                serviceLogiqueMetier.supprimerDocumentSocietaire(docSelectionne.getIdDoc(), docSelectionne.getCheminFichierComplet()); // BusinessLogic gère suppression fichier + BDD
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
}