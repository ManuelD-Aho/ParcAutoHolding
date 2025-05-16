package main.java.com.miage.parcauto;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.Mission;
import main.java.com.miage.parcauto.AppModels.StatutMission;
import main.java.com.miage.parcauto.AppModels.Vehicule;
import main.java.com.miage.parcauto.AppModels.EtatVoiture; // Pour filtrer les véhicules disponibles
import main.java.com.miage.parcauto.AppExceptions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FormulaireMissionController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_FORM_MISSION_LOGGER = Logger.getLogger(FormulaireMissionController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private TextField champLibelleMissionForm;
    @FXML private ChoiceBox<Vehicule> choiceBoxVehiculeMissionForm;
    @FXML private TextField champSiteMissionForm;
    @FXML private DatePicker datePickerDebutMissionForm;
    @FXML private TextField champHeureDebutMissionForm; // Format HH:MM
    @FXML private DatePicker datePickerFinMissionForm;
    @FXML private TextField champHeureFinMissionForm;   // Format HH:MM
    @FXML private TextField champKmPrevuMissionForm;
    @FXML private TextArea textAreaCircuitMissionForm;
    @FXML private TextArea textAreaObservationMissionForm;
    @FXML private Button boutonEnregistrerMissionForm;
    @FXML private Button boutonAnnulerMissionForm;
    @FXML private Label labelTitreFormulaireMission;

    private Mission missionExistantePourModification;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_FORM_MISSION_LOGGER.fine("Dépendances services injectées.");
    }

    @Override
    public void initialiserDonneesVue() { /* Non utilisé ici, voir preparerFormulairePourEdition */ }

    public void preparerFormulairePourEdition(Mission mission) {
        this.missionExistantePourModification = mission;
        chargerVehiculesDisponiblesPourFormulaire();

        if (mission == null) { // Mode création
            labelTitreFormulaireMission.setText("Planifier une Nouvelle Mission");
            champLibelleMissionForm.clear();
            choiceBoxVehiculeMissionForm.getSelectionModel().clearSelection();
            champSiteMissionForm.clear();
            datePickerDebutMissionForm.setValue(LocalDate.now());
            champHeureDebutMissionForm.setText(LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            datePickerFinMissionForm.setValue(null);
            champHeureFinMissionForm.clear();
            champKmPrevuMissionForm.clear();
            textAreaCircuitMissionForm.clear();
            textAreaObservationMissionForm.clear();
        } else { // Mode modification
            labelTitreFormulaireMission.setText("Modifier la Mission : " + mission.getLibMission());
            champLibelleMissionForm.setText(mission.getLibMission());
            Vehicule vAssigne = servicePersistance.trouverVehiculeParId(mission.getIdVehicule());
            if (vAssigne != null) { // Ajouter le véhicule assigné à la liste s'il n'est pas "Disponible"
                if (!choiceBoxVehiculeMissionForm.getItems().contains(vAssigne)) {
                    choiceBoxVehiculeMissionForm.getItems().add(vAssigne);
                }
                choiceBoxVehiculeMissionForm.setValue(vAssigne);
            }
            champSiteMissionForm.setText(mission.getSite());
            if (mission.getDateDebutMission() != null) {
                datePickerDebutMissionForm.setValue(mission.getDateDebutMission().toLocalDate());
                champHeureDebutMissionForm.setText(mission.getDateDebutMission().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            if (mission.getDateFinMission() != null) {
                datePickerFinMissionForm.setValue(mission.getDateFinMission().toLocalDate());
                champHeureFinMissionForm.setText(mission.getDateFinMission().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            champKmPrevuMissionForm.setText(mission.getKmPrevu() != null ? String.valueOf(mission.getKmPrevu()) : "");
            textAreaCircuitMissionForm.setText(mission.getCircuitMission());
            textAreaObservationMissionForm.setText(mission.getObservationMission());
        }
    }

    private void chargerVehiculesDisponiblesPourFormulaire() {
        try {
            EtatVoiture etatDisponible = servicePersistance.trouverEtatVoitureParLibelle("Disponible");
            List<Vehicule> vehiculesDisponibles;
            if (etatDisponible != null) {
                vehiculesDisponibles = servicePersistance.trouverVehiculesParEtat(etatDisponible.getIdEtatVoiture());
            } else {
                CONTROLEUR_FORM_MISSION_LOGGER.warning("L'état 'Disponible' non trouvé, chargement de tous les véhicules.");
                vehiculesDisponibles = servicePersistance.trouverTousLesVehicules(); // Fallback
            }

            choiceBoxVehiculeMissionForm.setItems(FXCollections.observableArrayList(vehiculesDisponibles));
            choiceBoxVehiculeMissionForm.setConverter(new StringConverter<Vehicule>() {
                @Override public String toString(Vehicule v) { return v == null ? "Sélectionner..." : v.getMarque() + " " + v.getModele() + " (" + v.getImmatriculation() + ")"; }
                @Override public Vehicule fromString(String string) { return null; }
            });
        } catch (Exception e) {
            CONTROLEUR_FORM_MISSION_LOGGER.log(Level.SEVERE, "Impossible de charger les véhicules pour le formulaire de mission.", e);
            afficherNotificationAlerteInterface("Erreur Données Véhicules", "Liste des véhicules indisponible: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionEnregistrerMission() {
        String libelle = champLibelleMissionForm.getText();
        Vehicule vehiculeSelectionne = choiceBoxVehiculeMissionForm.getValue();
        String site = champSiteMissionForm.getText();
        LocalDate dateDebut = datePickerDebutMissionForm.getValue();
        String heureDebutStr = champHeureDebutMissionForm.getText();
        LocalDate dateFin = datePickerFinMissionForm.getValue();
        String heureFinStr = champHeureFinMissionForm.getText();
        String kmPrevuStr = champKmPrevuMissionForm.getText();
        String circuit = textAreaCircuitMissionForm.getText();
        String observation = textAreaObservationMissionForm.getText();

        if (libelle.trim().isEmpty() || vehiculeSelectionne == null || dateDebut == null || heureDebutStr.trim().isEmpty()) {
            afficherNotificationAlerteInterface("Champs Obligatoires", "Libellé, véhicule, date et heure de début sont requis.", Alert.AlertType.WARNING);
            return;
        }

        LocalDateTime dateTimeDebut;
        LocalDateTime dateTimeFin = null;
        Integer kmPrevu = null;

        try {
            LocalTime heureDebut = LocalTime.parse(heureDebutStr, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            dateTimeDebut = LocalDateTime.of(dateDebut, heureDebut);

            if (dateFin != null && !heureFinStr.trim().isEmpty()) {
                LocalTime heureFin = LocalTime.parse(heureFinStr, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                dateTimeFin = LocalDateTime.of(dateFin, heureFin);
            } else if (dateFin != null) { // Si date mais pas heure, prendre fin de journée
                dateTimeFin = LocalDateTime.of(dateFin, LocalTime.MAX);
            }


            if (kmPrevuStr != null && !kmPrevuStr.trim().isEmpty()) {
                kmPrevu = Integer.parseInt(kmPrevuStr);
            }
        } catch (java.time.format.DateTimeParseException e) {
            afficherNotificationAlerteInterface("Format Heure Invalide", "Veuillez entrer les heures au format HH:MM (ex: 09:30).", Alert.AlertType.ERROR);
            return;
        } catch (NumberFormatException e) {
            afficherNotificationAlerteInterface("Format Nombre Invalide", "Le kilométrage prévu doit être un nombre entier.", Alert.AlertType.ERROR);
            return;
        }

        try {
            if (missionExistantePourModification == null) { // Création
                Mission nouvelleMission = new Mission();
                nouvelleMission.setLibMission(libelle);
                nouvelleMission.setIdVehicule(vehiculeSelectionne.getIdVehicule());
                nouvelleMission.setSite(site);
                nouvelleMission.setDateDebutMission(dateTimeDebut);
                nouvelleMission.setDateFinMission(dateTimeFin);
                nouvelleMission.setKmPrevu(kmPrevu);
                nouvelleMission.setCircuitMission(circuit);
                nouvelleMission.setObservationMission(observation);
                serviceLogiqueMetier.planifierNouvelleMission(nouvelleMission);
                afficherNotificationAlerteInterface("Mission Planifiée", "La nouvelle mission a été enregistrée avec succès.", Alert.AlertType.INFORMATION);
            } else { // Modification
                missionExistantePourModification.setLibMission(libelle);
                missionExistantePourModification.setIdVehicule(vehiculeSelectionne.getIdVehicule());
                missionExistantePourModification.setSite(site);
                missionExistantePourModification.setDateDebutMission(dateTimeDebut);
                missionExistantePourModification.setDateFinMission(dateTimeFin);
                missionExistantePourModification.setKmPrevu(kmPrevu);
                missionExistantePourModification.setCircuitMission(circuit);
                missionExistantePourModification.setObservationMission(observation);
                // Le statut est géré par BusinessLogicService pour la modification
                serviceLogiqueMetier.modifierMission(missionExistantePourModification);
                afficherNotificationAlerteInterface("Mission Modifiée", "Les informations de la mission ont été mises à jour.", Alert.AlertType.INFORMATION);
            }
            actionAnnulerEtFermer();
        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            CONTROLEUR_FORM_MISSION_LOGGER.log(Level.WARNING, "Erreur de validation/logique métier lors de l'enregistrement de la mission.", e);
            afficherNotificationAlerteInterface("Échec Enregistrement Mission", e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            CONTROLEUR_FORM_MISSION_LOGGER.log(Level.SEVERE, "Erreur inattendue lors de l'enregistrement de la mission.", e);
            afficherNotificationAlerteInterface("Erreur Système", "Une erreur imprévue est survenue: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    @FXML
    private void actionAnnulerEtFermer() {
        Stage stage = (Stage) boutonAnnulerMissionForm.getScene().getWindow();
        stage.close();
    }

    private void afficherNotificationAlerteInterface(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) champLibelleMissionForm.getScene().getWindow();
        if (stageProprietaire != null && stageProprietaire.isShowing()) {
            alerte.initOwner(stageProprietaire);
        }
        alerte.showAndWait();
    }
}