package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppDataTransferObjects.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class DataMapper {

    private DataMapper() {
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
        dto.setEtatLibelle(etat != null ? etat.getLibEtatVoiture() : "État ID: " + vehicule.getIdEtatVoiture());

        return dto;
    }

    public static List<VehiculeDTO> convertirVersListeDeVehiculeDTO(List<Vehicule> vehicules, PersistenceService persistenceService) {
        Objects.requireNonNull(vehicules, "La liste de Vehicule ne peut être nulle.");
        return vehicules.stream()
                .filter(Objects::nonNull)
                .map(v -> convertirVersVehiculeDTO(v, persistenceService))
                .collect(Collectors.toList());
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
            dto.setImmatriculationVehicule("Véhicule ID: " + mission.getIdVehicule());
            dto.setMarqueModeleVehicule("Inconnu");
        }
        return dto;
    }

    public static List<MissionDTO> convertirVersListeDeMissionDTO(List<Mission> missions, PersistenceService persistenceService) {
        Objects.requireNonNull(missions, "La liste de Mission ne peut être nulle.");
        return missions.stream()
                .filter(Objects::nonNull)
                .map(m -> convertirVersMissionDTO(m, persistenceService))
                .collect(Collectors.toList());
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
            dto.setImmatriculationVehicule("Véhicule ID: " + entretien.getIdVehicule());
            dto.setMarqueModeleVehicule("Inconnu");
        }
        return dto;
    }

    public static List<EntretienDTO> convertirVersListeDeEntretienDTO(List<Entretien> entretiens, PersistenceService persistenceService) {
        Objects.requireNonNull(entretiens, "La liste d'Entretien ne peut être nulle.");
        return entretiens.stream()
                .filter(Objects::nonNull)
                .map(e -> convertirVersEntretienDTO(e, persistenceService))
                .collect(Collectors.toList());
    }

    public static UtilisateurDTO convertirVersUtilisateurDTO(Utilisateur utilisateur, PersistenceService persistenceService) {
        Objects.requireNonNull(utilisateur, "L'entité Utilisateur ne peut être nulle.");
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setLogin(utilisateur.getLogin());
        dto.setRole(utilisateur.getRole());

        if (utilisateur.getIdPersonnel() != null && persistenceService != null) {
            Personnel personnelAssocie = persistenceService.trouverPersonnelParId(utilisateur.getIdPersonnel());
            dto.setNomPersonnelAssocie(personnelAssocie != null ? personnelAssocie.getPrenomPersonnel() + " " + personnelAssocie.getNomPersonnel() : "Personnel ID " + utilisateur.getIdPersonnel() + " introuvable");
        } else {
            dto.setNomPersonnelAssocie(utilisateur.getIdPersonnel() != null ? "ID Personnel: " + utilisateur.getIdPersonnel() : "Non associé");
        }
        return dto;
    }

    public static List<UtilisateurDTO> convertirVersListeDeUtilisateurDTO(List<Utilisateur> utilisateurs, PersistenceService persistenceService) {
        Objects.requireNonNull(utilisateurs, "La liste d'Utilisateur ne peut être nulle.");
        return utilisateurs.stream()
                .filter(Objects::nonNull)
                .map(u -> convertirVersUtilisateurDTO(u, persistenceService))
                .collect(Collectors.toList());
    }

    public static SocietaireCompteDTO convertirVersSocietaireCompteDTO(SocietaireCompte compte, PersistenceService persistenceService) {
        Objects.requireNonNull(compte, "L'entité SocietaireCompte ne peut être nulle.");
        SocietaireCompteDTO dto = new SocietaireCompteDTO();
        dto.setIdSocietaire(compte.getIdSocietaire());
        dto.setNomSocietaire(compte.getNom());
        dto.setNumeroCompte(compte.getNumero());
        dto.setSolde(compte.getSolde());
        dto.setEmail(compte.getEmail());
        dto.setTelephone(compte.getTelephone());
        dto.setIdPersonnelAssocie(compte.getIdPersonnel()); // Important pour les permissions U3
        return dto;
    }

    public static List<SocietaireCompteDTO> convertirVersListeDeSocietaireCompteDTO(List<SocietaireCompte> comptes, PersistenceService persistenceService) {
        Objects.requireNonNull(comptes, "La liste de SocietaireCompte ne peut être nulle.");
        return comptes.stream()
                .filter(Objects::nonNull)
                .map(c -> convertirVersSocietaireCompteDTO(c, persistenceService))
                .collect(Collectors.toList());
    }

    public static MouvementDTO convertirVersMouvementDTO(Mouvement mouvement) {
        Objects.requireNonNull(mouvement, "L'entité Mouvement ne peut être nulle.");
        MouvementDTO dto = new MouvementDTO();
        dto.setIdMouvement(mouvement.getId());
        dto.setDateHeureMouvement(mouvement.getDate());
        dto.setType(mouvement.getType());
        dto.setMontant(mouvement.getMontant());
        return dto;
    }

    public static List<MouvementDTO> convertirVersListeDeMouvementDTO(List<Mouvement> mouvements) {
        Objects.requireNonNull(mouvements, "La liste de Mouvement ne peut être nulle.");
        return mouvements.stream()
                .filter(Objects::nonNull)
                .map(DataMapper::convertirVersMouvementDTO)
                .collect(Collectors.toList());
    }

    public static DocumentSocietaireDTO convertirVersDocumentSocietaireDTO(DocumentSocietaire document, PersistenceService persistenceService) {
        Objects.requireNonNull(document, "L'entité DocumentSocietaire ne peut être nulle.");
        Objects.requireNonNull(persistenceService, "PersistenceService est requis pour mapper DocumentSocietaireDTO.");
        DocumentSocietaireDTO dto = new DocumentSocietaireDTO();
        dto.setIdDoc(document.getIdDoc());
        dto.setIdSocietaire(document.getIdSocietaire());
        dto.setTypeDoc(document.getTypeDoc());
        dto.setDateUpload(document.getDateUpload());
        dto.setCheminFichierComplet(document.getCheminFichier()); // Le chemin stocké en BDD est le chemin complet

        if (document.getCheminFichier() != null && !document.getCheminFichier().isEmpty()) {
            dto.setNomFichier(Paths.get(document.getCheminFichier()).getFileName().toString());
        } else {
            dto.setNomFichier("Nom de fichier non disponible");
        }

        SocietaireCompte societaire = persistenceService.trouverSocietaireCompteParId(document.getIdSocietaire());
        dto.setNomSocietaire(societaire != null ? societaire.getNom() : "Sociétaire ID: " + document.getIdSocietaire());

        return dto;
    }

    public static List<DocumentSocietaireDTO> convertirVersListeDeDocumentSocietaireDTO(List<DocumentSocietaire> documents, PersistenceService persistenceService) {
        Objects.requireNonNull(documents, "La liste de DocumentSocietaire ne peut être nulle.");
        return documents.stream()
                .filter(Objects::nonNull)
                .map(d -> convertirVersDocumentSocietaireDTO(d, persistenceService))
                .collect(Collectors.toList());
    }
}