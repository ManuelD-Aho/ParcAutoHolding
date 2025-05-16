package main.java.com.miage.parcauto;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.util.StringConverter;
import main.java.com.miage.parcauto.AppModels.EnergieVehicule;
import main.java.com.miage.parcauto.AppModels.EtatVoiture;
import main.java.com.miage.parcauto.AppModels.Vehicule;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur; // Pour les permissions
import main.java.com.miage.parcauto.AppDataTransferObjects.VehiculeDTO;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VehiculePanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_LOGGER = Logger.getLogger(VehiculePanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private TableView<VehiculeDTO> tableVueVehicules;
    @FXML private TableColumn<VehiculeDTO, Integer> colIdVehiculeTable;
    @FXML private TableColumn<VehiculeDTO, String> colImmatriculationTable;
    @FXML private TableColumn<VehiculeDTO, String> colMarqueTable;
    @FXML private TableColumn<VehiculeDTO, String> colModeleTable;
    @FXML private TableColumn<VehiculeDTO, String> colEtatVehiculeTable;
    @FXML private TableColumn<VehiculeDTO, String> colEnergieTable;
    @FXML private TableColumn<VehiculeDTO, Integer> colKmActuelsTable;
    @FXML private TableColumn<VehiculeDTO, String> colDateMiseServiceTable;

    @FXML private Button boutonAjouterVehicule;
    @FXML private Button boutonModifierVehicule;
    @FXML private Button boutonSupprimerVehicule;
    @FXML private Button boutonActualiserListeVehicules;
    @FXML private Button boutonDetailsVehicule;
    @FXML private Button boutonChangerEtatVehicule;

    @FXML private TextField champRechercheVehiculeImmat;
    @FXML private ChoiceBox<String> choiceBoxFiltreEtatVehicule;
    @FXML private ChoiceBox<EnergieVehicule> choiceBoxFiltreEnergieVehicule;


    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_LOGGER.fine("Dépendances services injectées dans VehiculePanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_LOGGER.info("Initialisation des données et de la configuration pour le panneau de gestion des véhicules.");
        configurerColonnesTableVehicules();
        chargerOptionsFiltresVehicules();
        configurerPermissionsActionsVehicules();
        actionActualiserListeVehicules(); // Charger les données initiales

        tableVueVehicules.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean estSelectionne = newSelection != null;
            if (boutonModifierVehicule != null) boutonModifierVehicule.setDisable(!estSelectionne || !peutModifierVehicule());
            if (boutonSupprimerVehicule != null) boutonSupprimerVehicule.setDisable(!estSelectionne || !peutSupprimerVehicule());
            if (boutonDetailsVehicule != null) boutonDetailsVehicule.setDisable(!estSelectionne);
            if (boutonChangerEtatVehicule != null) boutonChangerEtatVehicule.setDisable(!estSelectionne || !peutChangerEtatVehicule());
        });
    }

    private void configurerColonnesTableVehicules() {
        colIdVehiculeTable.setCellValueFactory(new PropertyValueFactory<>("idVehicule"));
        colImmatriculationTable.setCellValueFactory(new PropertyValueFactory<>("immatriculation"));
        colMarqueTable.setCellValueFactory(new PropertyValueFactory<>("marque"));
        colModeleTable.setCellValueFactory(new PropertyValueFactory<>("modele"));
        colEtatVehiculeTable.setCellValueFactory(new PropertyValueFactory<>("etatLibelle"));
        colEnergieTable.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEnergie() != null ? cellData.getValue().getEnergie().getDbValue() : "N/A"));
        colKmActuelsTable.setCellValueFactory(new PropertyValueFactory<>("kmActuels"));
        colDateMiseServiceTable.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDateMiseEnService();
            return new SimpleStringProperty(date != null ? date.format(ViewController.FORMATTEUR_DATE_STANDARD_VUE) : "N/A");
        });
        CONTROLEUR_LOGGER.fine("Colonnes de la table des véhicules configurées.");
    }

    private void chargerOptionsFiltresVehicules() {
        try {
            List<EtatVoiture> etats = servicePersistance.trouverTousLesEtatsVoiture();
            ObservableList<String> libellesEtats = FXCollections.observableArrayList("Tous les états");
            libellesEtats.addAll(etats.stream().map(EtatVoiture::getLibEtatVoiture).collect(Collectors.toList()));
            choiceBoxFiltreEtatVehicule.setItems(libellesEtats);
            choiceBoxFiltreEtatVehicule.setValue("Tous les états");

            ObservableList<EnergieVehicule> energies = FXCollections.observableArrayList(EnergieVehicule.values());
            choiceBoxFiltreEnergieVehicule.setItems(energies);
            choiceBoxFiltreEnergieVehicule.setConverter(new StringConverter<EnergieVehicule>() {
                @Override public String toString(EnergieVehicule energie) { return energie == null ? "" : energie.getDbValue(); }
                @Override public EnergieVehicule fromString(String string) { return EnergieVehicule.fromDbValue(string); }
            });
            // Ajouter une option "Toutes énergies" ou laisser null pour cela
        } catch (Exception e) {
            CONTROLEUR_LOGGER.log(Level.SEVERE, "Erreur lors du chargement des options pour les filtres véhicules.", e);
            afficherNotificationAlerteInterface("Erreur de Chargement Filtres", "Impossible de charger les options de filtrage : " + e.getMessage(), Alert.AlertType.ERROR);
        }
        CONTROLEUR_LOGGER.fine("Options des filtres pour la liste des véhicules chargées.");
    }

    private void configurerPermissionsActionsVehicules() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        if (role == null) { // Sécurité, ne devrait pas arriver si session gérée correctement
            Arrays.asList(boutonAjouterVehicule, boutonModifierVehicule, boutonSupprimerVehicule, boutonChangerEtatVehicule).stream().filter(Objects::nonNull).forEach(b -> b.setDisable(true));
            return;
        }
        if (boutonAjouterVehicule != null) boutonAjouterVehicule.setDisable(!gestionnaireSecurite.estAutorise(role, Permissions.VEHICULE_CREER));
        // La désactivation des boutons modifier/supprimer/changer état est gérée par la sélection dans la table
    }

    private boolean peutModifierVehicule() {
        return gestionnaireSecurite.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.VEHICULE_MODIFIER);
    }
    private boolean peutSupprimerVehicule() {
        return gestionnaireSecurite.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.VEHICULE_SUPPRIMER);
    }
    private boolean peutChangerEtatVehicule() {
        return gestionnaireSecurite.estAutorise(SessionManager.obtenirRoleUtilisateurActuel(), Permissions.VEHICULE_CHANGER_ETAT);
    }


    @FXML
    public void actionActualiserListeVehicules() {
        CONTROLEUR_LOGGER.info("Tentative d'actualisation de la liste des véhicules.");
        try {
            String rechercheImmat = champRechercheVehiculeImmat.getText();
            String filtreEtatLibelle = choiceBoxFiltreEtatVehicule.getValue();
            EnergieVehicule filtreEnergie = choiceBoxFiltreEnergieVehicule.getValue();

            List<Vehicule> vehiculesModel = serviceLogiqueMetier.rechercherVehiculesFiltres(
                    rechercheImmat,
                    "Tous les états".equals(filtreEtatLibelle) ? null : filtreEtatLibelle,
                    filtreEnergie
            );

            List<VehiculeDTO> vehiculesDto = DataMapper.convertirVersListeDeVehiculeDTO(vehiculesModel, servicePersistance);
            tableVueVehicules.setItems(FXCollections.observableArrayList(vehiculesDto));
            tableVueVehicules.refresh(); // S'assurer du rafraîchissement visuel
            CONTROLEUR_LOGGER.info(vehiculesDto.size() + " véhicules chargés et affichés dans la table après actualisation/filtrage.");
        } catch (Exception e) {
            CONTROLEUR_LOGGER.log(Level.SEVERE, "Erreur majeure lors de l'actualisation ou du filtrage des données des véhicules.", e);
            afficherNotificationAlerteInterface("Erreur de Données Véhicules", "Impossible de charger ou filtrer la liste des véhicules : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulaireAjoutVehicule() {
        CONTROLEUR_LOGGER.fine("Action utilisateur : ouvrir formulaire d'ajout de véhicule.");
        ouvrirDialogueFormulaireVehicule(null); // null indique le mode création
    }

    @FXML
    private void actionOuvrirFormulaireModificationVehicule() {
        VehiculeDTO vehiculeSelectionneDto = tableVueVehicules.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionneDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un véhicule dans la liste pour le modifier.", Alert.AlertType.INFORMATION);
            return;
        }
        CONTROLEUR_LOGGER.fine("Action utilisateur : ouvrir formulaire de modification pour véhicule ID: " + vehiculeSelectionneDto.getIdVehicule());
        Vehicule vehiculeModel = servicePersistance.trouverVehiculeParId(vehiculeSelectionneDto.getIdVehicule());
        if (vehiculeModel == null){
            afficherNotificationAlerteInterface("Erreur Donnée Introuvable", "Le véhicule sélectionné (ID: "+vehiculeSelectionneDto.getIdVehicule()+") n'a pas pu être retrouvé pour modification.", Alert.AlertType.ERROR);
            actionActualiserListeVehicules();
            return;
        }
        ouvrirDialogueFormulaireVehicule(vehiculeModel);
    }

    private void ouvrirDialogueFormulaireVehicule(Vehicule vehiculeAEditer) {
        try {
            String titreDialogue = (vehiculeAEditer == null) ? "Ajouter un Nouveau Véhicule au Parc" : "Modifier les Informations du Véhicule";
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireVehiculeView.fxml";
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire véhicule introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            FormulaireVehiculeController controleurFormulaire = chargeur.getController();
            controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            controleurFormulaire.preparerFormulairePourEdition(vehiculeAEditer); // Passe le véhicule pour modification, ou null pour création

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();

            actionActualiserListeVehicules();
            CONTROLEUR_LOGGER.info("Dialogue du formulaire véhicule fermé. Table des véhicules rafraîchie si nécessaire.");

        } catch (IOException e) {
            CONTROLEUR_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture ou de la gestion du formulaire véhicule.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire de gestion des véhicules : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionSupprimerVehiculeSelectionne() {
        VehiculeDTO vehiculeSelectionneDto = tableVueVehicules.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionneDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un véhicule à supprimer de la liste.", Alert.AlertType.INFORMATION);
            return;
        }

        CONTROLEUR_LOGGER.fine("Action utilisateur : tentative de suppression du véhicule ID: " + vehiculeSelectionneDto.getIdVehicule());
        Optional<ButtonType> reponse = afficherDialogueConfirmationInterface("Confirmation de Suppression de Véhicule",
                "Êtes-vous certain de vouloir supprimer définitivement le véhicule immatriculé '" +
                        vehiculeSelectionneDto.getImmatriculation() + "' (ID: " + vehiculeSelectionneDto.getIdVehicule() + ") ?\n" +
                        "Cette action est irréversible et supprimera également les données associées (entretiens, missions, etc.).");

        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            try {
                serviceLogiqueMetier.supprimerVehicule(vehiculeSelectionneDto.getIdVehicule());
                actionActualiserListeVehicules();
                afficherNotificationAlerteInterface("Suppression Réussie", "Le véhicule '" + vehiculeSelectionneDto.getImmatriculation() + "' a été supprimé avec succès.", Alert.AlertType.INFORMATION);
                CONTROLEUR_LOGGER.info("Véhicule ID: " + vehiculeSelectionneDto.getIdVehicule() + " supprimé avec succès après confirmation.");
            } catch (ErreurLogiqueMetier | ErreurValidation e) {
                CONTROLEUR_LOGGER.log(Level.WARNING, "Échec de la suppression du véhicule ID: " + vehiculeSelectionneDto.getIdVehicule() + ". Raison: " + e.getMessage(), e);
                afficherNotificationAlerteInterface("Échec de la Suppression", e.getMessage(), Alert.AlertType.WARNING);
            } catch (Exception e) {
                CONTROLEUR_LOGGER.log(Level.SEVERE, "Erreur système inattendue lors de la suppression du véhicule ID: " + vehiculeSelectionneDto.getIdVehicule(), e);
                afficherNotificationAlerteInterface("Erreur Système Critique", "Une erreur imprévue est survenue lors de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            CONTROLEUR_LOGGER.info("Suppression du véhicule ID: " + vehiculeSelectionneDto.getIdVehicule() + " annulée par l'utilisateur.");
        }
    }

    @FXML
    private void actionAfficherDetailsVehicule() {
        VehiculeDTO vehiculeSelectionne = tableVueVehicules.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un véhicule pour afficher ses détails.", Alert.AlertType.INFORMATION);
            return;
        }
        // Pourrait ouvrir un nouveau dialogue/panneau avec toutes les informations du VehiculeDTO et des données liées (assurances, dernières missions, etc.)
        // Pour l'instant, affichons une simple alerte avec plus d'infos.
        Vehicule vehiculeComplet = servicePersistance.trouverVehiculeParId(vehiculeSelectionne.getIdVehicule());
        if (vehiculeComplet == null) {
            afficherNotificationAlerteInterface("Donnée Introuvable", "Impossible de récupérer les détails complets du véhicule.", Alert.AlertType.WARNING);
            return;
        }

        StringBuilder details = new StringBuilder();
        details.append("ID Véhicule: ").append(vehiculeComplet.getIdVehicule()).append("\n");
        details.append("Immatriculation: ").append(vehiculeComplet.getImmatriculation()).append("\n");
        details.append("Marque: ").append(vehiculeComplet.getMarque()).append(", Modèle: ").append(vehiculeComplet.getModele()).append("\n");
        details.append("Numéro de Châssis: ").append(vehiculeComplet.getNumeroChassi()).append("\n");
        details.append("Énergie: ").append(vehiculeComplet.getEnergie().getDbValue()).append("\n");
        details.append("Couleur: ").append(vehiculeComplet.getCouleur()).append(", Nb Places: ").append(vehiculeComplet.getNbPlaces()).append("\n");
        details.append("Puissance (CV): ").append(vehiculeComplet.getPuissance()).append("\n");
        details.append("KM Actuels: ").append(vehiculeComplet.getKmActuels()).append("\n");
        EtatVoiture etat = servicePersistance.trouverEtatVoitureParId(vehiculeComplet.getIdEtatVoiture());
        details.append("État Actuel: ").append(etat != null ? etat.getLibEtatVoiture() : "Inconnu").append(" (depuis le ").append(vehiculeComplet.getDateEtat() != null ? vehiculeComplet.getDateEtat().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append(")\n");
        details.append("Date Acquisition: ").append(vehiculeComplet.getDateAcquisition() != null ? vehiculeComplet.getDateAcquisition().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append("\n");
        details.append("Date Mise en Service: ").append(vehiculeComplet.getDateMiseEnService() != null ? vehiculeComplet.getDateMiseEnService().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append("\n");
        details.append("Prix Acquisition: ").append(vehiculeComplet.getPrixVehicule()).append(" EUR\n");

        afficherNotificationAlerteInterface("Détails du Véhicule: " + vehiculeComplet.getImmatriculation(), details.toString(), Alert.AlertType.INFORMATION);
    }

    @FXML
    private void actionOuvrirDialogueChangerEtatVehicule() {
        VehiculeDTO vehiculeSelectionne = tableVueVehicules.getSelectionModel().getSelectedItem();
        if (vehiculeSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un véhicule pour modifier son état.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<Pair<EtatVoiture, LocalDate>> dialogue = new Dialog<>();
        dialogue.setTitle("Changer l'État du Véhicule");
        dialogue.setHeaderText("Modifier l'état opérationnel pour : " + vehiculeSelectionne.getMarque() + " " + vehiculeSelectionne.getModele() + " (" + vehiculeSelectionne.getImmatriculation() + ")");
        Stage stageProprietaire = MainApp.getPrimaryStage();
        if (stageProprietaire != null) dialogue.initOwner(stageProprietaire);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);

        ChoiceBox<EtatVoiture> choixNouvelEtat = new ChoiceBox<>();
        List<EtatVoiture> tousLesEtats = servicePersistance.trouverTousLesEtatsVoiture();
        choixNouvelEtat.setItems(FXCollections.observableArrayList(tousLesEtats));
        choixNouvelEtat.setConverter(new StringConverter<EtatVoiture>() {
            @Override public String toString(EtatVoiture etat) { return etat == null ? "" : etat.getLibEtatVoiture(); }
            @Override public EtatVoiture fromString(String string) { // Non utilisé pour ChoiceBox simple
                return tousLesEtats.stream().filter(e -> e.getLibEtatVoiture().equals(string)).findFirst().orElse(null);
            }
        });
        EtatVoiture etatActuel = tousLesEtats.stream().filter(e -> e.getLibEtatVoiture().equals(vehiculeSelectionne.getEtatLibelle())).findFirst().orElse(null);
        if(etatActuel != null) choixNouvelEtat.setValue(etatActuel);

        DatePicker selecteurDateChangement = new DatePicker(LocalDate.now());
        selecteurDateChangement.setConverter(ViewController.obtenirConvertisseurDateStandard());

        grille.add(new Label("Nouvel état:"), 0, 0);
        grille.add(choixNouvelEtat, 1, 0);
        grille.add(new Label("Date d'effet:"), 0, 1);
        grille.add(selecteurDateChangement, 1, 1);
        dialogue.getDialogPane().setContent(grille);

        ButtonType boutonConfirmerChangement = ButtonType.OK;
        dialogue.getDialogPane().getButtonTypes().addAll(boutonConfirmerChangement, ButtonType.CANCEL);

        dialogue.setResultConverter(typeBouton -> {
            if (typeBouton == boutonConfirmerChangement) {
                if (choixNouvelEtat.getValue() == null || selecteurDateChangement.getValue() == null) {
                    afficherNotificationAlerteInterface("Saisie Incomplète", "Veuillez sélectionner un nouvel état et une date d'effet.", Alert.AlertType.WARNING);
                    return null; // Empêche la fermeture si invalide
                }
                return new Pair<>(choixNouvelEtat.getValue(), selecteurDateChangement.getValue());
            }
            return null;
        });

        Optional<Pair<EtatVoiture, LocalDate>> resultat = dialogue.showAndWait();
        resultat.ifPresent(pair -> {
            try {
                LocalDateTime dateHeureChangement = pair.getValue().atTime(LocalTime.now()); // Ou une heure fixe si besoin
                serviceLogiqueMetier.changerEtatVehicule(vehiculeSelectionne.getIdVehicule(), pair.getKey().getIdEtatVoiture(), dateHeureChangement);
                afficherNotificationAlerteInterface("Mise à Jour Réussie", "L'état du véhicule a été mis à jour avec succès.", Alert.AlertType.INFORMATION);
                actionActualiserListeVehicules();
            } catch (ErreurValidation | ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec de la Mise à Jour", e.getMessage(), Alert.AlertType.ERROR);
            } catch (Exception e) {
                CONTROLEUR_LOGGER.log(Level.SEVERE, "Erreur imprévue lors du changement d'état du véhicule.", e);
                afficherNotificationAlerteInterface("Erreur Système", "Une erreur inattendue s'est produite: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
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

    private Optional<ButtonType> afficherDialogueConfirmationInterface(String titre, String message) {
        Alert dialogue = new Alert(Alert.AlertType.CONFIRMATION);
        dialogue.setTitle(titre);
        dialogue.setHeaderText(null);
        dialogue.setContentText(message);
        Stage stagePrincipal = MainApp.getPrimaryStage();
        if (stagePrincipal != null && stagePrincipal.getScene() != null && stagePrincipal.isShowing()) {
            dialogue.initOwner(stagePrincipal);
        }
        return dialogue.showAndWait();
    }

    // Classe utilitaire Pair simple si non disponible dans JavaFX/JDK utilisé (JavaFX a javafx.util.Pair)
    private static class Pair<K, V> {
        private K key;
        private V value;
        public Pair(K key, V value) { this.key = key; this.value = value; }
        public K getKey() { return key; }
        public V getValue() { return value; }
    }
}