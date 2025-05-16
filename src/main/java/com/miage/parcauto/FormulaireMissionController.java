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
    private static final Logger FORM_MISSION_LOGGER = Logger.getLogger(FormulaireMissionController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    private Mission missionEnCoursEdition;
    private boolean modeCreation;

    @FXML private TextField champLibelleMission;
    @FXML private ChoiceBox<Vehicule> choiceBoxVehiculeMission;
    @FXML private TextField champSiteMission;
    @FXML private DatePicker datePickerDebutMission;
    @FXML private TextField champHeureDebutMission; // Format HH:mm
    @FXML private DatePicker datePickerFinMissionPrevue; // Optionnel, ou calculé
    @FXML private TextField champHeureFinMissionPrevue; // Optionnel
    @FXML private TextField champKmPrevuMission;
    @FXML private TextArea textAreaCircuitMission;
    @FXML private TextArea textAreaObservationMission;

    @FXML private Button boutonSauvegarderMission;
    @FXML private Button boutonAnnulerFormulaireMission;
    @FXML private Label etiquetteTitreFormulaireMission;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_MISSION_LOGGER.fine("Dépendances services injectées dans FormulaireMissionController.");
    }

    @Override
    public void initialiserDonneesVue() {
        FORM_MISSION_LOGGER.info("Initialisation des données pour le formulaire de mission.");
        chargerOptionsChoiceBoxVehicules();
        configurerConvertisseursDateHeure();

        if (modeCreation) {
            etiquetteTitreFormulaireMission.setText("Planification d'une Nouvelle Mission");
            this.missionEnCoursEdition = new Mission();
            datePickerDebutMission.setValue(LocalDate.now()); // Pré-remplir date début
            champHeureDebutMission.setText(LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        } else if (this.missionEnCoursEdition != null) {
            etiquetteTitreFormulaireMission.setText("Modification de la Mission : " + missionEnCoursEdition.getLibMission());
            preRemplirChampsFormulaireMission();
        } else {
            FORM_MISSION_LOGGER.severe("Formulaire mission ouvert en mode édition sans mission fournie.");
            afficherNotificationAlerteFormulaire("Erreur Critique de Données", "Aucune mission n'a été spécifiée pour la modification.", Alert.AlertType.ERROR);
            actionAnnulerFormulaire();
        }
    }

    public void preparerFormulairePourEdition(Mission mission) {
        this.missionEnCoursEdition = mission;
        this.modeCreation = (mission == null);
    }

    private void chargerOptionsChoiceBoxVehicules() {
        try {
            // Charger uniquement les véhicules disponibles
            EtatVoiture etatDisponible = servicePersistance.trouverEtatVoitureParLibelle("Disponible");
            List<Vehicule> vehiculesDisponibles;
            if (etatDisponible != null) {
                vehiculesDisponibles = servicePersistance.trouverVehiculesParEtat(etatDisponible.getIdEtatVoiture());
            } else {
                FORM_MISSION_LOGGER.warning("L'état 'Disponible' n'a pas été trouvé. Tous les véhicules sont listés pour la mission.");
                vehiculesDisponibles = servicePersistance.trouverTousLesVehicules(); // Fallback, moins idéal
            }

            // Si en mode édition et que le véhicule de la mission n'est plus "Disponible", l'ajouter quand même à la liste pour qu'il soit sélectionné
            if (!modeCreation && missionEnCoursEdition != null) {
                Vehicule vehiculeMissionActuelle = servicePersistance.trouverVehiculeParId(missionEnCoursEdition.getIdVehicule());
                if (vehiculeMissionActuelle != null && !vehiculesDisponibles.contains(vehiculeMissionActuelle)) {
                    // Vérifier si l'état actuel du véhicule de la mission est celui de la mission.
                    // Si la mission est "Planifiée", le véhicule devrait être "Disponible" ou celui assigné.
                    // Si la mission est déjà "En cours" (ne devrait pas arriver ici car on modifie que les planifiées),
                    // le véhicule serait "En mission".
                    boolean estPresent = vehiculesDisponibles.stream().anyMatch(v -> v.getIdVehicule() == vehiculeMissionActuelle.getIdVehicule());
                    if (!estPresent) {
                        vehiculesDisponibles.add(0, vehiculeMissionActuelle); // L'ajouter en tête
                    }
                }
            }


            choiceBoxVehiculeMission.setItems(FXCollections.observableArrayList(vehiculesDisponibles));
            choiceBoxVehiculeMission.setConverter(new StringConverter<Vehicule>() {
                @Override public String toString(Vehicule vehicule) {
                    return vehicule == null ? "" : vehicule.getImmatriculation() + " (" + vehicule.getMarque() + " " + vehicule.getModele() + ")";
                }
                @Override public Vehicule fromString(String string) { return null; } // Non nécessaire pour la sélection
            });
        } catch (Exception e) {
            FORM_MISSION_LOGGER.log(Level.SEVERE, "Erreur lors du chargement de la liste des véhicules disponibles.", e);
            afficherNotificationAlerteFormulaire("Erreur de Chargement", "Impossible de charger la liste des véhicules : " + e.getMessage(), Alert.AlertType.ERROR);
        }
        FORM_MISSION_LOGGER.fine("Liste des véhicules disponibles chargée pour la sélection.");
    }

    private void configurerConvertisseursDateHeure() {
        datePickerDebutMission.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerFinMissionPrevue.setConverter(ViewController.obtenirConvertisseurDateStandard());
        // Ajouter des validateurs ou des formateurs pour les champs d'heure si nécessaire
        FORM_MISSION_LOGGER.fine("Convertisseurs de date pour les DatePicker du formulaire mission configurés.");
    }

    private void preRemplirChampsFormulaireMission() {
        if (missionEnCoursEdition == null) return;

        champLibelleMission.setText(missionEnCoursEdition.getLibMission());
        Vehicule vehiculeAssigne = servicePersistance.trouverVehiculeParId(missionEnCoursEdition.getIdVehicule());
        if (vehiculeAssigne != null) choiceBoxVehiculeMission.setValue(vehiculeAssigne);

        champSiteMission.setText(missionEnCoursEdition.getSite());
        if (missionEnCoursEdition.getDateDebutMission() != null) {
            datePickerDebutMission.setValue(missionEnCoursEdition.getDateDebutMission().toLocalDate());
            champHeureDebutMission.setText(missionEnCoursEdition.getDateDebutMission().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (missionEnCoursEdition.getDateFinMission() != null) { // Date de fin prévue
            datePickerFinMissionPrevue.setValue(missionEnCoursEdition.getDateFinMission().toLocalDate());
            champHeureFinMissionPrevue.setText(missionEnCoursEdition.getDateFinMission().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
        champKmPrevuMission.setText(missionEnCoursEdition.getKmPrevu() != null ? String.valueOf(missionEnCoursEdition.getKmPrevu()) : "");
        textAreaCircuitMission.setText(missionEnCoursEdition.getCircuitMission());
        textAreaObservationMission.setText(missionEnCoursEdition.getObservationMission());

        FORM_MISSION_LOGGER.info("Champs du formulaire pré-remplis avec les données de la mission ID: " + missionEnCoursEdition.getIdMission());
    }

    @FXML
    private void actionSauvegarderMission() {
        FORM_MISSION_LOGGER.fine("Tentative de sauvegarde de la mission.");
        if (!validerSaisiesFormulaireMission()) {
            FORM_MISSION_LOGGER.warning("Validation des saisies du formulaire mission échouée.");
            return;
        }

        try {
            missionEnCoursEdition.setLibMission(champLibelleMission.getText().trim());
            if (choiceBoxVehiculeMission.getValue() == null) {
                throw new ErreurValidation("Un véhicule doit être sélectionné pour la mission.");
            }
            missionEnCoursEdition.setIdVehicule(choiceBoxVehiculeMission.getValue().getIdVehicule());
            missionEnCoursEdition.setSite(champSiteMission.getText().trim());

            LocalDate dateDebut = datePickerDebutMission.getValue();
            LocalTime heureDebut = LocalTime.parse(champHeureDebutMission.getText(), java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            missionEnCoursEdition.setDateDebutMission(LocalDateTime.of(dateDebut, heureDebut));

            if (datePickerFinMissionPrevue.getValue() != null && !champHeureFinMissionPrevue.getText().trim().isEmpty()) {
                LocalDate dateFinPrevue = datePickerFinMissionPrevue.getValue();
                LocalTime heureFinPrevue = LocalTime.parse(champHeureFinMissionPrevue.getText(), java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                missionEnCoursEdition.setDateFinMission(LocalDateTime.of(dateFinPrevue, heureFinPrevue)); // Pour la date de fin *prévue*
            } else {
                missionEnCoursEdition.setDateFinMission(null); // Pas de date de fin prévue explicitement
            }

            missionEnCoursEdition.setKmPrevu(champKmPrevuMission.getText().isEmpty() ? null : Integer.parseInt(champKmPrevuMission.getText()));
            missionEnCoursEdition.setCircuitMission(textAreaCircuitMission.getText());
            missionEnCoursEdition.setObservationMission(textAreaObservationMission.getText());
            // Le statut est géré par BusinessLogicService (PLANIFIEE par défaut à la création)

            if (modeCreation) {
                serviceLogiqueMetier.planifierNouvelleMission(missionEnCoursEdition);
                FORM_MISSION_LOGGER.info("Nouvelle mission planifiée avec succès : " + missionEnCoursEdition.getLibMission());
                afficherNotificationAlerteFormulaire("Planification Réussie", "La nouvelle mission a été planifiée avec succès.", Alert.AlertType.INFORMATION);
            } else {
                // S'assurer que le statut n'est pas modifié ici si ce n'est pas l'intention
                // La modification d'une mission planifiée ne change généralement pas son statut (reste PLANIFIEE)
                missionEnCoursEdition.setStatus(StatutMission.PLANIFIEE); // Redondant si déjà planifiée, mais sûr
                serviceLogiqueMetier.modifierMissionPlanifiee(missionEnCoursEdition); // Méthode à créer dans BusinessLogicService
                FORM_MISSION_LOGGER.info("Mission ID " + missionEnCoursEdition.getIdMission() + " modifiée avec succès.");
                afficherNotificationAlerteFormulaire("Modification Réussie", "Les informations de la mission ont été mises à jour.", Alert.AlertType.INFORMATION);
            }
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            FORM_MISSION_LOGGER.log(Level.WARNING, "Échec de la sauvegarde de la mission : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec de la Sauvegarde", e.getMessage(), Alert.AlertType.WARNING);
        } catch (NumberFormatException e) {
            FORM_MISSION_LOGGER.log(Level.WARNING, "Erreur de format numérique lors de la sauvegarde de la mission : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Erreur de Format Numérique", "Veuillez vérifier le champ de kilométrage prévu.", Alert.AlertType.ERROR);
        } catch (java.time.format.DateTimeParseException e) {
            FORM_MISSION_LOGGER.log(Level.WARNING, "Erreur de format d'heure lors de la sauvegarde de la mission : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Erreur de Format d'Heure", "Veuillez utiliser le format HH:mm pour les heures (ex: 09:30).", Alert.AlertType.ERROR);
        }
        catch (Exception e) {
            FORM_MISSION_LOGGER.log(Level.SEVERE, "Erreur système inattendue lors de la sauvegarde de la mission.", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Une erreur imprévue est survenue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validerSaisiesFormulaireMission() {
        StringBuilder messagesErreur = new StringBuilder();

        if (champLibelleMission.getText() == null || champLibelleMission.getText().trim().isEmpty()) {
            messagesErreur.append("Le libellé de la mission est requis.\n");
        }
        if (choiceBoxVehiculeMission.getValue() == null) {
            messagesErreur.append("Un véhicule doit être sélectionné.\n");
        }
        if (datePickerDebutMission.getValue() == null) {
            messagesErreur.append("La date de début de mission est requise.\n");
        }
        if (champHeureDebutMission.getText() == null || champHeureDebutMission.getText().trim().isEmpty() || !champHeureDebutMission.getText().matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
            messagesErreur.append("L'heure de début de mission est requise au format HH:mm.\n");
        }
        if (!champHeureFinMissionPrevue.getText().trim().isEmpty() && !champHeureFinMissionPrevue.getText().matches("^([01]\\d|2[0-3]):([0-5]\\d)$")){
            messagesErreur.append("L'heure de fin prévue doit être au format HH:mm si renseignée.\n");
        }

        if (datePickerFinMissionPrevue.getValue() != null && datePickerDebutMission.getValue() != null && datePickerFinMissionPrevue.getValue().isBefore(datePickerDebutMission.getValue())){
            messagesErreur.append("La date de fin prévue ne peut être antérieure à la date de début.\n");
        } else if (datePickerFinMissionPrevue.getValue() != null && datePickerDebutMission.getValue() != null && datePickerFinMissionPrevue.getValue().isEqual(datePickerDebutMission.getValue())) {
            if (!champHeureDebutMission.getText().trim().isEmpty() && !champHeureFinMissionPrevue.getText().trim().isEmpty() &&
                    LocalTime.parse(champHeureFinMissionPrevue.getText()).isBefore(LocalTime.parse(champHeureDebutMission.getText()))) {
                messagesErreur.append("L'heure de fin prévue ne peut être antérieure à l'heure de début si les dates sont identiques.\n");
            }
        }


        try { if (!champKmPrevuMission.getText().trim().isEmpty()) Integer.parseInt(champKmPrevuMission.getText().trim()); }
        catch (NumberFormatException e) { messagesErreur.append("Le kilométrage prévu doit être un entier valide.\n"); }

        if (messagesErreur.length() > 0) {
            afficherNotificationAlerteFormulaire("Erreurs de Validation de Saisie", messagesErreur.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void actionAnnulerFormulaire() {
        FORM_MISSION_LOGGER.fine("Action annuler formulaire mission.");
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerFormulaireMission.getScene().getWindow();
        stage.close();
        FORM_MISSION_LOGGER.info("Formulaire mission fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) boutonAnnulerFormulaireMission.getScene().getWindow();
        if (stageProprietaire != null) {
            alerte.initOwner(stageProprietaire);
        }
        alerte.showAndWait();
    }
}