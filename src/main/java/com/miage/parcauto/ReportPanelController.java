package main.java.com.miage.parcauto;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportPanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_RAPPORT_LOGGER = Logger.getLogger(ReportPanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private DatePicker datePickerRapportDebut;
    @FXML private DatePicker datePickerRapportFin;
    @FXML private Button boutonGenererRapportInventaireVehicules;
    @FXML private Button boutonGenererRapportMissionsPeriode;
    @FXML private Button boutonGenererRapportTCOVehicule; // Nécessitera un moyen de sélectionner un véhicule
    @FXML private TextField champIdVehiculePourTCO; // Pour saisir l'ID du véhicule pour le TCO
    @FXML private TextArea zoneAffichageRapport; // Pour afficher des rapports textuels ou des résumés


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_RAPPORT_LOGGER.fine("Dépendances services injectées dans ReportPanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_RAPPORT_LOGGER.info("Initialisation du panneau des rapports et statistiques.");
        configurerConvertisseursDateRapport();
        configurerPermissionsRapports();
        zoneAffichageRapport.setEditable(false);
        zoneAffichageRapport.setText("Sélectionnez les paramètres et générez un rapport.\nLes rapports CSV seront sauvegardés dans le dossier 'rapports_generes'.");
    }

    private void configurerConvertisseursDateRapport() {
        datePickerRapportDebut.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerRapportFin.setConverter(ViewController.obtenirConvertisseurDateStandard());
    }

    private void configurerPermissionsRapports() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        // Pour l'instant, supposons que tous ceux qui accèdent au panel peuvent voir les options de rapport.
        // Des permissions plus fines pourraient être appliquées par type de rapport.
        boolean peutGenererRapports = gestionnaireSecurite.estAutorise(role, Permissions.ACCES_RAPPORTS_STATISTIQUES);

        if (boutonGenererRapportInventaireVehicules!=null) boutonGenererRapportInventaireVehicules.setDisable(!peutGenererRapports);
        if (boutonGenererRapportMissionsPeriode!=null) boutonGenererRapportMissionsPeriode.setDisable(!peutGenererRapports);
        if (boutonGenererRapportTCOVehicule!=null) boutonGenererRapportTCOVehicule.setDisable(!peutGenererRapports);
        if (champIdVehiculePourTCO!=null) champIdVehiculePourTCO.setDisable(!peutGenererRapports);
    }

    @FXML
    private void actionGenererRapportInventaireVehicules() {
        CONTROLEUR_RAPPORT_LOGGER.info("Génération du rapport d'inventaire des véhicules (CSV).");
        try {
            File rapportGenere = moteurRapports.genererRapportInventaireVehiculesCsv();
            zoneAffichageRapport.setText("Rapport d'inventaire des véhicules généré avec succès :\n" + rapportGenere.getAbsolutePath() +
                    "\n\nCe fichier CSV peut être ouvert avec un tableur (Excel, LibreOffice Calc, etc.).");
            afficherNotificationAlerteInterface("Rapport Généré", "Inventaire des véhicules exporté en CSV :\n" + rapportGenere.getName(), Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            CONTROLEUR_RAPPORT_LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport d'inventaire CSV.", e);
            afficherNotificationAlerteInterface("Erreur de Rapport", "Impossible de générer le rapport d'inventaire : " + e.getMessage(), Alert.AlertType.ERROR);
            zoneAffichageRapport.setText("Erreur lors de la génération du rapport d'inventaire : \n" + e.getMessage());
        }
    }

    @FXML
    private void actionGenererRapportMissionsSurPeriode() {
        LocalDate dateDebut = datePickerRapportDebut.getValue();
        LocalDate dateFin = datePickerRapportFin.getValue();

        if (dateDebut == null || dateFin == null) {
            afficherNotificationAlerteInterface("Paramètres Manquants", "Veuillez sélectionner une date de début et une date de fin pour le rapport des missions.", Alert.AlertType.WARNING);
            zoneAffichageRapport.setText("Veuillez spécifier une période (date de début et date de fin) pour générer le rapport des missions.");
            return;
        }
        if (dateFin.isBefore(dateDebut)) {
            afficherNotificationAlerteInterface("Dates Invalides", "La date de fin ne peut être antérieure à la date de début.", Alert.AlertType.WARNING);
            zoneAffichageRapport.setText("La date de fin sélectionnée est antérieure à la date de début. Veuillez corriger.");
            return;
        }

        LocalDateTime dateTimeDebut = dateDebut.atStartOfDay();
        LocalDateTime dateTimeFin = dateFin.atTime(LocalTime.MAX);

        CONTROLEUR_RAPPORT_LOGGER.info("Génération du rapport des missions (CSV) pour la période du " + dateDebut + " au " + dateFin + ".");
        try {
            File rapportGenere = moteurRapports.genererRapportMissionsParPeriodeCsv(dateTimeDebut, dateTimeFin);
            zoneAffichageRapport.setText("Rapport des missions pour la période du " + dateDebut.format(ViewController.FORMATTEUR_DATE_STANDARD_VUE) +
                    " au " + dateFin.format(ViewController.FORMATTEUR_DATE_STANDARD_VUE) + " généré avec succès :\n" + rapportGenere.getAbsolutePath() +
                    "\n\nCe fichier CSV peut être ouvert avec un tableur.");
            afficherNotificationAlerteInterface("Rapport Généré", "Rapport des missions exporté en CSV :\n" + rapportGenere.getName(), Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            CONTROLEUR_RAPPORT_LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des missions CSV.", e);
            afficherNotificationAlerteInterface("Erreur de Rapport", "Impossible de générer le rapport des missions : " + e.getMessage(), Alert.AlertType.ERROR);
            zoneAffichageRapport.setText("Erreur lors de la génération du rapport des missions : \n" + e.getMessage());
        }
    }

    @FXML
    private void actionGenererRapportTCO() {
        String idVehiculeSaisi = champIdVehiculePourTCO.getText();
        if (idVehiculeSaisi == null || idVehiculeSaisi.trim().isEmpty()) {
            afficherNotificationAlerteInterface("ID Véhicule Requis", "Veuillez saisir l'identifiant du véhicule pour calculer son TCO.", Alert.AlertType.WARNING);
            zoneAffichageRapport.setText("Veuillez entrer l'ID d'un véhicule pour générer son rapport TCO.");
            return;
        }
        try {
            int idVehicule = Integer.parseInt(idVehiculeSaisi.trim());
            CONTROLEUR_RAPPORT_LOGGER.info("Génération du rapport TCO pour le véhicule ID: " + idVehicule + ".");
            File rapportGenere = moteurRapports.genererRapportTCOVehicule(idVehicule); // Génère un .txt

            // Lire le contenu du fichier texte pour l'afficher dans la TextArea
            String contenuRapportTxt = Files.readString(rapportGenere.toPath());
            zoneAffichageRapport.setText("Rapport TCO pour Véhicule ID " + idVehicule + " :\n" +
                    "Fichier sauvegardé ici : " + rapportGenere.getAbsolutePath() + "\n\n" +
                    "------------------------------------------------------\n" +
                    contenuRapportTxt);
            afficherNotificationAlerteInterface("Rapport TCO Généré", "Rapport TCO pour véhicule ID " + idVehicule + " généré :\n" + rapportGenere.getName(), Alert.AlertType.INFORMATION);

        } catch (NumberFormatException e) {
            afficherNotificationAlerteInterface("ID Invalide", "L'identifiant du véhicule doit être un nombre entier.", Alert.AlertType.ERROR);
            zoneAffichageRapport.setText("L'ID du véhicule saisi n'est pas un nombre valide.");
        } catch (IllegalArgumentException e) { // Lancée par ReportingEngine si véhicule non trouvé
            afficherNotificationAlerteInterface("Véhicule Non Trouvé", e.getMessage(), Alert.AlertType.WARNING);
            zoneAffichageRapport.setText(e.getMessage());
        } catch (IOException e) {
            CONTROLEUR_RAPPORT_LOGGER.log(Level.SEVERE, "Erreur lors de la génération ou lecture du rapport TCO.", e);
            afficherNotificationAlerteInterface("Erreur de Rapport TCO", "Impossible de générer ou lire le rapport TCO : " + e.getMessage(), Alert.AlertType.ERROR);
            zoneAffichageRapport.setText("Erreur lors de la génération du rapport TCO : \n" + e.getMessage());
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