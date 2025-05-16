package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppDataTransferObjects.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DataMapper {

    private DataMapper() {
        // Classe utilitaire, pas d'instanciation
    }

    public static VehiculeDTO convertirVersVehiculeDTO(Vehicule vehicule, PersistenceService persistenceService) {
        Objects.requireNonNull(vehicule, "L'entité Vehicule ne peut être nulle pour le mapping.");
        Objects.requireNonNull(persistenceService, "PersistenceService ne peut être nul pour le mapping de VehiculeDTO.");

        VehiculeDTO dto = new VehiculeDTO();
        dto.setIdVehicule(vehicule.getIdVehicule());
        dto.setImmatriculation(vehicule.getImmatriculation());
        dto.setMarque(vehicule.getMarque());
        dto.setModele(vehicule.getModele());
        dto.setEnergie(vehicule.getEnergie());
        dto.setKmActuels(vehicule.getKmActuels());
        dto.setDateMiseEnService(vehicule.getDateMiseEnService());

        EtatVoiture etat = persistenceService.trouverEtatVoitureParId(vehicule.getIdEtatVoiture());
        if (etat != null) {
            dto.setEtatLibelle(etat.getLibEtatVoiture());
        } else {
            dto.setEtatLibelle("État inconnu (ID: " + vehicule.getIdEtatVoiture() + ")");
        }

        // Exemple pour trouver le personnel lié (si une affectation de type fonction existe)
        // Cette logique peut être plus complexe et dépend des règles métier exactes.
        // Pour simplifier, on pourrait chercher la dernière affectation de type "CREDIT_5_ANS" ou similaire.
        // List<Affectation> affectations = persistenceService.trouverAffectationsPourVehicule(vehicule.getIdVehicule());
        // affectations.stream()
        //    .filter(a -> a.getType() == TypeAffectation.CREDIT_5_ANS && (a.getDateFin() == null || a.getDateFin().isAfter(LocalDateTime.now())))
        //    .findFirst()
        //    .ifPresent(aff -> {
        //        if (aff.getIdPersonnel() != null) {
        //            Personnel p = persistenceService.trouverPersonnelParId(aff.getIdPersonnel());
        //            if (p != null) dto.setNomPersonnelAttribution(p.getPrenomPersonnel() + " " + p.getNomPersonnel());
        //        }
        //    });
        // Pour l'instant, laissons nomPersonnelAttribution à null ou à initialiser via une logique plus spécifique dans BusinessLogicService.

        return dto;
    }

    public static List<VehiculeDTO> convertirVersListeDeVehiculeDTO(List<Vehicule> vehicules, PersistenceService persistenceService) {
        Objects.requireNonNull(vehicules, "La liste de Vehicule ne peut être nulle.");
        List<VehiculeDTO> dtos = new ArrayList<>();
        for (Vehicule vehicule : vehicules) {
            if (vehicule != null) {
                dtos.add(convertirVersVehiculeDTO(vehicule, persistenceService));
            }
        }
        return dtos;
    }

    public static MissionDTO convertirVersMissionDTO(Mission mission, PersistenceService persistenceService) {
        Objects.requireNonNull(mission, "L'entité Mission ne peut être nulle.");
        Objects.requireNonNull(persistenceService, "PersistenceService ne peut être nul.");

        MissionDTO dto = new MissionDTO();
        dto.setIdMission(mission.getIdMission());
        dto.setLibMission(mission.getLibMission());
        dto.setDateDebutMission(mission.getDateDebutMission());
        dto.setDateFinMission(mission.getDateFinMission());
        dto.setStatus(mission.getStatus());
        dto.setSite(mission.getSite());
        dto.setKmPrevu(mission.getKmPrevu());
        dto.setKmReel(mission.getKmReel());

        Vehicule vehiculeAssocie = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        if (vehiculeAssocie != null) {
            dto.setImmatriculationVehicule(vehiculeAssocie.getImmatriculation());
            dto.setMarqueModeleVehicule(vehiculeAssocie.getMarque() + " " + vehiculeAssocie.getModele());
        } else {
            dto.setImmatriculationVehicule("Véhicule inconnu (ID: " + mission.getIdVehicule() + ")");
            dto.setMarqueModeleVehicule("");
        }
        return dto;
    }

    public static List<MissionDTO> convertirVersListeDeMissionDTO(List<Mission> missions, PersistenceService persistenceService) {
        Objects.requireNonNull(missions, "La liste de Mission ne peut être nulle.");
        List<MissionDTO> dtos = new ArrayList<>();
        for (Mission mission : missions) {
            if (mission != null) {
                dtos.add(convertirVersMissionDTO(mission, persistenceService));
            }
        }
        return dtos;
    }

    public static EntretienDTO convertirVersEntretienDTO(Entretien entretien, PersistenceService persistenceService) {
        Objects.requireNonNull(entretien, "L'entité Entretien ne peut être nulle.");
        Objects.requireNonNull(persistenceService, "PersistenceService ne peut être nul.");

        EntretienDTO dto = new EntretienDTO();
        dto.setIdEntretien(entretien.getIdEntretien());
        dto.setDateEntreeEntr(entretien.getDateEntreeEntr());
        dto.setDateSortieEntr(entretien.getDateSortieEntr());
        dto.setMotifEntr(entretien.getMotifEntr());
        dto.setCoutEntr(entretien.getCoutEntr());
        dto.setType(entretien.getType());
        dto.setStatutOt(entretien.getStatutOt());

        Vehicule vehiculeAssocie = persistenceService.trouverVehiculeParId(entretien.getIdVehicule());
        if (vehiculeAssocie != null) {
            dto.setImmatriculationVehicule(vehiculeAssocie.getImmatriculation());
            dto.setMarqueModeleVehicule(vehiculeAssocie.getMarque() + " " + vehiculeAssocie.getModele());
        } else {
            dto.setImmatriculationVehicule("Véhicule inconnu (ID: " + entretien.getIdVehicule() + ")");
            dto.setMarqueModeleVehicule("");
        }
        return dto;
    }

    public static List<EntretienDTO> convertirVersListeDeEntretienDTO(List<Entretien> entretiens, PersistenceService persistenceService) {
        Objects.requireNonNull(entretiens, "La liste d'Entretien ne peut être nulle.");
        List<EntretienDTO> dtos = new ArrayList<>();
        for (Entretien entretien : entretiens) {
            if (entretien != null) {
                dtos.add(convertirVersEntretienDTO(entretien, persistenceService));
            }
        }
        return dtos;
    }

    public static UtilisateurDTO convertirVersUtilisateurDTO(Utilisateur utilisateur, PersistenceService persistenceService) {
        Objects.requireNonNull(utilisateur, "L'entité Utilisateur ne peut être nulle.");
        // persistenceService peut être nul si on ne mappe pas le nom du personnel

        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setLogin(utilisateur.getLogin());
        dto.setRole(utilisateur.getRole());

        if (utilisateur.getIdPersonnel() != null && persistenceService != null) {
            Personnel personnelAssocie = persistenceService.trouverPersonnelParId(utilisateur.getIdPersonnel());
            if (personnelAssocie != null) {
                dto.setNomPersonnelAssocie(personnelAssocie.getPrenomPersonnel() + " " + personnelAssocie.getNomPersonnel());
            } else {
                dto.setNomPersonnelAssocie("Personnel ID " + utilisateur.getIdPersonnel() + " non trouvé");
            }
        } else if (utilisateur.getIdPersonnel() != null) {
            dto.setNomPersonnelAssocie("ID Personnel: " + utilisateur.getIdPersonnel());
        } else {
            dto.setNomPersonnelAssocie("Non associé");
        }
        return dto;
    }

    public static List<UtilisateurDTO> convertirVersListeDeUtilisateurDTO(List<Utilisateur> utilisateurs, PersistenceService persistenceService) {
        Objects.requireNonNull(utilisateurs, "La liste d'Utilisateur ne peut être nulle.");
        List<UtilisateurDTO> dtos = new ArrayList<>();
        for (Utilisateur utilisateur : utilisateurs) {
            if (utilisateur != null) {
                dtos.add(convertirVersUtilisateurDTO(utilisateur, persistenceService));
            }
        }
        return dtos;
    }
}