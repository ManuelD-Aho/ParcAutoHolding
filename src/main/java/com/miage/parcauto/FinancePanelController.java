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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.SocietaireCompte;
import main.java.com.miage.parcauto.AppModels.Mouvement;
import main.java.com.miage.parcauto.AppModels.TypeMouvement;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppDataTransferObjects.SocietaireCompteDTO; // Supposons un DTO pour SocietaireCompte
import main.java.com.miage.parcauto.AppDataTransferObjects.MouvementDTO; // Supposons un DTO pour Mouvement
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FinancePanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_FINANCE_LOGGER = Logger.getLogger(FinancePanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private TableView<SocietaireCompteDTO> tableVueComptesSocietaires;
    @FXML private TableColumn<SocietaireCompteDTO, Integer> colIdSocietaireTable;
    @FXML private TableColumn<SocietaireCompteDTO, String> colNomSocietaireTable;
    @FXML private TableColumn<SocietaireCompteDTO, String> colNumeroCompteTable;
    @FXML private TableColumn<SocietaireCompteDTO, BigDecimal> colSoldeCompteTable;
    @FXML private TableColumn<SocietaireCompteDTO, String> colEmailSocietaireTable;
    @FXML private TableColumn<SocietaireCompteDTO, String> colTelephoneSocietaireTable;

    @FXML private TableView<MouvementDTO> tableVueMouvementsCompte;
    @FXML private TableColumn<MouvementDTO, Integer> colIdMouvementTable;
    @FXML private TableColumn<MouvementDTO, String> colDateMouvementTable;
    @FXML private TableColumn<MouvementDTO, String> colTypeMouvementTable;
    @FXML private TableColumn<MouvementDTO, BigDecimal> colMontantMouvementTable;

    @FXML private Button boutonCreerCompteSocietaire;
    @FXML private Button boutonModifierCompteSocietaire;
    @FXML private Button boutonEffectuerDepot;
    @FXML private Button boutonEffectuerRetrait;
    @FXML private Button boutonActualiserFinances;
    @FXML private TextField champRechercheSocietaire;
    @FXML private Label labelSocietaireSelectionnePourMouvements;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_FINANCE_LOGGER.fine("Dépendances services injectées dans FinancePanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_FINANCE_LOGGER.info("Initialisation des données et de la configuration pour le panneau de gestion financière.");
        configurerColonnesTableComptesSocietaires();
        configurerColonnesTableMouvements();
        configurerPermissionsActionsFinances();
        actionActualiserComptesSocietaires();

        tableVueComptesSocietaires.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            configurerEtatBoutonsContextuelsFinances(newSelection);
            if (newSelection != null) {
                labelSocietaireSelectionnePourMouvements.setText("Mouvements pour : " + newSelection.getNom() + " (N° " + newSelection.getNumeroCompte() + ")");
                chargerMouvementsPourSocietaire(newSelection.getIdSocietaire());
            } else {
                labelSocietaireSelectionnePourMouvements.setText("Sélectionnez un sociétaire pour voir ses mouvements");
                tableVueMouvementsCompte.setItems(FXCollections.emptyObservableList());
            }
        });
    }

    private void configurerColonnesTableComptesSocietaires() {
        colIdSocietaireTable.setCellValueFactory(new PropertyValueFactory<>("idSocietaire"));
        colNomSocietaireTable.setCellValueFactory(new PropertyValueFactory<>("nomSocietaire"));
        colNumeroCompteTable.setCellValueFactory(new PropertyValueFactory<>("numeroCompte"));
        colSoldeCompteTable.setCellValueFactory(new PropertyValueFactory<>("solde"));
        colEmailSocietaireTable.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephoneSocietaireTable.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        CONTROLEUR_FINANCE_LOGGER.fine("Colonnes de la table des comptes sociétaires configurées.");
    }

    private void configurerColonnesTableMouvements() {
        colIdMouvementTable.setCellValueFactory(new PropertyValueFactory<>("idMouvement"));
        colDateMouvementTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateHeureMouvement() != null ? cellData.getValue().getDateHeureMouvement().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        colTypeMouvementTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getType() != null ? cellData.getValue().getType().getDbValue() : "N/A"
        ));
        colMontantMouvementTable.setCellValueFactory(new PropertyValueFactory<>("montant"));
        CONTROLEUR_FINANCE_LOGGER.fine("Colonnes de la table des mouvements configurées.");
    }

    private void configurerPermissionsActionsFinances() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        if (role == null) {
            Arrays.asList(boutonCreerCompteSocietaire, boutonModifierCompteSocietaire, boutonEffectuerDepot, boutonEffectuerRetrait)
                    .stream().filter(Objects::nonNull).forEach(b -> b.setDisable(true));
            return;
        }
        boolean peutGererComptesTous = gestionnaireSecurite.estAutorise(role, Permissions.FINANCE_GERER_OPERATIONS_TOUS_COMPTES);
        boolean peutOpererPropreCompte = gestionnaireSecurite.estAutorise(role, Permissions.FINANCE_EFFECTUER_DEPOT_RETRAIT_PROPRE_COMPTE);

        if (boutonCreerCompteSocietaire != null) boutonCreerCompteSocietaire.setDisable(!peutGererComptesTous);
        // Les autres boutons sont contextuels
    }

    private void configurerEtatBoutonsContextuelsFinances(SocietaireCompteDTO compteSelectionne) {
        boolean aucuneSelection = compteSelectionne == null;
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean peutGererComptesTous = gestionnaireSecurite.estAutorise(role, Permissions.FINANCE_GERER_OPERATIONS_TOUS_COMPTES);
        boolean peutOpererSurCeCompte = false;

        if (!aucuneSelection) {
            Integer idPersonnelSession = SessionManager.obtenirIdPersonnelUtilisateurActuel();
            // Si l'utilisateur est un U3 (sociétaire) et que le compte sélectionné est le sien (via idPersonnel)
            if (role == RoleUtilisateur.U3 && idPersonnelSession != null && idPersonnelSession.equals(compteSelectionne.getIdPersonnelAssocie())) {
                peutOpererSurCeCompte = gestionnaireSecurite.estAutorise(role, Permissions.FINANCE_EFFECTUER_DEPOT_RETRAIT_PROPRE_COMPTE);
            }
        }

        if (boutonModifierCompteSocietaire != null) boutonModifierCompteSocietaire.setDisable(aucuneSelection || !peutGererComptesTous);
        if (boutonEffectuerDepot != null) boutonEffectuerDepot.setDisable(aucuneSelection || (!peutGererComptesTous && !peutOpererSurCeCompte));
        if (boutonEffectuerRetrait != null) boutonEffectuerRetrait.setDisable(aucuneSelection || (!peutGererComptesTous && !peutOpererSurCeCompte));
    }

    @FXML
    public void actionActualiserComptesSocietaires() {
        CONTROLEUR_FINANCE_LOGGER.info("Tentative d'actualisation de la liste des comptes sociétaires.");
        try {
            String recherche = champRechercheSocietaire.getText();
            List<SocietaireCompte> comptesModel = serviceLogiqueMetier.rechercherComptesSocietaires(recherche);
            // Assurez-vous d'avoir une méthode convertirVersListeDeSocietaireCompteDTO dans DataMapper
            List<SocietaireCompteDTO> comptesDto = DataMapper.convertirVersListeDeSocietaireCompteDTO(comptesModel, servicePersistance);
            tableVueComptesSocietaires.setItems(FXCollections.observableArrayList(comptesDto));
            tableVueComptesSocietaires.refresh();
            tableVueMouvementsCompte.setItems(FXCollections.emptyObservableList()); // Vider les mouvements
            labelSocietaireSelectionnePourMouvements.setText("Sélectionnez un sociétaire");
            configurerEtatBoutonsContextuelsFinances(null);
            CONTROLEUR_FINANCE_LOGGER.info(comptesDto.size() + " comptes sociétaires chargés.");
        } catch (Exception e) {
            CONTROLEUR_FINANCE_LOGGER.log(Level.SEVERE, "Erreur lors de l'actualisation des comptes sociétaires.", e);
            afficherNotificationAlerteInterface("Erreur de Données", "Impossible de charger la liste des comptes : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void chargerMouvementsPourSocietaire(int idSocietaire) {
        try {
            List<Mouvement> mouvementsModel = servicePersistance.trouverMouvementsParSocietaireId(idSocietaire);
            // Assurez-vous d'avoir une méthode convertirVersListeDeMouvementDTO dans DataMapper
            List<MouvementDTO> mouvementsDto = DataMapper.convertirVersListeDeMouvementDTO(mouvementsModel);
            tableVueMouvementsCompte.setItems(FXCollections.observableArrayList(mouvementsDto));
            tableVueMouvementsCompte.refresh();
            CONTROLEUR_FINANCE_LOGGER.info(mouvementsDto.size() + " mouvements chargés pour le sociétaire ID: " + idSocietaire);
        } catch (Exception e) {
            CONTROLEUR_FINANCE_LOGGER.log(Level.SEVERE, "Erreur lors du chargement des mouvements pour le sociétaire ID: " + idSocietaire, e);
            afficherNotificationAlerteInterface("Erreur de Données", "Impossible de charger les mouvements du compte : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulaireCreationCompteSocietaire() {
        CONTROLEUR_FINANCE_LOGGER.fine("Action utilisateur : ouvrir formulaire de création de compte sociétaire.");
        ouvrirDialogueFormulaireCompteSocietaire(null);
    }

    @FXML
    private void actionOuvrirFormulaireModificationCompteSocietaire() {
        SocietaireCompteDTO compteSelectionneDto = tableVueComptesSocietaires.getSelectionModel().getSelectedItem();
        if (compteSelectionneDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un compte à modifier.", Alert.AlertType.INFORMATION);
            return;
        }
        CONTROLEUR_FINANCE_LOGGER.fine("Action utilisateur : ouvrir formulaire de modification pour compte ID: " + compteSelectionneDto.getIdSocietaire());
        SocietaireCompte compteModel = servicePersistance.trouverSocietaireCompteParId(compteSelectionneDto.getIdSocietaire());
        if (compteModel == null){
            afficherNotificationAlerteInterface("Erreur Donnée Introuvable", "Le compte sélectionné (ID: "+compteSelectionneDto.getIdSocietaire()+") n'a pas pu être retrouvé.", Alert.AlertType.ERROR);
            actionActualiserComptesSocietaires();
            return;
        }
        ouvrirDialogueFormulaireCompteSocietaire(compteModel);
    }

    private void ouvrirDialogueFormulaireCompteSocietaire(SocietaireCompte compteAEditer) {
        try {
            String titreDialogue = (compteAEditer == null) ? "Créer un Nouveau Compte Sociétaire" : "Modifier le Compte Sociétaire : " + compteAEditer.getNom();
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireCompteSocietaireView.fxml"; // FXML à créer
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire compte sociétaire introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            // Supposons un FormulaireCompteSocietaireController.java
            // FormulaireCompteSocietaireController controleurFormulaire = chargeur.getController();
            // controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            // controleurFormulaire.preparerFormulairePourEdition(compteAEditer);

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();
            actionActualiserComptesSocietaires();
            CONTROLEUR_FINANCE_LOGGER.info("Dialogue du formulaire compte sociétaire fermé.");

        } catch (IOException e) {
            CONTROLEUR_FINANCE_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture du formulaire compte sociétaire.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionEffectuerOperationCompte(TypeMouvement typeOperation) {
        SocietaireCompteDTO compteSelectionne = tableVueComptesSocietaires.getSelectionModel().getSelectedItem();
        if (compteSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un compte sociétaire pour effectuer un " + typeOperation.getDbValue().toLowerCase() + ".", Alert.AlertType.INFORMATION);
            return;
        }
        CONTROLEUR_FINANCE_LOGGER.fine("Action utilisateur : effectuer " + typeOperation.getDbValue() + " pour compte ID: " + compteSelectionne.getIdSocietaire());
        ouvrirDialogueFormulaireMouvement(compteSelectionne, typeOperation);
    }

    @FXML private void actionEffectuerDepotSurCompte() { actionEffectuerOperationCompte(TypeMouvement.DEPOT); }
    @FXML private void actionEffectuerRetraitSurCompte() { actionEffectuerOperationCompte(TypeMouvement.RETRAIT); }

    private void ouvrirDialogueFormulaireMouvement(SocietaireCompteDTO compteConcerne, TypeMouvement typeMouvement) {
        try {
            String titreDialogue = typeMouvement.getDbValue() + " sur le Compte de " + compteConcerne.getNomSocietaire();
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireMouvementView.fxml"; // FXML à créer
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire mouvement introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            // Supposons un FormulaireMouvementController.java
            FormulaireMouvementController controleurFormulaire = chargeur.getController();
            controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            controleurFormulaire.preparerFormulaire(compteConcerne.getIdSocietaire(), typeMouvement);

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();
            actionActualiserComptesSocietaires(); // Rafraîchir pour voir le nouveau solde et le mouvement
            // Re-sélectionner le sociétaire pour afficher ses mouvements mis à jour
            if (compteConcerne != null) {
                final int idSocietaireAReselectionner = compteConcerne.getIdSocietaire();
                Platform.runLater(() -> {
                    tableVueComptesSocietaires.getItems().stream()
                            .filter(sc -> sc.getIdSocietaire() == idSocietaireAReselectionner)
                            .findFirst()
                            .ifPresent(scDto -> {
                                tableVueComptesSocietaires.getSelectionModel().select(scDto);
                                tableVueComptesSocietaires.scrollTo(scDto);
                            });
                });
            }


            CONTROLEUR_FINANCE_LOGGER.info("Dialogue du formulaire mouvement fermé.");

        } catch (IOException e) {
            CONTROLEUR_FINANCE_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture du formulaire mouvement.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire de mouvement : " + e.getMessage(), Alert.AlertType.ERROR);
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