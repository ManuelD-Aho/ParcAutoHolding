package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BusinessLogicService {
    private final PersistenceService persistenceService;
    private static final Logger LOGGER_METIER = Logger.getLogger(BusinessLogicService.class.getName());
    private static final String DOSSIER_STOCKAGE_DOCUMENTS = "documents_societaires_parcauto";


    public BusinessLogicService(PersistenceService persistenceService) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "Le PersistenceService ne peut pas être nul.");
    }

    public Vehicule creerNouveauVehicule(Vehicule vehicule) {
        Objects.requireNonNull(vehicule, "L'objet Vehicule ne peut pas être nul.");
        if (vehicule.getNumeroChassi() == null || vehicule.getNumeroChassi().trim().isEmpty()) {
            throw new ErreurValidation("Le numéro de châssis du véhicule est requis.");
        }
        if (vehicule.getImmatriculation() == null || vehicule.getImmatriculation().trim().isEmpty()) {
            throw new ErreurValidation("L'immatriculation du véhicule est requise.");
        }
        if (persistenceService.trouverVehiculeParNumeroChassi(vehicule.getNumeroChassi()) != null) {
            throw new ErreurValidation("Un véhicule avec le numéro de châssis '" + vehicule.getNumeroChassi() + "' existe déjà.");
        }
        if (persistenceService.trouverVehiculeParImmatriculation(vehicule.getImmatriculation()) != null) {
            throw new ErreurValidation("Un véhicule avec l'immatriculation '" + vehicule.getImmatriculation() + "' existe déjà.");
        }

        if (vehicule.getIdEtatVoiture() == 0) {
            EtatVoiture etatInitialDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
            if (etatInitialDisponible != null) {
                vehicule.setIdEtatVoiture(etatInitialDisponible.getIdEtatVoiture());
            } else {
                LOGGER_METIER.warning("L'état 'Disponible' par défaut est introuvable. Le véhicule pourrait être créé sans état valide initial.");
                throw new ErreurLogiqueMetier("L'état initial par défaut 'Disponible' pour les véhicules n'est pas configuré.");
            }
        }
        if (vehicule.getDateEtat() == null) {
            vehicule.setDateEtat(LocalDateTime.now());
        }
        return persistenceService.sauvegarderVehicule(vehicule);
    }

    public Vehicule modifierVehicule(Vehicule vehicule) {
        Objects.requireNonNull(vehicule, "L'objet Vehicule ne peut pas être nul pour la modification.");
        if (vehicule.getIdVehicule() == 0) {
            throw new ErreurValidation("L'identifiant du véhicule est requis pour la modification.");
        }
        Vehicule vehiculeExistant = persistenceService.trouverVehiculeParId(vehicule.getIdVehicule());
        if (vehiculeExistant == null) {
            throw new ErreurLogiqueMetier("Aucun véhicule trouvé avec l'identifiant " + vehicule.getIdVehicule() + " pour la modification.");
        }

        if (vehicule.getNumeroChassi() != null && !vehiculeExistant.getNumeroChassi().equals(vehicule.getNumeroChassi())) {
            if (persistenceService.trouverVehiculeParNumeroChassi(vehicule.getNumeroChassi()) != null) {
                throw new ErreurValidation("Un autre véhicule utilise déjà le numéro de châssis '" + vehicule.getNumeroChassi() + "'.");
            }
        }
        if (vehicule.getImmatriculation() != null && !vehiculeExistant.getImmatriculation().equals(vehicule.getImmatriculation())) {
            if (persistenceService.trouverVehiculeParImmatriculation(vehicule.getImmatriculation()) != null) {
                throw new ErreurValidation("Un autre véhicule utilise déjà l'immatriculation '" + vehicule.getImmatriculation() + "'.");
            }
        }
        if (vehiculeExistant.getIdEtatVoiture() != vehicule.getIdEtatVoiture()) {
            vehicule.setDateEtat(LocalDateTime.now());
        } else if (vehicule.getDateEtat() == null) { // Si l'état n'a pas changé mais la date est nulle, la mettre à jour
            vehicule.setDateEtat(vehiculeExistant.getDateEtat() != null ? vehiculeExistant.getDateEtat() : LocalDateTime.now());
        }
        return persistenceService.sauvegarderVehicule(vehicule);
    }

    public void supprimerVehicule(int idVehicule) {
        if (idVehicule == 0) throw new ErreurValidation("L'identifiant du véhicule est requis pour la suppression.");
        Vehicule vehiculeASupprimer = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehiculeASupprimer == null) throw new ErreurLogiqueMetier("Aucun véhicule trouvé avec l'ID " + idVehicule + ".");

        if (!persistenceService.trouverMissionsActivesPourVehicule(idVehicule).isEmpty()) {
            throw new ErreurLogiqueMetier("Impossible de supprimer le véhicule ID " + idVehicule + ": missions actives ou planifiées associées.");
        }
        if (!persistenceService.trouverAffectationsActivesPourVehicule(idVehicule).isEmpty()) {
            throw new ErreurLogiqueMetier("Impossible de supprimer le véhicule ID " + idVehicule + ": affectations en cours.");
        }

        persistenceService.supprimerToutesCouverturesPourVehicule(idVehicule);
        persistenceService.supprimerTousEntretiensPourVehicule(idVehicule);
        persistenceService.supprimerToutesAffectationsPourVehicule(idVehicule);
        // Les dépenses de mission sont en cascade avec Mission
        persistenceService.supprimerToutesMissionsPourVehicule(idVehicule);
        persistenceService.supprimerVehiculeParId(idVehicule);
        LOGGER_METIER.info("Véhicule ID " + idVehicule + " et toutes ses données associées ont été supprimés.");
    }

    public void changerEtatVehicule(int idVehicule, int idNouvelEtatVoiture, LocalDateTime dateEffectiveChangement) {
        if (idVehicule == 0 || idNouvelEtatVoiture == 0) throw new ErreurValidation("ID véhicule et ID nouvel état requis.");
        Vehicule vehicule = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehicule == null) throw new ErreurLogiqueMetier("Véhicule ID " + idVehicule + " introuvable.");
        EtatVoiture nouvelEtat = persistenceService.trouverEtatVoitureParId(idNouvelEtatVoiture);
        if (nouvelEtat == null) throw new ErreurLogiqueMetier("État voiture ID " + idNouvelEtatVoiture + " introuvable.");

        vehicule.setIdEtatVoiture(idNouvelEtatVoiture);
        vehicule.setDateEtat(Objects.requireNonNullElse(dateEffectiveChangement, LocalDateTime.now()));
        persistenceService.sauvegarderVehicule(vehicule);
        LOGGER_METIER.info("État du véhicule ID " + idVehicule + " changé à '" + nouvelEtat.getLibEtatVoiture() + "'.");
    }

    public List<Vehicule> rechercherVehiculesFiltres(String immatriculationPartielle, String etatLibelleFiltre, EnergieVehicule energieFiltre) {
        // Cette méthode devra construire une requête SQL dynamique ou appeler une procédure stockée
        // Pour l'instant, une implémentation simple filtrant en mémoire une liste complète (non optimal pour grosses BDD)
        List<Vehicule> tousLesVehicules = persistenceService.trouverTousLesVehicules();
        return tousLesVehicules.stream()
                .filter(v -> immatriculationPartielle == null || v.getImmatriculation().toLowerCase().contains(immatriculationPartielle.toLowerCase()))
                .filter(v -> {
                    if (etatLibelleFiltre == null) return true;
                    EtatVoiture etat = persistenceService.trouverEtatVoitureParId(v.getIdEtatVoiture());
                    return etat != null && etat.getLibEtatVoiture().equalsIgnoreCase(etatLibelleFiltre);
                })
                .filter(v -> energieFiltre == null || v.getEnergie() == energieFiltre)
                .collect(Collectors.toList());
    }


    public Mission planifierNouvelleMission(Mission mission) {
        Objects.requireNonNull(mission, "L'objet Mission ne peut pas être nul.");
        if (mission.getIdVehicule() == 0) throw new ErreurValidation("Un véhicule doit être assigné à la mission.");
        if (mission.getDateDebutMission() == null) throw new ErreurValidation("La date de début de mission est requise.");

        Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        if (vehiculeConcerne == null) throw new ErreurLogiqueMetier("Véhicule ID " + mission.getIdVehicule() + " introuvable.");

        EtatVoiture etatActuelVehicule = persistenceService.trouverEtatVoitureParId(vehiculeConcerne.getIdEtatVoiture());
        if (etatActuelVehicule == null || !"Disponible".equalsIgnoreCase(etatActuelVehicule.getLibEtatVoiture())) {
            throw new ErreurLogiqueMetier("Le véhicule '" + vehiculeConcerne.getImmatriculation() + "' n'est pas disponible (État: " + (etatActuelVehicule != null ? etatActuelVehicule.getLibEtatVoiture() : "Inconnu") + ").");
        }

        EtatVoiture etatEnMission = persistenceService.trouverEtatVoitureParLibelle("En mission");
        if (etatEnMission == null) throw new ErreurLogiqueMetier("L'état 'En mission' est introuvable dans la configuration.");

        // Ne pas changer l'état ici, le faire au démarrage de la mission.
        // La planification réserve le véhicule, mais il reste "Disponible" jusqu'au démarrage.
        mission.setStatus(StatutMission.PLANIFIEE);
        Mission missionPlanifiee = persistenceService.sauvegarderMission(mission);
        LOGGER_METIER.info("Mission ID " + missionPlanifiee.getIdMission() + " planifiée pour véhicule ID " + mission.getIdVehicule());
        return missionPlanifiee;
    }

    public Mission modifierMissionPlanifiee(Mission mission) {
        Objects.requireNonNull(mission, "L'objet Mission ne peut pas être nul pour la modification.");
        if (mission.getIdMission() == 0) throw new ErreurValidation("L'ID de la mission est requis pour la modification.");
        Mission missionExistante = persistenceService.trouverMissionParId(mission.getIdMission());
        if (missionExistante == null) throw new ErreurLogiqueMetier("Mission ID " + mission.getIdMission() + " introuvable.");
        if (missionExistante.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("Seules les missions planifiées peuvent être modifiées. Statut actuel: " + missionExistante.getStatus().getDbValue());
        }
        // Copier les champs modifiables, en s'assurant que le statut reste PLANIFIEE
        missionExistante.setLibMission(mission.getLibMission());
        missionExistante.setIdVehicule(mission.getIdVehicule()); // Peut-être vérifier la dispo du nouveau véhicule
        missionExistante.setSite(mission.getSite());
        missionExistante.setDateDebutMission(mission.getDateDebutMission());
        missionExistante.setDateFinMission(mission.getDateFinMission()); // Date fin *prévue*
        missionExistante.setKmPrevu(mission.getKmPrevu());
        missionExistante.setCircuitMission(mission.getCircuitMission());
        missionExistante.setObservationMission(mission.getObservationMission());

        return persistenceService.sauvegarderMission(missionExistante);
    }


    public void demarrerUneMission(int idMission) {
        if (idMission == 0) throw new ErreurValidation("ID mission requis.");
        Mission mission = persistenceService.trouverMissionParId(idMission);
        if (mission == null) throw new ErreurLogiqueMetier("Mission ID " + idMission + " introuvable.");
        if (mission.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("La mission doit être 'Planifiée' pour être démarrée. Statut: " + mission.getStatus().getDbValue());
        }

        Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        EtatVoiture etatActuelVehicule = persistenceService.trouverEtatVoitureParId(vehiculeConcerne.getIdEtatVoiture());
        if (etatActuelVehicule == null || !"Disponible".equalsIgnoreCase(etatActuelVehicule.getLibEtatVoiture())) {
            // Exception si un autre processus a pris le véhicule entre-temps
            throw new ErreurLogiqueMetier("Le véhicule '" + vehiculeConcerne.getImmatriculation() + "' n'est plus disponible (État: " + (etatActuelVehicule != null ? etatActuelVehicule.getLibEtatVoiture() : "Inconnu") + "). Impossible de démarrer la mission.");
        }
        EtatVoiture etatEnMission = persistenceService.trouverEtatVoitureParLibelle("En mission");
        if (etatEnMission == null) throw new ErreurLogiqueMetier("L'état 'En mission' est introuvable.");

        changerEtatVehicule(mission.getIdVehicule(), etatEnMission.getIdEtatVoiture(), LocalDateTime.now());

        mission.setStatus(StatutMission.EN_COURS);
        // Optionnel: Mettre à jour la date de début réelle si différente de la planifiée
        // mission.setDateDebutMission(LocalDateTime.now());
        persistenceService.sauvegarderMission(mission);
        LOGGER_METIER.info("Mission ID " + idMission + " démarrée.");
    }

    public void cloturerUneMission(int idMission, Integer kmReel, BigDecimal coutTotal, List<DepenseMission> depenses) {
        if (idMission == 0) throw new ErreurValidation("ID mission requis.");
        Mission mission = persistenceService.trouverMissionParId(idMission);
        if (mission == null) throw new ErreurLogiqueMetier("Mission ID " + idMission + " introuvable.");
        if (mission.getStatus() != StatutMission.EN_COURS) {
            throw new ErreurLogiqueMetier("La mission doit être 'En Cours' pour être clôturée. Statut: " + mission.getStatus().getDbValue());
        }
        if (kmReel == null || kmReel < 0) throw new ErreurValidation("Kilométrage réel positif ou nul requis.");

        mission.setStatus(StatutMission.CLOTUREE);
        mission.setKmReel(kmReel);
        mission.setCoutTotal(coutTotal); // Peut être null
        mission.setDateFinMission(LocalDateTime.now()); // Date de fin réelle
        persistenceService.sauvegarderMission(mission); // Le trigger SQL met à jour VEHICULES.km_actuels

        if (depenses != null) {
            for (DepenseMission depense : depenses) {
                depense.setIdMission(idMission);
                persistenceService.sauvegarderDepenseMission(depense);
            }
        }

        EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
        if (etatDisponible == null) throw new ErreurLogiqueMetier("L'état 'Disponible' est introuvable.");
        changerEtatVehicule(mission.getIdVehicule(), etatDisponible.getIdEtatVoiture(), LocalDateTime.now());
        LOGGER_METIER.info("Mission ID " + idMission + " clôturée. Véhicule remis à 'Disponible'.");
    }

    public void annulerMissionPlanifiee(int idMission) {
        if (idMission == 0) throw new ErreurValidation("ID mission requis.");
        Mission mission = persistenceService.trouverMissionParId(idMission);
        if (mission == null) throw new ErreurLogiqueMetier("Mission ID " + idMission + " introuvable.");
        if (mission.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("Seule une mission 'Planifiée' peut être annulée. Statut: " + mission.getStatus().getDbValue());
        }
        // Supprimer la mission. Si le véhicule avait été mis "En mission" par erreur à la planification,
        // il faudrait le remettre "Disponible". Mais la logique actuelle le fait au démarrage.
        // Donc, juste supprimer la mission suffit.
        persistenceService.supprimerToutesDepensesPourMission(idMission); // Méthode à créer dans PersistenceService
        persistenceService.supprimerMissionParId(idMission); // Méthode à créer dans PersistenceService
        LOGGER_METIER.info("Mission planifiée ID " + idMission + " annulée et supprimée.");
    }

    public List<Mission> rechercherMissionsFiltrees(LocalDateTime dateDebutPeriode, LocalDateTime dateFinPeriode, StatutMission statutFiltre, String rechercheVehiculeImmat) {
        // Implémentation simplifiée, filtrage en mémoire. Une requête SQL optimisée serait préférable.
        List<Mission> toutesLesMissions = persistenceService.trouverToutesLesMissions(); // Méthode à créer
        return toutesLesMissions.stream()
                .filter(m -> dateDebutPeriode == null || !m.getDateDebutMission().isBefore(dateDebutPeriode))
                .filter(m -> dateFinPeriode == null || (m.getDateFinMission() != null && !m.getDateFinMission().isAfter(dateFinPeriode)) || (m.getDateFinMission() == null && !m.getDateDebutMission().isAfter(dateFinPeriode)))
                .filter(m -> statutFiltre == null || m.getStatus() == statutFiltre)
                .filter(m -> {
                    if (rechercheVehiculeImmat == null || rechercheVehiculeImmat.trim().isEmpty()) return true;
                    Vehicule v = persistenceService.trouverVehiculeParId(m.getIdVehicule());
                    return v != null && v.getImmatriculation().toLowerCase().contains(rechercheVehiculeImmat.toLowerCase());
                })
                .collect(Collectors.toList());
    }


    public Entretien creerNouvelEntretien(Entretien entretien) {
        Objects.requireNonNull(entretien, "L'objet Entretien ne peut être nul.");
        if (entretien.getIdVehicule() == 0) throw new ErreurValidation("Un véhicule doit être sélectionné pour l'entretien.");
        if (entretien.getDateEntreeEntr() == null) throw new ErreurValidation("La date d'entrée en entretien est requise.");
        if (entretien.getMotifEntr() == null || entretien.getMotifEntr().trim().isEmpty()) throw new ErreurValidation("Le motif est requis.");
        if (entretien.getType() == null) throw new ErreurValidation("Le type d'entretien est requis.");

        if (entretien.getStatutOt() == null) entretien.setStatutOt(StatutOrdreTravail.OUVERT);

        // Si l'entretien est créé et que le véhicule n'est pas déjà "En entretien" ou "Panne", le mettre "En entretien"
        Vehicule vehicule = persistenceService.trouverVehiculeParId(entretien.getIdVehicule());
        if (vehicule != null) {
            EtatVoiture etatActuel = persistenceService.trouverEtatVoitureParId(vehicule.getIdEtatVoiture());
            if (etatActuel != null && !"En entretien".equalsIgnoreCase(etatActuel.getLibEtatVoiture()) && !"Panne".equalsIgnoreCase(etatActuel.getLibEtatVoiture())) {
                EtatVoiture etatEnEntretien = persistenceService.trouverEtatVoitureParLibelle("En entretien");
                if (etatEnEntretien != null) {
                    changerEtatVehicule(vehicule.getIdVehicule(), etatEnEntretien.getIdEtatVoiture(), entretien.getDateEntreeEntr());
                } else {
                    LOGGER_METIER.warning("L'état 'En entretien' est introuvable. L'état du véhicule ne sera pas mis à jour.");
                }
            }
        }
        return persistenceService.sauvegarderEntretien(entretien);
    }

    public Entretien modifierEntretien(Entretien entretien) {
        Objects.requireNonNull(entretien, "L'objet Entretien ne peut être nul pour la modification.");
        if (entretien.getIdEntretien() == 0) throw new ErreurValidation("L'ID de l'entretien est requis.");
        Entretien entretienExistant = persistenceService.trouverEntretienParId(entretien.getIdEntretien());
        if (entretienExistant == null) throw new ErreurLogiqueMetier("Entretien ID " + entretien.getIdEntretien() + " introuvable.");
        if (entretienExistant.getStatutOt() == StatutOrdreTravail.CLOTURE) {
            throw new ErreurLogiqueMetier("Un entretien clôturé ne peut pas être modifié.");
        }
        // Copier les champs modifiables. Le statut OT est géré par une autre méthode.
        entretienExistant.setIdVehicule(entretien.getIdVehicule());
        entretienExistant.setDateEntreeEntr(entretien.getDateEntreeEntr());
        entretienExistant.setDateSortieEntr(entretien.getDateSortieEntr());
        entretienExistant.setMotifEntr(entretien.getMotifEntr());
        entretienExistant.setObservation(entretien.getObservation());
        entretienExistant.setCoutEntr(entretien.getCoutEntr());
        entretienExistant.setLieuEntr(entretien.getLieuEntr());
        entretienExistant.setType(entretien.getType());
        // Ne pas modifier le statutOt ici directement, utiliser changerStatutOrdreTravailEntretien

        // Si l'entretien est marqué comme sorti et était "En entretien", remettre le véhicule "Disponible"
        if (entretienExistant.getDateSortieEntr() != null && entretienExistant.getStatutOt() == StatutOrdreTravail.CLOTURE) {
            Vehicule vehicule = persistenceService.trouverVehiculeParId(entretienExistant.getIdVehicule());
            if (vehicule != null) {
                EtatVoiture etatActuel = persistenceService.trouverEtatVoitureParId(vehicule.getIdEtatVoiture());
                if (etatActuel != null && "En entretien".equalsIgnoreCase(etatActuel.getLibEtatVoiture())) {
                    EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
                    if (etatDisponible != null) {
                        changerEtatVehicule(vehicule.getIdVehicule(), etatDisponible.getIdEtatVoiture(), entretienExistant.getDateSortieEntr());
                    }
                }
            }
        }
        return persistenceService.sauvegarderEntretien(entretienExistant);
    }

    public void supprimerEntretien(int idEntretien) {
        if (idEntretien == 0) throw new ErreurValidation("ID entretien requis.");
        Entretien entretien = persistenceService.trouverEntretienParId(idEntretien);
        if (entretien == null) throw new ErreurLogiqueMetier("Entretien ID " + idEntretien + " introuvable.");
        if (entretien.getStatutOt() != StatutOrdreTravail.OUVERT) {
            throw new ErreurLogiqueMetier("Seul un entretien 'Ouvert' peut être supprimé. Statut: " + entretien.getStatutOt().getDbValue());
        }
        persistenceService.supprimerEntretienParId(idEntretien);
        LOGGER_METIER.info("Entretien ID " + idEntretien + " supprimé.");
    }

    public void changerStatutOrdreTravailEntretien(int idEntretien, StatutOrdreTravail nouveauStatut) {
        if (idEntretien == 0) throw new ErreurValidation("ID entretien requis.");
        Objects.requireNonNull(nouveauStatut, "Le nouveau statut OT ne peut être nul.");
        Entretien entretien = persistenceService.trouverEntretienParId(idEntretien);
        if (entretien == null) throw new ErreurLogiqueMetier("Entretien ID " + idEntretien + " introuvable.");
        if (entretien.getStatutOt() == StatutOrdreTravail.CLOTURE) {
            throw new ErreurLogiqueMetier("Un OT clôturé ne peut changer de statut.");
        }
        if (entretien.getStatutOt() == nouveauStatut) return; // Pas de changement

        entretien.setStatutOt(nouveauStatut);
        if (nouveauStatut == StatutOrdreTravail.CLOTURE && entretien.getDateSortieEntr() == null) {
            entretien.setDateSortieEntr(LocalDateTime.now()); // Date de sortie par défaut si clôture
        }

        // Logique de mise à jour de l'état du véhicule
        Vehicule vehicule = persistenceService.trouverVehiculeParId(entretien.getIdVehicule());
        if (vehicule != null) {
            if (nouveauStatut == StatutOrdreTravail.CLOTURE) {
                EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
                if (etatDisponible != null) {
                    changerEtatVehicule(vehicule.getIdVehicule(), etatDisponible.getIdEtatVoiture(), Objects.requireNonNullElse(entretien.getDateSortieEntr(), LocalDateTime.now()));
                }
            } else if (nouveauStatut == StatutOrdreTravail.EN_COURS || nouveauStatut == StatutOrdreTravail.OUVERT) {
                // S'assurer que le véhicule est marqué "En entretien" ou "Panne"
                EtatVoiture etatActuelVehicule = persistenceService.trouverEtatVoitureParId(vehicule.getIdEtatVoiture());
                if (etatActuelVehicule != null && !"En entretien".equalsIgnoreCase(etatActuelVehicule.getLibEtatVoiture()) && !"Panne".equalsIgnoreCase(etatActuelVehicule.getLibEtatVoiture())) {
                    EtatVoiture etatEnEntretien = persistenceService.trouverEtatVoitureParLibelle("En entretien");
                    if (etatEnEntretien != null) {
                        changerEtatVehicule(vehicule.getIdVehicule(), etatEnEntretien.getIdEtatVoiture(), entretien.getDateEntreeEntr());
                    }
                }
            }
        }
        persistenceService.sauvegarderEntretien(entretien);
        LOGGER_METIER.info("Statut OT de l'entretien ID " + idEntretien + " changé à " + nouveauStatut.getDbValue());
    }

    public List<Entretien> rechercherEntretiensFiltres(LocalDateTime dateDebutPeriode, LocalDateTime dateFinPeriode, TypeEntretien typeFiltre, StatutOrdreTravail statutOTFiltre, String rechercheVehiculeImmat) {
        List<Entretien> tousLesEntretiens = persistenceService.trouverTousLesEntretiens();
        return tousLesEntretiens.stream()
                .filter(e -> dateDebutPeriode == null || !e.getDateEntreeEntr().isBefore(dateDebutPeriode))
                .filter(e -> dateFinPeriode == null || (e.getDateSortieEntr() != null && !e.getDateSortieEntr().isAfter(dateFinPeriode)) || (e.getDateSortieEntr() == null && !e.getDateEntreeEntr().isAfter(dateFinPeriode)))
                .filter(e -> typeFiltre == null || e.getType() == typeFiltre)
                .filter(e -> statutOTFiltre == null || e.getStatutOt() == statutOTFiltre)
                .filter(e -> {
                    if (rechercheVehiculeImmat == null || rechercheVehiculeImmat.trim().isEmpty()) return true;
                    Vehicule v = persistenceService.trouverVehiculeParId(e.getIdVehicule());
                    return v != null && v.getImmatriculation().toLowerCase().contains(rechercheVehiculeImmat.toLowerCase());
                })
                .collect(Collectors.toList());
    }


    public SocietaireCompte creerNouveauCompteSocietaire(SocietaireCompte compte) {
        Objects.requireNonNull(compte, "L'objet SocietaireCompte ne peut être nul.");
        if (compte.getNom() == null || compte.getNom().trim().isEmpty()) throw new ErreurValidation("Le nom du sociétaire est requis.");
        if (compte.getNumero() == null || compte.getNumero().trim().isEmpty()) throw new ErreurValidation("Le numéro de compte est requis.");
        if (persistenceService.trouverSocietaireCompteParNumero(compte.getNumero()) != null) { // Méthode à créer dans PersistenceService
            throw new ErreurValidation("Un compte sociétaire avec le numéro '" + compte.getNumero() + "' existe déjà.");
        }
        if (compte.getSolde() == null) compte.setSolde(BigDecimal.ZERO);
        return persistenceService.sauvegarderSocietaireCompte(compte);
    }

    public SocietaireCompte modifierCompteSocietaire(SocietaireCompte compte) {
        Objects.requireNonNull(compte, "L'objet SocietaireCompte ne peut être nul pour la modification.");
        if (compte.getIdSocietaire() == 0) throw new ErreurValidation("L'ID du compte sociétaire est requis.");
        SocietaireCompte compteExistant = persistenceService.trouverSocietaireCompteParId(compte.getIdSocietaire());
        if (compteExistant == null) throw new ErreurLogiqueMetier("Compte sociétaire ID " + compte.getIdSocietaire() + " introuvable.");

        if (compte.getNumero() != null && !compteExistant.getNumero().equals(compte.getNumero())) {
            SocietaireCompte autreCompteAvecNumero = persistenceService.trouverSocietaireCompteParNumero(compte.getNumero());
            if (autreCompteAvecNumero != null && autreCompteAvecNumero.getIdSocietaire() != compte.getIdSocietaire()) {
                throw new ErreurValidation("Un autre compte sociétaire utilise déjà le numéro '" + compte.getNumero() + "'.");
            }
        }
        // Le solde n'est pas modifié directement ici mais par des mouvements.
        compteExistant.setNom(compte.getNom());
        compteExistant.setNumero(compte.getNumero());
        compteExistant.setEmail(compte.getEmail());
        compteExistant.setTelephone(compte.getTelephone());
        compteExistant.setIdPersonnel(compte.getIdPersonnel());
        return persistenceService.sauvegarderSocietaireCompte(compteExistant);
    }

    public List<SocietaireCompte> rechercherComptesSocietaires(String rechercheNomOuNumero) {
        List<SocietaireCompte> tousLesComptes = persistenceService.trouverTousLesSocietairesComptes();
        if (rechercheNomOuNumero == null || rechercheNomOuNumero.trim().isEmpty()) return tousLesComptes;
        String rechercheLower = rechercheNomOuNumero.toLowerCase();
        return tousLesComptes.stream()
                .filter(c -> c.getNom().toLowerCase().contains(rechercheLower) || c.getNumero().toLowerCase().contains(rechercheLower))
                .collect(Collectors.toList());
    }


    public SocietaireCompte effectuerDepotSurCompteSocietaire(int idSocietaire, BigDecimal montant) {
        if (idSocietaire == 0) throw new ErreurValidation("ID sociétaire requis.");
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) throw new ErreurValidation("Montant de dépôt doit être positif.");
        SocietaireCompte compte = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compte == null) throw new ErreurLogiqueMetier("Compte sociétaire ID " + idSocietaire + " introuvable.");

        compte.setSolde(compte.getSolde().add(montant));
        persistenceService.sauvegarderSocietaireCompte(compte);

        Mouvement mvt = new Mouvement(0, idSocietaire, LocalDateTime.now(), TypeMouvement.DEPOT, montant);
        persistenceService.sauvegarderMouvement(mvt);
        LOGGER_METIER.info("Dépôt de " + montant + " effectué sur compte ID " + idSocietaire);
        return compte;
    }

    public SocietaireCompte effectuerRetraitDeCompteSocietaire(int idSocietaire, BigDecimal montant) {
        if (idSocietaire == 0) throw new ErreurValidation("ID sociétaire requis.");
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) throw new ErreurValidation("Montant de retrait doit être positif.");
        SocietaireCompte compte = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compte == null) throw new ErreurLogiqueMetier("Compte sociétaire ID " + idSocietaire + " introuvable.");
        if (compte.getSolde().compareTo(montant) < 0) {
            throw new ErreurLogiqueMetier("Solde insuffisant sur le compte ID " + idSocietaire + " (Solde: " + compte.getSolde() + ", Retrait: " + montant + ").");
        }

        compte.setSolde(compte.getSolde().subtract(montant));
        persistenceService.sauvegarderSocietaireCompte(compte);

        Mouvement mvt = new Mouvement(0, idSocietaire, LocalDateTime.now(), TypeMouvement.RETRAIT, montant);
        persistenceService.sauvegarderMouvement(mvt);
        LOGGER_METIER.info("Retrait de " + montant + " effectué sur compte ID " + idSocietaire);
        return compte;
    }

    public DocumentSocietaire enregistrerNouveauDocumentSocietaire(int idSocietaire, TypeDocumentSocietaire typeDoc, File fichierSource) throws IOException {
        Objects.requireNonNull(fichierSource, "Le fichier source ne peut être nul.");
        if (!fichierSource.exists() || !fichierSource.isFile()) throw new ErreurValidation("Le fichier source n'existe pas ou n'est pas un fichier valide.");
        if (idSocietaire == 0) throw new ErreurValidation("ID sociétaire requis pour l'upload.");
        Objects.requireNonNull(typeDoc, "Le type de document est requis.");

        SocietaireCompte sc = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (sc == null) throw new ErreurLogiqueMetier("Sociétaire ID " + idSocietaire + " introuvable.");

        Path dossierStockage = Paths.get(DOSSIER_STOCKAGE_DOCUMENTS, String.valueOf(idSocietaire));
        Files.createDirectories(dossierStockage); // Crée si n'existe pas

        String nomFichierOriginal = fichierSource.getName();
        String extension = "";
        int i = nomFichierOriginal.lastIndexOf('.');
        if (i > 0) extension = nomFichierOriginal.substring(i);
        String nomFichierStocke = UUID.randomUUID().toString() + extension; // Nom unique pour éviter collisions
        Path cheminDestination = dossierStockage.resolve(nomFichierStocke);

        Files.copy(fichierSource.toPath(), cheminDestination, StandardCopyOption.REPLACE_EXISTING);

        DocumentSocietaire doc = new DocumentSocietaire();
        doc.setIdSocietaire(idSocietaire);
        doc.setTypeDoc(typeDoc);
        doc.setCheminFichier(cheminDestination.toString()); // Stocker le chemin complet
        doc.setDateUpload(LocalDateTime.now());

        DocumentSocietaire docSauvegarde = persistenceService.sauvegarderDocumentSocietaire(doc);
        LOGGER_METIER.info("Document '" + nomFichierOriginal + "' uploadé et enregistré pour sociétaire ID " + idSocietaire + " sous '" + cheminDestination + "'.");
        return docSauvegarde;
    }

    public void supprimerDocumentSocietaire(int idDoc, String cheminFichierComplet) throws IOException {
        if (idDoc == 0) throw new ErreurValidation("ID document requis.");
        DocumentSocietaire docASupprimer = persistenceService.trouverDocumentSocietaireParId(idDoc);
        if (docASupprimer == null) throw new ErreurLogiqueMetier("Document ID " + idDoc + " introuvable.");

        Path cheminFichier = Paths.get(cheminFichierComplet); // Utiliser le chemin stocké
        if (Files.exists(cheminFichier)) {
            Files.delete(cheminFichier);
            LOGGER_METIER.info("Fichier physique supprimé : " + cheminFichier);
        } else {
            LOGGER_METIER.warning("Fichier physique non trouvé lors de la tentative de suppression : " + cheminFichier);
        }
        persistenceService.supprimerDocumentSocietaireParId(idDoc);
        LOGGER_METIER.info("Entrée document ID " + idDoc + " supprimée de la base de données.");
    }

    public List<DocumentSocietaire> rechercherDocumentsSocietaires(Integer idSocietaireFiltre, TypeDocumentSocietaire typeDocFiltre) {
        if (idSocietaireFiltre != null && typeDocFiltre != null) {
            return persistenceService.trouverDocumentsParSocietaireEtType(idSocietaireFiltre, typeDocFiltre);
        } else if (idSocietaireFiltre != null) {
            return persistenceService.trouverDocumentsParSocietaireId(idSocietaireFiltre);
        } else if (typeDocFiltre != null) {
            return persistenceService.trouverDocumentsParType(typeDocFiltre);
        } else {
            return persistenceService.trouverTousLesDocumentsSocietaires();
        }
    }

    public Utilisateur creerNouvelUtilisateur(Utilisateur utilisateur, String motDePasseEnClair) {
        Objects.requireNonNull(utilisateur, "L'objet Utilisateur ne peut être nul.");
        Objects.requireNonNull(motDePasseEnClair, "Le mot de passe en clair est requis.");
        if (utilisateur.getLogin() == null || utilisateur.getLogin().trim().isEmpty()) throw new ErreurValidation("Login requis.");
        if (persistenceService.trouverUtilisateurParLogin(utilisateur.getLogin()) != null) {
            throw new ErreurValidation("Login '" + utilisateur.getLogin() + "' déjà utilisé.");
        }
        if (utilisateur.getRole() == null) throw new ErreurValidation("Rôle requis.");
        if (motDePasseEnClair.trim().isEmpty()) throw new ErreurValidation("Mot de passe ne peut être vide.");

        SecurityManager sm = new SecurityManager(this.persistenceService); // Instanciation locale pour hachage
        utilisateur.setHashMdp(sm.genererHashMotDePasse(motDePasseEnClair));
        // MFA Secret peut être généré ici si besoin, ou laissé null

        return persistenceService.sauvegarderUtilisateur(utilisateur);
    }

    public Utilisateur modifierUtilisateur(Utilisateur utilisateur) { // Pour rôle, personnel
        Objects.requireNonNull(utilisateur, "L'objet Utilisateur ne peut être nul pour la modification.");
        if (utilisateur.getId() == 0) throw new ErreurValidation("ID utilisateur requis.");
        Utilisateur utilisateurExistant = persistenceService.trouverUtilisateurParId(utilisateur.getId());
        if (utilisateurExistant == null) throw new ErreurLogiqueMetier("Utilisateur ID " + utilisateur.getId() + " introuvable.");

        // Le login et le hashMdp ne sont pas modifiés par cette méthode.
        // Seuls rôle et idPersonnel.
        utilisateurExistant.setRole(utilisateur.getRole());
        utilisateurExistant.setIdPersonnel(utilisateur.getIdPersonnel());
        // Gérer mfaSecret si applicable

        return persistenceService.sauvegarderUtilisateur(utilisateurExistant);
    }

    public Utilisateur modifierMotDePasseUtilisateur(int idUtilisateur, String nouveauMotDePasseEnClair) {
        if (idUtilisateur == 0) throw new ErreurValidation("ID utilisateur requis.");
        Objects.requireNonNull(nouveauMotDePasseEnClair, "Nouveau mot de passe requis.");
        if (nouveauMotDePasseEnClair.trim().isEmpty()) throw new ErreurValidation("Mot de passe ne peut être vide.");

        Utilisateur utilisateur = persistenceService.trouverUtilisateurParId(idUtilisateur);
        if (utilisateur == null) throw new ErreurLogiqueMetier("Utilisateur ID " + idUtilisateur + " introuvable.");

        SecurityManager sm = new SecurityManager(this.persistenceService);
        utilisateur.setHashMdp(sm.genererHashMotDePasse(nouveauMotDePasseEnClair));
        return persistenceService.sauvegarderUtilisateur(utilisateur);
    }

    public void supprimerUtilisateur(int idUtilisateur) {
        if (idUtilisateur == 0) throw new ErreurValidation("ID utilisateur requis.");
        Utilisateur utilisateur = persistenceService.trouverUtilisateurParId(idUtilisateur);
        if (utilisateur == null) throw new ErreurLogiqueMetier("Utilisateur ID " + idUtilisateur + " introuvable.");
        // Ajouter des vérifications si l'utilisateur est lié à des entités critiques non supprimables par cascade.
        // Par exemple, ne pas supprimer un utilisateur s'il est le seul admin (U4).
        if (utilisateur.getRole() == RoleUtilisateur.U4) {
            List<Utilisateur> admins = persistenceService.trouverUtilisateursParRole(RoleUtilisateur.U4); // Méthode à créer
            if (admins.size() <= 1 && admins.get(0).getId() == idUtilisateur) {
                throw new ErreurLogiqueMetier("Impossible de supprimer le seul administrateur système.");
            }
        }
        persistenceService.supprimerUtilisateurParId(idUtilisateur);
        LOGGER_METIER.info("Utilisateur ID " + idUtilisateur + " (" + utilisateur.getLogin() + ") supprimé.");
    }

    public List<Utilisateur> rechercherUtilisateurs(String loginPartiel, RoleUtilisateur roleFiltre) {
        List<Utilisateur> tousLesUtilisateurs = persistenceService.trouverTousLesUtilisateurs();
        return tousLesUtilisateurs.stream()
                .filter(u -> loginPartiel == null || u.getLogin().toLowerCase().contains(loginPartiel.toLowerCase()))
                .filter(u -> roleFiltre == null || u.getRole() == roleFiltre)
                .collect(Collectors.toList());
    }

    // Logique pour les paramètres (simplifiée, car pas de stockage persistant défini pour les paramètres eux-mêmes)
    public boolean obtenirParametreBooleen(String cleParametre, boolean valeurParDefaut) {
        // Simuler la lecture
        LOGGER_METIER.info("Lecture (fictive) du paramètre booléen : " + cleParametre);
        return valeurParDefaut;
    }
    public int obtenirParametreEntier(String cleParametre, int valeurParDefaut) {
        LOGGER_METIER.info("Lecture (fictive) du paramètre entier : " + cleParametre);
        return valeurParDefaut;
    }
    public void sauvegarderParametre(String cleParametre, boolean valeur) {
        LOGGER_METIER.info("Sauvegarde (fictive) du paramètre : " + cleParametre + " = " + valeur);
    }
    public void sauvegarderParametre(String cleParametre, int valeur) {
        LOGGER_METIER.info("Sauvegarde (fictive) du paramètre : " + cleParametre + " = " + valeur);
    }

}