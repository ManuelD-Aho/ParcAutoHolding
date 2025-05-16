package main.java.com.miage.parcauto;

public final class Permissions {
    private Permissions() {} // Classe utilitaire non instanciable

    // Permissions Générales
    public static final String ACCES_ADMINISTRATION_SYSTEME = "TOUT_ACCES_ADMINISTRATEUR"; // U4
    public static final String ACCES_TABLEAU_DE_BORD = "ACCES_TABLEAU_DE_BORD_GENERAL"; // Pour tous
    public static final String ACCES_RAPPORTS_STATISTIQUES = "ACCES_RAPPORTS_ET_STATISTIQUES"; // Pour tous, filtrage dans le module

    // Permissions Véhicules
    public static final String VEHICULE_CREER = "VEHICULE_CREER_NOUVEAU";
    public static final String VEHICULE_MODIFIER = "VEHICULE_MODIFIER_EXISTANT";
    public static final String VEHICULE_SUPPRIMER = "VEHICULE_SUPPRIMER_EXISTANT";
    public static final String VEHICULE_CONSULTER_TOUS = "VEHICULE_CONSULTER_LISTE_COMPLETE";
    public static final String VEHICULE_CONSULTER_PROPRES = "VEHICULE_CONSULTER_SEULEMENT_PROPRES"; // Pour U1 par exemple
    public static final String VEHICULE_CHANGER_ETAT = "VEHICULE_CHANGER_ETAT_OPERATIONNEL";

    // Permissions Missions
    public static final String MISSION_PLANIFIER_NOUVELLE = "MISSION_PLANIFIER_NOUVELLE";
    public static final String MISSION_GERER_TOUTES = "MISSION_GERER_CYCLE_VIE_COMPLET_TOUTES"; // U2, U4
    public static final String MISSION_CONSULTER_PROPRES = "MISSION_CONSULTER_SEULEMENT_PROPRES"; // U1
    public static final String MISSION_DEMARRER = "MISSION_DEMARRER_PLANIFIEE";
    public static final String MISSION_CLOTURER = "MISSION_CLOTURER_EN_COURS";
    public static final String MISSION_AJOUTER_DEPENSES = "MISSION_AJOUTER_DEPENSES_ASSOCIEES";

    // Permissions Entretiens
    public static final String ENTRETIEN_PLANIFIER_PREVENTIF = "ENTRETIEN_PLANIFIER_MAINTENANCE_PREVENTIVE";
    public static final String ENTRETIEN_DECLARER_PANNE_CORRECTIF = "ENTRETIEN_DECLARER_PANNE_MAINTENANCE_CORRECTIVE";
    public static final String ENTRETIEN_GERER_TOUS = "ENTRETIEN_GERER_CYCLE_VIE_COMPLET_TOUS"; // U2, U4
    public static final String ENTRETIEN_CONSULTER_PROPRES = "ENTRETIEN_CONSULTER_PROPRES_VEHICULES"; // U1
    public static final String ENTRETIEN_CHANGER_STATUT_OT = "ENTRETIEN_CHANGER_STATUT_ORDRE_TRAVAIL";

    // Permissions Finances (Sociétaires)
    public static final String FINANCE_CONSULTER_COMPTES_SOCIETAIRES = "FINANCE_CONSULTER_COMPTES_SOCIETAIRES_TOUS"; // U3, U4
    public static final String FINANCE_EFFECTUER_DEPOT_RETRAIT_PROPRE_COMPTE = "FINANCE_EFFECTUER_OPERATIONS_PROPRE_COMPTE_SOCIETAIRE"; // U3
    public static final String FINANCE_GERER_OPERATIONS_TOUS_COMPTES = "FINANCE_GERER_OPERATIONS_FINANCIERES_TOUS_COMPTES"; // U4

    // Permissions Documents
    public static final String DOCUMENT_UPLOAD_PROPRES = "DOCUMENT_UPLOAD_PROPRES_DOCUMENTS_SOCIETAIRES"; // U3
    public static final String DOCUMENT_CONSULTER_PROPRES = "DOCUMENT_CONSULTER_PROPRES_DOCUMENTS_SOCIETAIRES"; // U3
    public static final String DOCUMENT_GERER_PROPRES = "DOCUMENT_GERER_PROPRES_DOCUMENTS"; // Pour U3
    public static final String DOCUMENT_CONSULTER_TOUS = "DOCUMENT_CONSULTER_TOUS_DOCUMENTS_SOCIETAIRES"; // U4, U2 (limité?)
    public static final String DOCUMENT_SUPPRIMER_PROPRES = "DOCUMENT_SUPPRIMER_PROPRES_DOCUMENTS";

    // Permissions Utilisateurs (Administration)
    public static final String UTILISATEUR_CREER_COMPTE = "UTILISATEUR_CREER_NOUVEAU_COMPTE"; // U4
    public static final String UTILISATEUR_MODIFIER_COMPTE = "UTILISATEUR_MODIFIER_COMPTE_EXISTANT"; // U4
    public static final String UTILISATEUR_SUPPRIMER_COMPTE = "UTILISATEUR_SUPPRIMER_COMPTE_EXISTANT"; // U4
    public static final String UTILISATEUR_CHANGER_ROLE = "UTILISATEUR_CHANGER_ROLE_COMPTE"; // U4
    public static final String UTILISATEUR_GERER_COMPTES = "UTILISATEUR_GERER_ENSEMBLE_COMPTES"; // U4 (accès au panel)

    // Permissions Paramètres Application
    public static final String APPLICATION_CONFIGURER_PARAMETRES = "APPLICATION_CONFIGURER_PARAMETRES_GENERAUX"; // U4

}