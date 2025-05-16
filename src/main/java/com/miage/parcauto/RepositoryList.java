package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;

import java.util.List;
import java.util.Objects;

public class RepositoryList {
    private final PersistenceService persistenceService;

    public RepositoryList(PersistenceService persistenceService) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "Le PersistenceService injecté ne peut être nul.");
    }

    public Affectation trouverAffectationParId(int idAffectation) {
        return persistenceService.trouverAffectationParId(idAffectation);
    }

    public List<Affectation> trouverToutesLesAffectations() {
        return persistenceService.trouverToutesLesAffectations();
    }

    public List<Affectation> trouverAffectationsActivesPourVehicule(int idVehicule) {
        return persistenceService.trouverAffectationsActivesPourVehicule(idVehicule);
    }

    public Affectation sauvegarderAffectation(Affectation affectation) {
        return persistenceService.sauvegarderAffectation(affectation);
    }

    public void supprimerAffectationParId(int idAffectation) {
        persistenceService.supprimerAffectationParId(idAffectation);
    }

    public void supprimerToutesAffectationsPourVehicule(int idVehicule) {
        persistenceService.supprimerToutesAffectationsPourVehicule(idVehicule);
    }

    public Assurance trouverAssuranceParNumCarte(int numCarteAssurance) {
        return persistenceService.trouverAssuranceParNumCarte(numCarteAssurance);
    }

    public List<Assurance> trouverToutesLesAssurances() {
        return persistenceService.trouverToutesLesAssurances();
    }

    public Assurance sauvegarderAssurance(Assurance assurance) {
        return persistenceService.sauvegarderAssurance(assurance);
    }

    public void supprimerAssuranceParNumCarte(int numCarteAssurance) {
        persistenceService.supprimerAssuranceParNumCarte(numCarteAssurance);
    }

    public void ajouterCouvrir(Couvrir couvrir) {
        persistenceService.ajouterCouvrir(couvrir);
    }

    public void supprimerCouvrir(int idVehicule, int numCarteAssurance) {
        persistenceService.supprimerCouvrir(idVehicule, numCarteAssurance);
    }

    public void supprimerToutesCouverturesPourVehicule(int idVehicule) {
        persistenceService.supprimerToutesCouverturesPourVehicule(idVehicule);
    }

    public List<Assurance> trouverAssurancesPourVehicule(int idVehicule) {
        return persistenceService.trouverAssurancesPourVehicule(idVehicule);
    }

    public List<Vehicule> trouverVehiculesPourAssurance(int numCarteAssurance) {
        return persistenceService.trouverVehiculesPourAssurance(numCarteAssurance);
    }

    public DepenseMission trouverDepenseMissionParId(int idDepense) {
        return persistenceService.trouverDepenseMissionParId(idDepense);
    }

    public List<DepenseMission> trouverToutesLesDepensesMission() {
        return persistenceService.trouverToutesLesDepensesMission();
    }

    public List<DepenseMission> trouverDepensesParMissionId(int idMission) {
        return persistenceService.trouverDepensesParMissionId(idMission);
    }

    public DepenseMission sauvegarderDepenseMission(DepenseMission depenseMission) {
        return persistenceService.sauvegarderDepenseMission(depenseMission);
    }

    public void supprimerDepenseMissionParId(int idDepense) {
        persistenceService.supprimerDepenseMissionParId(idDepense);
    }

    public DocumentSocietaire trouverDocumentSocietaireParId(int idDoc) {
        return persistenceService.trouverDocumentSocietaireParId(idDoc);
    }

    public List<DocumentSocietaire> trouverTousLesDocumentsSocietaire() {
        return persistenceService.trouverTousLesDocumentsSocietaire();
    }

    public List<DocumentSocietaire> trouverDocumentsParSocietaireId(int idSocietaire) {
        return persistenceService.trouverDocumentsParSocietaireId(idSocietaire);
    }

    public DocumentSocietaire sauvegarderDocumentSocietaire(DocumentSocietaire document) {
        return persistenceService.sauvegarderDocumentSocietaire(document);
    }

    public void supprimerDocumentSocietaireParId(int idDoc) {
        persistenceService.supprimerDocumentSocietaireParId(idDoc);
    }

    public Entretien trouverEntretienParId(int idEntretien) {
        return persistenceService.trouverEntretienParId(idEntretien);
    }

    public List<Entretien> trouverTousLesEntretiens() {
        return persistenceService.trouverTousLesEntretiens();
    }

    public List<Entretien> trouverEntretiensPourVehicule(int idVehicule) {
        return persistenceService.trouverEntretiensPourVehicule(idVehicule);
    }

    public Entretien sauvegarderEntretien(Entretien entretien) {
        return persistenceService.sauvegarderEntretien(entretien);
    }

    public void supprimerEntretienParId(int idEntretien) {
        persistenceService.supprimerEntretienParId(idEntretien);
    }

    public void supprimerTousEntretiensPourVehicule(int idVehicule) {
        persistenceService.supprimerTousEntretiensPourVehicule(idVehicule);
    }

    public EtatVoiture trouverEtatVoitureParId(int idEtatVoiture) {
        return persistenceService.trouverEtatVoitureParId(idEtatVoiture);
    }

    public EtatVoiture trouverEtatVoitureParLibelle(String libelle) {
        return persistenceService.trouverEtatVoitureParLibelle(libelle);
    }

    public List<EtatVoiture> trouverTousLesEtatsVoiture() {
        return persistenceService.trouverTousLesEtatsVoiture();
    }

    public EtatVoiture sauvegarderEtatVoiture(EtatVoiture etatVoiture) {
        return persistenceService.sauvegarderEtatVoiture(etatVoiture);
    }

    public void supprimerEtatVoitureParId(int idEtatVoiture) {
        persistenceService.supprimerEtatVoitureParId(idEtatVoiture);
    }

    public FonctionPersonnel trouverFonctionPersonnelParId(int idFonction) {
        return persistenceService.trouverFonctionPersonnelParId(idFonction);
    }

    public List<FonctionPersonnel> trouverToutesLesFonctionsPersonnel() {
        return persistenceService.trouverToutesLesFonctionsPersonnel();
    }

    public FonctionPersonnel sauvegarderFonctionPersonnel(FonctionPersonnel fonction) {
        return persistenceService.sauvegarderFonctionPersonnel(fonction);
    }

    public void supprimerFonctionPersonnelParId(int idFonction) {
        persistenceService.supprimerFonctionPersonnelParId(idFonction);
    }

    public Mission trouverMissionParId(int idMission) {
        return persistenceService.trouverMissionParId(idMission);
    }

    public List<Mission> trouverMissionsActivesPourVehicule(int idVehicule) {
        return persistenceService.trouverMissionsActivesPourVehicule(idVehicule);
    }

    public List<Mission> trouverMissionsPourVehicule(int idVehicule) {
        return persistenceService.trouverMissionsPourVehicule(idVehicule);
    }

    public List<Mission> trouverToutesLesMissions() {
        return persistenceService.trouverToutesLesMissions();
    }

    public Mission sauvegarderMission(Mission mission) {
        return persistenceService.sauvegarderMission(mission);
    }

    public void supprimerMissionParId(int idMission) {
        persistenceService.supprimerMissionParId(idMission);
    }

    public void supprimerToutesMissionsPourVehicule(int idVehicule) {
        persistenceService.supprimerToutesMissionsPourVehicule(idVehicule);
    }

    public Mouvement trouverMouvementParId(int idMouvement) {
        return persistenceService.trouverMouvementParId(idMouvement);
    }

    public List<Mouvement> trouverTousLesMouvements() {
        return persistenceService.trouverTousLesMouvements();
    }

    public List<Mouvement> trouverMouvementsParSocietaireId(int idSocietaire) {
        return persistenceService.trouverMouvementsParSocietaireId(idSocietaire);
    }

    public Mouvement sauvegarderMouvement(Mouvement mouvement) {
        return persistenceService.sauvegarderMouvement(mouvement);
    }

    public void supprimerMouvementParId(int idMouvement) {
        persistenceService.supprimerMouvementParId(idMouvement);
    }

    public Personnel trouverPersonnelParId(int idPersonnel) {
        return persistenceService.trouverPersonnelParId(idPersonnel);
    }

    public List<Personnel> trouverToutLePersonnel() {
        return persistenceService.trouverToutLePersonnel();
    }

    public Personnel sauvegarderPersonnel(Personnel personnel) {
        return persistenceService.sauvegarderPersonnel(personnel);
    }

    public void supprimerPersonnelParId(int idPersonnel) {
        persistenceService.supprimerPersonnelParId(idPersonnel);
    }

    public ServiceEntreprise trouverServiceEntrepriseParId(int idService) {
        return persistenceService.trouverServiceEntrepriseParId(idService);
    }

    public List<ServiceEntreprise> trouverTousLesServicesEntreprise() {
        return persistenceService.trouverTousLesServicesEntreprise();
    }

    public ServiceEntreprise sauvegarderServiceEntreprise(ServiceEntreprise service) {
        return persistenceService.sauvegarderServiceEntreprise(service);
    }

    public void supprimerServiceEntrepriseParId(int idService) {
        persistenceService.supprimerServiceEntrepriseParId(idService);
    }

    public SocietaireCompte trouverSocietaireCompteParId(int idSocietaire) {
        return persistenceService.trouverSocietaireCompteParId(idSocietaire);
    }

    public List<SocietaireCompte> trouverTousLesSocietairesComptes() {
        return persistenceService.trouverTousLesSocietairesComptes();
    }

    public SocietaireCompte sauvegarderSocietaireCompte(SocietaireCompte compte) {
        return persistenceService.sauvegarderSocietaireCompte(compte);
    }

    public void supprimerSocietaireCompteParId(int idSocietaire) {
        persistenceService.supprimerSocietaireCompteParId(idSocietaire);
    }

    public Utilisateur trouverUtilisateurParLogin(String login) {
        return persistenceService.trouverUtilisateurParLogin(login);
    }

    public Utilisateur trouverUtilisateurParId(int idUtilisateur) {
        return persistenceService.trouverUtilisateurParId(idUtilisateur);
    }

    public List<Utilisateur> trouverTousLesUtilisateurs() {
        return persistenceService.trouverTousLesUtilisateurs();
    }

    public Utilisateur sauvegarderUtilisateur(Utilisateur utilisateur) {
        return persistenceService.sauvegarderUtilisateur(utilisateur);
    }

    public void supprimerUtilisateurParId(int idUtilisateur) {
        persistenceService.supprimerUtilisateurParId(idUtilisateur);
    }

    public Vehicule trouverVehiculeParId(int idVehicule) {
        return persistenceService.trouverVehiculeParId(idVehicule);
    }

    public Vehicule trouverVehiculeParImmatriculation(String immatriculation) {
        return persistenceService.trouverVehiculeParImmatriculation(immatriculation);
    }

    public Vehicule trouverVehiculeParNumeroChassi(String numeroChassi) {
        return persistenceService.trouverVehiculeParNumeroChassi(numeroChassi);
    }

    public List<Vehicule> trouverTousLesVehicules() {
        return persistenceService.trouverTousLesVehicules();
    }

    public Vehicule sauvegarderVehicule(Vehicule vehicule) {
        return persistenceService.sauvegarderVehicule(vehicule);
    }

    public void supprimerVehiculeParId(int idVehicule) {
        persistenceService.supprimerVehiculeParId(idVehicule);
    }
}