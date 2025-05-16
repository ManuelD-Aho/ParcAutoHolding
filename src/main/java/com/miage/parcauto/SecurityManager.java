package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.Utilisateur;
import main.java.com.miage.parcauto.AppModels.RoleUtilisateur;
import main.java.com.miage.parcauto.AppExceptions.ErreurAuthentification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class SecurityManager {
    private final PersistenceService persistenceService;
    private static final String ALGORITHME_HACHAGE = "SHA-256";
    private static final int TAILLE_SEL_OCTETS = 16;
    private static final String DELIMITEUR_SEL_HASH = ":";

    public SecurityManager(PersistenceService persistenceService) {
        if (persistenceService == null) {
            throw new IllegalArgumentException("Le PersistenceService ne peut pas être nul.");
        }
        this.persistenceService = persistenceService;
    }

    public Utilisateur authentifierUtilisateur(String login, String motDePasseFourni) {
        if (login == null || login.trim().isEmpty() || motDePasseFourni == null || motDePasseFourni.isEmpty()) {
            throw new ErreurAuthentification("Login et mot de passe sont requis.");
        }
        Utilisateur utilisateur = persistenceService.trouverUtilisateurParLogin(login);
        if (utilisateur == null) {
            throw new ErreurAuthentification("Login inconnu ou mot de passe incorrect.");
        }
        if (!verifierMotDePasse(motDePasseFourni, utilisateur.getHashMdp())) {
            throw new ErreurAuthentification("Login inconnu ou mot de passe incorrect.");
        }
        return utilisateur;
    }

    private boolean verifierMotDePasse(String motDePasseClair, String hashStockeComplet) {
        if (motDePasseClair == null || hashStockeComplet == null || !hashStockeComplet.contains(DELIMITEUR_SEL_HASH)) {
            return false;
        }
        String[] parties = hashStockeComplet.split(DELIMITEUR_SEL_HASH, 2);
        if (parties.length != 2) {
            return false;
        }

        byte[] sel;
        String hashStocke;
        try {
            sel = Base64.getDecoder().decode(parties[0]);
            hashStocke = parties[1];
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (sel.length != TAILLE_SEL_OCTETS) {
            return false;
        }

        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHME_HACHAGE);
            md.update(sel);
            byte[] hashMotDePasseFourniBytes = md.digest(motDePasseClair.getBytes(StandardCharsets.UTF_8));
            String hashMotDePasseFourniEncode = Base64.getEncoder().encodeToString(hashMotDePasseFourniBytes);
            return MessageDigest.isEqual(hashMotDePasseFourniEncode.getBytes(StandardCharsets.UTF_8), hashStocke.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hachage non supporté : " + ALGORITHME_HACHAGE, e);
        }
    }

    public String genererHashMotDePasse(String motDePasseClair) {
        if (motDePasseClair == null || motDePasseClair.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide pour le hachage.");
        }
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            byte[] sel = new byte[TAILLE_SEL_OCTETS];
            sr.nextBytes(sel);

            MessageDigest md = MessageDigest.getInstance(ALGORITHME_HACHAGE);
            md.update(sel);
            byte[] hashMotDePasseBytes = md.digest(motDePasseClair.getBytes(StandardCharsets.UTF_8));

            String selEncode = Base64.getEncoder().encodeToString(sel);
            String hashMotDePasseEncode = Base64.getEncoder().encodeToString(hashMotDePasseBytes);

            return selEncode + DELIMITEUR_SEL_HASH + hashMotDePasseEncode;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hachage ou SecureRandom non disponible : " + e.getMessage(), e);
        }
    }

    public boolean estAutorise(RoleUtilisateur roleUtilisateur, String actionOuModuleCible) {
        if (roleUtilisateur == null || actionOuModuleCible == null || actionOuModuleCible.trim().isEmpty()) {
            return false;
        }

        Map<RoleUtilisateur, Set<String>> definitionsPermissions = new HashMap<>();

        definitionsPermissions.put(RoleUtilisateur.U1,
                new HashSet<>(Set.of(
                        "CONSULTER_VEHICULES_PROPRES",
                        "CONSULTER_MISSIONS_PROPRES",
                        "DECLARER_PANNE_ENTRETIEN_PROPRE_VEHICULE"
                )));

        definitionsPermissions.put(RoleUtilisateur.U2,
                new HashSet<>(Set.of(
                        "CONSULTER_VEHICULES_TOUS",
                        "GERER_MISSIONS_TOUTES",
                        "CONSULTER_ENTRETIENS_VEHICULES",
                        "DECLARER_PANNE_ENTRETIEN_TOUS_VEHICULES"
                )));

        definitionsPermissions.put(RoleUtilisateur.U3,
                new HashSet<>(Set.of(
                        "CONSULTER_VEHICULES_TOUS",
                        "GERER_MISSIONS_TOUTES",
                        "GERER_ENTRETIENS_VEHICULES",
                        "CONSULTER_FINANCES_SOCIETAIRES",
                        "GERER_DOCUMENTS_SOCIETAIRES_PROPRES",
                        "EFFECTUER_OPERATIONS_COMPTE_PROPRE"
                )));

        definitionsPermissions.put(RoleUtilisateur.U4,
                new HashSet<>(Set.of("TOUT_ACCES_ADMINISTRATEUR")));

        Set<String> actionsPermisesPourRole = definitionsPermissions.get(roleUtilisateur);

        if (actionsPermisesPourRole == null) {
            return false;
        }

        if (actionsPermisesPourRole.contains("TOUT_ACCES_ADMINISTRATEUR")) {
            return true;
        }

        return actionsPermisesPourRole.contains(actionOuModuleCible);
    }
}