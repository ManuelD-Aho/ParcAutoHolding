package main.java.com.miage.parcauto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import main.java.com.miage.parcauto.AppModels.EnergieVehicule;
import main.java.com.miage.parcauto.AppModels.StatutMission;
import main.java.com.miage.parcauto.AppModels.TypeEntretien;
import main.java.com.miage.parcauto.AppModels.StatutOrdreTravail;
import main.java.com.miage.parcauto.AppModels.TypeMouvement;
import main.java.com.miage.parcauto.AppModels.TypeDocumentSocietaire;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;


public class AppDataTransferObjects {

    public static class VehiculeDTO {
        private int idVehicule;
        private String immatriculation;
        private String marque;
        private String modele;
        private String etatLibelle;
        private EnergieVehicule energie;
        private Integer kmActuels;
        private LocalDateTime dateMiseEnService;
        private String nomPersonnelAttribution;

        public VehiculeDTO(int idVehicule, String immatriculation, String marque, String modele, String etatLibelle, EnergieVehicule energie, Integer kmActuels, LocalDateTime dateMiseEnService, String nomPersonnelAttribution) {
            this.idVehicule = idVehicule;
            this.immatriculation = immatriculation;
            this.marque = marque;
            this.modele = modele;
            this.etatLibelle = etatLibelle;
            this.energie = energie;
            this.kmActuels = kmActuels;
            this.dateMiseEnService = dateMiseEnService;
            this.nomPersonnelAttribution = nomPersonnelAttribution;
        }

        public VehiculeDTO() {}

        public int getIdVehicule() { return idVehicule; }
        public void setIdVehicule(int idVehicule) { this.idVehicule = idVehicule; }
        public String getImmatriculation() { return immatriculation; }
        public void setImmatriculation(String immatriculation) { this.immatriculation = immatriculation; }
        public String getMarque() { return marque; }
        public void setMarque(String marque) { this.marque = marque; }
        public String getModele() { return modele; }
        public void setModele(String modele) { this.modele = modele; }
        public String getEtatLibelle() { return etatLibelle; }
        public void setEtatLibelle(String etatLibelle) { this.etatLibelle = etatLibelle; }
        public EnergieVehicule getEnergie() { return energie; }
        public void setEnergie(EnergieVehicule energie) { this.energie = energie; }
        public Integer getKmActuels() { return kmActuels; }
        public void setKmActuels(Integer kmActuels) { this.kmActuels = kmActuels; }
        public LocalDateTime getDateMiseEnService() { return dateMiseEnService; }
        public void setDateMiseEnService(LocalDateTime dateMiseEnService) { this.dateMiseEnService = dateMiseEnService; }
        public String getNomPersonnelAttribution() { return nomPersonnelAttribution; }
        public void setNomPersonnelAttribution(String nomPersonnelAttribution) { this.nomPersonnelAttribution = nomPersonnelAttribution; }
    }

    public static class MissionDTO {
        private int idMission;
        private String libMission;
        private String immatriculationVehicule;
        private String marqueModeleVehicule;
        private LocalDateTime dateDebutMission;
        private LocalDateTime dateFinMission;
        private StatutMission status;
        private String site;
        private Integer kmPrevu;
        private Integer kmReel;

        public MissionDTO(int idMission, String libMission, String immatriculationVehicule, String marqueModeleVehicule, LocalDateTime dateDebutMission, LocalDateTime dateFinMission, StatutMission status, String site, Integer kmPrevu, Integer kmReel) {
            this.idMission = idMission;
            this.libMission = libMission;
            this.immatriculationVehicule = immatriculationVehicule;
            this.marqueModeleVehicule = marqueModeleVehicule;
            this.dateDebutMission = dateDebutMission;
            this.dateFinMission = dateFinMission;
            this.status = status;
            this.site = site;
            this.kmPrevu = kmPrevu;
            this.kmReel = kmReel;
        }

        public MissionDTO() {}

        public int getIdMission() { return idMission; }
        public void setIdMission(int idMission) { this.idMission = idMission; }
        public String getLibMission() { return libMission; }
        public void setLibMission(String libMission) { this.libMission = libMission; }
        public String getImmatriculationVehicule() { return immatriculationVehicule; }
        public void setImmatriculationVehicule(String immatriculationVehicule) { this.immatriculationVehicule = immatriculationVehicule; }
        public String getMarqueModeleVehicule() { return marqueModeleVehicule; }
        public void setMarqueModeleVehicule(String marqueModeleVehicule) { this.marqueModeleVehicule = marqueModeleVehicule; }
        public LocalDateTime getDateDebutMission() { return dateDebutMission; }
        public void setDateDebutMission(LocalDateTime dateDebutMission) { this.dateDebutMission = dateDebutMission; }
        public LocalDateTime getDateFinMission() { return dateFinMission; }
        public void setDateFinMission(LocalDateTime dateFinMission) { this.dateFinMission = dateFinMission; }
        public StatutMission getStatus() { return status; }
        public void setStatus(StatutMission status) { this.status = status; }
        public String getSite() { return site; }
        public void setSite(String site) { this.site = site; }
        public Integer getKmPrevu() { return kmPrevu; }
        public void setKmPrevu(Integer kmPrevu) { this.kmPrevu = kmPrevu; }
        public Integer getKmReel() { return kmReel; }
        public void setKmReel(Integer kmReel) { this.kmReel = kmReel; }
    }

    public static class EntretienDTO {
        private int idEntretien;
        private String immatriculationVehicule;
        private String marqueModeleVehicule;
        private LocalDateTime dateEntreeEntr;
        private LocalDateTime dateSortieEntr;
        private String motifEntr;
        private BigDecimal coutEntr;
        private TypeEntretien type;
        private StatutOrdreTravail statutOt;

        public EntretienDTO(int idEntretien, String immatriculationVehicule, String marqueModeleVehicule, LocalDateTime dateEntreeEntr, LocalDateTime dateSortieEntr, String motifEntr, BigDecimal coutEntr, TypeEntretien type, StatutOrdreTravail statutOt) {
            this.idEntretien = idEntretien;
            this.immatriculationVehicule = immatriculationVehicule;
            this.marqueModeleVehicule = marqueModeleVehicule;
            this.dateEntreeEntr = dateEntreeEntr;
            this.dateSortieEntr = dateSortieEntr;
            this.motifEntr = motifEntr;
            this.coutEntr = coutEntr;
            this.type = type;
            this.statutOt = statutOt;
        }

        public EntretienDTO() {}

        public int getIdEntretien() { return idEntretien; }
        public void setIdEntretien(int idEntretien) { this.idEntretien = idEntretien; }
        public String getImmatriculationVehicule() { return immatriculationVehicule; }
        public void setImmatriculationVehicule(String immatriculationVehicule) { this.immatriculationVehicule = immatriculationVehicule; }
        public String getMarqueModeleVehicule() { return marqueModeleVehicule; }
        public void setMarqueModeleVehicule(String marqueModeleVehicule) { this.marqueModeleVehicule = marqueModeleVehicule; }
        public LocalDateTime getDateEntreeEntr() { return dateEntreeEntr; }
        public void setDateEntreeEntr(LocalDateTime dateEntreeEntr) { this.dateEntreeEntr = dateEntreeEntr; }
        public LocalDateTime getDateSortieEntr() { return dateSortieEntr; }
        public void setDateSortieEntr(LocalDateTime dateSortieEntr) { this.dateSortieEntr = dateSortieEntr; }
        public String getMotifEntr() { return motifEntr; }
        public void setMotifEntr(String motifEntr) { this.motifEntr = motifEntr; }
        public BigDecimal getCoutEntr() { return coutEntr; }
        public void setCoutEntr(BigDecimal coutEntr) { this.coutEntr = coutEntr; }
        public TypeEntretien getType() { return type; }
        public void setType(TypeEntretien type) { this.type = type; }
        public StatutOrdreTravail getStatutOt() { return statutOt; }
        public void setStatutOt(StatutOrdreTravail statutOt) { this.statutOt = statutOt; }
    }

    public static class UtilisateurDTO {
        private int id;
        private String login;
        private RoleUtilisateur role;
        private String nomPersonnelAssocie;

        public UtilisateurDTO(int id, String login, RoleUtilisateur role, String nomPersonnelAssocie) {
            this.id = id;
            this.login = login;
            this.role = role;
            this.nomPersonnelAssocie = nomPersonnelAssocie;
        }

        public UtilisateurDTO() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public RoleUtilisateur getRole() { return role; }
        public void setRole(RoleUtilisateur role) { this.role = role; }
        public String getNomPersonnelAssocie() { return nomPersonnelAssocie; }
        public void setNomPersonnelAssocie(String nomPersonnelAssocie) { this.nomPersonnelAssocie = nomPersonnelAssocie; }
    }

    public static class SocietaireCompteDTO {
        private int idSocietaire;
        private String nomSocietaire; // Correspond à 'nom' dans le modèle SocietaireCompte
        private String numeroCompte;
        private BigDecimal solde;
        private String email;
        private String telephone;
        private Integer idPersonnelAssocie; // Pour la logique de permission U3

        public SocietaireCompteDTO() {}

        public int getIdSocietaire() { return idSocietaire; }
        public void setIdSocietaire(int idSocietaire) { this.idSocietaire = idSocietaire; }
        public String getNomSocietaire() { return nomSocietaire; }
        public void setNomSocietaire(String nomSocietaire) { this.nomSocietaire = nomSocietaire; }
        public String getNumeroCompte() { return numeroCompte; }
        public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }
        public BigDecimal getSolde() { return solde; }
        public void setSolde(BigDecimal solde) { this.solde = solde; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }
        public Integer getIdPersonnelAssocie() { return idPersonnelAssocie; }
        public void setIdPersonnelAssocie(Integer idPersonnelAssocie) { this.idPersonnelAssocie = idPersonnelAssocie; }
    }

    public static class MouvementDTO {
        private int idMouvement;
        private LocalDateTime dateHeureMouvement;
        private TypeMouvement type;
        private BigDecimal montant;
        // idSocietaire n'est pas nécessaire ici car les mouvements sont affichés dans le contexte d'un sociétaire

        public MouvementDTO() {}

        public int getIdMouvement() { return idMouvement; }
        public void setIdMouvement(int idMouvement) { this.idMouvement = idMouvement; }
        public LocalDateTime getDateHeureMouvement() { return dateHeureMouvement; }
        public void setDateHeureMouvement(LocalDateTime dateHeureMouvement) { this.dateHeureMouvement = dateHeureMouvement; }
        public TypeMouvement getType() { return type; }
        public void setType(TypeMouvement type) { this.type = type; }
        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
    }

    public static class DocumentSocietaireDTO {
        private int idDoc;
        private int idSocietaire; // Garder pour référence interne si besoin
        private String nomSocietaire;
        private TypeDocumentSocietaire typeDoc;
        private String nomFichier; // Juste le nom du fichier pour l'affichage
        private String cheminFichierComplet; // Chemin réel sur le serveur pour téléchargement/suppression
        private LocalDateTime dateUpload;

        public DocumentSocietaireDTO() {}

        public int getIdDoc() { return idDoc; }
        public void setIdDoc(int idDoc) { this.idDoc = idDoc; }
        public int getIdSocietaire() { return idSocietaire; }
        public void setIdSocietaire(int idSocietaire) { this.idSocietaire = idSocietaire; }
        public String getNomSocietaire() { return nomSocietaire; }
        public void setNomSocietaire(String nomSocietaire) { this.nomSocietaire = nomSocietaire; }
        public TypeDocumentSocietaire getTypeDoc() { return typeDoc; }
        public void setTypeDoc(TypeDocumentSocietaire typeDoc) { this.typeDoc = typeDoc; }
        public String getNomFichier() { return nomFichier; }
        public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }
        public String getCheminFichierComplet() { return cheminFichierComplet; }
        public void setCheminFichierComplet(String cheminFichierComplet) { this.cheminFichierComplet = cheminFichierComplet; }
        public LocalDateTime getDateUpload() { return dateUpload; }
        public void setDateUpload(LocalDateTime dateUpload) { this.dateUpload = dateUpload; }
    }
}