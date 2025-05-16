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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.Mission;
import main.java.com.miage.parcauto.AppModels.StatutMission;
import main.java.com.miage.parcauto.AppModels.Vehicule;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppDataTransferObjects.MissionDTO;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MissionPanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_MISSION_LOGGER = Logger.getLogger(MissionPanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private TableView<MissionDTO> tableVueMissions;
    @FXML private TableColumn<MissionDTO, Integer> colIdMissionTable;
    @FXML private TableColumn<MissionDTO, String> colLibelleMissionTable;
    @FXML private TableColumn<MissionDTO, String> colVehiculeImmatTable;
    @FXML private TableColumn<MissionDTO, String> colVehiculeModeleTable;
    @FXML private TableColumn<MissionDTO, String> colDateDebutMissionTable;
    @FXML private TableColumn<MissionDTO, String> colDateFinMissionTable;
    @FXML private TableColumn<MissionDTO, String> colStatutMissionTable;
    @FXML private TableColumn<MissionDTO, String> colSiteMissionTable;
    @FXML private TableColumn<MissionDTO, Integer> colKmPrevuMissionTable;
    @FXML private TableColumn<MissionDTO, Integer> colKmReelMissionTable;

    @FXML private Button boutonPlanifierMission;
    @FXML private Button boutonModifierMission;
    @FXML private Button boutonDemarrerMission;
    @FXML private Button boutonCloturerMission;
    @FXML private Button boutonAnnulerMission; // Annuler une mission planifiée
    @FXML private Button boutonDetailsMission;
    @FXML private Button boutonActualiserListeMissions;

    @FXML private DatePicker datePickerFiltreDebutMission;
    @FXML private DatePicker datePickerFiltreFinMission;
    @FXML private ChoiceBox<StatutMission> choiceBoxFiltreStatutMission;
    @FXML private TextField champRechercheVehiculeMission;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_MISSION_LOGGER.fine("Dépendances services injectées dans MissionPanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_MISSION_LOGGER.info("Initialisation des données et de la configuration pour le panneau de gestion des missions.");
        configurerColonnesTableMissions();
        chargerOptionsFiltresMissions();
        configurerPermissionsActionsMissions();
        actionActualiserListeMissions();

        tableVueMissions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            configurerEtatBoutonsContextuels(newSelection);
        });
    }

    private void configurerColonnesTableMissions() {
        colIdMissionTable.setCellValueFactory(new PropertyValueFactory<>("idMission"));
        colLibelleMissionTable.setCellValueFactory(new PropertyValueFactory<>("libMission"));
        colVehiculeImmatTable.setCellValueFactory(new PropertyValueFactory<>("immatriculationVehicule"));
        colVehiculeModeleTable.setCellValueFactory(new PropertyValueFactory<>("marqueModeleVehicule"));
        colDateDebutMissionTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateDebutMission() != null ? cellData.getValue().getDateDebutMission().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        colDateFinMissionTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateFinMission() != null ? cellData.getValue().getDateFinMission().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        colStatutMissionTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().getDbValue() : "N/A"
        ));
        colSiteMissionTable.setCellValueFactory(new PropertyValueFactory<>("site"));
        colKmPrevuMissionTable.setCellValueFactory(new PropertyValueFactory<>("kmPrevu"));
        colKmReelMissionTable.setCellValueFactory(new PropertyValueFactory<>("kmReel"));
        CONTROLEUR_MISSION_LOGGER.fine("Colonnes de la table des missions configurées.");
    }

    private void chargerOptionsFiltresMissions() {
        datePickerFiltreDebutMission.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerFiltreFinMission.setConverter(ViewController.obtenirConvertisseurDateStandard());

        ObservableList<StatutMission> statuts = FXCollections.observableArrayList(StatutMission.values());
        // Ajouter une option "Tous les statuts"
        choiceBoxFiltreStatutMission.setItems(statuts); // Le premier sera sélectionné ou laisser vide pour "tous"
        choiceBoxFiltreStatutMission.getItems().add(0, null); // Pour représenter "Tous"
        choiceBoxFiltreStatutMission.setConverter(new StringConverter<StatutMission>() {
            @Override public String toString(StatutMission statut) { return statut == null ? "Tous les statuts" : statut.getDbValue(); }
            @Override public StatutMission fromString(String string) { // Non essentiel pour ChoiceBox simple
                if ("Tous les statuts".equals(string)) return null;
                return StatutMission.fromDbValue(string);
            }
        });
        choiceBoxFiltreStatutMission.setValue(null); // Sélectionner "Tous les statuts" par défaut
        CONTROLEUR_MISSION_LOGGER.fine("Options des filtres pour la liste des missions chargées.");
    }

    private void configurerPermissionsActionsMissions() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        if (role == null) {
            Arrays.asList(boutonPlanifierMission, boutonModifierMission, boutonDemarrerMission, boutonCloturerMission, boutonAnnulerMission, boutonDetailsMission)
                    .stream().filter(Objects::nonNull).forEach(b -> b.setDisable(true));
            return;
        }
        if (boutonPlanifierMission != null) boutonPlanifierMission.setDisable(!gestionnaireSecurite.estAutorise(role, Permissions.MISSION_PLANIFIER_NOUVELLE));
        // Les autres boutons sont contextuels à la sélection
    }

    private void configurerEtatBoutonsContextuels(MissionDTO missionSelectionnee) {
        boolean aucuneSelection = missionSelectionnee == null;
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();

        if (boutonModifierMission != null) {
            boolean peutModifier = !aucuneSelection &&
                    missionSelectionnee.getStatus() == StatutMission.PLANIFIEE &&
                    gestionnaireSecurite.estAutorise(role, Permissions.MISSION_GERER_TOUTES); // Ou une permission plus fine
            boutonModifierMission.setDisable(!peutModifier);
        }
        if (boutonDemarrerMission != null) {
            boolean peutDemarrer = !aucuneSelection &&
                    missionSelectionnee.getStatus() == StatutMission.PLANIFIEE &&
                    gestionnaireSecurite.estAutorise(role, Permissions.MISSION_DEMARRER);
            boutonDemarrerMission.setDisable(!peutDemarrer);
        }
        if (boutonCloturerMission != null) {
            boolean peutCloturer = !aucuneSelection &&
                    missionSelectionnee.getStatus() == StatutMission.EN_COURS &&
                    gestionnaireSecurite.estAutorise(role, Permissions.MISSION_CLOTURER);
            boutonCloturerMission.setDisable(!peutCloturer);
        }
        if (boutonAnnulerMission != null) {
            boolean peutAnnuler = !aucuneSelection &&
                    missionSelectionnee.getStatus() == StatutMission.PLANIFIEE &&
                    gestionnaireSecurite.estAutorise(role, Permissions.MISSION_GERER_TOUTES); // Ou une permission d'annulation
            boutonAnnulerMission.setDisable(!peutAnnuler);
        }
        if (boutonDetailsMission != null) {
            boutonDetailsMission.setDisable(aucuneSelection);
        }
    }

    @FXML
    public void actionActualiserListeMissions() {
        CONTROLEUR_MISSION_LOGGER.info("Tentative d'actualisation de la liste des missions.");
        try {
            LocalDate dateDebutFiltre = datePickerFiltreDebutMission.getValue();
            LocalDate dateFinFiltre = datePickerFiltreFinMission.getValue();
            StatutMission statutFiltre = choiceBoxFiltreStatutMission.getValue();
            String rechercheVehicule = champRechercheVehiculeMission.getText();

            // Convertir LocalDate en LocalDateTime pour la recherche si nécessaire
            LocalDateTime dateTimeDebutFiltre = (dateDebutFiltre != null) ? dateDebutFiltre.atStartOfDay() : null;
            LocalDateTime dateTimeFinFiltre = (dateFinFiltre != null) ? dateFinFiltre.atTime(LocalTime.MAX) : null;

            List<Mission> missionsModel = serviceLogiqueMetier.rechercherMissionsFiltrees(
                    dateTimeDebutFiltre, dateTimeFinFiltre, statutFiltre, rechercheVehicule
            );

            List<MissionDTO> missionsDto = DataMapper.convertirVersListeDeMissionDTO(missionsModel, servicePersistance);
            tableVueMissions.setItems(FXCollections.observableArrayList(missionsDto));
            tableVueMissions.refresh();
            configurerEtatBoutonsContextuels(null); // Réinitialiser l'état des boutons après MAJ
            CONTROLEUR_MISSION_LOGGER.info(missionsDto.size() + " missions chargées et affichées après actualisation/filtrage.");
        } catch (Exception e) {
            CONTROLEUR_MISSION_LOGGER.log(Level.SEVERE, "Erreur majeure lors de l'actualisation ou du filtrage des données des missions.", e);
            afficherNotificationAlerteInterface("Erreur de Données Missions", "Impossible de charger ou filtrer la liste des missions : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulairePlanificationMission() {
        CONTROLEUR_MISSION_LOGGER.fine("Action utilisateur : ouvrir formulaire de planification de mission.");
        ouvrirDialogueFormulaireMission(null); // null indique le mode création
    }

    @FXML
    private void actionOuvrirFormulaireModificationMission() {
        MissionDTO missionSelectionneeDto = tableVueMissions.getSelectionModel().getSelectedItem();
        if (missionSelectionneeDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner une mission dans la liste pour la modifier.", Alert.AlertType.INFORMATION);
            return;
        }
        if (missionSelectionneeDto.getStatus() != StatutMission.PLANIFIEE) {
            afficherNotificationAlerteInterface("Action Impossible", "Seules les missions planifiées peuvent être modifiées.", Alert.AlertType.WARNING);
            return;
        }
        CONTROLEUR_MISSION_LOGGER.fine("Action utilisateur : ouvrir formulaire de modification pour mission ID: " + missionSelectionneeDto.getIdMission());
        Mission missionModel = servicePersistance.trouverMissionParId(missionSelectionneeDto.getIdMission());
        if (missionModel == null){
            afficherNotificationAlerteInterface("Erreur Donnée Introuvable", "La mission sélectionnée (ID: "+missionSelectionneeDto.getIdMission()+") n'a pas pu être retrouvée pour modification.", Alert.AlertType.ERROR);
            actionActualiserListeMissions();
            return;
        }
        ouvrirDialogueFormulaireMission(missionModel);
    }

    private void ouvrirDialogueFormulaireMission(Mission missionAEditer) {
        try {
            String titreDialogue = (missionAEditer == null) ? "Planifier une Nouvelle Mission" : "Modifier la Mission : " + missionAEditer.getLibMission();
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireMissionView.fxml";
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire mission introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            FormulaireMissionController controleurFormulaire = chargeur.getController();
            controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            controleurFormulaire.preparerFormulairePourEdition(missionAEditer);

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();
            actionActualiserListeMissions();
            CONTROLEUR_MISSION_LOGGER.info("Dialogue du formulaire mission fermé. Table des missions rafraîchie si nécessaire.");

        } catch (IOException e) {
            CONTROLEUR_MISSION_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture ou de la gestion du formulaire mission.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire de gestion des missions : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionDemarrerMissionSelectionnee() {
        MissionDTO missionSelectionnee = tableVueMissions.getSelectionModel().getSelectedItem();
        if (missionSelectionnee == null || missionSelectionnee.getStatus() != StatutMission.PLANIFIEE) {
            afficherNotificationAlerteInterface("Action Impossible", "Veuillez sélectionner une mission planifiée pour la démarrer.", Alert.AlertType.WARNING);
            return;
        }
        CONTROLEUR_MISSION_LOGGER.fine("Action utilisateur : démarrer mission ID: " + missionSelectionnee.getIdMission());
        try {
            serviceLogiqueMetier.demarrerUneMission(missionSelectionnee.getIdMission());
            afficherNotificationAlerteInterface("Mission Démarrée", "La mission '" + missionSelectionnee.getLibMission() + "' a été marquée comme 'En cours'.", Alert.AlertType.INFORMATION);
            actionActualiserListeMissions();
        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            afficherNotificationAlerteInterface("Échec du Démarrage", e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            CONTROLEUR_MISSION_LOGGER.log(Level.SEVERE, "Erreur inattendue lors du démarrage de la mission.", e);
            afficherNotificationAlerteInterface("Erreur Système", "Une erreur imprévue est survenue: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirDialogueClotureMission() {
        MissionDTO missionSelectionnee = tableVueMissions.getSelectionModel().getSelectedItem();
        if (missionSelectionnee == null || missionSelectionnee.getStatus() != StatutMission.EN_COURS) {
            afficherNotificationAlerteInterface("Action Impossible", "Veuillez sélectionner une mission 'En cours' pour la clôturer.", Alert.AlertType.WARNING);
            return;
        }
        CONTROLEUR_MISSION_LOGGER.fine("Action utilisateur : ouvrir dialogue de clôture pour mission ID: " + missionSelectionnee.getIdMission());

        Dialog<Pair<Integer, BigDecimal>> dialogueCloture = new Dialog<>();
        dialogueCloture.setTitle("Clôturer la Mission");
        dialogueCloture.setHeaderText("Finalisation de la mission : " + missionSelectionnee.getLibMission());
        Stage stageProprietaire = MainApp.getPrimaryStage();
        if (stageProprietaire != null) dialogueCloture.initOwner(stageProprietaire);

        GridPane grilleCloture = new GridPane();
        grilleCloture.setHgap(10);
        grilleCloture.setVgap(10);

        TextField champKmReelCloture = new TextField();
        champKmReelCloture.setPromptText("KM parcourus");
        TextField champCoutTotalCloture = new TextField();
        champCoutTotalCloture.setPromptText("Coût total (optionnel)");
        // Pour les dépenses, un mécanisme plus complexe serait nécessaire (ex: un autre dialogue ou un tableau)
        // Pour simplifier ici, on ne gère pas l'ajout de dépenses directement dans ce dialogue.

        grilleCloture.add(new Label("Kilométrage réel:"), 0, 0);
        grilleCloture.add(champKmReelCloture, 1, 0);
        grilleCloture.add(new Label("Coût total estimé:"), 0, 1);
        grilleCloture.add(champCoutTotalCloture, 1, 1);
        dialogueCloture.getDialogPane().setContent(grilleCloture);

        ButtonType boutonConfirmerCloture = ButtonType.OK;
        dialogueCloture.getDialogPane().getButtonTypes().addAll(boutonConfirmerCloture, ButtonType.CANCEL);

        // Validation à la volée du bouton OK
        Button boutonOkActive = (Button) dialogueCloture.getDialogPane().lookupButton(boutonConfirmerCloture);
        boutonOkActive.setDisable(true);
        champKmReelCloture.textProperty().addListener((observable, oldValue, newValue) -> {
            boutonOkActive.setDisable(newValue.trim().isEmpty() || !newValue.matches("\\d+"));
        });

        dialogueCloture.setResultConverter(typeBouton -> {
            if (typeBouton == boutonConfirmerCloture) {
                try {
                    Integer kmReel = Integer.parseInt(champKmReelCloture.getText());
                    BigDecimal coutTotal = null;
                    if (!champCoutTotalCloture.getText().trim().isEmpty()) {
                        coutTotal = new BigDecimal(champCoutTotalCloture.getText().replace(",", "."));
                    }
                    return new Pair<>(kmReel, coutTotal);
                } catch (NumberFormatException e) {
                    afficherNotificationAlerteInterface("Format Numérique Invalide", "Veuillez entrer des valeurs numériques valides pour le kilométrage et le coût.", Alert.AlertType.ERROR);
                    return null; // Empêche la fermeture si invalide
                }
            }
            return null;
        });

        Optional<Pair<Integer, BigDecimal>> resultat = dialogueCloture.showAndWait();
        resultat.ifPresent(pair -> {
            try {
                // Ici, on ne gère pas les dépenses via ce dialogue simplifié
                serviceLogiqueMetier.cloturerUneMission(missionSelectionnee.getIdMission(), pair.getKey(), pair.getValue(), null);
                afficherNotificationAlerteInterface("Mission Clôturée", "La mission '" + missionSelectionnee.getLibMission() + "' a été clôturée avec succès.", Alert.AlertType.INFORMATION);
                actionActualiserListeMissions();
            } catch (ErreurValidation | ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec de la Clôture", e.getMessage(), Alert.AlertType.ERROR);
            } catch (Exception e) {
                CONTROLEUR_MISSION_LOGGER.log(Level.SEVERE, "Erreur imprévue lors de la clôture de la mission.", e);
                afficherNotificationAlerteInterface("Erreur Système", "Une erreur inattendue s'est produite: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void actionAnnulerMissionPlanifiee() {
        MissionDTO missionSelectionnee = tableVueMissions.getSelectionModel().getSelectedItem();
        if (missionSelectionnee == null || missionSelectionnee.getStatus() != StatutMission.PLANIFIEE) {
            afficherNotificationAlerteInterface("Action Impossible", "Veuillez sélectionner une mission 'Planifiée' pour l'annuler.", Alert.AlertType.WARNING);
            return;
        }
        CONTROLEUR_MISSION_LOGGER.fine("Action utilisateur : annuler mission ID: " + missionSelectionnee.getIdMission());
        Optional<ButtonType> reponse = afficherDialogueConfirmationInterface("Confirmation d'Annulation",
                "Êtes-vous sûr de vouloir annuler la mission planifiée '" + missionSelectionnee.getLibMission() + "' ?");
        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            try {
                serviceLogiqueMetier.annulerMissionPlanifiee(missionSelectionnee.getIdMission()); // Méthode à créer dans BusinessLogicService
                afficherNotificationAlerteInterface("Mission Annulée", "La mission a été annulée avec succès.", Alert.AlertType.INFORMATION);
                actionActualiserListeMissions();
            } catch (ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec de l'Annulation", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void actionAfficherDetailsMission() {
        MissionDTO missionSelectionnee = tableVueMissions.getSelectionModel().getSelectedItem();
        if (missionSelectionnee == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner une mission pour en voir les détails.", Alert.AlertType.INFORMATION);
            return;
        }
        Mission missionComplete = servicePersistance.trouverMissionParId(missionSelectionnee.getIdMission());
        if (missionComplete == null) {
            afficherNotificationAlerteInterface("Donnée Introuvable", "Impossible de récupérer les détails complets de la mission.", Alert.AlertType.WARNING);
            return;
        }
        Vehicule vehiculeAssocie = servicePersistance.trouverVehiculeParId(missionComplete.getIdVehicule());

        StringBuilder details = new StringBuilder();
        details.append("ID Mission: ").append(missionComplete.getIdMission()).append("\n");
        details.append("Libellé: ").append(missionComplete.getLibMission()).append("\n");
        details.append("Véhicule: ").append(vehiculeAssocie != null ? vehiculeAssocie.getImmatriculation() + " (" + vehiculeAssocie.getMarque() + " " + vehiculeAssocie.getModele() + ")" : "N/A").append("\n");
        details.append("Site: ").append(missionComplete.getSite()).append("\n");
        details.append("Circuit: ").append(missionComplete.getCircuitMission() != null ? missionComplete.getCircuitMission() : "Non spécifié").append("\n");
        details.append("Date Début: ").append(missionComplete.getDateDebutMission() != null ? missionComplete.getDateDebutMission().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append("\n");
        details.append("Date Fin: ").append(missionComplete.getDateFinMission() != null ? missionComplete.getDateFinMission().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append("\n");
        details.append("Statut: ").append(missionComplete.getStatus().getDbValue()).append("\n");
        details.append("KM Prévus: ").append(missionComplete.getKmPrevu()).append(", KM Réels: ").append(missionComplete.getKmReel() != null ? missionComplete.getKmReel() : "N/A").append("\n");
        details.append("Coût Total Estimé: ").append(missionComplete.getCoutTotal() != null ? missionComplete.getCoutTotal() : "N/A").append(" EUR\n");
        details.append("Observations: ").append(missionComplete.getObservationMission() != null ? missionComplete.getObservationMission() : "Aucune").append("\n\n");

        List<AppModels.DepenseMission> depenses = servicePersistance.trouverDepensesParMissionId(missionComplete.getIdMission());
        if (!depenses.isEmpty()) {
            details.append("Dépenses Associées:\n");
            for(AppModels.DepenseMission dep : depenses) {
                details.append("  - ").append(dep.getNature().getDbValue()).append(": ").append(dep.getMontant()).append(" EUR (Justificatif: ").append(dep.getJustificatif() != null ? dep.getJustificatif() : "N/A").append(")\n");
            }
        } else {
            details.append("Aucune dépense enregistrée pour cette mission.\n");
        }

        afficherNotificationAlerteInterface("Détails de la Mission: " + missionComplete.getLibMission(), details.toString(), Alert.AlertType.INFORMATION);
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

    // Classe utilitaire Pair simple si non disponible dans JavaFX/JDK utilisé (JavaFX a javafx.util.Pair)
    private static class Pair<K, V> {
        private K key;
        private V value;
        public Pair(K key, V value) { this.key = key; this.value = value; }
        public K getKey() { return key; }
        public V getValue() { return value; }
    }
}