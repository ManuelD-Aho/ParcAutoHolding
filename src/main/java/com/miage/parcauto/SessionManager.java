package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.Utilisateur;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;

public class SessionManager {
    private static Utilisateur utilisateurConnecteActuellement;

    private SessionManager() {
        // Constructeur privé pour empêcher l'instanciation, classe utilitaire statique.
    }

    public static void definirUtilisateurActuel(Utilisateur utilisateur) {
        utilisateurConnecteActuellement = utilisateur;
    }

    public static Utilisateur obtenirUtilisateurActuel() {
        return utilisateurConnecteActuellement;
    }

    public static void deconnecterUtilisateur() {
        utilisateurConnecteActuellement = null;
    }

    public static boolean estUtilisateurConnecte() {
        return utilisateurConnecteActuellement != null;
    }

    public static RoleUtilisateur obtenirRoleUtilisateurActuel() {
        if (estUtilisateurConnecte()) {
            return utilisateurConnecteActuellement.getRole();
        }
        return null;
    }

    public static Integer obtenirIdPersonnelUtilisateurActuel() {
        if (estUtilisateurConnecte() && utilisateurConnecteActuellement.getIdPersonnel() != null) {
            return utilisateurConnecteActuellement.getIdPersonnel();
        }
        return null;
    }
}