package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppExceptions.*;
import main.java.com.miage.parcauto.PersistenceService;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class BusinessLogicService {
    private final PersistenceService persistenceService;

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
                if (vehicule.getDateEtat() == null) {
                    vehicule.setDateEtat(LocalDateTime.now());
                }
            } else {
                throw new ErreurLogiqueMetier("L'état initial par défaut 'Disponible' pour les véhicules n'est pas configuré dans la base de données.");
            }
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
        // S'assurer que la date de l'état est mise à jour si l'état change
        if (vehiculeExistant.getIdEtatVoiture() != vehicule.getIdEtatVoiture() || vehicule.getDateEtat() == null) {
            vehicule.setDateEtat(LocalDateTime.now());
        }
        return persistenceService.sauvegarderVehicule(vehicule);
    }

    public void supprimerVehicule(int idVehicule) {
        if (idVehicule == 0) {
            throw new ErreurValidation("L'identifiant du véhicule est requis pour la suppression.");
        }
        Vehicule vehiculeASupprimer = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehiculeASupprimer == null) {
            throw new ErreurLogiqueMetier("Aucun véhicule trouvé avec l'identifiant " + idVehicule + ", suppression annulée.");
        }

        List<Mission> missionsActivesOuPlanifiees = persistenceService.trouverMissionsActivesPourVehicule(idVehicule);
        if (!missionsActivesOuPlanifiees.isEmpty()) {
            throw new ErreurLogiqueMetier("Impossible de supprimer le véhicule (ID: " + idVehicule + ") car il est associé à des missions actives ou planifiées.");
        }

        List<Affectation> affectationsActives = persistenceService.trouverAffectationsActivesPourVehicule(idVehicule);
        if (!affectationsActives.isEmpty()) {
            throw new ErreurLogiqueMetier("Impossible de supprimer le véhicule (ID: " + idVehicule + ") car il a des affectations en cours ou non terminées.");
        }

        persistenceService.supprimerToutesCouverturesPourVehicule(idVehicule); // Supprimer les liaisons d'assurance
        persistenceService.supprimerTousEntretiensPourVehicule(idVehicule); // Supprimer les entretiens liés
        persistenceService.supprimerToutesAffectationsPourVehicule(idVehicule); // Supprimer les affectations liées
        persistenceService.supprimerToutesMissionsPourVehicule(idVehicule); // Supprimer les missions liées (si pas déjà géré par contraintes)

        persistenceService.supprimerVehiculeParId(idVehicule);
    }

    public void changerEtatVehicule(int idVehicule, int idNouvelEtatVoiture, LocalDateTime dateEffectiveChangement) {
        if (idVehicule == 0 || idNouvelEtatVoiture == 0) {
            throw new ErreurValidation("Les identifiants du véhicule et du nouvel état sont requis.");
        }
        Vehicule vehicule = persistenceService.trouverVehiculeParId(idVehicule);
        if (vehicule == null) {
            throw new ErreurLogiqueMetier("Aucun véhicule trouvé avec l'identifiant " + idVehicule + ".");
        }
        EtatVoiture nouvelEtat = persistenceService.trouverEtatVoitureParId(idNouvelEtatVoiture);
        if (nouvelEtat == null) {
            throw new ErreurLogiqueMetier("Aucun état de voiture trouvé avec l'identifiant " + idNouvelEtatVoiture + ".");
        }

        vehicule.setIdEtatVoiture(idNouvelEtatVoiture);
        vehicule.setDateEtat(dateEffectiveChangement != null ? dateEffectiveChangement : LocalDateTime.now());
        persistenceService.sauvegarderVehicule(vehicule);
    }

    public Mission planifierNouvelleMission(Mission mission) {
        Objects.requireNonNull(mission, "L'objet Mission ne peut pas être nul.");
        if (mission.getIdVehicule() == 0) {
            throw new ErreurValidation("L'identifiant du véhicule est requis pour planifier une mission.");
        }
        if (mission.getDateDebutMission() == null) {
            throw new ErreurValidation("La date de début de la mission est requise.");
        }
        Vehicule vehiculeConcerne = persistenceService.trouverVehiculeParId(mission.getIdVehicule());
        if (vehiculeConcerne == null) {
            throw new ErreurLogiqueMetier("Aucun véhicule trouvé avec l'identifiant " + mission.getIdVehicule() + " pour cette mission.");
        }
        EtatVoiture etatActuelDuVehicule = persistenceService.trouverEtatVoitureParId(vehiculeConcerne.getIdEtatVoiture());
        if (etatActuelDuVehicule == null || !"Disponible".equalsIgnoreCase(etatActuelDuVehicule.getLibEtatVoiture())) {
            String libelleEtatActuel = etatActuelDuVehicule != null ? etatActuelDuVehicule.getLibEtatVoiture() : "Inconnu";
            throw new ErreurLogiqueMetier("Le véhicule (ID: " + vehiculeConcerne.getIdVehicule() + ", État: " + libelleEtatActuel + ") n'est pas disponible pour une nouvelle mission.");
        }

        EtatVoiture etatVehiculeEnMission = persistenceService.trouverEtatVoitureParLibelle("En mission");
        if (etatVehiculeEnMission == null) {
            throw new ErreurLogiqueMetier("L'état 'En mission', requis pour la planification, n'est pas configuré dans la base de données.");
        }

        changerEtatVehicule(vehiculeConcerne.getIdVehicule(), etatVehiculeEnMission.getIdEtatVoiture(), mission.getDateDebutMission());

        mission.setStatus(StatutMission.PLANIFIEE);
        return persistenceService.sauvegarderMission(mission);
    }

    public void demarrerUneMission(int idMission) {
        if (idMission == 0) {
            throw new ErreurValidation("L'identifiant de la mission est requis pour la démarrer.");
        }
        Mission missionADemarrer = persistenceService.trouverMissionParId(idMission);
        if (missionADemarrer == null) {
            throw new ErreurLogiqueMetier("Aucune mission trouvée avec l'identifiant " + idMission + ".");
        }
        if (missionADemarrer.getStatus() != StatutMission.PLANIFIEE) {
            throw new ErreurLogiqueMetier("La mission (ID: " + idMission + ", Statut actuel: " + missionADemarrer.getStatus() + ") ne peut être démarrée que si son statut est 'Planifiée'.");
        }
        missionADemarrer.setStatus(StatutMission.EN_COURS);
        // La date de début réelle pourrait être mise à jour ici si nécessaire, par exemple missionADemarrer.setDateDebutMission(LocalDateTime.now());
        persistenceService.sauvegarderMission(missionADemarrer);
    }

    public void cloturerUneMission(int idMission, Integer kmReelFinDeMission, BigDecimal coutTotalCalculePourMission, List<DepenseMission> depensesDeLaMission) {
        if (idMission == 0) {
            throw new ErreurValidation("L'identifiant de la mission est requis pour la clôturer.");
        }
        Mission missionACloturer = persistenceService.trouverMissionParId(idMission);
        if (missionACloturer == null) {
            throw new ErreurLogiqueMetier("Aucune mission trouvée avec l'identifiant " + idMission + ".");
        }
        if (missionACloturer.getStatus() != StatutMission.EN_COURS) {
            throw new ErreurLogiqueMetier("La mission (ID: " + idMission + ", Statut actuel: " + missionACloturer.getStatus() + ") ne peut être clôturée que si son statut est 'En cours'.");
        }
        if (kmReelFinDeMission == null || kmReelFinDeMission < 0) {
            throw new ErreurValidation("Le kilométrage réel de fin de mission (ID: " + idMission + ") est requis et doit être positif ou nul.");
        }
        if (missionACloturer.getKmPrevu() != null && kmReelFinDeMission < missionACloturer.getKmPrevu() && (missionACloturer.getKmPrevu() - kmReelFinDeMission > missionACloturer.getKmPrevu()*0.5) ) {
            // Juste un exemple de validation, à affiner. Peut-être une alerte plutôt qu'une erreur bloquante.
            System.out.println("Avertissement: Kilométrage réel ("+kmReelFinDeMission+") significativement inférieur au prévu ("+missionACloturer.getKmPrevu()+") pour mission ID "+idMission);
        }


        missionACloturer.setStatus(StatutMission.CLOTUREE);
        missionACloturer.setKmReel(kmReelFinDeMission);
        missionACloturer.setCoutTotal(coutTotalCalculePourMission); // Peut être nul si non calculé/fourni
        missionACloturer.setDateFinMission(LocalDateTime.now());
        persistenceService.sauvegarderMission(missionACloturer); // Le trigger SQL s'occupera de mettre à jour VEHICULES.km_actuels

        if (depensesDeLaMission != null) {
            for (DepenseMission depense : depensesDeLaMission) {
                Objects.requireNonNull(depense, "Une dépense dans la liste ne peut pas être nulle.");
                depense.setIdMission(idMission);
                persistenceService.sauvegarderDepenseMission(depense);
            }
        }

        Vehicule vehiculeDeLaMission = persistenceService.trouverVehiculeParId(missionACloturer.getIdVehicule());
        if (vehiculeDeLaMission != null) {
            EtatVoiture etatVehiculeDisponible = persistenceService.trouverEtatVoitureParLibelle("Disponible");
            if (etatVehiculeDisponible != null) {
                changerEtatVehicule(vehiculeDeLaMission.getIdVehicule(), etatVehiculeDisponible.getIdEtatVoiture(), LocalDateTime.now());
            } else {
                throw new ErreurLogiqueMetier("L'état 'Disponible', requis après clôture de mission, n'est pas configuré dans la base de données.");
            }
        }
    }

    public SocietaireCompte effectuerDepotSurCompteSocietaire(int idSocietaire, BigDecimal montantADeposer) {
        if (idSocietaire == 0) {
            throw new ErreurValidation("L'identifiant du sociétaire est requis.");
        }
        if (montantADeposer == null || montantADeposer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErreurValidation("Le montant du dépôt doit être un nombre strictement positif.");
        }
        SocietaireCompte compteSocietaire = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compteSocietaire == null) {
            throw new ErreurLogiqueMetier("Aucun compte sociétaire trouvé avec l'identifiant " + idSocietaire + ".");
        }

        compteSocietaire.setSolde(compteSocietaire.getSolde().add(montantADeposer));
        persistenceService.sauvegarderSocietaireCompte(compteSocietaire);

        Mouvement nouveauMouvement = new Mouvement();
        nouveauMouvement.setIdSocietaire(idSocietaire);
        nouveauMouvement.setDate(LocalDateTime.now());
        nouveauMouvement.setType(TypeMouvement.DEPOT);
        nouveauMouvement.setMontant(montantADeposer);
        persistenceService.sauvegarderMouvement(nouveauMouvement);

        return compteSocietaire;
    }

    public SocietaireCompte effectuerRetraitDeCompteSocietaire(int idSocietaire, BigDecimal montantARetirer) {
        if (idSocietaire == 0) {
            throw new ErreurValidation("L'identifiant du sociétaire est requis.");
        }
        if (montantARetirer == null || montantARetirer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErreurValidation("Le montant du retrait doit être un nombre strictement positif.");
        }
        SocietaireCompte compteSocietaire = persistenceService.trouverSocietaireCompteParId(idSocietaire);
        if (compteSocietaire == null) {
            throw new ErreurLogiqueMetier("Aucun compte sociétaire trouvé avec l'identifiant " + idSocietaire + ".");
        }
        if (compteSocietaire.getSolde().compareTo(montantARetirer) < 0) {
            throw new ErreurLogiqueMetier("Solde insuffisant (Solde actuel: " + compteSocietaire.getSolde() + ", Retrait demandé: " + montantARetirer + ") pour le compte sociétaire ID " + idSocietaire + ".");
        }

        compteSocietaire.setSolde(compteSocietaire.getSolde().subtract(montantARetirer));
        persistenceService.sauvegarderSocietaireCompte(compteSocietaire);

        Mouvement nouveauMouvement = new Mouvement();
        nouveauMouvement.setIdSocietaire(idSocietaire);
        nouveauMouvement.setDate(LocalDateTime.now());
        nouveauMouvement.setType(TypeMouvement.RETRAIT);
        nouveauMouvement.setMontant(montantARetirer);
        persistenceService.sauvegarderMouvement(nouveauMouvement);

        return compteSocietaire;
    }

    public Utilisateur creerNouvelUtilisateur(Utilisateur utilisateur, String motDePasseEnClair) {
        Objects.requireNonNull(utilisateur, "L'objet Utilisateur ne peut pas être nul.");
        Objects.requireNonNull(motDePasseEnClair, "Le mot de passe en clair ne peut pas être nul.");
        if (utilisateur.getLogin() == null || utilisateur.getLogin().trim().isEmpty()) {
            throw new ErreurValidation("Le login de l'utilisateur est requis.");
        }
        if (persistenceService.trouverUtilisateurParLogin(utilisateur.getLogin()) != null) {
            throw new ErreurValidation("Un utilisateur avec le login '" + utilisateur.getLogin() + "' existe déjà.");
        }
        if (utilisateur.getRole() == null) {
            throw new ErreurValidation("Le rôle de l'utilisateur est requis.");
        }

        SecurityManager sm = new SecurityManager(this.persistenceService); // Temporaire, ou injecter si service
        utilisateur.setHashMdp(sm.genererHashMotDePasse(motDePasseEnClair));

        return persistenceService.sauvegarderUtilisateur(utilisateur);
    }

    public Utilisateur modifierMotDePasseUtilisateur(int idUtilisateur, String nouveauMotDePasseEnClair) {
        if (idUtilisateur == 0) {
            throw new ErreurValidation("L'identifiant de l'utilisateur est requis.");
        }
        Objects.requireNonNull(nouveauMotDePasseEnClair, "Le nouveau mot de passe en clair ne peut pas être nul.");
        if (nouveauMotDePasseEnClair.trim().isEmpty()){
            throw new ErreurValidation("Le nouveau mot de passe ne peut pas être vide.");
        }

        Utilisateur utilisateurAModifier = persistenceService.trouverUtilisateurParId(idUtilisateur);
        if (utilisateurAModifier == null) {
            throw new ErreurLogiqueMetier("Aucun utilisateur trouvé avec l'identifiant " + idUtilisateur + ".");
        }

        SecurityManager sm = new SecurityManager(this.persistenceService); // Idem
        utilisateurAModifier.setHashMdp(sm.genererHashMotDePasse(nouveauMotDePasseEnClair));
        return persistenceService.sauvegarderUtilisateur(utilisateurAModifier);
    }
}