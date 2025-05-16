# Instructions pour la Génération du Code - Projet ParcAuto

## Préambule et Consignes Générales

- **Langage :** Java 21 (compatible avec les fonctionnalités Java 17)
- **Framework UI :** JavaFX
- **Base de Données :** MySQL 8.3 (schéma fourni dans `db/ParcAuto.sql`)
- **Package Principal Java :** `com.miage.parcauto`
- **Style de Code :**
  - Aucune ligne de commentaire dans le code source Java généré.
  - Le code doit être complet, fonctionnel et ne pas laisser de sections "à implémenter".
  - Utiliser exclusivement les attributs définis dans le schéma de la base de données pour les classes modèles. Aucune invention d'attributs.
  - Respecter les conventions de nommage Java (camelCase pour les variables et méthodes, PascalCase pour les classes et enums). Les enums Java utiliseront la convention `NOM_EN_MAJUSCULES`.
  - Les imports doivent être spécifiques (pas d'imports `*`).
  - La langue de tous les identifiants (variables, méthodes, classes), hors ceux directement issus de la BDD, sera le français pour améliorer la lisibilité. Pour les attributs des modèles, utiliser les noms des colonnes de la BDD en camelCase.
  - La gestion des exceptions doit être robuste. Utiliser les exceptions personnalisées définies dans `AppExceptions.java` lorsque c'est pertinent.
- **Utilitaires Fournis :**
  - `dbUtil.java` : Utiliser cette classe fournie pour toutes les interactions avec la base de données (connexion, fermeture). S'assurer que `db.properties` est placé à la racine du classpath des ressources (`src/main/resources/db.properties`) pour un chargement correct.
  - **Configuration Base de Données :** Le fichier `db.properties` (`src/main/resources/db.properties`) doit être configuré avec l'URL de base de données appropriée pour l'environnement d'exécution. Pour une exécution dans Docker via le `docker-compose.yml` fourni, l'URL doit être `jdbc:mysql://db:3306/ParcAuto...` (le service MySQL s'appelle `db`).
- **Structure du Projet :** Suivre l'arborescence fournie.

---

## Fichier `AppModels.java`

**Emplacement :** `src/main/java/com/miage/parcauto/AppModels.java`

Ce fichier contiendra toutes les classes modèles (entités) représentant les tables de la base de données. Chaque classe doit correspondre à une table, et ses attributs aux colonnes de cette table, avec les types Java appropriés. Inclure constructeurs (par défaut et avec tous les arguments), getters et setters pour tous les attributs. Pour les types `ENUM` de MySQL avec des caractères spéciaux ou des différences de casse par rapport aux identifiants Java, inclure une méthode `getDbValue()` et une méthode statique `fromDbValue(String dbValue)`.

### Enum `TypeAffectation`
    `CREDIT_5_ANS("Credit5Ans")`, `MISSION("Mission")`

### Classe `Affectation`
    - `private int id;`
    - `private int idVehicule;`
    - `private Integer idPersonnel;`
    - `private Integer idSocietaire;`
    - `private TypeAffectation type;`
    - `private java.time.LocalDateTime dateDebut;`
    - `private java.time.LocalDateTime dateFin;`

### Classe `Assurance`
    - `private int numCarteAssurance;`
    - `private java.time.LocalDateTime dateDebutAssurance;`
    - `private java.time.LocalDateTime dateFinAssurance;`
    - `private String agence;`
    - `private java.math.BigDecimal coutAssurance;`

### Classe `Couvrir`
    - `private int idVehicule;`
    - `private int numCarteAssurance;`

### Enum `NatureDepense`
    `CARBURANT("Carburant")`, `FRAIS_ANNEXES("FraisAnnexes")`

### Classe `DepenseMission`
    - `private int id;`
    - `private int idMission;`
    - `private NatureDepense nature;`
    - `private java.math.BigDecimal montant;`
    - `private String justificatif;`

### Enum `TypeDocumentSocietaire` (pour `type_doc` dans `DOCUMENT_SOCIETAIRE`)
    `CARTE_GRISE("CarteGrise")`, `ASSURANCE("Assurance")`, `ID("ID")`, `PERMIS("Permis")`

### Classe `DocumentSocietaire`
    - `private int idDoc;`
    - `private int idSocietaire;`
    - `private TypeDocumentSocietaire typeDoc;`
    - `private String cheminFichier;`
    - `private java.time.LocalDateTime dateUpload;`

### Enum `TypeEntretien`
    `PREVENTIF("Preventif")`, `CORRECTIF("Correctif")`

### Enum `StatutOrdreTravail` (pour `statut_ot` dans `ENTRETIEN`)
    `OUVERT("Ouvert")`, `EN_COURS("EnCours")`, `CLOTURE("Cloture")`

### Classe `Entretien`
    - `private int idEntretien;`
    - `private int idVehicule;`
    - `private java.time.LocalDateTime dateEntreeEntr;`
    - `private java.time.LocalDateTime dateSortieEntr;`
    - `private String motifEntr;`
    - `private String observation;`
    - `private java.math.BigDecimal coutEntr;`
    - `private String lieuEntr;`
    - `private TypeEntretien type;`
    - `private StatutOrdreTravail statutOt;`

### Classe `EtatVoiture`
    - `private int idEtatVoiture;`
    - `private String libEtatVoiture;`
    // Pour les valeurs: "Attribuer", "Disponible", "En entretien", "En mission", "Hors Service", "Panne"
    // On peut envisager un enum Java si ces valeurs sont fixes et utilisées en logique, sinon String suffit.
    // Pour l'instant, on s'en tient à String car la BDD pourrait avoir d'autres valeurs via IHM admin.

### Classe `FonctionPersonnel` (pour la table `FONCTION`)
    - `private int idFonction;`
    - `private String libFonction;`

### Enum `StatutMission`
    `PLANIFIEE("Planifiee")`, `EN_COURS("EnCours")`, `CLOTUREE("Cloturee")`

### Classe `Mission`
    - `private int idMission;`
    - `private int idVehicule;`
    - `private String libMission;`
    - `private String site;`
    - `private java.time.LocalDateTime dateDebutMission;`
    - `private java.time.LocalDateTime dateFinMission;`
    - `private Integer kmPrevu;`
    - `private Integer kmReel;`
    - `private StatutMission status;`
    - `private java.math.BigDecimal coutTotal;`
    - `private String circuitMission;`
    - `private String observationMission;`

### Enum `TypeMouvement`
    `DEPOT("Depot")`, `RETRAIT("Retrait")`, `MENSUALITE("Mensualite")`

### Classe `Mouvement`
    - `private int id;`
    - `private int idSocietaire;`
    - `private java.time.LocalDateTime date;`
    - `private TypeMouvement type;`
    - `private java.math.BigDecimal montant;`

### Enum `SexePersonnel` (pour `sexe` dans `PERSONNEL`)
    `M("M")`, `F("F")`

### Classe `Personnel`
    - `private int idPersonnel;`
    - `private Integer idService;`
    - `private Integer idFonction;`
    - `private Integer idVehicule;` // Attribut présent dans la table PERSONNEL, conserver.
    - `private String matricule;`
    - `private String nomPersonnel;`
    - `private String prenomPersonnel;`
    - `private String email;`
    - `private String telephone;`
    - `private String adresse;`
    - `private java.time.LocalDate dateNaissance;`
    - `private SexePersonnel sexe;`
    - `private java.time.LocalDateTime dateAttribution;`

### Classe `ServiceEntreprise` (pour la table `SERVICE`)
    - `private int idService;`
    - `private String libService;`
    - `private String localisationService;`

### Classe `SocietaireCompte`
    - `private int idSocietaire;`
    - `private Integer idPersonnel;`
    - `private String nom;`
    - `private String numero;`
    - `private java.math.BigDecimal solde;`
    - `private String email;`
    - `private String telephone;`

### Enum `RoleUtilisateur`
    `U1("U1")`, `U2("U2")`, `U3("U3")`, `U4("U4")`

### Classe `Utilisateur`
    - `private int id;`
    - `private String login;`
    - `private String hashMdp;` // Renommé pour clarté (correspond à `hash` dans la BDD)
    - `private RoleUtilisateur role;`
    - `private Integer idPersonnel;`
    - `private String mfaSecret;`

### Enum `EnergieVehicule` (pour `energie` dans `VEHICULES`)
    `DIESEL("Diesel")`, `ESSENCE("Essence")`, `ELECTRIQUE("Électrique")`, `HYBRIDE("Hybride")`

### Classe `Vehicule`
    - `private int idVehicule;`
    - `private int idEtatVoiture;`
    - `private EnergieVehicule energie;`
    - `private String numeroChassi;`
    - `private String immatriculation;`
    - `private String marque;`
    - `private String modele;`
    - `private Integer nbPlaces;`
    - `private java.time.LocalDateTime dateAcquisition;`
    - `private java.time.LocalDateTime dateAmmortissement;`
    - `private java.time.LocalDateTime dateMiseEnService;`
    - `private Integer puissance;`
    - `private String couleur;`
    - `private java.math.BigDecimal prixVehicule;`
    - `private Integer kmActuels;`
    - `private java.time.LocalDateTime dateEtat;`

---

## Fichier `AppExceptions.java`

**Emplacement :** `src/main/java/com/miage/parcauto/AppExceptions.java`
Définir les classes d'exceptions personnalisées.
- `public class ErreurBaseDeDonnees extends RuntimeException { ... }`
- `public class ErreurLogiqueMetier extends RuntimeException { ... }`
- `public class ErreurAuthentification extends RuntimeException { ... }`
- `public class ErreurAutorisation extends RuntimeException { ... }`
- `public class ErreurValidation extends RuntimeException { ... }`
  Chaque exception doit avoir des constructeurs : un constructeur par défaut, un constructeur prenant un `String message`, et un constructeur prenant `String message, Throwable cause`.

---

## Fichier `PersistenceService.java`

**Emplacement :** `src/main/java/com/miage/parcauto/PersistenceService.java`
Cette classe gérera toutes les opérations CRUD pour chaque entité. Utiliser `dbUtil.java` fourni pour la gestion des connexions et la fermeture des ressources (`try-with-resources` fortement recommandé). Mapper les `ResultSet` aux objets de `AppModels.java`.

**Pattern général pour chaque entité (ex: `Vehicule`) :**
- **`public Vehicule trouverVehiculeParId(int idVehicule)`**
  - `String sql = "SELECT * FROM VEHICULES WHERE id_vehicule = ?";`
  - Utiliser `dbUtil.getConnection()`, `PreparedStatement`.
  - Mapper le `ResultSet` à un objet `Vehicule`. Pour les champs `ENUM` de la BDD, utiliser la méthode `fromDbValue()` de l'enum Java correspondant. Pour les `DATETIME`, `rs.getTimestamp("nom_colonne").toLocalDateTime()`. Pour `DATE`, `rs.getDate("nom_colonne").toLocalDate()`.
  - Gérer le cas où aucun enregistrement n'est trouvé (retourner `null`).
  - Lever `ErreurBaseDeDonnees` en cas de `SQLException`.
- **`public java.util.List<Vehicule> trouverTousLesVehicules()`**
- **`public Vehicule sauvegarderVehicule(Vehicule vehicule)`**
  - Si `vehicule.getIdVehicule() == 0` (ou une valeur sentinelle pour un nouvel objet), exécuter un `INSERT`. Récupérer la clé générée (`Statement.RETURN_GENERATED_KEYS` et `getGeneratedKeys()`) et la mettre à jour dans l'objet `vehicule` retourné.
  - Sinon, exécuter un `UPDATE`.
  - Pour les champs `ENUM` Java, utiliser `monEnum.getDbValue()` pour `setString()` dans le `PreparedStatement`.
  - Retourner l'objet `Vehicule` sauvegardé/mis à jour.
- **`public void supprimerVehiculeParId(int idVehicule)`**

**Appliquer ce pattern CRUD (et méthodes de recherche spécifiques si besoin) pour TOUTES les entités :**
`Affectation`, `Assurance`, `Couvrir`, `DepenseMission`, `DocumentSocietaire`, `Entretien`, `EtatVoiture`, `FonctionPersonnel`, `Mission`, `Mouvement`, `Personnel`, `ServiceEntreprise`, `SocietaireCompte`, `Utilisateur`.

**Méthodes spécifiques pour `COUVRIR` :**
- `public void ajouterCouvrir(Couvrir couvrir)`
- `public void supprimerCouvrir(int idVehicule, int numCarteAssurance)`
- `public java.util.List<Assurance> trouverAssurancesPourVehicule(int idVehicule)`
- `public java.util.List<Vehicule> trouverVehiculesPourAssurance(int numCarteAssurance)`

---

## Fichier `RepositoryList.java` (Optionnel/Façade)

**Emplacement :** `src/main/java/com/miage/parcauto/RepositoryList.java`
Cette classe peut servir de point d'accès centralisé au `PersistenceService`.
- `private final PersistenceService persistenceService;`
- Constructeur : `public RepositoryList(PersistenceService persistenceService) { this.persistenceService = persistenceService; }`
- Méthodes qui délèguent simplement les appels au `persistenceService` (ex: `public Vehicule trouverVehiculeParId(int id) { return persistenceService.trouverVehiculeParId(id); }`).
  Si `PersistenceService` est bien structuré, `RepositoryList` peut ne pas être strictement nécessaire et les services métier peuvent directement utiliser `PersistenceService`. Créer ce fichier s'il aide à l'organisation.

---

## Fichier `BusinessLogicService.java`

**Emplacement :** `src/main/java/com/miage/parcauto/BusinessLogicService.java`
Contient la logique métier, orchestre les appels à `PersistenceService`.
- `private final PersistenceService persistenceService;` (Injecté via constructeur)
- **Gestion des Véhicules :**
  - `public Vehicule creerNouveauVehicule(Vehicule vehicule)` : Valider données (ex: unicité `numeroChassi`, `immatriculation`), puis `persistenceService.sauvegarderVehicule(vehicule)`. Lever `ErreurValidation` si échec.
  - `public Vehicule modifierVehicule(Vehicule vehicule)` : Valider, puis `persistenceService.sauvegarderVehicule(vehicule)`.
  - `public void supprimerVehicule(int idVehicule)` : Vérifier contraintes (pas de missions actives, pas d'affectations actives non terminées), puis `persistenceService.supprimerVehiculeParId(idVehicule)`. Lever `ErreurLogiqueMetier` si contraintes non respectées.
  - `public void changerEtatVehicule(int idVehicule, int idNouvelEtatVoiture, java.time.LocalDateTime dateChangement)` : Charger véhicule, mettre à jour `idEtatVoiture` et `dateEtat`, puis sauvegarder.
- **Gestion des Missions :**
  - `public Mission planifierNouvelleMission(Mission mission)` : Valider données (ex: véhicule disponible), mettre statut du véhicule à "En mission" (`idEtatVoiture` correspondant), sauvegarder mission.
  - `public void demarrerUneMission(int idMission)` : Changer `mission.setStatus(StatutMission.EN_COURS)`, sauvegarder.
  - `public void cloturerUneMission(int idMission, Integer kmReel, java.math.BigDecimal coutTotalCalcule, java.util.List<DepenseMission> depenses)` : Mettre à jour `mission` (statut `CLOTUREE`, `kmReel`, `coutTotal`), sauvegarder `mission`. Sauvegarder les `depenses` associées. Mettre à jour le statut du véhicule associé à "Disponible" (ou son état précédent avant la mission). Le trigger `trg_mission_cloturee` met déjà à jour `km_actuels` du véhicule.
- **Gestion Financière :**
  - `public SocietaireCompte effectuerDepotSurCompteSocietaire(int idSocietaire, java.math.BigDecimal montant)` : Valider montant > 0. Charger `SocietaireCompte`. Mettre à jour `solde`. Créer et sauvegarder un `Mouvement` de type `DEPOT`. Retourner le `SocietaireCompte` mis à jour.
  - `public SocietaireCompte effectuerRetraitDeCompteSocietaire(int idSocietaire, java.math.BigDecimal montant)` : Valider montant > 0. Charger `SocietaireCompte`. Vérifier `solde` suffisant. Mettre à jour `solde`. Créer et sauvegarder `Mouvement` de type `RETRAIT`. Retourner compte mis à jour. Lever `ErreurLogiqueMetier` si solde insuffisant.
- **Implémenter toutes les fonctionnalités** décrites dans la section "Fonctionnalités principales" du dossier technique OCR, en utilisant les entités et le `PersistenceService`.

---

## Fichier `SecurityManager.java`

**Emplacement :** `src/main/java/com/miage/parcauto/SecurityManager.java`
Gère authentification et autorisation.
- `private final PersistenceService persistenceService;` (Injecté via constructeur)
- **Méthode `public Utilisateur authentifierUtilisateur(String login, String motDePasseFourni)`**
  - `Utilisateur utilisateur = persistenceService.trouverUtilisateurParLogin(login);`
  - Si `utilisateur != null` et `verifierMotDePasse(motDePasseFourni, utilisateur.getHashMdp())` est vrai, retourner `utilisateur`.
  - Sinon, lever `ErreurAuthentification("Identifiants incorrects.")`.
- **Méthode `private boolean verifierMotDePasse(String motDePasseClair, String hashStocke)`**
  - Implémenter la vérification de hash. Pas de bibliothèque
- **Méthode `public String genererHashMotDePasse(String motDePasseClair)`**
  - `return BCrypt.hashpw(motDePasseClair, BCrypt.gensalt());` (Pour la création de nouveaux utilisateurs ou la modification de MDP).
- **Méthode `public boolean estAutorise(RoleUtilisateur role, String actionOuModuleCible)`**
  - Implémenter la logique de vérification des droits basée sur le `role` et une représentation de `l'actionOuModuleCible`. Utiliser la "Matrice des responsabilités et accès par profil utilisateur" fournie précédemment.

---

## Fichier `SessionManager.java`

**Emplacement :** `src/main/java/com/miage/parcauto/SessionManager.java`
Gère la session de l'utilisateur connecté (Singleton ou classe statique).
- `private static Utilisateur utilisateurActuel;`
- `public static void definirUtilisateurActuel(Utilisateur utilisateur) { utilisateurActuel = utilisateur; }`
- `public static Utilisateur obtenirUtilisateurActuel() { return utilisateurActuel; }`
- `public static void deconnecterUtilisateur() { utilisateurActuel = null; }`
- `public static boolean estUtilisateurConnecte() { return utilisateurActuel != null; }`
- `public static RoleUtilisateur obtenirRoleUtilisateurActuel() { return estUtilisateurConnecte() ? utilisateurActuel.getRole() : null; }`

---

## Fichier `ViewController.java`

**Emplacement :** `src/main/java/com/miage/parcauto/ViewController.java`
Contrôleur JavaFX principal qui sera lié aux vues FXML. Il orchestrera l'affichage et les interactions utilisateur.
- **Services injectés :**
  - `private BusinessLogicService businessLogicService;`
  - `private SecurityManager securityManager;`
  - (Initialiser via un constructeur ou une méthode d'initialisation appelée par `MainApp`).
- **Références `@FXML` :** Déclarer les composants UI de chaque vue FXML qui nécessitent une interaction (ex: `TextField`, `Button`, `TableView`, `ChoiceBox`).
- **Méthode `public void initialiser()` (ou appelée de manière similaire par JavaFX après chargement FXML) :**
  - Initialiser les services si ce n'est pas fait via constructeur.
  - Charger les données initiales pour la vue par défaut (ex: Dashboard si l'utilisateur est déjà connecté, sinon vue Login).
  - Configurer les `TableView` (colonnes, cell value factories).
  - Peupler les `ChoiceBox` / `ComboBox` avec les valeurs des enums ou des données de la BDD (ex: listes d'états de voiture).
- **Vue Login (`LoginView.fxml`) :**
  - `@FXML private javafx.scene.control.TextField champLogin;`
  - `@FXML private javafx.scene.control.PasswordField champMotDePasse;`
  - `@FXML public void actionSeConnecter()` :
    - Récupérer `login` et `motDePasse`.
    - `try { Utilisateur u = securityManager.authentifierUtilisateur(login, motDePasse); SessionManager.definirUtilisateurActuel(u); chargerVuePrincipale(); } catch (ErreurAuthentification e) { afficherAlerteErreur(e.getMessage()); }`
- **Navigation :**
  - Méthodes pour charger dynamiquement les FXML dans la zone de contenu principale de l'application (ex: `chargerContenu("VehiculePanelView.fxml")`).
  - Utiliser `javafx.fxml.FXMLLoader`.
- **Pour chaque module (Gestion des Véhicules, Missions, etc., correspondant aux `XYZPanelView.fxml`) :**
  - Créer des méthodes `@FXML` pour les actions des boutons (`actionAjouterVehicule`, `actionModifierVehiculeSelectionne`, `actionSupprimerVehiculeSelectionne`).
  - **Peuplement des `TableView` :**
    - `javafx.collections.ObservableList<TypeModele> observableListData = FXCollections.observableArrayList();`
    - `observableListData.addAll(businessLogicService.trouverTousLesXYZ());`
    - `maTableView.setItems(observableListData);`
  - **Dialogues pour formulaires :** Utiliser `javafx.scene.control.Dialog` et `DialogPane` pour les formulaires d'ajout/modification. Charger un FXML dédié pour le formulaire dans le dialogue.
  - **Validation des saisies :** Avant d'appeler `businessLogicService`, valider les champs du formulaire (non vides, format correct pour nombres/dates). Afficher des `Alert` d'erreur.
  - **Gestion des droits :** Activer/désactiver ou masquer/afficher des boutons/fonctionnalités en fonction de `SessionManager.obtenirRoleUtilisateurActuel()` et `securityManager.estAutorise(...)`.
- **Affichage des alertes :** Méthode utilitaire `private void afficherAlerteInformation(String message)`, `private void afficherAlerteErreur(String message)`, `private boolean afficherAlerteConfirmation(String message)`.

---

## Fichier `MainApp.java`

**Emplacement :** `src/main/java/com/miage/parcauto/MainApp.java`
Point d'entrée de l'application JavaFX.
- `public class MainApp extends javafx.application.Application`
- **Méthode `public void start(javafx.stage.Stage primaryStage)` :**
  - Initialiser les services (créer instances de `PersistenceService`, `BusinessLogicService`, `SecurityManager`). Il faudra peut-être un mécanisme simple d'injection ou de passage de ces instances aux contrôleurs FXML. Une solution simple est de les rendre accessibles statiquement (via des singletons) ou de les passer lors du chargement des FXML si les contrôleurs ont des méthodes d'initialisation acceptant ces services.
  - Configurer `primaryStage` (titre, icône, taille minimale).
  - Charger la vue de login initiale (`LoginView.fxml`).
  - `primaryStage.show();`
- **Méthode `public static void main(String[] args) { launch(args); }`**
- **Gestion de la navigation principale après login :** Après un login réussi, `ViewController` (ou une méthode dans `MainApp` appelée par `ViewController`) devrait remplacer la scène de `primaryStage` par la vue principale de l'application (ex: `MainDashboardView.fxml`).

---

## Fichiers FXML et CSS

**Emplacement :** `src/main/resources/com/miage/parcauto/fxml/` et `src/main/resources/com/miage/parcauto/css/`
Les instructions ici se concentrent sur la logique Java. Les fichiers FXML doivent être créés avec les `fx:id` correspondants aux champs et méthodes `@FXML` déclarés dans `ViewController.java` (ou les contrôleurs spécifiques si l'application est décomposée en plusieurs contrôleurs FXML). Les fichiers CSS seront utilisés pour styliser l'application.

**Considérations pour FXML dans `ViewController.java`:**
- Pour chaque FXML (ex: `VehiculePanelView.fxml`), les `TableView`, `TextField`, `ChoiceBox`, `Button` etc. doivent avoir des `fx:id` uniques.
- Les méthodes `onAction` des `Button` doivent correspondre aux méthodes `@FXML` du contrôleur.
- Les `TableView` nécessitent la configuration de `TableColumn` et de leurs `cellValueFactory` (typiquement `new PropertyValueFactory<>("nomAttributModele")`).