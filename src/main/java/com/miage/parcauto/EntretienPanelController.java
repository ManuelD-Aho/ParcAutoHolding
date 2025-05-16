package main.java.com.miage.parcauto;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import main.java.com.miage.parcauto.AppModels.Entretien;
import main.java.com.miage.parcauto.AppModels.TypeEntretien;
import main.java.com.miage.parcauto.AppModels.StatutOrdreTravail;
import main.java.com.miage.parcauto.AppModels.Vehicule;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppDataTransferObjects.EntretienDTO;
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

public class EntretienPanelController implements ViewController.InitializableServices {

    private static final Logger CONTROLEUR_ENTRETIEN_LOGGER = Logger.getLogger(EntretienPanelController.class.getName());
    private BusinessLogicService serviceLogiqueMetier;
    private SecurityManager gestionnaireSecurite;
    private ReportingEngine moteurRapports;
    private PersistenceService servicePersistance;

    @FXML private TableView<EntretienDTO> tableVueEntretiens;
    @FXML private TableColumn<EntretienDTO, Integer> colIdEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colVehiculeImmatEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colVehiculeModeleEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colDateEntreeEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colDateSortieEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colMotifEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colTypeEntretienTable;
    @FXML private TableColumn<EntretienDTO, String> colStatutOTEntretienTable;
    @FXML private TableColumn<EntretienDTO, BigDecimal> colCoutEntretienTable;

    @FXML private Button boutonCreerEntretien;
    @FXML private Button boutonModifierEntretien;
    @FXML private Button boutonSupprimerEntretien; // Moins courant, mais possible pour erreurs de saisie
    @FXML private Button boutonChangerStatutOT;
    @FXML private Button boutonDetailsEntretien;
    @FXML private Button boutonActualiserListeEntretiens;

    @FXML private DatePicker datePickerFiltreDebutEntretien;
    @FXML private DatePicker datePickerFiltreFinEntretien;
    @FXML private ChoiceBox<TypeEntretien> choiceBoxFiltreTypeEntretien;
    @FXML private ChoiceBox<StatutOrdreTravail> choiceBoxFiltreStatutOT;
    @FXML private TextField champRechercheVehiculeEntretien;

    @Override
    public void injecterDependancesServices(BusinessLogicService bls, SecurityManager sm, ReportingEngine re, PersistenceService ps) {
        this.serviceLogiqueMetier = Objects.requireNonNull(bls);
        this.gestionnaireSecurite = Objects.requireNonNull(sm);
        this.moteurRapports = Objects.requireNonNull(re);
        this.servicePersistance = Objects.requireNonNull(ps);
        CONTROLEUR_ENTRETIEN_LOGGER.fine("Dépendances services injectées dans EntretienPanelController.");
    }

    @Override
    public void initialiserDonneesVue() {
        CONTROLEUR_ENTRETIEN_LOGGER.info("Initialisation des données et de la configuration pour le panneau de gestion des entretiens.");
        configurerColonnesTableEntretiens();
        chargerOptionsFiltresEntretiens();
        configurerPermissionsActionsEntretiens();
        actionActualiserListeEntretiens();

        tableVueEntretiens.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            configurerEtatBoutonsContextuels(newSelection);
        });
    }

    private void configurerColonnesTableEntretiens() {
        colIdEntretienTable.setCellValueFactory(new PropertyValueFactory<>("idEntretien"));
        colVehiculeImmatEntretienTable.setCellValueFactory(new PropertyValueFactory<>("immatriculationVehicule"));
        colVehiculeModeleEntretienTable.setCellValueFactory(new PropertyValueFactory<>("marqueModeleVehicule"));
        colDateEntreeEntretienTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateEntreeEntr() != null ? cellData.getValue().getDateEntreeEntr().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        colDateSortieEntretienTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateSortieEntr() != null ? cellData.getValue().getDateSortieEntr().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A"
        ));
        colMotifEntretienTable.setCellValueFactory(new PropertyValueFactory<>("motifEntr"));
        colTypeEntretienTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getType() != null ? cellData.getValue().getType().getDbValue() : "N/A"
        ));
        colStatutOTEntretienTable.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatutOt() != null ? cellData.getValue().getStatutOt().getDbValue() : "N/A"
        ));
        colCoutEntretienTable.setCellValueFactory(new PropertyValueFactory<>("coutEntr"));
        CONTROLEUR_ENTRETIEN_LOGGER.fine("Colonnes de la table des entretiens configurées.");
    }

    private void chargerOptionsFiltresEntretiens() {
        datePickerFiltreDebutEntretien.setConverter(ViewController.obtenirConvertisseurDateStandard());
        datePickerFiltreFinEntretien.setConverter(ViewController.obtenirConvertisseurDateStandard());

        ObservableList<TypeEntretien> types = FXCollections.observableArrayList(TypeEntretien.values());
        choiceBoxFiltreTypeEntretien.getItems().add(0, null); // Pour "Tous les types"
        choiceBoxFiltreTypeEntretien.setItems(types);
        choiceBoxFiltreTypeEntretien.setConverter(new StringConverter<TypeEntretien>() {
            @Override public String toString(TypeEntretien type) { return type == null ? "Tous les types" : type.getDbValue(); }
            @Override public TypeEntretien fromString(String string) { return "Tous les types".equals(string) ? null : TypeEntretien.fromDbValue(string); }
        });
        choiceBoxFiltreTypeEntretien.setValue(null);

        ObservableList<StatutOrdreTravail> statutsOT = FXCollections.observableArrayList(StatutOrdreTravail.values());
        choiceBoxFiltreStatutOT.getItems().add(0, null); // Pour "Tous les statuts"
        choiceBoxFiltreStatutOT.setItems(statutsOT);
        choiceBoxFiltreStatutOT.setConverter(new StringConverter<StatutOrdreTravail>() {
            @Override public String toString(StatutOrdreTravail statut) { return statut == null ? "Tous les statuts OT" : statut.getDbValue(); }
            @Override public StatutOrdreTravail fromString(String string) { return "Tous les statuts OT".equals(string) ? null : StatutOrdreTravail.fromDbValue(string); }
        });
        choiceBoxFiltreStatutOT.setValue(null);
        CONTROLEUR_ENTRETIEN_LOGGER.fine("Options des filtres pour la liste des entretiens chargées.");
    }

    private void configurerPermissionsActionsEntretiens() {
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        if (role == null) {
            Arrays.asList(boutonCreerEntretien, boutonModifierEntretien, boutonSupprimerEntretien, boutonChangerStatutOT, boutonDetailsEntretien)
                    .stream().filter(Objects::nonNull).forEach(b -> b.setDisable(true));
            return;
        }
        boolean peutGererEntretiens = gestionnaireSecurite.estAutorise(role, Permissions.ENTRETIEN_GERER_TOUS);
        if (boutonCreerEntretien != null) boutonCreerEntretien.setDisable(!peutGererEntretiens && !gestionnaireSecurite.estAutorise(role, Permissions.ENTRETIEN_DECLARER_PANNE_CORRECTIF));
        // Les autres boutons sont contextuels
    }

    private void configurerEtatBoutonsContextuels(EntretienDTO entretienSelectionne) {
        boolean aucuneSelection = entretienSelectionne == null;
        RoleUtilisateur role = SessionManager.obtenirRoleUtilisateurActuel();
        boolean peutGerer = gestionnaireSecurite.estAutorise(role, Permissions.ENTRETIEN_GERER_TOUS);

        if (boutonModifierEntretien != null) {
            boolean peutModifier = !aucuneSelection && peutGerer && entretienSelectionne.getStatutOt() != StatutOrdreTravail.CLOTURE;
            boutonModifierEntretien.setDisable(!peutModifier);
        }
        if (boutonSupprimerEntretien != null) {
            boolean peutSupprimer = !aucuneSelection && peutGerer && entretienSelectionne.getStatutOt() == StatutOrdreTravail.OUVERT; // Supprimer que si Ouvert par ex.
            boutonSupprimerEntretien.setDisable(!peutSupprimer);
        }
        if (boutonChangerStatutOT != null) {
            boolean peutChangerStatut = !aucuneSelection && peutGerer && entretienSelectionne.getStatutOt() != StatutOrdreTravail.CLOTURE;
            boutonChangerStatutOT.setDisable(!peutChangerStatut);
        }
        if (boutonDetailsEntretien != null) {
            boutonDetailsEntretien.setDisable(aucuneSelection);
        }
    }

    @FXML
    public void actionActualiserListeEntretiens() {
        CONTROLEUR_ENTRETIEN_LOGGER.info("Tentative d'actualisation de la liste des entretiens.");
        try {
            LocalDate dateDebutFiltre = datePickerFiltreDebutEntretien.getValue();
            LocalDate dateFinFiltre = datePickerFiltreFinEntretien.getValue();
            TypeEntretien typeFiltre = choiceBoxFiltreTypeEntretien.getValue();
            StatutOrdreTravail statutOTFiltre = choiceBoxFiltreStatutOT.getValue();
            String rechercheVehicule = champRechercheVehiculeEntretien.getText();

            LocalDateTime dateTimeDebutFiltre = (dateDebutFiltre != null) ? dateDebutFiltre.atStartOfDay() : null;
            LocalDateTime dateTimeFinFiltre = (dateFinFiltre != null) ? dateFinFiltre.atTime(LocalTime.MAX) : null;

            List<Entretien> entretiensModel = serviceLogiqueMetier.rechercherEntretiensFiltres(
                    dateTimeDebutFiltre, dateTimeFinFiltre, typeFiltre, statutOTFiltre, rechercheVehicule
            );

            List<EntretienDTO> entretiensDto = DataMapper.convertirVersListeDeEntretienDTO(entretiensModel, servicePersistance);
            tableVueEntretiens.setItems(FXCollections.observableArrayList(entretiensDto));
            tableVueEntretiens.refresh();
            configurerEtatBoutonsContextuels(null);
            CONTROLEUR_ENTRETIEN_LOGGER.info(entretiensDto.size() + " entretiens chargés et affichés après actualisation/filtrage.");
        } catch (Exception e) {
            CONTROLEUR_ENTRETIEN_LOGGER.log(Level.SEVERE, "Erreur majeure lors de l'actualisation ou du filtrage des données des entretiens.", e);
            afficherNotificationAlerteInterface("Erreur de Données Entretiens", "Impossible de charger ou filtrer la liste des entretiens : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionOuvrirFormulaireCreationEntretien() {
        CONTROLEUR_ENTRETIEN_LOGGER.fine("Action utilisateur : ouvrir formulaire de création d'entretien.");
        ouvrirDialogueFormulaireEntretien(null);
    }

    @FXML
    private void actionOuvrirFormulaireModificationEntretien() {
        EntretienDTO entretienSelectionneDto = tableVueEntretiens.getSelectionModel().getSelectedItem();
        if (entretienSelectionneDto == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un entretien pour le modifier.", Alert.AlertType.INFORMATION);
            return;
        }
        if (entretienSelectionneDto.getStatutOt() == StatutOrdreTravail.CLOTURE) {
            afficherNotificationAlerteInterface("Action Impossible", "Un entretien clôturé ne peut pas être modifié.", Alert.AlertType.WARNING);
            return;
        }
        CONTROLEUR_ENTRETIEN_LOGGER.fine("Action utilisateur : ouvrir formulaire de modification pour entretien ID: " + entretienSelectionneDto.getIdEntretien());
        Entretien entretienModel = servicePersistance.trouverEntretienParId(entretienSelectionneDto.getIdEntretien());
        if (entretienModel == null){
            afficherNotificationAlerteInterface("Erreur Donnée Introuvable", "L'entretien sélectionné (ID: "+entretienSelectionneDto.getIdEntretien()+") n'a pas pu être retrouvé.", Alert.AlertType.ERROR);
            actionActualiserListeEntretiens();
            return;
        }
        ouvrirDialogueFormulaireEntretien(entretienModel);
    }

    private void ouvrirDialogueFormulaireEntretien(Entretien entretienAEditer) {
        try {
            String titreDialogue = (entretienAEditer == null) ? "Enregistrer un Nouvel Entretien/Maintenance" : "Modifier l'Entretien ID: " + entretienAEditer.getIdEntretien();
            String cheminFxmlFormulaire = "/main/java/com/miage/parcauto/fxml/FormulaireEntretienView.fxml";
            URL urlFxml = getClass().getResource(cheminFxmlFormulaire);
            if (urlFxml == null) throw new IOException("Fichier FXML du formulaire entretien introuvable: " + cheminFxmlFormulaire);

            FXMLLoader chargeur = new FXMLLoader(urlFxml);
            Parent racineFormulaire = chargeur.load();

            FormulaireEntretienController controleurFormulaire = chargeur.getController();
            controleurFormulaire.injecterDependancesServices(serviceLogiqueMetier, gestionnaireSecurite, moteurRapports, servicePersistance);
            controleurFormulaire.preparerFormulairePourEdition(entretienAEditer);

            Stage stageDialogue = new Stage();
            stageDialogue.setTitle(titreDialogue);
            stageDialogue.initModality(Modality.WINDOW_MODAL);
            Stage stagePrincipal = MainApp.getPrimaryStage();
            if (stagePrincipal != null) stageDialogue.initOwner(stagePrincipal);

            Scene sceneDialogue = new Scene(racineFormulaire);
            stageDialogue.setScene(sceneDialogue);
            stageDialogue.setResizable(false);

            stageDialogue.showAndWait();
            actionActualiserListeEntretiens();
            CONTROLEUR_ENTRETIEN_LOGGER.info("Dialogue du formulaire entretien fermé. Table des entretiens rafraîchie si nécessaire.");

        } catch (IOException e) {
            CONTROLEUR_ENTRETIEN_LOGGER.log(Level.SEVERE, "Erreur critique lors de l'ouverture ou de la gestion du formulaire entretien.", e);
            afficherNotificationAlerteInterface("Erreur d'Interface Majeure", "Impossible d'ouvrir le formulaire de gestion des entretiens : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actionSupprimerEntretienSelectionne() {
        EntretienDTO entretienSelectionne = tableVueEntretiens.getSelectionModel().getSelectedItem();
        if (entretienSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un entretien à supprimer.", Alert.AlertType.INFORMATION);
            return;
        }
        if (entretienSelectionne.getStatutOt() != StatutOrdreTravail.OUVERT) {
            afficherNotificationAlerteInterface("Action Impossible", "Seuls les entretiens avec le statut 'Ouvert' peuvent être supprimés.", Alert.AlertType.WARNING);
            return;
        }

        Optional<ButtonType> reponse = afficherDialogueConfirmationInterface("Confirmation de Suppression",
                "Êtes-vous sûr de vouloir supprimer l'entrée d'entretien ID " + entretienSelectionne.getIdEntretien() + " ?");
        if (reponse.isPresent() && reponse.get() == ButtonType.OK) {
            try {
                serviceLogiqueMetier.supprimerEntretien(entretienSelectionne.getIdEntretien());
                actionActualiserListeEntretiens();
                afficherNotificationAlerteInterface("Suppression Réussie", "L'entretien a été supprimé.", Alert.AlertType.INFORMATION);
            } catch (ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec de Suppression", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void actionOuvrirDialogueChangerStatutOT() {
        EntretienDTO entretienSelectionne = tableVueEntretiens.getSelectionModel().getSelectedItem();
        if (entretienSelectionne == null || entretienSelectionne.getStatutOt() == StatutOrdreTravail.CLOTURE) {
            afficherNotificationAlerteInterface("Action Impossible", "Veuillez sélectionner un entretien non clôturé pour changer son statut d'OT.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<StatutOrdreTravail> dialogue = new Dialog<>();
        dialogue.setTitle("Changer Statut Ordre de Travail");
        dialogue.setHeaderText("Modifier le statut de l'OT pour l'entretien ID: " + entretienSelectionne.getIdEntretien());
        Stage stageProprietaire = MainApp.getPrimaryStage();
        if (stageProprietaire != null) dialogue.initOwner(stageProprietaire);

        ChoiceBox<StatutOrdreTravail> choixNouveauStatutOT = new ChoiceBox<>();
        // Exclure le statut actuel et les statuts illogiques (on ne revient pas de Cloturé à Ouvert par ex)
        List<StatutOrdreTravail> statutsPossibles = Arrays.stream(StatutOrdreTravail.values())
                .filter(s -> s != entretienSelectionne.getStatutOt())
                .collect(Collectors.toList());
        if (entretienSelectionne.getStatutOt() == StatutOrdreTravail.OUVERT) {
            statutsPossibles.remove(StatutOrdreTravail.CLOTURE); // On passe par EnCours d'abord
        } else if (entretienSelectionne.getStatutOt() == StatutOrdreTravail.EN_COURS) {
            // Peut aller à Cloturé ou (moins probable) revenir à Ouvert si erreur.
        }


        choixNouveauStatutOT.setItems(FXCollections.observableArrayList(statutsPossibles));
        choixNouveauStatutOT.setConverter(new StringConverter<StatutOrdreTravail>() {
            @Override public String toString(StatutOrdreTravail statut) { return statut == null ? "" : statut.getDbValue(); }
            @Override public StatutOrdreTravail fromString(String string) { return StatutOrdreTravail.fromDbValue(string); }
        });
        dialogue.getDialogPane().setContent(choixNouveauStatutOT);
        ButtonType boutonConfirmerChangement = ButtonType.OK;
        dialogue.getDialogPane().getButtonTypes().addAll(boutonConfirmerChangement, ButtonType.CANCEL);

        dialogue.setResultConverter(typeBouton -> (typeBouton == boutonConfirmerChangement && choixNouveauStatutOT.getValue() != null) ? choixNouveauStatutOT.getValue() : null);

        Optional<StatutOrdreTravail> resultat = dialogue.showAndWait();
        resultat.ifPresent(nouveauStatut -> {
            try {
                serviceLogiqueMetier.changerStatutOrdreTravailEntretien(entretienSelectionne.getIdEntretien(), nouveauStatut);
                actionActualiserListeEntretiens();
                afficherNotificationAlerteInterface("Statut Mis à Jour", "Le statut de l'ordre de travail a été changé.", Alert.AlertType.INFORMATION);
            } catch (ErreurLogiqueMetier e) {
                afficherNotificationAlerteInterface("Échec Mise à Jour Statut", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void actionAfficherDetailsEntretien() {
        EntretienDTO entretienSelectionne = tableVueEntretiens.getSelectionModel().getSelectedItem();
        if (entretienSelectionne == null) {
            afficherNotificationAlerteInterface("Sélection Requise", "Veuillez sélectionner un entretien pour en voir les détails.", Alert.AlertType.INFORMATION);
            return;
        }
        Entretien entretienComplet = servicePersistance.trouverEntretienParId(entretienSelectionne.getIdEntretien());
        if (entretienComplet == null) {
            afficherNotificationAlerteInterface("Donnée Introuvable", "Impossible de récupérer les détails complets de l'entretien.", Alert.AlertType.WARNING);
            return;
        }
        Vehicule vehiculeConcerne = servicePersistance.trouverVehiculeParId(entretienComplet.getIdVehicule());

        StringBuilder details = new StringBuilder();
        details.append("ID Entretien: ").append(entretienComplet.getIdEntretien()).append("\n");
        details.append("Véhicule: ").append(entretienSelectionne.getImmatriculationVehicule()).append(" (").append(entretienSelectionne.getMarqueModeleVehicule()).append(")\n");
        details.append("Date d'Entrée: ").append(entretienComplet.getDateEntreeEntr() != null ? entretienComplet.getDateEntreeEntr().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append("\n");
        details.append("Date de Sortie: ").append(entretienComplet.getDateSortieEntr() != null ? entretienComplet.getDateSortieEntr().format(ViewController.FORMATTEUR_DATETIME_STANDARD_VUE) : "N/A").append("\n");
        details.append("Motif: ").append(entretienComplet.getMotifEntr()).append("\n");
        details.append("Type: ").append(entretienComplet.getType().getDbValue()).append("\n");
        details.append("Statut OT: ").append(entretienComplet.getStatutOt().getDbValue()).append("\n");
        details.append("Lieu: ").append(entretienComplet.getLieuEntr() != null ? entretienComplet.getLieuEntr() : "Non spécifié").append("\n");
        details.append("Coût: ").append(entretienComplet.getCoutEntr() != null ? entretienComplet.getCoutEntr() : "N/A").append(" EUR\n");
        details.append("Observation: ").append(entretienComplet.getObservation() != null ? entretienComplet.getObservation() : "Aucune").append("\n");

        afficherNotificationAlerteInterface("Détails de l'Entretien ID: " + entretienComplet.getIdEntretien(), details.toString(), Alert.AlertType.INFORMATION);
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
}