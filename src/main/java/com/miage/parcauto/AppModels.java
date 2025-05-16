package main.java.com.miage.parcauto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

public class AppModels {

    public enum TypeAffectation {
        CREDIT_5_ANS("Credit5Ans"),
        MISSION("Mission");

        private final String dbValue;

        TypeAffectation(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static TypeAffectation fromDbValue(String dbValue) {
            for (TypeAffectation type : values()) {
                if (type.dbValue.equalsIgnoreCase(dbValue)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour TypeAffectation: " + dbValue);
        }
    }

    public static class Affectation {
        private int id;
        private int idVehicule;
        private Integer idPersonnel;
        private Integer idSocietaire;
        private TypeAffectation type;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;

        public Affectation() {}

        public Affectation(int id, int idVehicule, Integer idPersonnel, Integer idSocietaire, TypeAffectation type, LocalDateTime dateDebut, LocalDateTime dateFin) {
            this.id = id;
            this.idVehicule = idVehicule;
            this.idPersonnel = idPersonnel;
            this.idSocietaire = idSocietaire;
            this.type = type;
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getIdVehicule() { return idVehicule; }
        public void setIdVehicule(int idVehicule) { this.idVehicule = idVehicule; }
        public Integer getIdPersonnel() { return idPersonnel; }
        public void setIdPersonnel(Integer idPersonnel) { this.idPersonnel = idPersonnel; }
        public Integer getIdSocietaire() { return idSocietaire; }
        public void setIdSocietaire(Integer idSocietaire) { this.idSocietaire = idSocietaire; }
        public TypeAffectation getType() { return type; }
        public void setType(TypeAffectation type) { this.type = type; }
        public LocalDateTime getDateDebut() { return dateDebut; }
        public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
        public LocalDateTime getDateFin() { return dateFin; }
        public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    }

    public static class Assurance {
        private int numCarteAssurance;
        private LocalDateTime dateDebutAssurance;
        private LocalDateTime dateFinAssurance;
        private String agence;
        private BigDecimal coutAssurance;

        public Assurance() {}

        public Assurance(int numCarteAssurance, LocalDateTime dateDebutAssurance, LocalDateTime dateFinAssurance, String agence, BigDecimal coutAssurance) {
            this.numCarteAssurance = numCarteAssurance;
            this.dateDebutAssurance = dateDebutAssurance;
            this.dateFinAssurance = dateFinAssurance;
            this.agence = agence;
            this.coutAssurance = coutAssurance;
        }

        public int getNumCarteAssurance() { return numCarteAssurance; }
        public void setNumCarteAssurance(int numCarteAssurance) { this.numCarteAssurance = numCarteAssurance; }
        public LocalDateTime getDateDebutAssurance() { return dateDebutAssurance; }
        public void setDateDebutAssurance(LocalDateTime dateDebutAssurance) { this.dateDebutAssurance = dateDebutAssurance; }
        public LocalDateTime getDateFinAssurance() { return dateFinAssurance; }
        public void setDateFinAssurance(LocalDateTime dateFinAssurance) { this.dateFinAssurance = dateFinAssurance; }
        public String getAgence() { return agence; }
        public void setAgence(String agence) { this.agence = agence; }
        public BigDecimal getCoutAssurance() { return coutAssurance; }
        public void setCoutAssurance(BigDecimal coutAssurance) { this.coutAssurance = coutAssurance; }
    }

    public static class Couvrir {
        private int idVehicule;
        private int numCarteAssurance;

        public Couvrir() {}

        public Couvrir(int idVehicule, int numCarteAssurance) {
            this.idVehicule = idVehicule;
            this.numCarteAssurance = numCarteAssurance;
        }

        public int getIdVehicule() { return idVehicule; }
        public void setIdVehicule(int idVehicule) { this.idVehicule = idVehicule; }
        public int getNumCarteAssurance() { return numCarteAssurance; }
        public void setNumCarteAssurance(int numCarteAssurance) { this.numCarteAssurance = numCarteAssurance; }
    }

    public enum NatureDepense {
        CARBURANT("Carburant"),
        FRAIS_ANNEXES("FraisAnnexes");

        private final String dbValue;

        NatureDepense(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static NatureDepense fromDbValue(String dbValue) {
            for (NatureDepense nature : values()) {
                if (nature.dbValue.equalsIgnoreCase(dbValue)) {
                    return nature;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour NatureDepense: " + dbValue);
        }
    }

    public static class DepenseMission {
        private int id;
        private int idMission;
        private NatureDepense nature;
        private BigDecimal montant;
        private String justificatif;

        public DepenseMission() {}

        public DepenseMission(int id, int idMission, NatureDepense nature, BigDecimal montant, String justificatif) {
            this.id = id;
            this.idMission = idMission;
            this.nature = nature;
            this.montant = montant;
            this.justificatif = justificatif;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getIdMission() { return idMission; }
        public void setIdMission(int idMission) { this.idMission = idMission; }
        public NatureDepense getNature() { return nature; }
        public void setNature(NatureDepense nature) { this.nature = nature; }
        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
        public String getJustificatif() { return justificatif; }
        public void setJustificatif(String justificatif) { this.justificatif = justificatif; }
    }

    public enum TypeDocumentSocietaire {
        CARTE_GRISE("CarteGrise"),
        ASSURANCE("Assurance"),
        ID("ID"),
        PERMIS("Permis");

        private final String dbValue;

        TypeDocumentSocietaire(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static TypeDocumentSocietaire fromDbValue(String dbValue) {
            for (TypeDocumentSocietaire type : values()) {
                if (type.dbValue.equalsIgnoreCase(dbValue)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour TypeDocumentSocietaire: " + dbValue);
        }
    }

    public static class DocumentSocietaire {
        private int idDoc;
        private int idSocietaire;
        private TypeDocumentSocietaire typeDoc;
        private String cheminFichier;
        private LocalDateTime dateUpload;

        public DocumentSocietaire() {}

        public DocumentSocietaire(int idDoc, int idSocietaire, TypeDocumentSocietaire typeDoc, String cheminFichier, LocalDateTime dateUpload) {
            this.idDoc = idDoc;
            this.idSocietaire = idSocietaire;
            this.typeDoc = typeDoc;
            this.cheminFichier = cheminFichier;
            this.dateUpload = dateUpload;
        }

        public int getIdDoc() { return idDoc; }
        public void setIdDoc(int idDoc) { this.idDoc = idDoc; }
        public int getIdSocietaire() { return idSocietaire; }
        public void setIdSocietaire(int idSocietaire) { this.idSocietaire = idSocietaire; }
        public TypeDocumentSocietaire getTypeDoc() { return typeDoc; }
        public void setTypeDoc(TypeDocumentSocietaire typeDoc) { this.typeDoc = typeDoc; }
        public String getCheminFichier() { return cheminFichier; }
        public void setCheminFichier(String cheminFichier) { this.cheminFichier = cheminFichier; }
        public LocalDateTime getDateUpload() { return dateUpload; }
        public void setDateUpload(LocalDateTime dateUpload) { this.dateUpload = dateUpload; }
    }

    public enum TypeEntretien {
        PREVENTIF("Preventif"),
        CORRECTIF("Correctif");

        private final String dbValue;

        TypeEntretien(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static TypeEntretien fromDbValue(String dbValue) {
            for (TypeEntretien type : values()) {
                if (type.dbValue.equalsIgnoreCase(dbValue)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour TypeEntretien: " + dbValue);
        }
    }

    public enum StatutOrdreTravail {
        OUVERT("Ouvert"),
        EN_COURS("EnCours"),
        CLOTURE("Cloture");

        private final String dbValue;

        StatutOrdreTravail(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static StatutOrdreTravail fromDbValue(String dbValue) {
            for (StatutOrdreTravail statut : values()) {
                if (statut.dbValue.equalsIgnoreCase(dbValue)) {
                    return statut;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour StatutOrdreTravail: " + dbValue);
        }
    }

    public static class Entretien {
        private int idEntretien;
        private int idVehicule;
        private LocalDateTime dateEntreeEntr;
        private LocalDateTime dateSortieEntr;
        private String motifEntr;
        private String observation;
        private BigDecimal coutEntr;
        private String lieuEntr;
        private TypeEntretien type;
        private StatutOrdreTravail statutOt;

        public Entretien() {}

        public Entretien(int idEntretien, int idVehicule, LocalDateTime dateEntreeEntr, LocalDateTime dateSortieEntr, String motifEntr, String observation, BigDecimal coutEntr, String lieuEntr, TypeEntretien type, StatutOrdreTravail statutOt) {
            this.idEntretien = idEntretien;
            this.idVehicule = idVehicule;
            this.dateEntreeEntr = dateEntreeEntr;
            this.dateSortieEntr = dateSortieEntr;
            this.motifEntr = motifEntr;
            this.observation = observation;
            this.coutEntr = coutEntr;
            this.lieuEntr = lieuEntr;
            this.type = type;
            this.statutOt = statutOt;
        }

        public int getIdEntretien() { return idEntretien; }
        public void setIdEntretien(int idEntretien) { this.idEntretien = idEntretien; }
        public int getIdVehicule() { return idVehicule; }
        public void setIdVehicule(int idVehicule) { this.idVehicule = idVehicule; }
        public LocalDateTime getDateEntreeEntr() { return dateEntreeEntr; }
        public void setDateEntreeEntr(LocalDateTime dateEntreeEntr) { this.dateEntreeEntr = dateEntreeEntr; }
        public LocalDateTime getDateSortieEntr() { return dateSortieEntr; }
        public void setDateSortieEntr(LocalDateTime dateSortieEntr) { this.dateSortieEntr = dateSortieEntr; }
        public String getMotifEntr() { return motifEntr; }
        public void setMotifEntr(String motifEntr) { this.motifEntr = motifEntr; }
        public String getObservation() { return observation; }
        public void setObservation(String observation) { this.observation = observation; }
        public BigDecimal getCoutEntr() { return coutEntr; }
        public void setCoutEntr(BigDecimal coutEntr) { this.coutEntr = coutEntr; }
        public String getLieuEntr() { return lieuEntr; }
        public void setLieuEntr(String lieuEntr) { this.lieuEntr = lieuEntr; }
        public TypeEntretien getType() { return type; }
        public void setType(TypeEntretien type) { this.type = type; }
        public StatutOrdreTravail getStatutOt() { return statutOt; }
        public void setStatutOt(StatutOrdreTravail statutOt) { this.statutOt = statutOt; }
    }

    public static class EtatVoiture {
        private int idEtatVoiture;
        private String libEtatVoiture;

        public EtatVoiture() {}

        public EtatVoiture(int idEtatVoiture, String libEtatVoiture) {
            this.idEtatVoiture = idEtatVoiture;
            this.libEtatVoiture = libEtatVoiture;
        }

        public int getIdEtatVoiture() { return idEtatVoiture; }
        public void setIdEtatVoiture(int idEtatVoiture) { this.idEtatVoiture = idEtatVoiture; }
        public String getLibEtatVoiture() { return libEtatVoiture; }
        public void setLibEtatVoiture(String libEtatVoiture) { this.libEtatVoiture = libEtatVoiture; }
    }

    public static class FonctionPersonnel {
        private int idFonction;
        private String libFonction;

        public FonctionPersonnel() {}

        public FonctionPersonnel(int idFonction, String libFonction) {
            this.idFonction = idFonction;
            this.libFonction = libFonction;
        }

        public int getIdFonction() { return idFonction; }
        public void setIdFonction(int idFonction) { this.idFonction = idFonction; }
        public String getLibFonction() { return libFonction; }
        public void setLibFonction(String libFonction) { this.libFonction = libFonction; }
    }

    public enum StatutMission {
        PLANIFIEE("Planifiee"),
        EN_COURS("EnCours"),
        CLOTUREE("Cloturee");

        private final String dbValue;

        StatutMission(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static StatutMission fromDbValue(String dbValue) {
            for (StatutMission statut : values()) {
                if (statut.dbValue.equalsIgnoreCase(dbValue)) {
                    return statut;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour StatutMission: " + dbValue);
        }
    }

    public static class Mission {
        private int idMission;
        private int idVehicule;
        private String libMission;
        private String site;
        private LocalDateTime dateDebutMission;
        private LocalDateTime dateFinMission;
        private Integer kmPrevu;
        private Integer kmReel;
        private StatutMission status;
        private BigDecimal coutTotal;
        private String circuitMission;
        private String observationMission;

        public Mission() {}

        public Mission(int idMission, int idVehicule, String libMission, String site, LocalDateTime dateDebutMission, LocalDateTime dateFinMission, Integer kmPrevu, Integer kmReel, StatutMission status, BigDecimal coutTotal, String circuitMission, String observationMission) {
            this.idMission = idMission;
            this.idVehicule = idVehicule;
            this.libMission = libMission;
            this.site = site;
            this.dateDebutMission = dateDebutMission;
            this.dateFinMission = dateFinMission;
            this.kmPrevu = kmPrevu;
            this.kmReel = kmReel;
            this.status = status;
            this.coutTotal = coutTotal;
            this.circuitMission = circuitMission;
            this.observationMission = observationMission;
        }

        public int getIdMission() { return idMission; }
        public void setIdMission(int idMission) { this.idMission = idMission; }
        public int getIdVehicule() { return idVehicule; }
        public void setIdVehicule(int idVehicule) { this.idVehicule = idVehicule; }
        public String getLibMission() { return libMission; }
        public void setLibMission(String libMission) { this.libMission = libMission; }
        public String getSite() { return site; }
        public void setSite(String site) { this.site = site; }
        public LocalDateTime getDateDebutMission() { return dateDebutMission; }
        public void setDateDebutMission(LocalDateTime dateDebutMission) { this.dateDebutMission = dateDebutMission; }
        public LocalDateTime getDateFinMission() { return dateFinMission; }
        public void setDateFinMission(LocalDateTime dateFinMission) { this.dateFinMission = dateFinMission; }
        public Integer getKmPrevu() { return kmPrevu; }
        public void setKmPrevu(Integer kmPrevu) { this.kmPrevu = kmPrevu; }
        public Integer getKmReel() { return kmReel; }
        public void setKmReel(Integer kmReel) { this.kmReel = kmReel; }
        public StatutMission getStatus() { return status; }
        public void setStatus(StatutMission status) { this.status = status; }
        public BigDecimal getCoutTotal() { return coutTotal; }
        public void setCoutTotal(BigDecimal coutTotal) { this.coutTotal = coutTotal; }
        public String getCircuitMission() { return circuitMission; }
        public void setCircuitMission(String circuitMission) { this.circuitMission = circuitMission; }
        public String getObservationMission() { return observationMission; }
        public void setObservationMission(String observationMission) { this.observationMission = observationMission; }
    }

    public enum TypeMouvement {
        DEPOT("Depot"),
        RETRAIT("Retrait"),
        MENSUALITE("Mensualite");

        private final String dbValue;

        TypeMouvement(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static TypeMouvement fromDbValue(String dbValue) {
            for (TypeMouvement type : values()) {
                if (type.dbValue.equalsIgnoreCase(dbValue)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour TypeMouvement: " + dbValue);
        }
    }

    public static class Mouvement {
        private int id;
        private int idSocietaire;
        private LocalDateTime date;
        private TypeMouvement type;
        private BigDecimal montant;

        public Mouvement() {}

        public Mouvement(int id, int idSocietaire, LocalDateTime date, TypeMouvement type, BigDecimal montant) {
            this.id = id;
            this.idSocietaire = idSocietaire;
            this.date = date;
            this.type = type;
            this.montant = montant;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getIdSocietaire() { return idSocietaire; }
        public void setIdSocietaire(int idSocietaire) { this.idSocietaire = idSocietaire; }
        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
        public TypeMouvement getType() { return type; }
        public void setType(TypeMouvement type) { this.type = type; }
        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
    }

    public enum SexePersonnel {
        M("M"),
        F("F");

        private final String dbValue;

        SexePersonnel(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static SexePersonnel fromDbValue(String dbValue) {
            for (SexePersonnel sexe : values()) {
                if (sexe.dbValue.equalsIgnoreCase(dbValue)) {
                    return sexe;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour SexePersonnel: " + dbValue);
        }
    }

    public static class Personnel {
        private int idPersonnel;
        private Integer idService;
        private Integer idFonction;
        private Integer idVehicule;
        private String matricule;
        private String nomPersonnel;
        private String prenomPersonnel;
        private String email;
        private String telephone;
        private String adresse;
        private LocalDate dateNaissance;
        private SexePersonnel sexe;
        private LocalDateTime dateAttribution;

        public Personnel() {}

        public Personnel(int idPersonnel, Integer idService, Integer idFonction, Integer idVehicule, String matricule, String nomPersonnel, String prenomPersonnel, String email, String telephone, String adresse, LocalDate dateNaissance, SexePersonnel sexe, LocalDateTime dateAttribution) {
            this.idPersonnel = idPersonnel;
            this.idService = idService;
            this.idFonction = idFonction;
            this.idVehicule = idVehicule;
            this.matricule = matricule;
            this.nomPersonnel = nomPersonnel;
            this.prenomPersonnel = prenomPersonnel;
            this.email = email;
            this.telephone = telephone;
            this.adresse = adresse;
            this.dateNaissance = dateNaissance;
            this.sexe = sexe;
            this.dateAttribution = dateAttribution;
        }

        public int getIdPersonnel() { return idPersonnel; }
        public void setIdPersonnel(int idPersonnel) { this.idPersonnel = idPersonnel; }
        public Integer getIdService() { return idService; }
        public void setIdService(Integer idService) { this.idService = idService; }
        public Integer getIdFonction() { return idFonction; }
        public void setIdFonction(Integer idFonction) { this.idFonction = idFonction; }
        public Integer getIdVehicule() { return idVehicule; }
        public void setIdVehicule(Integer idVehicule) { this.idVehicule = idVehicule; }
        public String getMatricule() { return matricule; }
        public void setMatricule(String matricule) { this.matricule = matricule; }
        public String getNomPersonnel() { return nomPersonnel; }
        public void setNomPersonnel(String nomPersonnel) { this.nomPersonnel = nomPersonnel; }
        public String getPrenomPersonnel() { return prenomPersonnel; }
        public void setPrenomPersonnel(String prenomPersonnel) { this.prenomPersonnel = prenomPersonnel; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }
        public String getAdresse() { return adresse; }
        public void setAdresse(String adresse) { this.adresse = adresse; }
        public LocalDate getDateNaissance() { return dateNaissance; }
        public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
        public SexePersonnel getSexe() { return sexe; }
        public void setSexe(SexePersonnel sexe) { this.sexe = sexe; }
        public LocalDateTime getDateAttribution() { return dateAttribution; }
        public void setDateAttribution(LocalDateTime dateAttribution) { this.dateAttribution = dateAttribution; }
    }

    public static class ServiceEntreprise {
        private int idService;
        private String libService;
        private String localisationService;

        public ServiceEntreprise() {}

        public ServiceEntreprise(int idService, String libService, String localisationService) {
            this.idService = idService;
            this.libService = libService;
            this.localisationService = localisationService;
        }

        public int getIdService() { return idService; }
        public void setIdService(int idService) { this.idService = idService; }
        public String getLibService() { return libService; }
        public void setLibService(String libService) { this.libService = libService; }
        public String getLocalisationService() { return localisationService; }
        public void setLocalisationService(String localisationService) { this.localisationService = localisationService; }
    }

    public static class SocietaireCompte {
        private int idSocietaire;
        private Integer idPersonnel;
        private String nom;
        private String numero;
        private BigDecimal solde;
        private String email;
        private String telephone;

        public SocietaireCompte() {}

        public SocietaireCompte(int idSocietaire, Integer idPersonnel, String nom, String numero, BigDecimal solde, String email, String telephone) {
            this.idSocietaire = idSocietaire;
            this.idPersonnel = idPersonnel;
            this.nom = nom;
            this.numero = numero;
            this.solde = solde;
            this.email = email;
            this.telephone = telephone;
        }

        public int getIdSocietaire() { return idSocietaire; }
        public void setIdSocietaire(int idSocietaire) { this.idSocietaire = idSocietaire; }
        public Integer getIdPersonnel() { return idPersonnel; }
        public void setIdPersonnel(Integer idPersonnel) { this.idPersonnel = idPersonnel; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
        public BigDecimal getSolde() { return solde; }
        public void setSolde(BigDecimal solde) { this.solde = solde; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }
    }

    public enum RoleUtilisateur {
        U1("U1"),
        U2("U2"),
        U3("U3"),
        U4("U4");

        private final String dbValue;

        RoleUtilisateur(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static RoleUtilisateur fromDbValue(String dbValue) {
            for (RoleUtilisateur role : values()) {
                if (role.dbValue.equalsIgnoreCase(dbValue)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Valeur inconnue pour RoleUtilisateur: " + dbValue);
        }
    }

    public static class Utilisateur {
        private int id;
        private String login;
        private String hashMdp;
        private RoleUtilisateur role;
        private Integer idPersonnel;
        private String mfaSecret;

        public Utilisateur() {}

        public Utilisateur(int id, String login, String hashMdp, RoleUtilisateur role, Integer idPersonnel, String mfaSecret) {
            this.id = id;
            this.login = login;
            this.hashMdp = hashMdp;
            this.role = role;
            this.idPersonnel = idPersonnel;
            this.mfaSecret = mfaSecret;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getHashMdp() { return hashMdp; }
        public void setHashMdp(String hashMdp) { this.hashMdp = hashMdp; }
        public RoleUtilisateur getRole() { return role; }
        public void setRole(RoleUtilisateur role) { this.role = role; }
        public Integer getIdPersonnel() { return idPersonnel; }
        public void setIdPersonnel(Integer idPersonnel) { this.idPersonnel = idPersonnel; }
        public String getMfaSecret() { return mfaSecret; }
        public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
    }

    public enum EnergieVehicule {
        DIESEL("Diesel"),
        ESSENCE("Essence"),
        ELECTRIQUE("Électrique"),
        HYBRIDE("Hybride");

        private final String dbValue;

        EnergieVehicule(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static EnergieVehicule fromDbValue(String dbValue) {
            return Arrays.stream(values())
                    .filter(energie -> energie.dbValue.equalsIgnoreCase(dbValue))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Valeur inconnue pour EnergieVehicule: " + dbValue));
        }
    }

    public static class Vehicule {
        private int idVehicule;
        private int idEtatVoiture;
        private EnergieVehicule energie;
        private String numeroChassi;
        private String immatriculation;
        private String marque;
        private String modele;
        private Integer nbPlaces;
        private LocalDateTime dateAcquisition;
        private LocalDateTime dateAmmortissement;
        private LocalDateTime dateMiseEnService;
        private Integer puissance;
        private String couleur;
        private BigDecimal prixVehicule;
        private Integer kmActuels;
        private LocalDateTime dateEtat;

        public Vehicule() {}

        public Vehicule(int idVehicule, int idEtatVoiture, EnergieVehicule energie, String numeroChassi, String immatriculation, String marque, String modele, Integer nbPlaces, LocalDateTime dateAcquisition, LocalDateTime dateAmmortissement, LocalDateTime dateMiseEnService, Integer puissance, String couleur, BigDecimal prixVehicule, Integer kmActuels, LocalDateTime dateEtat) {
            this.idVehicule = idVehicule;
            this.idEtatVoiture = idEtatVoiture;
            this.energie = energie;
            this.numeroChassi = numeroChassi;
            this.immatriculation = immatriculation;
            this.marque = marque;
            this.modele = modele;
            this.nbPlaces = nbPlaces;
            this.dateAcquisition = dateAcquisition;
            this.dateAmmortissement = dateAmmortissement;
            this.dateMiseEnService = dateMiseEnService;
            this.puissance = puissance;
            this.couleur = couleur;
            this.prixVehicule = prixVehicule;
            this.kmActuels = kmActuels;
            this.dateEtat = dateEtat;
        }

        public int getIdVehicule() { return idVehicule; }
        public void setIdVehicule(int idVehicule) { this.idVehicule = idVehicule; }
        public int getIdEtatVoiture() { return idEtatVoiture; }
        public void setIdEtatVoiture(int idEtatVoiture) { this.idEtatVoiture = idEtatVoiture; }
        public EnergieVehicule getEnergie() { return energie; }
        public void setEnergie(EnergieVehicule energie) { this.energie = energie; }
        public String getNumeroChassi() { return numeroChassi; }
        public void setNumeroChassi(String numeroChassi) { this.numeroChassi = numeroChassi; }
        public String getImmatriculation() { return immatriculation; }
        public void setImmatriculation(String immatriculation) { this.immatriculation = immatriculation; }
        public String getMarque() { return marque; }
        public void setMarque(String marque) { this.marque = marque; }
        public String getModele() { return modele; }
        public void setModele(String modele) { this.modele = modele; }
        public Integer getNbPlaces() { return nbPlaces; }
        public void setNbPlaces(Integer nbPlaces) { this.nbPlaces = nbPlaces; }
        public LocalDateTime getDateAcquisition() { return dateAcquisition; }
        public void setDateAcquisition(LocalDateTime dateAcquisition) { this.dateAcquisition = dateAcquisition; }
        public LocalDateTime getDateAmmortissement() { return dateAmmortissement; }
        public void setDateAmmortissement(LocalDateTime dateAmmortissement) { this.dateAmmortissement = dateAmmortissement; }
        public LocalDateTime getDateMiseEnService() { return dateMiseEnService; }
        public void setDateMiseEnService(LocalDateTime dateMiseEnService) { this.dateMiseEnService = dateMiseEnService; }
        public Integer getPuissance() { return puissance; }
        public void setPuissance(Integer puissance) { this.puissance = puissance; }
        public String getCouleur() { return couleur; }
        public void setCouleur(String couleur) { this.couleur = couleur; }
        public BigDecimal getPrixVehicule() { return prixVehicule; }
        public void setPrixVehicule(BigDecimal prixVehicule) { this.prixVehicule = prixVehicule; }
        public Integer getKmActuels() { return kmActuels; }
        public void setKmActuels(Integer kmActuels) { this.kmActuels = kmActuels; }
        public LocalDateTime getDateEtat() { return dateEtat; }
        public void setDateEtat(LocalDateTime dateEtat) { this.dateEtat = dateEtat; }
    }
}