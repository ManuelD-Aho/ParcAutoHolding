package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppDataTransferObjects.*; // Si on utilise des DTOs pour les rapports
import main.java.com.miage.parcauto.PersistenceService;
import main.java.com.miage.parcauto.DataMapper; // Pour convertir en DTOs si besoin

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReportingEngine {

    private final PersistenceService persistenceService;
    private static final Logger LOGGER = Logger.getLogger(ReportingEngine.class.getName());
    private static final String DOSSIER_RAPPORTS = "rapports_generes";
    private static final DateTimeFormatter FORMATTEUR_DATE_FICHIER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FORMATTEUR_DATE_RAPPORT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATTEUR_DATE_SIMPLE_RAPPORT = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    public ReportingEngine(PersistenceService persistenceService) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "PersistenceService ne peut être nul pour ReportingEngine.");
        creerDossierRapportsSiInexistant();
    }

    private void creerDossierRapportsSiInexistant() {
        try {
            Path cheminDossier = Paths.get(DOSSIER_RAPPORTS);
            if (Files.notExists(cheminDossier)) {
                Files.createDirectories(cheminDossier);
                LOGGER.info("Dossier des rapports créé : " + cheminDossier.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible de créer le dossier des rapports : " + DOSSIER_RAPPORTS, e);
            // Ne pas bloquer l'application, mais les rapports pourraient ne pas être sauvegardés.
        }
    }

    public File genererRapportInventaireVehiculesCsv() throws IOException {
        List<Vehicule> vehicules = persistenceService.trouverTousLesVehicules();
        List<VehiculeDTO> vehiculesDto = DataMapper.convertirVersListeDeVehiculeDTO(vehicules, persistenceService);

        String nomFichier = "InventaireVehicules_" + LocalDateTime.now().format(FORMATTEUR_DATE_FICHIER) + ".csv";
        File fichierRapport = new File(DOSSIER_RAPPORTS, nomFichier);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierRapport))) {
            // Entête CSV
            writer.write("ID;Immatriculation;Marque;Modèle;Énergie;État;KM Actuels;Date Mise en Service\n");

            for (VehiculeDTO dto : vehiculesDto) {
                writer.write(String.join(";",
                        String.valueOf(dto.getIdVehicule()),
                        dto.getImmatriculation() != null ? dto.getImmatriculation() : "",
                        dto.getMarque() != null ? dto.getMarque() : "",
                        dto.getModele() != null ? dto.getModele() : "",
                        dto.getEnergie() != null ? dto.getEnergie().getDbValue() : "",
                        dto.getEtatLibelle() != null ? dto.getEtatLibelle() : "",
                        dto.getKmActuels() != null ? String.valueOf(dto.getKmActuels()) : "N/A",
                        dto.getDateMiseEnService() != null ? dto.getDateMiseEnService().format(FORMATTEUR_DATE_SIMPLE_RAPPORT) : "N/A"
                ));
                writer.newLine();
            }
            LOGGER.info("Rapport d'inventaire des véhicules généré : " + fichierRapport.getAbsolutePath());
            return fichierRapport;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport CSV d'inventaire des véhicules.", e);
            throw e;
        }
    }

    public File genererRapportMissionsParPeriodeCsv(LocalDateTime dateDebut, LocalDateTime dateFin) throws IOException {
        Objects.requireNonNull(dateDebut, "La date de début ne peut être nulle.");
        Objects.requireNonNull(dateFin, "La date de fin ne peut être nulle.");
        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException("La date de début ne peut être postérieure à la date de fin.");
        }

        List<Mission> toutesMissions = persistenceService.trouverToutesLesMissions();
        List<Mission> missionsFiltrees = toutesMissions.stream()
                .filter(m -> m.getDateDebutMission() != null &&
                        !m.getDateDebutMission().isBefore(dateDebut) &&
                        !m.getDateDebutMission().isAfter(dateFin))
                .collect(Collectors.toList());

        List<MissionDTO> missionsDto = DataMapper.convertirVersListeDeMissionDTO(missionsFiltrees, persistenceService);

        String nomFichier = "RapportMissions_" + dateDebut.format(FORMATTEUR_DATE_SIMPLE_RAPPORT).replace("/","") + "_au_" + dateFin.format(FORMATTEUR_DATE_SIMPLE_RAPPORT).replace("/","") + "_" + LocalDateTime.now().format(FORMATTEUR_DATE_FICHIER) + ".csv";
        File fichierRapport = new File(DOSSIER_RAPPORTS, nomFichier);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierRapport))) {
            writer.write("ID Mission;Libellé;Véhicule (Immat);Véhicule (Modèle);Date Début;Date Fin;Statut;Site;KM Prévus;KM Réels\n");
            for (MissionDTO dto : missionsDto) {
                writer.write(String.join(";",
                        String.valueOf(dto.getIdMission()),
                        dto.getLibMission() != null ? dto.getLibMission() : "",
                        dto.getImmatriculationVehicule() != null ? dto.getImmatriculationVehicule() : "",
                        dto.getMarqueModeleVehicule() != null ? dto.getMarqueModeleVehicule() : "",
                        dto.getDateDebutMission() != null ? dto.getDateDebutMission().format(FORMATTEUR_DATE_RAPPORT) : "",
                        dto.getDateFinMission() != null ? dto.getDateFinMission().format(FORMATTEUR_DATE_RAPPORT) : "",
                        dto.getStatus() != null ? dto.getStatus().getDbValue() : "",
                        dto.getSite() != null ? dto.getSite() : "",
                        dto.getKmPrevu() != null ? String.valueOf(dto.getKmPrevu()) : "",
                        dto.getKmReel() != null ? String.valueOf(dto.getKmReel()) : ""
                ));
                writer.newLine();
            }
            LOGGER.info("Rapport des missions généré : " + fichierRapport.getAbsolutePath());
            return fichierRapport;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport CSV des missions.", e);
            throw e;
        }
    }

    public File genererRapportTCOVehicule(int idVehicule) throws IOException {
        Vehicule vehicule = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehicule == null) {
            throw new IllegalArgumentException("Véhicule avec ID " + idVehicule + " non trouvé.");
        }
        VehiculeDTO vehiculeDto = DataMapper.convertirVersVehiculeDTO(vehicule, persistenceService);
        List<Entretien> entretiens = persistenceService.trouverEntretiensPourVehicule(idVehicule);
        List<Assurance> assurances = persistenceService.trouverAssurancesPourVehicule(idVehicule);
        List<Mission> missions = persistenceService.trouverMissionsPourVehicule(idVehicule); // Pour les coûts de carburant par exemple

        BigDecimal coutTotalAchat = vehicule.getPrixVehicule() != null ? vehicule.getPrixVehicule() : BigDecimal.ZERO;
        BigDecimal coutTotalEntretiens = entretiens.stream()
                .map(Entretien::getCoutEntr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coutTotalAssurances = assurances.stream()
                .map(Assurance::getCoutAssurance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pour les coûts de mission, on pourrait sommer les DepenseMission de nature CARBURANT
        BigDecimal coutTotalCarburantMissions = BigDecimal.ZERO;
        for(Mission m : missions) {
            List<DepenseMission> depenses = persistenceService.trouverDepensesParMissionId(m.getIdMission());
            coutTotalCarburantMissions = coutTotalCarburantMissions.add(
                    depenses.stream()
                            .filter(d -> d.getNature() == NatureDepense.CARBURANT && d.getMontant() != null)
                            .map(DepenseMission::getMontant)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
        }

        BigDecimal tco = coutTotalAchat.add(coutTotalEntretiens).add(coutTotalAssurances).add(coutTotalCarburantMissions);

        String nomFichier = "Rapport_TCO_Vehicule_" + vehicule.getImmatriculation().replaceAll("[^a-zA-Z0-9]", "") + "_" + LocalDateTime.now().format(FORMATTEUR_DATE_FICHIER) + ".txt";
        File fichierRapport = new File(DOSSIER_RAPPORTS, nomFichier);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierRapport))) {
            writer.write("RAPPORT DE COÛT TOTAL DE POSSESSION (TCO)\n");
            writer.write("------------------------------------------\n");
            writer.write("Date du rapport: " + LocalDateTime.now().format(FORMATTEUR_DATE_RAPPORT) + "\n\n");
            writer.write("Véhicule:\n");
            writer.write("  ID: " + vehiculeDto.getIdVehicule() + "\n");
            writer.write("  Immatriculation: " + vehiculeDto.getImmatriculation() + "\n");
            writer.write("  Marque/Modèle: " + vehiculeDto.getMarque() + " " + vehiculeDto.getModele() + "\n");
            writer.write("  Date de mise en service: " + (vehiculeDto.getDateMiseEnService() != null ? vehiculeDto.getDateMiseEnService().format(FORMATTEUR_DATE_SIMPLE_RAPPORT) : "N/A") + "\n");
            writer.write("  Kilométrage actuel: " + (vehiculeDto.getKmActuels() != null ? vehiculeDto.getKmActuels() : "N/A") + " km\n\n");

            writer.write("Détail des coûts:\n");
            writer.write("  Coût d'achat: " + coutTotalAchat + " EUR\n");
            writer.write("  Coût total des entretiens: " + coutTotalEntretiens + " EUR (" + entretiens.size() + " opérations)\n");
            writer.write("  Coût total des assurances: " + coutTotalAssurances + " EUR (" + assurances.size() + " polices/périodes)\n");
            writer.write("  Coût total carburant (via missions): " + coutTotalCarburantMissions + " EUR\n\n");
            writer.write("COÛT TOTAL DE POSSESSION (TCO) ESTIMÉ: " + tco + " EUR\n");

            LOGGER.info("Rapport TCO généré pour véhicule ID " + idVehicule + ": " + fichierRapport.getAbsolutePath());
            return fichierRapport;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport TCO pour véhicule ID " + idVehicule, e);
            throw e;
        }
    }
}