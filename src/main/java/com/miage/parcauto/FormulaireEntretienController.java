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

import main.java.com.miage.parcauto.AppModels.Entretien;
import main.java.com.miage.parcauto.AppModels.TypeEntretien;
import main.java.com.miage.parcauto.AppModels.StatutOrdreTravail;
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

public class FormulaireEntretienController implements ViewController.InitializableServices {
    private static final Logger FORM_ENTRETIEN_LOGGER = Logger.getLogger(FormulaireEntretienController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    private Entretien entretienEnCoursEdition;
    private boolean modeCreation;

    @FXML private ChoiceBox<Vehicule> choiceBoxVehiculeEntretien;
    @FXML private DatePicker datePickerEntreeEntretien;
    @FXML private TextField champHeureEntreeEntretien; // HH:mm
    @FXML private DatePicker datePickerSortieEntretien;
    @FXML private TextField champHeureSortieEntretien; // HH:mm
    @FXML private TextField champMotifEntretien;
    @FXML private TextArea textAreaObservationEntretien;
    @FXML private TextField champCoutEntretien;
    @FXML private TextField champLieuEntretien;
    @FXML private ChoiceBox<TypeEntretien> choiceBoxTypeEntretien;
    @FXML private ChoiceBox<StatutOrdreTravail> choiceBoxStatutOTEntretien;

    @FXML private Button boutonSauvegarderEntretien;
    @FXML private Button boutonAnnulerFormulaireEntretien;
    @FXML private Label etiquetteTitreFormulaireEntretien;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_ENTRETIEN_LOGGER.fine("Dépendances services injectées dans FormulaireEntretienController.");
    }

    @Override
    public void initialiserDonneesVue() {
        FORM_ENTRETIEN_LOGGER.info("Initialisation des données pour le formulaire d'entretien.");
        chargerOptionsChoiceBoxEntretien();
        configurerConvertisseursDateHeureEntretien();

        if (modeCreation) {
            etiquetteTitreFormulaireEntretien.setText("Enregistrer un Nouvel Entretien");
            this.entretienEnCoursEdition = new Entretien();
            datePickerEntreeEntretien.setValue(LocalDate.now());
            champHeureEntreeEntretien.setText(LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            choiceBoxStatutOTEntretien.setValue(StatutOrdreTravail.OUVERT); // Statut par défaut
            choiceBoxTypeEntretien.setValue(TypeEntretien.CORRECTIF); // Type par défaut
        } else if (this.entretienEnCoursEdition != null) {
            etiquetteTitreFormulaireEntretien.setText("Modification de l'Entretien ID: " + entretienEnCoursEdition.getIdEntretien());
            preRemplirChampsFormulaireEntretien();
        } else {
            FORM_ENTRETIEN_LOGGER.severe("Formulaire entretien ouvert en mode édition sans entretien fourni.");
            afficherNotificationAlerteFormulaire("Erreur Critique de Données", "Aucun entretien n'a été spécifié pour la modification.", Alert.AlertType.ERROR);
            actionAnnulerFormulaire();
        }
    }

    public void preparerFormulairePourEdition(Entretien entretien) {
        this.entretienEnCoursEdition = entretien;
        this.modeCreation = (entretien == null);
    }

    private void chargerOptionsChoiceBoxEntretien() {
        try {
            List<Vehicule> tousLesVehicules = servicePersistance.trouverTousLesVehicules();
            choiceBoxVehiculeEntretien.setItems(FXCollections.observableArrayList(tousLesVehicules));
            choiceBoxVehiculeEntretien.setConverter(new StringConverter<Vehicule>() {
                @Override public String toString(Vehicule v) { return v == null ? "" : v.getImmatriculation() + " (" + v.getMarque() + " " + v.getModele() + ")"; }
                @Override public Vehicule fromString(String string) { return null; }
            });

            choiceBoxTypeEntretien.setItems(FXCollections.observableArrayList(TypeEntretien.values()));
            choiceBoxTypeEntretien.setConverter(new StringConverter<TypeEntretien>() {
                @Override public String toString(TypeEntretien t) { return t == null ? "" : t.getDbValue(); }
                @Override public TypeEntretien fromString(String string) { return TypeEntretien.fromDbValue(string); }
            });

            choiceBoxStatutOTEntretien.setItems(FXCollections.observableArrayList(StatutOrdreTravail.values()));
            choiceBoxStatutOTEntretien.setConverter(new StringConverter<StatutOrdreTravail>() {
                @Override public String toString(StatutOrdreTravail s) { return s == null ? "" : s.getDbValue(); }
                @Override public StatutOrdreTravail fromString(String string) { return StatutOrdreTravail.fromDbValue(string); }
            });
            // En mode création, le statut est souvent "Ouvert" par défaut. En modification, il est pré-rempli.
            // Le changement de statut se fait plutôt via le dialogue dédié dans EntretienPanelController.
            // Ici, on le rend non-éditable si l'entretien est déjà clôturé.
            if (!modeCreation && entretienEnCoursEdition != null && entretienEnCoursEdition.getStatutOt() == StatutOrdreTravail.CLOTURE) {
                choiceBoxStatutOTEntretien.setDisable(true);
            }


        } catch (Exception e) {
            FORM_ENTRETIEN_LOGGER.log(Level.SEVERE, "Erreur lors du chargement des options pour les ChoiceBox du formulaire entretien.", e);
            afficherNotificationAlerteFormulaire("Erreur de Chargement", "Impossible de charger les listes déroulantes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        FORM_ENTRETIEN_LOGGER.fine("Options des ChoiceBox (Véhicule, Type, Statut OT) chargées.");
    }

    private void configurerConvertisseursDateHeureEntretien() {
        datePickerEntreeEntretien.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerSortieEntretien.setConverter(ViewController.obtenirConvertisseurDateStandard());
        FORM_ENTRETIEN_LOGGER.fine("Convertisseurs de date pour les DatePicker du formulaire entretien configurés.");
    }

    private void preRemplirChampsFormulaireEntretien() {
        if (entretienEnCoursEdition == null) return;

        Vehicule vehiculeConcerne = servicePersistance.trouverVehiculeParId(entretienEnCoursEdition.getIdVehicule());
        if (vehiculeConcerne != null) choiceBoxVehiculeEntretien.setValue(vehiculeConcerne);

        if (entretienEnCoursEdition.getDateEntreeEntr() != null) {
            datePickerEntreeEntretien.setValue(entretienEnCoursEdition.getDateEntreeEntr().toLocalDate());
            champHeureEntreeEntretien.setText(entretienEnCoursEdition.getDateEntreeEntr().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (entretienEnCoursEdition.getDateSortieEntr() != null) {
            datePickerSortieEntretien.setValue(entretienEnCoursEdition.getDateSortieEntr().toLocalDate());
            champHeureSortieEntretien.setText(entretienEnCoursEdition.getDateSortieEntr().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
        champMotifEntretien.setText(entretienEnCoursEdition.getMotifEntr());
        textAreaObservationEntretien.setText(entretienEnCoursEdition.getObservation());
        champCoutEntretien.setText(entretienEnCoursEdition.getCoutEntr() != null ? entretienEnCoursEdition.getCoutEntr().toPlainString() : "");
        champLieuEntretien.setText(entretienEnCoursEdition.getLieuEntr());
        choiceBoxTypeEntretien.setValue(entretienEnCoursEdition.getType());
        choiceBoxStatutOTEntretien.setValue(entretienEnCoursEdition.getStatutOt());

        FORM_ENTRETIEN_LOGGER.info("Champs du formulaire pré-remplis avec les données de l'entretien ID: " + entretienEnCoursEdition.getIdEntretien());
    }

    @FXML
    private void actionSauvegarderEntretien() {
        FORM_ENTRETIEN_LOGGER.fine("Tentative de sauvegarde de l'entretien.");
        if (!validerSaisiesFormulaireEntretien()) {
            FORM_ENTRETIEN_LOGGER.warning("Validation des saisies du formulaire entretien échouée.");
            return;
        }

        try {
            if (choiceBoxVehiculeEntretien.getValue() == null) throw new ErreurValidation("Un véhicule doit être sélectionné.");
            entretienEnCoursEdition.setIdVehicule(choiceBoxVehiculeEntretien.getValue().getIdVehicule());

            LocalDate dateEntree = datePickerEntreeEntretien.getValue();
            LocalTime heureEntree = LocalTime.parse(champHeureEntreeEntretien.getText(), java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            entretienEnCoursEdition.setDateEntreeEntr(LocalDateTime.of(dateEntree, heureEntree));

            if (datePickerSortieEntretien.getValue() != null && !champHeureSortieEntretien.getText().trim().isEmpty()) {
                LocalDate dateSortie = datePickerSortieEntretien.getValue();
                LocalTime heureSortie = LocalTime.parse(champHeureSortieEntretien.getText(), java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                entretienEnCoursEdition.setDateSortieEntr(LocalDateTime.of(dateSortie, heureSortie));
            } else {
                entretienEnCoursEdition.setDateSortieEntr(null);
            }

            entretienEnCoursEdition.setMotifEntr(champMotifEntretien.getText().trim());
            entretienEnCoursEdition.setObservation(textAreaObservationEntretien.getText());
            entretienEnCoursEdition.setCoutEntr(champCoutEntretien.getText().isEmpty() ? null : new BigDecimal(champCoutEntretien.getText().replace(",", ".")));
            entretienEnCoursEdition.setLieuEntr(champLieuEntretien.getText().trim());
            if (choiceBoxTypeEntretien.getValue() == null) throw new ErreurValidation("Le type d'entretien est requis.");
            entretienEnCoursEdition.setType(choiceBoxTypeEntretien.getValue());
            if (choiceBoxStatutOTEntretien.getValue() == null) throw new ErreurValidation("Le statut de l'ordre de travail est requis.");
            entretienEnCoursEdition.setStatutOt(choiceBoxStatutOTEntretien.getValue());


            if (modeCreation) {
                serviceLogiqueMetier.creerNouvelEntretien(entretienEnCoursEdition);
                FORM_ENTRETIEN_LOGGER.info("Nouvel entretien enregistré avec succès pour véhicule ID: " + entretienEnCoursEdition.getIdVehicule());
                afficherNotificationAlerteFormulaire("Enregistrement Réussi", "Le nouvel entretien a été enregistré.", Alert.AlertType.INFORMATION);
            } else {
                serviceLogiqueMetier.modifierEntretien(entretienEnCoursEdition);
                FORM_ENTRETIEN_LOGGER.info("Entretien ID " + entretienEnCoursEdition.getIdEntretien() + " modifié avec succès.");
                afficherNotificationAlerteFormulaire("Modification Réussie", "Les informations de l'entretien ont été mises à jour.", Alert.AlertType.INFORMATION);
            }
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            FORM_ENTRETIEN_LOGGER.log(Level.WARNING, "Échec de la sauvegarde de l'entretien : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec de la Sauvegarde", e.getMessage(), Alert.AlertType.WARNING);
        } catch (NumberFormatException e) {
            FORM_ENTRETIEN_LOGGER.log(Level.WARNING, "Erreur de format numérique lors de la sauvegarde de l'entretien : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Erreur de Format Numérique", "Veuillez vérifier le champ du coût. Utilisez le point '.' comme séparateur décimal.", Alert.AlertType.ERROR);
        } catch (java.time.format.DateTimeParseException e) {
            FORM_ENTRETIEN_LOGGER.log(Level.WARNING, "Erreur de format d'heure lors de la sauvegarde de l'entretien : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Erreur de Format d'Heure", "Veuillez utiliser le format HH:mm pour les heures (ex: 09:30).", Alert.AlertType.ERROR);
        }
        catch (Exception e) {
            FORM_ENTRETIEN_LOGGER.log(Level.SEVERE, "Erreur système inattendue lors de la sauvegarde de l'entretien.", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Une erreur imprévue est survenue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validerSaisiesFormulaireEntretien() {
        StringBuilder messagesErreur = new StringBuilder();

        if (choiceBoxVehiculeEntretien.getValue() == null) messagesErreur.append("Un véhicule doit être sélectionné.\n");
        if (datePickerEntreeEntretien.getValue() == null) messagesErreur.append("La date d'entrée en entretien est requise.\n");
        if (champHeureEntreeEntretien.getText() == null || champHeureEntreeEntretien.getText().trim().isEmpty() || !champHeureEntreeEntretien.getText().matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
            messagesErreur.append("L'heure d'entrée est requise au format HH:mm.\n");
        }
        if (champMotifEntretien.getText() == null || champMotifEntretien.getText().trim().isEmpty()) {
            messagesErreur.append("Le motif de l'entretien est requis.\n");
        }
        if (choiceBoxTypeEntretien.getValue() == null) messagesErreur.append("Le type d'entretien est requis.\n");
        if (choiceBoxStatutOTEntretien.getValue() == null) messagesErreur.append("Le statut de l'OT est requis.\n");

        if (datePickerSortieEntretien.getValue() != null && (champHeureSortieEntretien.getText() == null || champHeureSortieEntretien.getText().trim().isEmpty() || !champHeureSortieEntretien.getText().matches("^([01]\\d|2[0-3]):([0-5]\\d)$"))) {
            messagesErreur.append("Si une date de sortie est spécifiée, l'heure de sortie (HH:mm) est également requise.\n");
        } else if (datePickerSortieEntretien.getValue() == null && !champHeureSortieEntretien.getText().trim().isEmpty()) {
            messagesErreur.append("Si une heure de sortie est spécifiée, la date de sortie est également requise.\n");
        }

        if (datePickerEntreeEntretien.getValue() != null && datePickerSortieEntretien.getValue() != null &&
                datePickerSortieEntretien.getValue().isBefore(datePickerEntreeEntretien.getValue())) {
            messagesErreur.append("La date de sortie ne peut être antérieure à la date d'entrée.\n");
        } else if (datePickerEntreeEntretien.getValue() != null && datePickerSortieEntretien.getValue() != null &&
                datePickerSortieEntretien.getValue().isEqual(datePickerEntreeEntretien.getValue())) {
            if (!champHeureEntreeEntretien.getText().trim().isEmpty() && !champHeureSortieEntretien.getText().trim().isEmpty() &&
                    LocalTime.parse(champHeureSortieEntretien.getText()).isBefore(LocalTime.parse(champHeureEntreeEntretien.getText()))) {
                messagesErreur.append("L'heure de sortie ne peut être antérieure à l'heure d'entrée si les dates sont identiques.\n");
            }
        }


        try { if (!champCoutEntretien.getText().trim().isEmpty()) new BigDecimal(champCoutEntretien.getText().trim().replace(",", ".")); }
        catch (NumberFormatException e) { messagesErreur.append("Le coût de l'entretien doit être un nombre valide (ex: 150.75).\n"); }

        if (messagesErreur.length() > 0) {
            afficherNotificationAlerteFormulaire("Erreurs de Validation de Saisie", messagesErreur.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void actionAnnulerFormulaire() {
        FORM_ENTRETIEN_LOGGER.fine("Action annuler formulaire entretien.");
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerFormulaireEntretien.getScene().getWindow();
        stage.close();
        FORM_ENTRETIEN_LOGGER.info("Formulaire entretien fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) boutonAnnulerFormulaireEntretien.getScene().getWindow();
        if (stageProprietaire != null) {
            alerte.initOwner(stageProprietaire);
        }
        alerte.showAndWait();
    }
}