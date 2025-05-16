package main.java.com.miage.parcauto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Remarque: Le package pour AppModels est main.java.com.miage.parcauto.AppModels
// Si AppModels est une classe conteneur, les imports seraient comme suit:
import main.java.com.miage.parcauto.AppModels.EnergieVehicule;
import main.java.com.miage.parcauto.AppModels.StatutMission;
import main.java.com.miage.parcauto.AppModels.TypeEntretien;
import main.java.com.miage.parcauto.AppModels.StatutOrdreTravail;


public class AppDataTransferObjects {

    public static class VehiculeDTO {
        private int idVehicule;
        private String immatriculation;
        private String marque;
        private String modele;
        private String etatLibelle; // Libellé de l'état au lieu de l'ID
        private EnergieVehicule energie;
        private Integer kmActuels;
        private LocalDateTime dateMiseEnService;
        private String nomPersonnelAttribution; // Si applicable

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
        private String immatriculationVehicule; // Pour affichage direct
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
        private AppModels.RoleUtilisateur role;
        private String nomPersonnelAssocie; // Nom complet du personnel

        public UtilisateurDTO(int id, String login, AppModels.RoleUtilisateur role, String nomPersonnelAssocie) {
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
        public AppModels.RoleUtilisateur getRole() { return role; }
        public void setRole(AppModels.RoleUtilisateur role) { this.role = role; }
        public String getNomPersonnelAssocie() { return nomPersonnelAssocie; }
        public void setNomPersonnelAssocie(String nomPersonnelAssocie) { this.nomPersonnelAssocie = nomPersonnelAssocie; }
    }
}