package main.java.com.miage.parcauto;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import main.java.com.miage.parcauto.AppModels.Utilisateur; // Pour le nom d'utilisateur
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardApercuController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_DASHBOARD_LOGGER = Logger.getLogger(DashboardApercuController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private Label labelMessageBienvenueDashboard;
    @FXML private Label labelNombreVehiculesTotal;
    @FXML private Label labelNombreMissionsEnCours;
    @FXML private Label labelNombreEntretiensOuverts;
    // Ajouter d'autres labels pour des statistiques clés

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_DASHBOARD_LOGGER.fine("Dépendances services injectées dans DashboardApercuController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_DASHBOARD_LOGGER.info("Initialisation du tableau de bord principal.");
        if (SessionManager.estUtilisateurConnecte()) {
            Utilisateur user = SessionManager.obtenirUtilisateurActuel();
            if (labelMessageBienvenueDashboard != null) {
                labelMessageBienvenueDashboard.setText("Bienvenue, " + user.getLogin() + " ! Aperçu rapide de l'activité du parc automobile :");
            }
        }
        chargerStatistiquesApercu();
    }

    private void chargerStatistiquesApercu() {
        try {
            // Ces méthodes devront être créées dans PersistenceService ou BusinessLogicService
            if (labelNombreVehiculesTotal != null) {
                int nbVehicules = servicePersistance.compterTousLesVehicules();
                labelNombreVehiculesTotal.setText(String.valueOf(nbVehicules));
            }
            if (labelNombreMissionsEnCours != null) {
                int nbMissions = servicePersistance.compterMissionsParStatut(AppModels.StatutMission.EN_COURS);
                labelNombreMissionsEnCours.setText(String.valueOf(nbMissions));
            }
            if (labelNombreEntretiensOuverts != null) {
                int nbEntretiens = servicePersistance.compterEntretiensParStatutOT(AppModels.StatutOrdreTravail.OUVERT);
                labelNombreEntretiensOuverts.setText(String.valueOf(nbEntretiens));
            }
            CONTROLEUR_DASHBOARD_LOGGER.info("Statistiques d'aperçu chargées pour le tableau de bord.");
        } catch (Exception e) {
            CONTROLEUR_DASHBOARD_LOGGER.log(Level.SEVERE, "Erreur lors du chargement des statistiques du tableau de bord.", e);
            // Afficher un message d'erreur discret ou laisser les champs vides
            if (labelNombreVehiculesTotal != null) labelNombreVehiculesTotal.setText("Erreur");
            if (labelNombreMissionsEnCours != null) labelNombreMissionsEnCours.setText("Erreur");
            if (labelNombreEntretiensOuverts != null) labelNombreEntretiensOuverts.setText("Erreur");
        }
    }

    @FXML
    private void actionActualiserStatistiquesDashboard() {
        CONTROLEUR_DASHBOARD_LOGGER.info("Action d'actualisation manuelle des statistiques du tableau de bord.");
        chargerStatistiquesApercu();
    }
}