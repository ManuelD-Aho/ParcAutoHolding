package main.java.com.miage.parcauto;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import main.java.com.miage.parcauto.AppModels.TypeMouvement;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormulaireMouvementController implements ViewController.InitializableServices {
    private static final Logger FORM_MVT_LOGGER = Logger.getLogger(FormulaireMouvementController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private PersistenceService servicePersistance; // Pourrait être utile pour des infos complémentaires
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;

    private int idSocietaireConcerne;
    private TypeMouvement typeMouvementAEffectuer;

    @FXML private Label etiquetteTitreFormulaireMouvement;
    @FXML private Label labelTypeOperationMouvement;
    @FXML private TextField champMontantMouvement;
    @FXML private Button boutonConfirmerMouvement;
    @FXML private Button boutonAnnulerMouvement;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_MVT_LOGGER.fine("Dépendances services injectées dans FormulaireMouvementController.");
    }

    @Override
    public void initialiserDonneesVue() {
        // Pas grand chose à initialiser ici avant la préparation
    }

    public void preparerFormulaire(int idSocietaire, TypeMouvement typeMouvement) {
        this.idSocietaireConcerne = idSocietaire;
        this.typeMouvementAEffectuer = Objects.requireNonNull(typeMouvement);

        etiquetteTitreFormulaireMouvement.setText(typeMouvement.getDbValue() + " sur Compte");
        labelTypeOperationMouvement.setText("Montant du " + typeMouvement.getDbValue().toLowerCase() + " :");
        FORM_MVT_LOGGER.info("Formulaire préparé pour un " + typeMouvement.getDbValue() + " sur compte ID: " + idSocietaire);
    }

    @FXML
    private void actionConfirmerMouvement() {
        FORM_MVT_LOGGER.fine("Tentative de confirmation du mouvement.");
        String montantSaisi = champMontantMouvement.getText();
        if (montantSaisi == null || montantSaisi.trim().isEmpty()) {
            afficherNotificationAlerteFormulaire("Montant Requis", "Veuillez saisir un montant pour l'opération.", Alert.AlertType.WARNING);
            return;
        }

        BigDecimal montantOperation;
        try {
            montantOperation = new BigDecimal(montantSaisi.replace(",", "."));
            if (montantOperation.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("Le montant doit être positif.");
            }
        } catch (NumberFormatException e) {
            afficherNotificationAlerteFormulaire("Format Montant Invalide", "Le montant doit être un nombre positif. Utilisez '.' comme séparateur décimal.", Alert.AlertType.ERROR);
            return;
        }

        try {
            if (typeMouvementAEffectuer == TypeMouvement.DEPOT) {
                serviceLogiqueMetier.effectuerDepotSurCompteSocietaire(idSocietaireConcerne, montantOperation);
            } else if (typeMouvementAEffectuer == TypeMouvement.RETRAIT) {
                serviceLogiqueMetier.effectuerRetraitDeCompteSocietaire(idSocietaireConcerne, montantOperation);
            }
            // Gérer TypeMouvement.MENSUALITE si applicable via un autre mécanisme

            FORM_MVT_LOGGER.info(typeMouvementAEffectuer.getDbValue() + " de " + montantOperation + " effectué avec succès pour compte ID: " + idSocietaireConcerne);
            afficherNotificationAlerteFormulaire("Opération Réussie", "Le " + typeMouvementAEffectuer.getDbValue().toLowerCase() + " a été enregistré.", Alert.AlertType.INFORMATION);
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            FORM_MVT_LOGGER.log(Level.WARNING, "Échec de l'opération ("+typeMouvementAEffectuer.getDbValue()+") : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec Opération", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            FORM_MVT_LOGGER.log(Level.SEVERE, "Erreur système lors de l'opération ("+typeMouvementAEffectuer.getDbValue()+").", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Erreur imprévue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionAnnulerMouvement() {
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerMouvement.getScene().getWindow();
        stage.close();
        FORM_MVT_LOGGER.info("Formulaire mouvement fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) boutonAnnulerMouvement.getScene().getWindow();
        if (stageProprietaire != null) alerte.initOwner(stageProprietaire);
        alerte.showAndWait();
    }
}