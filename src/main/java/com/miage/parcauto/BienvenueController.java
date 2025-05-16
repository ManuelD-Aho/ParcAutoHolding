package main.java.com.miage.parcauto;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import main.java.com.miage.parcauto.AppModels.Utilisateur;
import java.util.Objects;
import java.util.logging.Logger;

public class BienvenueController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_BIENVENUE_LOGGER = Logger.getLogger(BienvenueController.class.getName());
    // Pas besoin de services pour une vue simple, mais on implémente l'interface par cohérence.
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private Label labelMessageAccueilUtilisateur;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = bls; // Peut être null si non utilisé
        this.gestionnaireSecurite = sm;
        this.moteurRapports = re;
        this.servicePersistance = ps;
        CONTROLEUR_BIENVENUE_LOGGER.fine("Dépendances services (potentiellement nulles) injectées dans BienvenueController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_BIENVENUE_LOGGER.info("Initialisation de la vue de bienvenue.");
        if (SessionManager.estUtilisateurConnecte() && labelMessageAccueilUtilisateur != null) {
            Utilisateur user = SessionManager.obtenirUtilisateurActuel();
            labelMessageAccueilUtilisateur.setText("Bienvenue dans le système de gestion du Parc Automobile MIAGE Holding, " + user.getLogin() + ".\n\n" +
                    "Utilisez le menu de navigation pour accéder aux différentes fonctionnalités.");
        } else if (labelMessageAccueilUtilisateur != null) {
            labelMessageAccueilUtilisateur.setText("Bienvenue dans le système de gestion du Parc Automobile MIAGE Holding.\n\n" +
                    "Veuillez vous connecter pour accéder aux fonctionnalités.");
        }
    }
}