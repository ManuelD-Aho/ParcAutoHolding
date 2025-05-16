package main.java.com.miage.parcauto;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.Personnel;
import main.java.com.miage.parcauto.AppModels.SocietaireCompte;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormulaireCompteSocietaireController implements ViewController.InitializableServices {
    private static final Logger FORM_COMPTE_LOGGER = Logger.getLogger(FormulaireCompteSocietaireController.class.getName());

    private BusinessLogicService serviceLogiqueMetier;
    private PersistenceService servicePersistance;
    // SecurityManager et ReportingEngine non directement utilisés ici mais injectés par cohérence
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;

    private SocietaireCompte compteEnCoursEdition;
    private boolean modeCreation;

    @FXML private TextField champNomSocietaire;
    @FXML private TextField champNumeroCompteSocietaire;
    @FXML private TextField champEmailSocietaire;
    @FXML private TextField champTelephoneSocietaire;
    @FXML private ChoiceBox<Personnel> choiceBoxPersonnelAssocie; // Optionnel, si un compte est lié à un employé
    @FXML private TextField champSoldeInitial; // Uniquement en mode création

    @FXML private Button boutonSauvegarderCompteSocietaire;
    @FXML private Button boutonAnnulerFormulaireCompte;
    @FXML private Label etiquetteTitreFormulaireCompte;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        FORM_COMPTE_LOGGER.fine("Dépendances services injectées dans FormulaireCompteSocietaireController.");
    }

    @Override
    public void initialiserDonneesVue() {
        FORM_COMPTE_LOGGER.info("Initialisation des données pour le formulaire compte sociétaire.");
        chargerOptionsPersonnelAssocie();

        champSoldeInitial.setVisible(modeCreation);
        champSoldeInitial.setManaged(modeCreation);


        if (modeCreation) {
            etiquetteTitreFormulaireCompte.setText("Création d'un Nouveau Compte Sociétaire");
            this.compteEnCoursEdition = new SocietaireCompte();
            champSoldeInitial.setText("0.00"); // Solde initial par défaut
        } else if (this.compteEnCoursEdition != null) {
            etiquetteTitreFormulaireCompte.setText("Modification du Compte : " + compteEnCoursEdition.getNom());
            preRemplirChampsFormulaireCompte();
        } else {
            FORM_COMPTE_LOGGER.severe("Formulaire compte sociétaire ouvert en mode édition sans compte fourni.");
            afficherNotificationAlerteFormulaire("Erreur Critique", "Aucun compte n'a été spécifié pour modification.", Alert.AlertType.ERROR);
            actionAnnulerFormulaire();
        }
    }

    public void preparerFormulairePourEdition(SocietaireCompte compte) {
        this.compteEnCoursEdition = compte;
        this.modeCreation = (compte == null);
    }

    private void chargerOptionsPersonnelAssocie() {
        try {
            List<Personnel> personnelList = servicePersistance.trouverToutLePersonnel(); // Ou une version filtrée
            choiceBoxPersonnelAssocie.setItems(FXCollections.observableArrayList(personnelList));
            choiceBoxPersonnelAssocie.setConverter(new StringConverter<Personnel>() {
                @Override public String toString(Personnel p) { return p == null ? "Aucun" : p.getNomPersonnel() + " " + p.getPrenomPersonnel() + " (Mat: " + p.getMatricule() + ")"; }
                @Override public Personnel fromString(String string) { return null; }
            });
            choiceBoxPersonnelAssocie.getItems().add(0, null); // Option pour "Aucun"
            choiceBoxPersonnelAssocie.setValue(null);
        } catch (Exception e) {
            FORM_COMPTE_LOGGER.log(Level.WARNING, "Impossible de charger la liste du personnel.", e);
        }
    }

    private void preRemplirChampsFormulaireCompte() {
        if (compteEnCoursEdition == null) return;
        champNomSocietaire.setText(compteEnCoursEdition.getNom());
        champNumeroCompteSocietaire.setText(compteEnCoursEdition.getNumero());
        champEmailSocietaire.setText(compteEnCoursEdition.getEmail());
        champTelephoneSocietaire.setText(compteEnCoursEdition.getTelephone());

        if (compteEnCoursEdition.getIdPersonnel() != null) {
            Personnel pAssocie = servicePersistance.trouverPersonnelParId(compteEnCoursEdition.getIdPersonnel());
            choiceBoxPersonnelAssocie.setValue(pAssocie);
        }
        // Le solde n'est pas modifié directement ici, mais via des mouvements.
        FORM_COMPTE_LOGGER.info("Champs du formulaire pré-remplis pour compte ID: " + compteEnCoursEdition.getIdSocietaire());
    }

    @FXML
    private void actionSauvegarderCompteSocietaire() {
        FORM_COMPTE_LOGGER.fine("Tentative de sauvegarde du compte sociétaire.");
        if (!validerSaisiesFormulaireCompte()) {
            FORM_COMPTE_LOGGER.warning("Validation des saisies du formulaire compte sociétaire échouée.");
            return;
        }

        try {
            compteEnCoursEdition.setNom(champNomSocietaire.getText().trim());
            compteEnCoursEdition.setNumero(champNumeroCompteSocietaire.getText().trim());
            compteEnCoursEdition.setEmail(champEmailSocietaire.getText() != null ? champEmailSocietaire.getText().trim() : null);
            compteEnCoursEdition.setTelephone(champTelephoneSocietaire.getText() != null ? champTelephoneSocietaire.getText().trim() : null);

            Personnel pSelectionne = choiceBoxPersonnelAssocie.getValue();
            compteEnCoursEdition.setIdPersonnel(pSelectionne != null ? pSelectionne.getIdPersonnel() : null);

            if (modeCreation) {
                BigDecimal soldeInitial = new BigDecimal(champSoldeInitial.getText().replace(",","."));
                compteEnCoursEdition.setSolde(soldeInitial); // Solde défini à la création
                serviceLogiqueMetier.creerNouveauCompteSocietaire(compteEnCoursEdition); // Méthode à créer
                FORM_COMPTE_LOGGER.info("Nouveau compte sociétaire créé : " + compteEnCoursEdition.getNom());
                afficherNotificationAlerteFormulaire("Création Réussie", "Le compte sociétaire a été créé.", Alert.AlertType.INFORMATION);
            } else {
                serviceLogiqueMetier.modifierCompteSocietaire(compteEnCoursEdition); // Méthode à créer
                FORM_COMPTE_LOGGER.info("Compte sociétaire ID " + compteEnCoursEdition.getIdSocietaire() + " modifié.");
                afficherNotificationAlerteFormulaire("Modification Réussie", "Les informations du compte ont été mises à jour.", Alert.AlertType.INFORMATION);
            }
            fermerFormulaire();

        } catch (ErreurValidation | ErreurLogiqueMetier e) {
            FORM_COMPTE_LOGGER.log(Level.WARNING, "Échec de la sauvegarde du compte sociétaire : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Échec Sauvegarde", e.getMessage(), Alert.AlertType.WARNING);
        } catch (NumberFormatException e) {
            FORM_COMPTE_LOGGER.log(Level.WARNING, "Erreur format solde initial : " + e.getMessage(), e);
            afficherNotificationAlerteFormulaire("Erreur Format", "Le solde initial doit être un nombre valide.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            FORM_COMPTE_LOGGER.log(Level.SEVERE, "Erreur système lors de la sauvegarde du compte sociétaire.", e);
            afficherNotificationAlerteFormulaire("Erreur Système", "Erreur imprévue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validerSaisiesFormulaireCompte() {
        StringBuilder messagesErreur = new StringBuilder();
        if (champNomSocietaire.getText() == null || champNomSocietaire.getText().trim().isEmpty()) {
            messagesErreur.append("Le nom du sociétaire est requis.\n");
        }
        if (champNumeroCompteSocietaire.getText() == null || champNumeroCompteSocietaire.getText().trim().isEmpty()) {
            messagesErreur.append("Le numéro de compte est requis.\n");
        }
        if (modeCreation) {
            if (champSoldeInitial.getText() == null || champSoldeInitial.getText().trim().isEmpty()) {
                messagesErreur.append("Le solde initial est requis pour un nouveau compte.\n");
            } else {
                try { new BigDecimal(champSoldeInitial.getText().replace(",",".")); }
                catch (NumberFormatException e) { messagesErreur.append("Format du solde initial invalide.\n");}
            }
        }
        // Ajouter d'autres validations (format email, téléphone, unicité numéro de compte, etc.)

        if (messagesErreur.length() > 0) {
            afficherNotificationAlerteFormulaire("Erreurs de Validation", messagesErreur.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void actionAnnulerFormulaire() {
        fermerFormulaire();
    }

    private void fermerFormulaire() {
        Stage stage = (Stage) boutonAnnulerFormulaireCompte.getScene().getWindow();
        stage.close();
        FORM_COMPTE_LOGGER.info("Formulaire compte sociétaire fermé.");
    }

    private void afficherNotificationAlerteFormulaire(String titre, String message, Alert.AlertType typeAlerte) {
        Alert alerte = new Alert(typeAlerte);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stageProprietaire = (Stage) boutonAnnulerFormulaireCompte.getScene().getWindow();
        if (stageProprietaire != null) alerte.initOwner(stageProprietaire);
        alerte.showAndWait();
    }
}