package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class BusinessLogicService {
    private final PersistenceService persistenceService;
    private final SecurityManager securityManager;
    private static final Logger BUSINESS_LOGGER = Logger.getLogger(BusinessLogicService.class.getName());
    private static final String DOSSIER_BASE_DOCUMENTS = "documents_societaires_parcauto";


    public BusinessLogicService(PersistenceService persistenceService, SecurityManager securityManager) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "PersistenceService ne peut être nul.");
        this.securityManager = Objects.requireNonNull(securityManager, "SecurityManager ne peut être nul.");
    }

    public Vehicule creerNouveauVehicule(Vehicule vehicule) throws ErreurValidation, ErreurBaseDeDonnees {
        Objects.requireNonNull(vehicule, "L'objet Vehicule ne peut pas être nul pour la création.");
        validerDonneesVehicule(vehicule, true);

        if (persistenceService.trouverVehiculeParImmatriculation(vehicule.getImmatriculation()) != null) {
            throw new ErreurValidation("Un véhicule avec l'immatriculation '" + vehicule.getImmatriculation() + "' existe déjà.");
        }
        if (persistenceService.trouverVehiculeParNumeroChassi(vehicule.getNumeroChassi()) != null) {
            throw new ErreurValidation("Un véhicule avec le numéro de châssis '" + vehicule.getNumeroChassi() + "' existe déjà.");
        }
        if (vehicule.getDateEtat() == null) {
            vehicule.setDateEtat(LocalDateTime.now());
        }

        return persistenceService.sauvegarderVehicule(vehicule);
    }

    public Vehicule modifierVehicule(Vehicule vehicule) throws ErreurValidation, ErreurBaseDeDonnees {
        Objects.requireNonNull(vehicule, "L'objet Vehicule ne peut pas être nul pour la modification.");
        if (vehicule.getIdVehicule() == 0) {
            throw new ErreurValidation("L'ID du véhicule doit être spécifié pour une modification.");
        }
        validerDonneesVehicule(vehicule, false);

        Vehicule vehiculeExistantImmat = persistenceService.trouverVehiculeParImmatriculation(vehicule.getImmatriculation());
        if (vehiculeExistantImmat != null && vehiculeExistantImmat.getIdVehicule() != vehicule.getIdVehicule()) {
            throw new ErreurValidation("Un autre véhicule avec l'immatriculation '" + vehicule.getImmatriculation() + "' existe déjà.");
        }
        Vehicule vehiculeExistantChassis = persistenceService.trouverVehiculeParNumeroChassi(vehicule.getNumeroChassi());
        if (vehiculeExistantChassis != null && vehiculeExistantChassis.getIdVehicule() != vehicule.getIdVehicule()) {
            throw new ErreurValidation("Un autre véhicule avec le numéro de châssis '" + vehicule.getNumeroChassi() + "' existe déjà.");
        }
        if (vehicule.getDateEtat() == null) {
            Vehicule vExistant = persistenceService.trouverVehiculeParId(vehicule.getIdVehicule());
            if (vExistant != null) vehicule.setDateEtat(vExistant.getDateEtat());
            else vehicule.setDateEtat(LocalDateTime.now());
        }

        return persistenceService.sauvegarderVehicule(vehicule);
    }

    private void validerDonneesVehicule(Vehicule vehicule, boolean estNouveau) throws ErreurValidation {
        if (vehicule.getImmatriculation() == null || vehicule.getImmatriculation().trim().isEmpty()) {
            throw new ErreurValidation("L'immatriculation du véhicule est obligatoire.");
        }
        if (vehicule.getNumeroChassi() == null || vehicule.getNumeroChassi().trim().isEmpty()) {
            throw new ErreurValidation("Le numéro de châssis du véhicule est obligatoire.");
        }
        if (vehicule.getMarque() == null || vehicule.getMarque().trim().isEmpty()) {
            throw new ErreurValidation("La marque du véhicule est obligatoire.");
        }
        if (vehicule.getModele() == null || vehicule.getModele().trim().isEmpty()) {
            throw new ErreurValidation("Le modèle du véhicule est obligatoire.");
        }
        if (vehicule.getEnergie() == null) {
            throw new ErreurValidation("Le type d'énergie du véhicule est obligatoire.");
        }
        if (vehicule.getIdEtatVoiture() == 0 && estNouveau) {
            throw new ErreurValidation("L'état initial du véhicule est obligatoire.");
        }
        if (vehicule.getDateAcquisition() != null && vehicule.getDateAcquisition().isAfter(LocalDateTime.now())) {
            throw new ErreurValidation("La date d'acquisition ne peut être dans le futur.");
        }
        if (vehicule.getDateMiseEnService() != null && vehicule.getDateAcquisition() != null && vehicule.getDateMiseEnService().isBefore(vehicule.getDateAcquisition())) {
            throw new ErreurValidation("La date de mise en service ne peut être antérieure à la date d'acquisition.");
        }
        if (vehicule.getPrixVehicule() != null && vehicule.getPrixVehicule().compareTo(BigDecimal.ZERO) < 0) {
            throw new ErreurValidation("Le prix du véhicule ne peut être négatif.");
        }
        if (vehicule.getKmActuels() != null && vehicule.getKmActuels() < 0) {
            throw new ErreurValidation("Le kilométrage actuel ne peut être négatif.");
        }
    }

    public void supprimerVehicule(int idVehicule) throws ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Vehicule vehiculeASupprimer = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehiculeASupprimer == null) {
            throw new ErreurLogiqueMetier("Le véhicule avec l'ID " + idVehicule + " n'existe pas et ne peut être supprimé.");
        }

        List<Mission> missionsActives = persistenceService.trouverMissionsActivesPourVehicule(idVehicule);
        if (!missionsActives.isEmpty()) {
            throw new ErreurLogiqueMetier("Impossible de supprimer le véhicule ID " + idVehicule + " car il a des missions actives ou planifiées.");
        }

        List<Affectation> affectationsActives = persistenceService.trouverAffectationsActivesPourVehicule(idVehicule);
        if (!affectationsActives.isEmpty()) {
            throw new ErreurLogiqueMetier("Impossible de supprimer le véhicule ID " + idVehicule + " car il a des affectations actives.");
        }

        persistenceService.supprimerToutesMissionsPourVehicule(idVehicule);
        persistenceService.supprimerTousEntretiensPourVehicule(idVehicule);
        persistenceService.supprimerToutesAffectationsPourVehicule(idVehicule);
        persistenceService.supprimerToutesCouverturesPourVehicule(idVehicule);

        persistenceService.supprimerVehiculeParId(idVehicule);
        BUSINESS_LOGGER.info("Véhicule ID " + idVehicule + " et toutes ses données associées ont été supprimés.");
    }

    public Vehicule changerEtatVehicule(int idVehicule, int idNouvelEtatVoiture, LocalDateTime dateChangement) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Vehicule vehicule = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehicule == null) {
            throw new ErreurLogiqueMetier("Véhicule ID " + idVehicule + " non trouvé.");
        }
        EtatVoiture nouvelEtat = persistenceService.trouverEtatVoitureParId(idNouvelEtatVoiture);
        if (nouvelEtat == null) {
            throw new ErreurValidation("Le nouvel état spécifié (ID: " + idNouvelEtatVoiture + ") est invalide.");
        }
        if (dateChangement == null) {
            dateChangement = LocalDateTime.now();
        }

        vehicule.setIdEtatVoiture(idNouvelEtatVoiture);
        vehicule.setDateEtat(dateChangement);
        return persistenceService.sauvegarderVehicule(vehicule);
    }

    public Mission planifierNouvelleMission(Mission mission) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Objects.requireNonNull(mission, "L'objet Mission ne peut être nul pour la planification.");
        validerDonneesMission(mission, true);

        Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        if (vehiculeConcerne == null) {
            throw new ErreurValidation("Le véhicule spécifié pour la mission (ID: " + mission.getIdVehicule() + ") n'existe pas.");
        }
        EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
        if (etatDisponible == null) {
            throw new ErreurLogiqueMetier("L'état 'Disponible' n'est pas configuré dans le système.");
        }
        if (vehiculeConcerne.getIdEtatVoiture() != etatDisponible.getIdEtatVoiture()) {
            EtatVoiture etatActuel = persistenceService.trouverEtatVoitureParId(vehiculeConcerne.getIdEtatVoiture());
            throw new ErreurLogiqueMetier("Le véhicule '" + vehiculeConcerne.getImmatriculation() + "' n'est pas disponible (État actuel: " + (etatActuel != null ? etatActuel.getLibEtatVoiture() : "Inconnu") + ").");
        }

        mission.setStatus(StatutMission.PLANIFIEE);
        Mission missionPlanifiee = persistenceService.sauvegarderMission(mission);

        EtatVoiture etatEnMission = persistenceService.trouverEtatVoitureParLibelle("En mission");
        if (etatEnMission != null) {
            changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatEnMission.getIdEtatVoiture(), mission.getDateDebutMission());
        } else {
            BUSINESS_LOGGER.warning("L'état 'En mission' n'est pas configuré. L'état du véhicule n'a pas été modifié automatiquement.");
        }
        return missionPlanifiee;
    }

    public Mission modifierMission(Mission mission) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Objects.requireNonNull(mission, "L'objet Mission ne peut être nul pour la modification.");
        if (mission.getIdMission() == 0) {
            throw new ErreurValidation("L'ID de la mission doit être spécifié pour une modification.");
        }
        Mission missionExistante = persistenceService.trouverMissionParId(mission.getIdMission());
        if (missionExistante == null) {
            throw new ErreurLogiqueMetier("Mission ID " + mission.getIdMission() + " non trouvée.");
        }
        if (missionExistante.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("Seules les missions planifiées peuvent être modifiées. Statut actuel: " + missionExistante.getStatus().getDbValue());
        }
        validerDonneesMission(mission, false);
        return persistenceService.sauvegarderMission(mission);
    }

    public void annulerMissionPlanifiee(int idMission) throws ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Mission mission = persistenceService.trouverMissionParId(idMission);
        if (mission == null) {
            throw new ErreurLogiqueMetier("Mission ID " + idMission + " non trouvée.");
        }
        if (mission.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("Seules les missions planifiées peuvent être annulées. Statut actuel: " + mission.getStatus().getDbValue());
        }

        Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        if (vehiculeConcerne != null) {
            EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
            if (etatDisponible != null) {
                changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatDisponible.getIdEtatVoiture(), LocalDateTime.now());
            } else {
                BUSINESS_LOGGER.warning("L'état 'Disponible' n'est pas configuré. L'état du véhicule pour la mission annulée n'a pas été modifié.");
            }
        }
        persistenceService.supprimerMissionParId(idMission);
        BUSINESS_LOGGER.info("Mission ID " + idMission + " annulée.");
    }

    private void validerDonneesMission(Mission mission, boolean estNouvelle) throws ErreurValidation {
        if (mission.getLibMission() == null || mission.getLibMission().trim().isEmpty()) {
            throw new ErreurValidation("Le libellé de la mission est obligatoire.");
        }
        if (mission.getIdVehicule() == 0) {
            throw new ErreurValidation("Un véhicule doit être assigné à la mission.");
        }
        if (mission.getDateDebutMission() == null) {
            throw new ErreurValidation("La date de début de la mission est obligatoire.");
        }
        if (mission.getDateDebutMission().isBefore(LocalDateTime.now().minusDays(1)) && estNouvelle) {
            throw new ErreurValidation("La date de début de la mission ne peut pas être dans le passé lointain.");
        }
        if (mission.getDateFinMission() != null && mission.getDateFinMission().isBefore(mission.getDateDebutMission())) {
            throw new ErreurValidation("La date de fin prévue de la mission ne peut être antérieure à sa date de début.");
        }
        if (mission.getKmPrevu() != null && mission.getKmPrevu() < 0) {
            throw new ErreurValidation("Le kilométrage prévu ne peut être négatif.");
        }
    }

    public Mission demarrerUneMission(int idMission) throws ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Mission mission = persistenceService.trouverMissionParId(idMission);
        if (mission == null) {
            throw new ErreurLogiqueMetier("Mission ID " + idMission + " non trouvée.");
        }
        if (mission.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("La mission ne peut être démarrée que si son statut est 'Planifiée'. Statut actuel: " + mission.getStatus().getDbValue());
        }
        mission.setStatus(StatutMission.EN_COURS);
        return persistenceService.sauvegarderMission(mission);
    }

    public Mission cloturerUneMission(int idMission, Integer kmReel, BigDecimal coutTotalCalcule, List<DepenseMission> depenses) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Mission mission = persistenceService.trouverMissionParId(idMission);
        if (mission == null) {
            throw new ErreurLogiqueMetier("Mission ID " + idMission + " non trouvée.");
        }
        if (mission.getStatus() != StatutMission.EN_COURS) {
            throw new ErreurLogiqueMetier("La mission ne peut être clôturée que si son statut est 'En cours'. Statut actuel: " + mission.getStatus().getDbValue());
        }
        if (kmReel == null || kmReel < 0) {
            throw new ErreurValidation("Le kilométrage réel est obligatoire pour clôturer la mission et ne peut être négatif.");
        }
        Vehicule vehicule = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        if (vehicule == null) {
            throw new ErreurLogiqueMetier("Véhicule associé à la mission (ID: " + mission.getIdVehicule() + ") introuvable.");
        }

        mission.setKmReel(kmReel);
        mission.setDateFinMission(LocalDateTime.now());
        mission.setStatus(StatutMission.CLOTUREE);

        BigDecimal coutDepenses = BigDecimal.ZERO;
        if (depenses != null && !depenses.isEmpty()) {
            for (DepenseMission depense : depenses) {
                if (depense.getMontant() == null || depense.getMontant().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ErreurValidation("Le montant d'une dépense ne peut être nul ou négatif.");
                }
                if (depense.getNature() == null) {
                    throw new ErreurValidation("La nature d'une dépense est obligatoire.");
                }
                depense.setIdMission(idMission);
                persistenceService.sauvegarderDepenseMission(depense);
                coutDepenses = coutDepenses.add(depense.getMontant());
            }
        }

        mission.setCoutTotal(coutTotalCalcule != null ? coutTotalCalcule : coutDepenses);

        Mission missionCloturee = persistenceService.sauvegarderMission(mission);

        vehicule.setKmActuels(kmReel);
        EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
        if (etatDisponible != null) {
            changerEtatVehicule(vehicule.getIdVehicule(), etatDisponible.getIdEtatVoiture(), LocalDateTime.now());
        } else {
            BUSINESS_LOGGER.warning("L'état 'Disponible' n'est pas configuré. L'état du véhicule après mission n'a pas été modifié.");
        }
        persistenceService.sauvegarderVehicule(vehicule);

        return missionCloturee;
    }

    public Entretien creerNouvelEntretien(Entretien entretien) throws ErreurValidation, ErreurBaseDeDonnees {
        Objects.requireNonNull(entretien, "L'objet Entretien ne peut être nul.");
        validerDonneesEntretien(entretien);

        Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(entretien.getIdVehicule());
        if (vehiculeConcerne == null) {
            throw new ErreurValidation("Véhicule ID " + entretien.getIdVehicule() + " non trouvé pour l'entretien.");
        }
        if (entretien.getStatutOt() == null) {
            entretien.setStatutOt(StatutOrdreTravail.OUVERT);
        }
        Entretien entretienCree = persistenceService.sauvegarderEntretien(entretien);

        EtatVoiture etatEnEntretien = persistenceService.trouverEtatVoitureParLibelle("En entretien");
        if (etatEnEntretien != null && entretien.getDateSortieEntr() == null) {
            changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatEnEntretien.getIdEtatVoiture(), entretien.getDateEntreeEntr());
        }
        return entretienCree;
    }

    public Entretien modifierEntretien(Entretien entretien) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Objects.requireNonNull(entretien, "L'objet Entretien ne peut être nul.");
        if (entretien.getIdEntretien() == 0) {
            throw new ErreurValidation("L'ID de l'entretien est requis pour la modification.");
        }
        Entretien entretienExistant = persistenceService.trouverEntretienParId(entretien.getIdEntretien());
        if (entretienExistant == null) {
            throw new ErreurLogiqueMetier("Entretien ID " + entretien.getIdEntretien() + " non trouvé.");
        }
        validerDonneesEntretien(entretien);

        Entretien entretienModifie = persistenceService.sauvegarderEntretien(entretien);

        if (entretienModifie.getDateSortieEntr() != null && entretienModifie.getStatutOt() == StatutOrdreTravail.CLOTURE) {
            Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(entretienModifie.getIdVehicule());
            if (vehiculeConcerne != null) {
                EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
                if (etatDisponible != null) {
                    changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatDisponible.getIdEtatVoiture(), entretienModifie.getDateSortieEntr());
                }
            }
        } else if (entretienModifie.getDateSortieEntr() == null && entretienModifie.getStatutOt() != StatutOrdreTravail.CLOTURE) {
            Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(entretienModifie.getIdVehicule());
            if (vehiculeConcerne != null) {
                EtatVoiture etatEnEntretien = persistenceService.trouverEtatVoitureParLibelle("En entretien");
                if (etatEnEntretien != null) {
                    changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatEnEntretien.getIdEtatVoiture(), entretienModifie.getDateEntreeEntr());
                }
            }
        }
        return entretienModifie;
    }

    public Entretien changerStatutOrdreTravail(int idEntretien, StatutOrdreTravail nouveauStatut) throws ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Entretien entretien = persistenceService.trouverEntretienParId(idEntretien);
        if (entretien == null) {
            throw new ErreurLogiqueMetier("Entretien ID " + idEntretien + " non trouvé.");
        }
        entretien.setStatutOt(nouveauStatut);
        if (nouveauStatut == StatutOrdreTravail.CLOTURE && entretien.getDateSortieEntr() == null) {
            entretien.setDateSortieEntr(LocalDateTime.now());
        }
        return modifierEntretien(entretien);
    }

    public void supprimerEntretien(int idEntretien) throws ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Entretien entretienASupprimer = persistenceService.trouverEntretienParId(idEntretien);
        if (entretienASupprimer == null) {
            throw new ErreurLogiqueMetier("L'entretien avec l'ID " + idEntretien + " n'existe pas et ne peut être supprimé.");
        }
        if (entretienASupprimer.getStatutOt() != StatutOrdreTravail.CLOTURE && entretienASupprimer.getDateSortieEntr() == null) {
            Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(entretienASupprimer.getIdVehicule());
            if (vehiculeConcerne != null) {
                EtatVoiture etatEnEntretien = persistenceService.trouverEtatVoitureParLibelle("En entretien");
                if (etatEnEntretien != null && vehiculeConcerne.getIdEtatVoiture() == etatEnEntretien.getIdEtatVoiture()) {
                    EtatVoiture etatDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
                    if (etatDisponible != null) {
                        changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatDisponible.getIdEtatVoiture(), LocalDateTime.now());
                    }
                }
            }
        }
        persistenceService.supprimerEntretienParId(idEntretien);
    }

    private void validerDonneesEntretien(Entretien entretien) throws ErreurValidation {
        if (entretien.getIdVehicule() == 0) {
            throw new ErreurValidation("Un véhicule doit être associé à l'entretien.");
        }
        if (entretien.getDateEntreeEntr() == null) {
            throw new ErreurValidation("La date d'entrée pour l'entretien est obligatoire.");
        }
        if (entretien.getDateSortieEntr() != null && entretien.getDateSortieEntr().isBefore(entretien.getDateEntreeEntr())) {
            throw new ErreurValidation("La date de sortie de l'entretien ne peut être antérieure à la date d'entrée.");
        }
        if (entretien.getMotifEntr() == null || entretien.getMotifEntr().trim().isEmpty()) {
            throw new ErreurValidation("Le motif de l'entretien est obligatoire.");
        }
        if (entretien.getType() == null) {
            throw new ErreurValidation("Le type d'entretien (Préventif/Correctif) est obligatoire.");
        }
        if (entretien.getCoutEntr() != null && entretien.getCoutEntr().compareTo(BigDecimal.ZERO) < 0) {
            throw new ErreurValidation("Le coût de l'entretien ne peut être négatif.");
        }
    }

    public SocietaireCompte creerNouveauCompteSocietaire(SocietaireCompte compte) throws ErreurValidation, ErreurBaseDeDonnees {
        Objects.requireNonNull(compte, "L'objet SocietaireCompte ne peut être nul.");
        validerDonneesCompteSocietaire(compte);
        if (persistenceService.trouverSocietaireCompteParNumero(compte.getNumero()) != null) {
            throw new ErreurValidation("Un compte sociétaire avec le numéro '" + compte.getNumero() + "' existe déjà.");
        }
        if (compte.getSolde() == null) {
            compte.setSolde(BigDecimal.ZERO);
        }
        return persistenceService.sauvegarderSocietaireCompte(compte);
    }

    public SocietaireCompte modifierCompteSocietaire(SocietaireCompte compte) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Objects.requireNonNull(compte, "L'objet SocietaireCompte ne peut être nul.");
        if (compte.getIdSocietaire() == 0) {
            throw new ErreurValidation("L'ID du compte sociétaire est requis pour la modification.");
        }
        SocietaireCompte compteExistant = persistenceService.trouverSocietaireCompteParId(compte.getIdSocietaire());
        if (compteExistant == null) {
            throw new ErreurLogiqueMetier("Compte sociétaire ID " + compte.getIdSocietaire() + " non trouvé.");
        }
        validerDonneesCompteSocietaire(compte);
        SocietaireCompte compteAvecNumero = persistenceService.trouverSocietaireCompteParNumero(compte.getNumero());
        if (compteAvecNumero != null && compteAvecNumero.getIdSocietaire() != compte.getIdSocietaire()) {
            throw new ErreurValidation("Un autre compte sociétaire avec le numéro '" + compte.getNumero() + "' existe déjà.");
        }
        compte.setSolde(compteExistant.getSolde());
        return persistenceService.sauvegarderSocietaireCompte(compte);
    }

    private void validerDonneesCompteSocietaire(SocietaireCompte compte) throws ErreurValidation {
        if (compte.getNom() == null || compte.getNom().trim().isEmpty()) {
            throw new ErreurValidation("Le nom du sociétaire est obligatoire.");
        }
        if (compte.getNumero() == null || compte.getNumero().trim().isEmpty()) {
            throw new ErreurValidation("Le numéro de compte est obligatoire.");
        }
        if (compte.getEmail() != null && !compte.getEmail().trim().isEmpty() && !compte.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new ErreurValidation("Format d'email invalide.");
        }
    }

    public SocietaireCompte effectuerDepotSurCompteSocietaire(int idSocietaire, BigDecimal montant) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErreurValidation("Le montant du dépôt doit être positif.");
        }
        SocietaireCompte compte = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compte == null) {
            throw new ErreurLogiqueMetier("Compte sociétaire ID " + idSocietaire + " non trouvé.");
        }
        compte.setSolde(compte.getSolde().add(montant));
        persistenceService.sauvegarderSocietaireCompte(compte);

        Mouvement mouvement = new Mouvement(0, idSocietaire, LocalDateTime.now(), TypeMouvement.DEPOT, montant);
        persistenceService.sauvegarderMouvement(mouvement);
        return compte;
    }

    public SocietaireCompte effectuerRetraitDeCompteSocietaire(int idSocietaire, BigDecimal montant) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErreurValidation("Le montant du retrait doit être positif.");
        }
        SocietaireCompte compte = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compte == null) {
            throw new ErreurLogiqueMetier("Compte sociétaire ID " + idSocietaire + " non trouvé.");
        }
        if (compte.getSolde().compareTo(montant) < 0) {
            throw new ErreurLogiqueMetier("Solde insuffisant pour effectuer le retrait. Solde actuel: " + compte.getSolde());
        }
        compte.setSolde(compte.getSolde().subtract(montant));
        persistenceService.sauvegarderSocietaireCompte(compte);

        Mouvement mouvement = new Mouvement(0, idSocietaire, LocalDateTime.now(), TypeMouvement.RETRAIT, montant);
        persistenceService.sauvegarderMouvement(mouvement);
        return compte;
    }

    public DocumentSocietaire televerserDocumentSocietaire(int idSocietaire, TypeDocumentSocietaire typeDoc, Path cheminFichierSource, String nomFichierOriginal) throws ErreurValidation, ErreurLogiqueMetier, IOException {
        SocietaireCompte compte = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compte == null) {
            throw new ErreurLogiqueMetier("Compte sociétaire ID " + idSocietaire + " non trouvé pour l'upload du document.");
        }
        if (typeDoc == null) {
            throw new ErreurValidation("Le type de document est obligatoire.");
        }
        if (cheminFichierSource == null || !Files.exists(cheminFichierSource)) {
            throw new ErreurValidation("Le fichier source est invalide ou n'existe pas.");
        }
        if (nomFichierOriginal == null || nomFichierOriginal.trim().isEmpty()) {
            throw new ErreurValidation("Le nom original du fichier est requis.");
        }

        Path dossierSocietaire = Paths.get(DOSSIER_BASE_DOCUMENTS, "societaire_" + idSocietaire);
        Files.createDirectories(dossierSocietaire);

        String extension = "";
        int i = nomFichierOriginal.lastIndexOf('.');
        if (i > 0) {
            extension = nomFichierOriginal.substring(i);
        }
        String nomFichierStocke = typeDoc.getDbValue() + "_" + System.currentTimeMillis() + extension;
        Path cheminFichierDestination = dossierSocietaire.resolve(nomFichierStocke);

        Files.copy(cheminFichierSource, cheminFichierDestination, StandardCopyOption.REPLACE_EXISTING);

        DocumentSocietaire document = new DocumentSocietaire();
        document.setIdSocietaire(idSocietaire);
        document.setTypeDoc(typeDoc);
        document.setCheminFichier(cheminFichierDestination.toString());
        document.setDateUpload(LocalDateTime.now());

        return persistenceService.sauvegarderDocumentSocietaire(document);
    }

    public void supprimerDocumentSocietaire(int idDoc, String cheminFichierStocke) throws ErreurLogiqueMetier, IOException {
        DocumentSocietaire doc = persistenceService.trouverDocumentSocietaireParId(idDoc);
        if (doc == null) {
            throw new ErreurLogiqueMetier("Document ID " + idDoc + " non trouvé.");
        }
        if (!Objects.equals(cheminFichierStocke, doc.getCheminFichier())) {
            BUSINESS_LOGGER.warning("Tentative de suppression de document ID " + idDoc + " avec un chemin de fichier (" + cheminFichierStocke + ") qui ne correspond pas à celui en BDD (" + doc.getCheminFichier() + "). Suppression du fichier annulée.");
        } else {
            Path cheminFichier = Paths.get(cheminFichierStocke);
            if (Files.exists(cheminFichier)) {
                Files.delete(cheminFichier);
                BUSINESS_LOGGER.info("Fichier " + cheminFichierStocke + " supprimé du disque.");
            } else {
                BUSINESS_LOGGER.warning("Fichier " + cheminFichierStocke + " non trouvé sur le disque pour suppression.");
            }
        }
        persistenceService.supprimerDocumentSocietaireParId(idDoc);
        BUSINESS_LOGGER.info("Document ID " + idDoc + " supprimé de la base de données.");
    }

    public Utilisateur creerNouvelUtilisateur(Utilisateur utilisateur, String motDePasseClair) throws ErreurValidation, ErreurBaseDeDonnees {
        Objects.requireNonNull(utilisateur, "L'objet Utilisateur ne peut être nul.");
        Objects.requireNonNull(motDePasseClair, "Le mot de passe ne peut être nul pour un nouvel utilisateur.");
        if (motDePasseClair.trim().isEmpty()) {
            throw new ErreurValidation("Le mot de passe ne peut être vide.");
        }
        validerDonneesUtilisateur(utilisateur);
        if (persistenceService.trouverUtilisateurParLogin(utilisateur.getLogin()) != null) {
            throw new ErreurValidation("Un utilisateur avec le login '" + utilisateur.getLogin() + "' existe déjà.");
        }
        if (utilisateur.getIdPersonnel() != null && persistenceService.trouverPersonnelParId(utilisateur.getIdPersonnel()) == null) {
            throw new ErreurValidation("Le personnel associé (ID: " + utilisateur.getIdPersonnel() + ") n'existe pas.");
        }

        utilisateur.setHashMdp(this.securityManager.genererHashMotDePasse(motDePasseClair));
        return persistenceService.sauvegarderUtilisateur(utilisateur);
    }

    public Utilisateur modifierUtilisateur(Utilisateur utilisateur, String nouveauMotDePasseClairOptionnel) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Objects.requireNonNull(utilisateur, "L'objet Utilisateur ne peut être nul.");
        if (utilisateur.getId() == 0) {
            throw new ErreurValidation("L'ID de l'utilisateur est requis pour la modification.");
        }

        Utilisateur utilisateurExistant = persistenceService.trouverUtilisateurParId(utilisateur.getId());
        if (utilisateurExistant == null) {
            throw new ErreurLogiqueMetier("Utilisateur ID " + utilisateur.getId() + " non trouvé.");
        }

        utilisateur.setLogin(utilisateurExistant.getLogin());

        validerDonneesUtilisateur(utilisateur);

        if (utilisateur.getIdPersonnel() != null && persistenceService.trouverPersonnelParId(utilisateur.getIdPersonnel()) == null) {
            throw new ErreurValidation("Le personnel associé (ID: " + utilisateur.getIdPersonnel() + ") n'existe pas.");
        }

        if (nouveauMotDePasseClairOptionnel != null && !nouveauMotDePasseClairOptionnel.trim().isEmpty()) {
            utilisateur.setHashMdp(this.securityManager.genererHashMotDePasse(nouveauMotDePasseClairOptionnel));
        } else {
            utilisateur.setHashMdp(utilisateurExistant.getHashMdp());
        }
        return persistenceService.sauvegarderUtilisateur(utilisateur);
    }

    public void reinitialiserMotDePasseUtilisateur(int idUtilisateur, String nouveauMotDePasseClair) throws ErreurValidation, ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Utilisateur utilisateur = persistenceService.trouverUtilisateurParId(idUtilisateur);
        if (utilisateur == null) {
            throw new ErreurLogiqueMetier("Utilisateur ID " + idUtilisateur + " non trouvé pour réinitialisation du mot de passe.");
        }
        if (nouveauMotDePasseClair == null || nouveauMotDePasseClair.trim().isEmpty()) {
            throw new ErreurValidation("Le nouveau mot de passe ne peut pas être vide.");
        }
        utilisateur.setHashMdp(this.securityManager.genererHashMotDePasse(nouveauMotDePasseClair));
        persistenceService.sauvegarderUtilisateur(utilisateur);
        BUSINESS_LOGGER.info("Mot de passe réinitialisé pour l'utilisateur ID " + idUtilisateur);
    }

    public void supprimerUtilisateur(int idUtilisateur) throws ErreurLogiqueMetier, ErreurBaseDeDonnees {
        Utilisateur utilisateurASupprimer = persistenceService.trouverUtilisateurParId(idUtilisateur);
        if (utilisateurASupprimer == null) {
            throw new ErreurLogiqueMetier("L'utilisateur avec l'ID " + idUtilisateur + " n'existe pas.");
        }
        if (utilisateurASupprimer.getRole() == RoleUtilisateur.U4) {
            List<Utilisateur> admins = persistenceService.trouverUtilisateursParRole(RoleUtilisateur.U4);
            if (admins.size() <= 1) {
                throw new ErreurLogiqueMetier("Impossible de supprimer le dernier administrateur système.");
            }
        }
        persistenceService.supprimerUtilisateurParId(idUtilisateur);
    }

    private void validerDonneesUtilisateur(Utilisateur utilisateur) throws ErreurValidation {
        if (utilisateur.getLogin() == null || utilisateur.getLogin().trim().isEmpty()) {
            throw new ErreurValidation("Le login de l'utilisateur est obligatoire.");
        }
        if (utilisateur.getRole() == null) {
            throw new ErreurValidation("Le rôle de l'utilisateur est obligatoire.");
        }
    }

    public List<Vehicule> rechercherVehiculesFiltres(String immatriculation, String etatLibelle, EnergieVehicule energie) {
        return persistenceService.rechercherVehiculesFiltres(immatriculation, etatLibelle, energie);
    }

    public List<Mission> rechercherMissionsFiltrees(LocalDateTime dateDebut, LocalDateTime dateFin, StatutMission statut, String rechercheVehiculeImmat) {
        return persistenceService.rechercherMissionsFiltrees(dateDebut, dateFin, statut, rechercheVehiculeImmat);
    }

    public List<Entretien> rechercherEntretiensFiltres(LocalDateTime dateDebut, LocalDateTime dateFin, TypeEntretien type, StatutOrdreTravail statutOT, String rechercheVehiculeImmat) {
        return persistenceService.rechercherEntretiensFiltres(dateDebut, dateFin, type, statutOT, rechercheVehiculeImmat);
    }

    public List<SocietaireCompte> rechercherComptesSocietaires(String rechercheNomOuNumero) {
        return persistenceService.rechercherComptesSocietaires(rechercheNomOuNumero);
    }

    public List<DocumentSocietaire> rechercherDocumentsSocietaires(Integer idSocietaire, TypeDocumentSocietaire typeDoc) {
        if (idSocietaire != null && typeDoc != null) {
            return persistenceService.trouverDocumentsParSocietaireEtType(idSocietaire, typeDoc);
        } else if (idSocietaire != null) {
            return persistenceService.trouverDocumentsParSocietaireId(idSocietaire);
        } else if (typeDoc != null) {
            return persistenceService.trouverDocumentsParType(typeDoc);
        } else {
            return persistenceService.trouverTousLesDocumentsSocietaires();
        }
    }

    public List<Utilisateur> rechercherUtilisateursFiltres(String login, RoleUtilisateur role) {
        if (login != null && !login.trim().isEmpty() && role != null) {
            Utilisateur u = persistenceService.trouverUtilisateurParLogin(login);
            if (u != null && u.getRole() == role) return List.of(u);
            return List.of();
        } else if (login != null && !login.trim().isEmpty()) {
            Utilisateur u = persistenceService.trouverUtilisateurParLogin(login);
            return u != null ? List.of(u) : List.of();
        } else if (role != null) {
            return persistenceService.trouverUtilisateursParRole(role);
        } else {
            return persistenceService.trouverTousLesUtilisateurs();
        }
    }
}