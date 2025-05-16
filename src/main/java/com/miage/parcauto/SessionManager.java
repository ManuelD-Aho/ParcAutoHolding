package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.Utilisateur;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;

public class SessionManager {

    private static Utilisateur utilisateurActuel;

    private SessionManager() {
    }

    public static void definirUtilisateurActuel(Utilisateur utilisateur) {
        utilisateurActuel = utilisateur;
    }

    public static Utilisateur obtenirUtilisateurActuel() {
        return utilisateurActuel;
    }

    public static void deconnecterUtilisateur() {
        utilisateurActuel = null;
    }

    public static boolean estUtilisateurConnecte() {
        return utilisateurActuel != null;
    }

    public static RoleUtilisateur obtenirRoleUtilisateurActuel() {
        return estUtilisateurConnecte() ? utilisateurActuel.getRole() : null;
    }

    public static Integer obtenirIdPersonnelUtilisateurActuel() {
        if (estUtilisateurConnecte() && utilisateurActuel.getIdPersonnel() != null) {
            return utilisateurActuel.getIdPersonnel();
        }
        return null;
    }

    public static Integer obtenirIdSocietaireUtilisateurActuel(PersistenceService persistenceService) {
        if (estUtilisateurConnecte() && utilisateurActuel.getRole() == RoleUtilisateur.U3 && utilisateurActuel.getIdPersonnel() != null) {
            AppModels.SocietaireCompte compte = persistenceService.trouverSocietaireCompteParIdPersonnel(utilisateurActuel.getIdPersonnel());
            if (compte != null) {
                return compte.getIdSocietaire();
            }
        }
        return null;
    }
}