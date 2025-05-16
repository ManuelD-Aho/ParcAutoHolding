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

import main.java.com.miage.parcauto.AppModels.EnergieVehicule;
import main.java.com.miage.parcauto.AppModels.EtatVoiture;
import main.java.com.miage.parcauto.AppModels.Vehicule;
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

public class FormulaireVehiculeController implements ViewController.InitializableServices {
    private static final Logger FORM_CONTROLLER_LOGGER = Logger.getLogger(FormulaireVehiculeController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite; // Non utilisé ici mais injecté par cohérence
    private ReportingEngine moteurRapports;     // Non utilisé ici
    private PersistenceService servicePersistance;

    private Vehicule vehiculeEnCoursEdition;
    private boolean modeCreation;

    @FXML private TextField champImmatriculationVehicule;
    @FXML private TextField champNumeroChassisVehicule;
    @FXML private TextField champMarqueVehicule;
    @FXML private TextField champModeleVehicule;
    @FXML private TextField champCouleurVehicule;
    @FXML private TextField champNombrePlacesVehicule;
    @FXML private TextField champPuissanceVehicule;
    @FXML private TextField champKilometrageActuelVehicule;
    @FXML private TextField champPrixAcquisitionVehicule;

    @FXML private ChoiceBox<EnergieVehicule> choiceBoxEnergieVehicule;
    @FXML private ChoiceBox<EtatVoiture> choiceBoxEtatVehicule;

    @FXML private DatePicker datePickerAcquisitionVehicule;
    @FXML private DatePicker datePickerMiseEnServiceVehicule;
    @FXML private DatePicker datePickerAmortissementVehicule;
    @FXML private DatePicker datePickerEtatVehicule;

    @FXML private Button boutonSauvegarderVehicule;
    @FXML private Button boutonAnnulerFormulaireVehicule;
    @FXML private Label etiquetteTitreFormulaireVehicule;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_CONTROLLER_LOGGER.fine("Dépendances services injectées dans FormulaireVehiculeController.");
    }

    @Override
    public void initialiserDonneesVue() {
        FORM_CONTROLLER_LOGGER.info("Initialisation des données pour le formulaire véhicule.");
        chargerOptionsChoiceBox();
        configurerConvertisseursDate();

        if (modeCreation) {
            etiquetteTitreFormulaireVehicule.setText("Création d'un Nouveau Véhicule");
            this.vehiculeEnCoursEdition = new Vehicule(); // Nouvel objet pour la création
            // Pré-remplir état à "Disponible" et date état à aujourd'hui
            EtatVoiture etatDisponible = servicePersistance.trouverEtatVoitureParLibelle("Disponible");
            if (etatDisponible != null) {
                choiceBoxEtatVehicule.setValue(etatDisponible);
            }
            datePickerEtatVehicule.setValue(LocalDate.now());

        } else if (this.vehiculeEnCoursEdition != null) {
            etiquetteTitreFormulaireVehicule.setText("Modification du Véhicule : " + vehiculeEnCoursEdition.getImmatriculation());
            preRemplirChampsFormulaire();
        } else {
            FORM_CONTROLLER_LOGGER.severe("Formulaire véhicule ouvert en mode édition sans véhicule fourni.");
            afficherNotificationAlerteFormulaire("Erreur Critique de Données", "Aucun véhicule n'a été spécifié pour la modification.", Alert.AlertType.ERROR);
            actionAnnulerFormulaire(); // Fermer le formulaire
        }
    }

    public void preparerFormulairePourEdition(Vehicule vehicule) {
        this.vehiculeEnCoursEdition = vehicule; // Peut être null pour la création
        this.modeCreation = (vehicule == null);
        // L'initialisation réelle se fait dans initialiserDonneesVue après l'injection des services
    }

    private void chargerOptionsChoiceBox() {
        try {
            List<EnergieVehicule> energies = List.of(EnergieVehicule.values());
            choiceBoxEnergieVehicule.setItems(FXCollections.observableArrayList(energies));
            choiceBoxEnergieVehicule.setConverter(new StringConverter<EnergieVehicule>() {
                @Override public String toString(EnergieVehicule energie) { return energie == null ? "" : energie.getDbValue(); }
                @Override public EnergieVehicule fromString(String string) { return EnergieVehicule.fromDbValue(string); }
            });

            List<EtatVoiture> etats = servicePersistance.trouverTousLesEtatsVoiture();
            choiceBoxEtatVehicule.setItems(FXCollections.observableArrayList(etats));
            choiceBoxEtatVehicule.setConverter(new StringConverter<EtatVoiture>() {
                @Override public String toString(EtatVoiture etat) { return etat == null ? "" : etat.getLibEtatVoiture(); }
                @Override public EtatVoiture fromString(String string) { // Non utilisé pour ChoiceBox simple
                    return etats.stream().filter(e -> e.getLibEtatVoiture().equals(string)).findFirst().orElse(null);
                }
            });
        } catch (Exception e) {
            FORM_CONTROLLER_LOGGER.log(Level.SEVERE, "Erreur lors du chargement des options pour les ChoiceBox du formulaire véhicule.", e);
            afficherNotificationAlerteFormulaire("Erreur de Chargement", "Impossible de charger les listes déroulantes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        FORM_CONTROLLER_LOGGER.fine("Options des ChoiceBox (Energie, Etat) chargées.");
    }

    private void configurerConvertisseursDate() {
        datePickerAcquisitionVehicule.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerMiseEnServiceVehicule.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerAmortissementVehicule.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerEtatVehicule.setConverter(ViewController.obtenirConvertisseurDateStandard());
        FORM_CONTROLLER_LOGGER.fine("Convertisseurs de date pour les DatePicker configurés.");
    }

    private void preRemplirChampsFormulaire() {
        if (vehiculeEnCoursEdition == null) return;

        champImmatriculationVehicule.setText(vehiculeEnCoursEdition.getImmatriculation());
        champNumeroChassisVehicule.setText(vehiculeEnCoursEdition.getNumeroChassi());
        champMarqueVehicule.setText(vehiculeEnCoursEdition.getMarque());
        champModeleVehicule.setText(vehiculeEnCoursEdition.getModele());
        champCouleurVehicule.setText(vehiculeEnCoursEdition.getCouleur());
        champNombrePlacesVehicule.setText(vehiculeEnCoursEdition.getNbPlaces() != null ? String.valueOf(vehiculeEnCoursEdition.getNbPlaces()) : "");
        champPuissanceVehicule.setText(vehiculeEnCoursEdition.getPuissance() != null ? String.valueOf(vehiculeEnCoursEdition.getPuissance()) : "");
        champKilometrageActuelVehicule.setText(vehiculeEnCoursEdition.getKmActuels() != null ? String.valueOf(vehiculeEnCoursEdition.getKmActuels()) : "");
        champPrixAcquisitionVehicule.setText(vehiculeEnCoursEdition.getPrixVehicule() != null ? vehiculeEnCoursEdition.getPrixVehicule().toPlainString() : "");

        choiceBoxEnergieVehicule.setValue(vehiculeEnCoursEdition.getEnergie());
        EtatVoiture etatActuel = servicePersistance.trouverEtatVoitureParId(vehiculeEnCoursEdition.getIdEtatVoiture());
        if (etatActuel != null) choiceBoxEtatVehicule.setValue(etatActuel);

        if (vehiculeEnCoursEdition.getDateAcquisition() != null) datePickerAcquisitionVehicule.setValue(vehiculeEnCoursEdition.getDateAcquisition().toLocalDate());
        if (vehiculeEnCoursEdition.getDateMiseEnService() != null) datePickerMiseEnServiceVehicule.setValue(vehiculeEnCoursEdition.getDateMiseEnService().toLocalDate());
        if (vehiculeEnCoursEdition.getDateAmmortissement() != null) datePickerAmortissementVehicule.setValue(vehiculeEnCoursEdition.getDateAmmortissement().toLocalDate());
        if (vehiculeEnCoursEdition.getDateEtat() != null) datePickerEtatVehicule.setValue(vehiculeEnCoursEdition.getDateEtat().toLocalDate());

        FORM_CONTROLLER_LOGGER.info("Champs du formulaire pré-remplis avec les données du véhicule ID: " + vehiculeEnCoursEdition.getIdVehicule());
    }

    @FXML
    private void actionSauvegarderVehicule() {
        FORM_CONTROLLER_LOGGER.fine("Tentative de sauvegarde du véhicule.");
        if (!validerSaisiesFormulaireVehicule()) {
            FORM_CONTROLLER_LOGGER.warning("Validation des saisies du formulaire véhicule échouée.");
            return; // Message d'erreur déjà affiché par la méthode de validation
        }

        try {
            // Alimenter l'objet vehiculeEnCoursEdition avec les données du formulaire
            vehiculeEnCoursEdition.setImmatriculation(champImmatriculationVehicule.getText().trim());
            vehiculeEnCoursEdition.setNumeroChassi(champNumeroChassisVehicule.getText().trim());
            vehiculeEnCoursEdition.setMarque(champMarqueVehicule.getText().trim());
            vehiculeEnCoursEdition.setModele(champModeleVehicule.getText().trim());
            vehiculeEnCoursEdition.setCouleur(champCouleurVehicule.getText().trim());
            vehiculeEnCoursEdition.setNbPlaces(champNombrePlacesVehicule.getText().isEmpty() ? null : Integer.parseInt(champNombrePlacesVehicule.getText()));
            vehiculeEnCoursEdition.setPuissance(champPuissanceVehicule.getText().isEmpty() ? null : Integer.parseInt(champPuissanceVehicule.getText()));
            vehiculeEnCoursEdition.setKmActuels(champKilometrageActuelVehicule.getText().isEmpty() ? null : Integer.parseInt(champKilometrageActuelVehicule.getText()));
            vehiculeEnCoursEdition.setPrixVehicule(champPrixAcquisitionVehicule.getText().isEmpty() ? null : new BigDecimal(champPrixAcquisitionVehicule.getText().replace(",", ".")));

            vehiculeEnCoursEdition.setEnergie(choiceBoxEnergieVehicule.getValue());
            if (choiceBoxEtatVehicule.getValue() != null) {
                vehiculeEnCoursEdition.setIdEtatVoiture(choiceBoxEtatVehicule.getValue().getIdEtatVoiture());
            } else {
                throw new ErreurValidation("L'état du véhicule est obligatoire.");
            }

            if (datePickerAcquisitionVehicule.getValue() != null) vehiculeEnCoursEdition.setDateAcquisition(datePickerAcquisitionVehicule.getValue().atStartOfDay());
            if (datePickerMiseEnServiceVehicule.getValue() != null) vehiculeEnCoursEdition.setDateMiseEnService(datePickerMiseEnServiceVehicule.getValue().atStartOfDay());
            if (datePickerAmortissementVehicule.getValue() != null) vehiculeEnCoursEdition.setDateAmmortissement(datePickerAmortissementVehicule.getValue().atStartOfDay());
            if (datePickerEtatVehicule.getValue() != null) vehiculeEnCoursEdition.setDateEtat(datePickerEtatVehicule.getValue().atTime(LocalTime.now())); // Ou heure fixe

            if (modeCreation) {
                serviceLogiqueMetier.creerNouveauVehicule(vehiculeEnCoursEdition);
                FORM_CONTROLLER_LOGGER.info("Nouveau véhicule créé avec succès : " + vehiculeEnCoursEdition.getImmatriculation());
                afficherNotificationAlerteFormulaire("Création Réussie", "Le nouveau véhicule a été ajouté avec succès au parc.", Alert.AlertType.INFORMATION);
            } else {
                serviceLogiqueMetier.modifierVehicule(vehiculeEnCoursEdition);
                FORM_CONTROLLER_LOGGER.info("Véhicule ID " + vehiculeEnCoursEdition.getIdVehicule() + " modifié avec succès.");
                afficherNotificationAlerteFormulaire("Modification Réussie", "Les informations du véhicule ont été mises à jour.", Alert.AlertType.INFORMATION);
            }
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            FORM_CONTROLLER_LOGGER.log(Level.WARNING, "Échec de la sauvegarde du véhicule : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec de la Sauvegarde", e.getMessage(), Alert.AlertType.WARNING);
        } catch (NumberFormatException e) {
            FORM_CONTROLLER_LOGGER.log(Level.WARNING, "Erreur de format numérique lors de la sauvegarde : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Erreur de Format Numérique", "Veuillez vérifier les champs numériques (Places, Puissance, KM, Prix). Utilisez le point '.' comme séparateur décimal pour le prix.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            FORM_CONTROLLER_LOGGER.log(Level.SEVERE, "Erreur système inattendue lors de la sauvegarde du véhicule.", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Une erreur imprévue est survenue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validerSaisiesFormulaireVehicule() {
        StringBuilder messagesErreur = new StringBuilder();

        if (champImmatriculationVehicule.getText() == null || champImmatriculationVehicule.getText().trim().isEmpty()) {
            messagesErreur.append("L'immatriculation est requise.\n");
        }
        if (champNumeroChassisVehicule.getText() == null || champNumeroChassisVehicule.getText().trim().isEmpty()) {
            messagesErreur.append("Le numéro de châssis est requis.\n");
        }
        if (champMarqueVehicule.getText() == null || champMarqueVehicule.getText().trim().isEmpty()) {
            messagesErreur.append("La marque est requise.\n");
        }
        if (champModeleVehicule.getText() == null || champModeleVehicule.getText().trim().isEmpty()) {
            messagesErreur.append("Le modèle est requis.\n");
        }
        if (choiceBoxEnergieVehicule.getValue() == null) {
            messagesErreur.append("Le type d'énergie est requis.\n");
        }
        if (choiceBoxEtatVehicule.getValue() == null) {
            messagesErreur.append("L'état actuel du véhicule est requis.\n");
        }
        if (datePickerEtatVehicule.getValue() == null) {
            messagesErreur.append("La date de l'état actuel est requise.\n");
        }

        // Validations numériques optionnelles
        try { if (!champNombrePlacesVehicule.getText().trim().isEmpty()) Integer.parseInt(champNombrePlacesVehicule.getText().trim()); }
        catch (NumberFormatException e) { messagesErreur.append("Le nombre de places doit être un entier valide.\n"); }

        try { if (!champPuissanceVehicule.getText().trim().isEmpty()) Integer.parseInt(champPuissanceVehicule.getText().trim()); }
        catch (NumberFormatException e) { messagesErreur.append("La puissance doit être un entier valide.\n"); }

        try { if (!champKilometrageActuelVehicule.getText().trim().isEmpty()) Integer.parseInt(champKilometrageActuelVehicule.getText().trim()); }
        catch (NumberFormatException e) { messagesErreur.append("Le kilométrage actuel doit être un entier valide.\n"); }

        try { if (!champPrixAcquisitionVehicule.getText().trim().isEmpty()) new BigDecimal(champPrixAcquisitionVehicule.getText().trim().replace(",", ".")); }
        catch (NumberFormatException e) { messagesErreur.append("Le prix d'acquisition doit être un nombre valide (ex: 15000.50).\n"); }


        if (messagesErreur.length() > 0) {
            afficherNotificationAlerteFormulaire("Erreurs de Validation", messagesErreur.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void actionAnnulerFormulaire() {
        FORM_CONTROLLER_LOGGER.fine("Action annuler formulaire véhicule.");
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerFormulaireVehicule.getScene().getWindow();
        stage.close();
        FORM_CONTROLLER_LOGGER.info("Formulaire véhicule fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        // Assurer la modalité par rapport à la fenêtre du formulaire elle-même
        Stage stageProprietaire = (Stage) boutonAnnulerFormulaireVehicule.getScene().getWindow();
        if (stageProprietaire != null) {
            alerte.initOwner(stageProprietaire);
        }
        alerte.showAndWait();
    }
}