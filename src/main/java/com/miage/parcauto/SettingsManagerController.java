package main.java.com.miage.parcauto;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField; // Pour d'éventuels paramètres textuels
import javafx.stage.Stage;

import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppExceptions.*;


import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsManagerController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_SETTINGS_LOGGER = Logger.getLogger(SettingsManagerController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    // Exemple de champs FXML pour des paramètres futurs
    @FXML private CheckBox checkboxActiverMaintenanceAutomatique;
    @FXML private TextField champSeuilKmMaintenancePreventive;
    @FXML private Button boutonSauvegarderParametres;
    @FXML private Label labelStatutSauvegardeParametres;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_SETTINGS_LOGGER.fine("Dépendances services injectées dans SettingsManagerController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_SETTINGS_LOGGER.info("Initialisation du panneau des paramètres de l'application.");
        if (!gestionnaireSecurite.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.APPLICATION_CONFIGURER_PARAMETRES)) {
            afficherNotificationAlerteInterface("Accès Interdit", "Vous n'avez pas les droits pour configurer les paramètres.", Alert.AlertType.ERROR);
            if (boutonSauvegarderParametres != null && boutonSauvegarderParametres.getParent() != null) {
                boutonSauvegarderParametres.getParent().setVisible(false); // Masquer tout le contenu si pas autorisé
            }
            return;
        }
        chargerParametresActuels();
        labelStatutSauvegardeParametres.setText("");
    }

    private void chargerParametresActuels() {
        // Logique pour charger les paramètres depuis une source (ex: fichier de config, BDD dédiée)
        // Par exemple:
        // boolean maintenanceAutoActive = serviceLogiqueMetier.obtenirParametreBooleen("maintenance.auto.active", false);
        // int seuilKm = serviceLogiqueMetier.obtenirParametreEntier("maintenance.seuil.km", 20000);
        // checkboxActiverMaintenanceAutomatique.setSelected(maintenanceAutoActive);
        // champSeuilKmMaintenancePreventive.setText(String.valueOf(seuilKm));
        CONTROLEUR_SETTINGS_LOGGER.info("Paramètres actuels (fictifs) chargés dans l'interface.");
        // Pour l'instant, valeurs par défaut si les champs FXML existent
        if (checkboxActiverMaintenanceAutomatique != null) checkboxActiverMaintenanceAutomatique.setSelected(false);
        if (champSeuilKmMaintenancePreventive != null) champSeuilKmMaintenancePreventive.setText("20000");
    }

    @FXML
    private void actionSauvegarderTousLesParametres() {
        CONTROLEUR_SETTINGS_LOGGER.info("Tentative de sauvegarde des paramètres de l'application.");
        labelStatutSauvegardeParametres.setText("Sauvegarde en cours...");
        try {
            // boolean maintenanceAuto = checkboxActiverMaintenanceAutomatique.isSelected();
            // int seuilKm = Integer.parseInt(champSeuilKmMaintenancePreventive.getText());

            // serviceLogiqueMetier.sauvegarderParametre("maintenance.auto.active", maintenanceAuto);
            // serviceLogiqueMetier.sauvegarderParametre("maintenance.seuil.km", seuilKm);

            // Simuler une sauvegarde
            Thread.sleep(500); // Simuler un délai réseau/IO

            labelStatutSauvegardeParametres.setText("Paramètres sauvegardés avec succès à " + LocalDateTime.now().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE));
            afficherNotificationAlerteInterface("Sauvegarde Réussie", "Les paramètres de l'application ont été mis à jour.", Alert.AlertType.INFORMATION);
            CONTROLEUR_SETTINGS_LOGGER.info("Paramètres de l'application sauvegardés.");

        } catch (NumberFormatException e) {
            CONTROLEUR_SETTINGS_LOGGER.log(Level.WARNING, "Erreur de format numérique dans les paramètres.", e);
            afficherNotificationAlerteInterface("Erreur de Format", "Veuillez vérifier les valeurs numériques des paramètres.", Alert.AlertType.ERROR);
            labelStatutSauvegardeParametres.setText("Échec : format numérique invalide.");
        } /*catch (ErreurLogiqueMetier e) { // Si la sauvegarde des paramètres peut lever des erreurs métier
            CONTROLEUR_SETTINGS_LOGGER.log(Level.SEVERE, "Erreur métier lors de la sauvegarde des paramètres.", e);
            afficherNotificationAlerteInterface("Erreur de Sauvegarde", e.getMessage(), Alert.AlertType.ERROR);
            labelStatutSauvegardeParametres.setText("Échec de la sauvegarde : " + e.getMessage());
        }*/
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CONTROLEUR_SETTINGS_LOGGER.log(Level.WARNING, "Sauvegarde interrompue.", e);
            labelStatutSauvegardeParametres.setText("Sauvegarde interrompue.");
        }
        catch (Exception e) {
            CONTROLEUR_SETTINGS_LOGGER.log(Level.SEVERE, "Erreur imprévue lors de la sauvegarde des paramètres.", e);
            afficherNotificationAlerteInterface("Erreur Système", "Une erreur est survenue: " + e.getMessage(), Alert.AlertType.ERROR);
            labelStatutSauvegardeParametres.setText("Échec : erreur système.");
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