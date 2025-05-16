package main.java.com.miage.parcauto;

import main.java.com.miage.parcauto.AppModels.*;
import main.java.com.miage.parcauto.AppExceptions.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PersistenceService {
    private static final Logger LOGGER_PERSISTANCE = Logger.getLogger(PersistenceService.class.getName());
    private Affectation mapResultSetToAffectation(ResultSet rs) throws SQLException {
        Affectation affectation = new Affectation();
        affectation.setId(rs.getInt("id"));
        affectation.setIdVehicule(rs.getInt("id_vehicule"));
        affectation.setIdPersonnel(rs.getObject("id_personnel", Integer.class));
        affectation.setIdSocietaire(rs.getObject("id_societaire", Integer.class));
        affectation.setType(TypeAffectation.fromDbValue(rs.getString("type")));

        Timestamp dateDebutTs = rs.getTimestamp("date_debut");
        if (dateDebutTs != null) {
            affectation.setDateDebut(dateDebutTs.toLocalDateTime());
        }
        Timestamp dateFinTs = rs.getTimestamp("date_fin");
        if (dateFinTs != null) {
            affectation.setDateFin(dateFinTs.toLocalDateTime());
        }
        return affectation;
    }

    public Affectation trouverAffectationParId(int idAffectation) {
        final String sql = "SELECT * FROM AFFECTATION WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idAffectation);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAffectation(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'affectation par ID: " + idAffectation, e);
        }
        return null;
    }

    public List<Affectation> trouverToutesLesAffectations() {
        List<Affectation> affectations = new ArrayList<>();
        final String sql = "SELECT * FROM AFFECTATION";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                affectations.add(mapResultSetToAffectation(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de toutes les affectations.", e);
        }
        return affectations;
    }

    public List<Affectation> trouverAffectationsActivesPourVehicule(int idVehicule) {
        List<Affectation> affectations = new ArrayList<>();
        final String sql = "SELECT * FROM AFFECTATION WHERE id_vehicule = ? AND (date_fin IS NULL OR date_fin > ?)";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    affectations.add(mapResultSetToAffectation(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des affectations actives pour le véhicule ID: " + idVehicule, e);
        }
        return affectations;
    }


    public Affectation sauvegarderAffectation(Affectation affectation) {
        Objects.requireNonNull(affectation, "L'objet Affectation ne peut être nul.");
        final boolean isInsert = affectation.getId() == 0;
        final String sql = isInsert ?
                "INSERT INTO AFFECTATION (id_vehicule, id_personnel, id_societaire, type, date_debut, date_fin) VALUES (?, ?, ?, ?, ?, ?)" :
                "UPDATE AFFECTATION SET id_vehicule = ?, id_personnel = ?, id_societaire = ?, type = ?, date_debut = ?, date_fin = ? WHERE id = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, affectation.getIdVehicule());
            pstmt.setObject(2, affectation.getIdPersonnel());
            pstmt.setObject(3, affectation.getIdSocietaire());
            pstmt.setString(4, affectation.getType().getDbValue());
            pstmt.setTimestamp(5, Timestamp.valueOf(affectation.getDateDebut()));
            if (affectation.getDateFin() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(affectation.getDateFin()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }

            if (!isInsert) {
                pstmt.setInt(7, affectation.getId());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de l'affectation a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        affectation.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de l'affectation a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return affectation;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de l'affectation.", e);
        }
    }

    public void supprimerAffectationParId(int idAffectation) {
        final String sql = "DELETE FROM AFFECTATION WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idAffectation);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucune affectation supprimée pour l'ID: " + idAffectation + " (elle n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de l'affectation par ID: " + idAffectation, e);
        }
    }

    public void supprimerToutesAffectationsPourVehicule(int idVehicule) {
        final String sql = "DELETE FROM AFFECTATION WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression des affectations pour le véhicule ID: " + idVehicule, e);
        }
    }


    private Assurance mapResultSetToAssurance(ResultSet rs) throws SQLException {
        Assurance assurance = new Assurance();
        assurance.setNumCarteAssurance(rs.getInt("num_carte_assurance"));
        Timestamp dateDebutTs = rs.getTimestamp("date_debut_assurance");
        if (dateDebutTs != null) {
            assurance.setDateDebutAssurance(dateDebutTs.toLocalDateTime());
        }
        Timestamp dateFinTs = rs.getTimestamp("date_fin_assurance");
        if (dateFinTs != null) {
            assurance.setDateFinAssurance(dateFinTs.toLocalDateTime());
        }
        assurance.setAgence(rs.getString("agence"));
        assurance.setCoutAssurance(rs.getBigDecimal("cout_assurance"));
        return assurance;
    }

    public Assurance trouverAssuranceParNumCarte(int numCarteAssurance) {
        final String sql = "SELECT * FROM ASSURANCE WHERE num_carte_assurance = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, numCarteAssurance);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAssurance(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'assurance par numéro: " + numCarteAssurance, e);
        }
        return null;
    }

    public List<Assurance> trouverToutesLesAssurances() {
        List<Assurance> assurances = new ArrayList<>();
        final String sql = "SELECT * FROM ASSURANCE";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                assurances.add(mapResultSetToAssurance(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de toutes les assurances.", e);
        }
        return assurances;
    }

    public Assurance sauvegarderAssurance(Assurance assurance) {
        Objects.requireNonNull(assurance, "L'objet Assurance ne peut être nul.");
        final boolean isInsert = assurance.getNumCarteAssurance() == 0;
        final String sql = isInsert ?
                "INSERT INTO ASSURANCE (date_debut_assurance, date_fin_assurance, agence, cout_assurance) VALUES (?, ?, ?, ?)" :
                "UPDATE ASSURANCE SET date_debut_assurance = ?, date_fin_assurance = ?, agence = ?, cout_assurance = ? WHERE num_carte_assurance = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (assurance.getDateDebutAssurance() != null) {
                pstmt.setTimestamp(1, Timestamp.valueOf(assurance.getDateDebutAssurance()));
            } else {
                pstmt.setNull(1, Types.TIMESTAMP);
            }
            if (assurance.getDateFinAssurance() != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(assurance.getDateFinAssurance()));
            } else {
                pstmt.setNull(2, Types.TIMESTAMP);
            }
            pstmt.setString(3, assurance.getAgence());
            pstmt.setBigDecimal(4, assurance.getCoutAssurance());

            if (!isInsert) {
                pstmt.setInt(5, assurance.getNumCarteAssurance());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de l'assurance a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        assurance.setNumCarteAssurance(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de l'assurance a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return assurance;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de l'assurance.", e);
        }
    }

    public void supprimerAssuranceParNumCarte(int numCarteAssurance) {
        final String sqlCouvrir = "DELETE FROM COUVRIR WHERE num_carte_assurance = ?";
        final String sqlAssurance = "DELETE FROM ASSURANCE WHERE num_carte_assurance = ?";
        Connection conn = null;
        try {
            conn = dbUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtCouvrir = conn.prepareStatement(sqlCouvrir)) {
                pstmtCouvrir.setInt(1, numCarteAssurance);
                pstmtCouvrir.executeUpdate();
            }

            try (PreparedStatement pstmtAssurance = conn.prepareStatement(sqlAssurance)) {
                pstmtAssurance.setInt(1, numCarteAssurance);
                int affectedRows = pstmtAssurance.executeUpdate();
                if (affectedRows == 0) {
                    System.out.println("Aucune assurance supprimée pour le numéro: " + numCarteAssurance + " (elle n'existait peut-être pas).");
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ErreurBaseDeDonnees("Erreur lors du rollback de la suppression de l'assurance: " + numCarteAssurance, ex);
                }
            }
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de l'assurance par numéro: " + numCarteAssurance, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    // Ignorer ou logger
                }
            }
        }
    }

    public void ajouterCouvrir(Couvrir couvrir) {
        Objects.requireNonNull(couvrir, "L'objet Couvrir ne peut être nul.");
        final String sql = "INSERT INTO COUVRIR (id_vehicule, num_carte_assurance) VALUES (?, ?)";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, couvrir.getIdVehicule());
            pstmt.setInt(2, couvrir.getNumCarteAssurance());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de l'ajout de la liaison Couvrir.", e);
        }
    }

    public void supprimerCouvrir(int idVehicule, int numCarteAssurance) {
        final String sql = "DELETE FROM COUVRIR WHERE id_vehicule = ? AND num_carte_assurance = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            pstmt.setInt(2, numCarteAssurance);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de la liaison Couvrir.", e);
        }
    }

    public void supprimerToutesCouverturesPourVehicule(int idVehicule) {
        final String sql = "DELETE FROM COUVRIR WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression des couvertures pour le véhicule ID: " + idVehicule, e);
        }
    }


    public List<Assurance> trouverAssurancesPourVehicule(int idVehicule) {
        List<Assurance> assurances = new ArrayList<>();
        final String sql = "SELECT a.* FROM ASSURANCE a INNER JOIN COUVRIR c ON a.num_carte_assurance = c.num_carte_assurance WHERE c.id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assurances.add(mapResultSetToAssurance(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des assurances pour le véhicule ID: " + idVehicule, e);
        }
        return assurances;
    }

    public List<Vehicule> trouverVehiculesPourAssurance(int numCarteAssurance) {
        List<Vehicule> vehicules = new ArrayList<>();
        final String sql = "SELECT v.* FROM VEHICULES v INNER JOIN COUVRIR c ON v.id_vehicule = c.id_vehicule WHERE c.num_carte_assurance = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, numCarteAssurance);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    vehicules.add(mapResultSetToVehicule(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des véhicules pour l'assurance numéro: " + numCarteAssurance, e);
        }
        return vehicules;
    }

    private DepenseMission mapResultSetToDepenseMission(ResultSet rs) throws SQLException {
        DepenseMission depense = new DepenseMission();
        depense.setId(rs.getInt("id"));
        depense.setIdMission(rs.getInt("id_mission"));
        depense.setNature(NatureDepense.fromDbValue(rs.getString("nature")));
        depense.setMontant(rs.getBigDecimal("montant"));
        depense.setJustificatif(rs.getString("justificatif"));
        return depense;
    }

    public DepenseMission trouverDepenseMissionParId(int idDepense) {
        final String sql = "SELECT * FROM DEPENSE_MISSION WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idDepense);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDepenseMission(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de la dépense de mission par ID: " + idDepense, e);
        }
        return null;
    }

    public List<DepenseMission> trouverToutesLesDepensesMission() {
        List<DepenseMission> depenses = new ArrayList<>();
        final String sql = "SELECT * FROM DEPENSE_MISSION";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                depenses.add(mapResultSetToDepenseMission(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de toutes les dépenses de mission.", e);
        }
        return depenses;
    }

    public List<DepenseMission> trouverDepensesParMissionId(int idMission) {
        List<DepenseMission> depenses = new ArrayList<>();
        final String sql = "SELECT * FROM DEPENSE_MISSION WHERE id_mission = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idMission);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    depenses.add(mapResultSetToDepenseMission(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des dépenses pour la mission ID: " + idMission, e);
        }
        return depenses;
    }


    public DepenseMission sauvegarderDepenseMission(DepenseMission depenseMission) {
        Objects.requireNonNull(depenseMission, "L'objet DepenseMission ne peut être nul.");
        final boolean isInsert = depenseMission.getId() == 0;
        final String sql = isInsert ?
                "INSERT INTO DEPENSE_MISSION (id_mission, nature, montant, justificatif) VALUES (?, ?, ?, ?)" :
                "UPDATE DEPENSE_MISSION SET id_mission = ?, nature = ?, montant = ?, justificatif = ? WHERE id = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, depenseMission.getIdMission());
            pstmt.setString(2, depenseMission.getNature().getDbValue());
            pstmt.setBigDecimal(3, depenseMission.getMontant());
            pstmt.setString(4, depenseMission.getJustificatif());
            if (!isInsert) {
                pstmt.setInt(5, depenseMission.getId());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de la dépense de mission a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        depenseMission.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de la dépense de mission a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return depenseMission;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de la dépense de mission.", e);
        }
    }

    public void supprimerDepenseMissionParId(int idDepense) {
        final String sql = "DELETE FROM DEPENSE_MISSION WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idDepense);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucune dépense de mission supprimée pour l'ID: " + idDepense + " (elle n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de la dépense de mission par ID: " + idDepense, e);
        }
    }

    private DocumentSocietaire mapResultSetToDocumentSocietaire(ResultSet rs) throws SQLException {
        DocumentSocietaire doc = new DocumentSocietaire();
        doc.setIdDoc(rs.getInt("id_doc"));
        doc.setIdSocietaire(rs.getInt("id_societaire"));
        doc.setTypeDoc(TypeDocumentSocietaire.fromDbValue(rs.getString("type_doc")));
        doc.setCheminFichier(rs.getString("chemin_fichier"));
        Timestamp dateUploadTs = rs.getTimestamp("date_upload");
        if (dateUploadTs != null) {
            doc.setDateUpload(dateUploadTs.toLocalDateTime());
        }
        return doc;
    }

    public DocumentSocietaire trouverDocumentSocietaireParId(int idDoc) {
        final String sql = "SELECT * FROM DOCUMENT_SOCIETAIRE WHERE id_doc = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idDoc);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDocumentSocietaire(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du document sociétaire par ID: " + idDoc, e);
        }
        return null;
    }

    public List<DocumentSocietaire> trouverTousLesDocumentsSocietaire() {
        List<DocumentSocietaire> documents = new ArrayList<>();
        final String sql = "SELECT * FROM DOCUMENT_SOCIETAIRE";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                documents.add(mapResultSetToDocumentSocietaire(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les documents sociétaire.", e);
        }
        return documents;
    }

    public List<DocumentSocietaire> trouverDocumentsParSocietaireId(int idSocietaire) {
        List<DocumentSocietaire> documents = new ArrayList<>();
        final String sql = "SELECT * FROM DOCUMENT_SOCIETAIRE WHERE id_societaire = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idSocietaire);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapResultSetToDocumentSocietaire(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des documents pour le sociétaire ID: " + idSocietaire, e);
        }
        return documents;
    }


    public DocumentSocietaire sauvegarderDocumentSocietaire(DocumentSocietaire document) {
        Objects.requireNonNull(document, "L'objet DocumentSocietaire ne peut être nul.");
        final boolean isInsert = document.getIdDoc() == 0;
        final String sql = isInsert ?
                "INSERT INTO DOCUMENT_SOCIETAIRE (id_societaire, type_doc, chemin_fichier, date_upload) VALUES (?, ?, ?, ?)" :
                "UPDATE DOCUMENT_SOCIETAIRE SET id_societaire = ?, type_doc = ?, chemin_fichier = ?, date_upload = ? WHERE id_doc = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, document.getIdSocietaire());
            pstmt.setString(2, document.getTypeDoc().getDbValue());
            pstmt.setString(3, document.getCheminFichier());
            pstmt.setTimestamp(4, Timestamp.valueOf(document.getDateUpload()));
            if (!isInsert) {
                pstmt.setInt(5, document.getIdDoc());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde du document sociétaire a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        document.setIdDoc(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde du document sociétaire a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return document;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde du document sociétaire.", e);
        }
    }

    public void supprimerDocumentSocietaireParId(int idDoc) {
        final String sql = "DELETE FROM DOCUMENT_SOCIETAIRE WHERE id_doc = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idDoc);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun document sociétaire supprimé pour l'ID: " + idDoc + " (il n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression du document sociétaire par ID: " + idDoc, e);
        }
    }

    private Entretien mapResultSetToEntretien(ResultSet rs) throws SQLException {
        Entretien entretien = new Entretien();
        entretien.setIdEntretien(rs.getInt("id_entretien"));
        entretien.setIdVehicule(rs.getInt("id_vehicule"));
        Timestamp dateEntreeTs = rs.getTimestamp("date_entree_entr");
        if (dateEntreeTs != null) entretien.setDateEntreeEntr(dateEntreeTs.toLocalDateTime());
        Timestamp dateSortieTs = rs.getTimestamp("date_sortie_entr");
        if (dateSortieTs != null) entretien.setDateSortieEntr(dateSortieTs.toLocalDateTime());
        entretien.setMotifEntr(rs.getString("motif_entr"));
        entretien.setObservation(rs.getString("observation"));
        entretien.setCoutEntr(rs.getBigDecimal("cout_entr"));
        entretien.setLieuEntr(rs.getString("lieu_entr"));
        String typeEntrStr = rs.getString("type");
        if (typeEntrStr != null) entretien.setType(TypeEntretien.fromDbValue(typeEntrStr));
        String statutOtStr = rs.getString("statut_ot");
        if (statutOtStr != null) entretien.setStatutOt(StatutOrdreTravail.fromDbValue(statutOtStr));
        return entretien;
    }

    public Entretien trouverEntretienParId(int idEntretien) {
        final String sql = "SELECT * FROM ENTRETIEN WHERE id_entretien = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEntretien);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntretien(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'entretien par ID: " + idEntretien, e);
        }
        return null;
    }

    public List<Entretien> trouverTousLesEntretiens() {
        List<Entretien> entretiens = new ArrayList<>();
        final String sql = "SELECT * FROM ENTRETIEN";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                entretiens.add(mapResultSetToEntretien(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les entretiens.", e);
        }
        return entretiens;
    }

    public List<Entretien> trouverEntretiensPourVehicule(int idVehicule) {
        List<Entretien> entretiens = new ArrayList<>();
        final String sql = "SELECT * FROM ENTRETIEN WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entretiens.add(mapResultSetToEntretien(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération des entretiens pour le véhicule ID: " + idVehicule, e);
        }
        return entretiens;
    }


    public Entretien sauvegarderEntretien(Entretien entretien) {
        Objects.requireNonNull(entretien, "L'objet Entretien ne peut être nul.");
        final boolean isInsert = entretien.getIdEntretien() == 0;
        final String sql = isInsert ?
                "INSERT INTO ENTRETIEN (id_vehicule, date_entree_entr, date_sortie_entr, motif_entr, observation, cout_entr, lieu_entr, type, statut_ot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                "UPDATE ENTRETIEN SET id_vehicule = ?, date_entree_entr = ?, date_sortie_entr = ?, motif_entr = ?, observation = ?, cout_entr = ?, lieu_entr = ?, type = ?, statut_ot = ? WHERE id_entretien = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, entretien.getIdVehicule());
            pstmt.setTimestamp(2, entretien.getDateEntreeEntr() != null ? Timestamp.valueOf(entretien.getDateEntreeEntr()) : null);
            pstmt.setTimestamp(3, entretien.getDateSortieEntr() != null ? Timestamp.valueOf(entretien.getDateSortieEntr()) : null);
            pstmt.setString(4, entretien.getMotifEntr());
            pstmt.setString(5, entretien.getObservation());
            pstmt.setBigDecimal(6, entretien.getCoutEntr());
            pstmt.setString(7, entretien.getLieuEntr());
            pstmt.setString(8, entretien.getType() != null ? entretien.getType().getDbValue() : null);
            pstmt.setString(9, entretien.getStatutOt() != null ? entretien.getStatutOt().getDbValue() : null);
            if (!isInsert) {
                pstmt.setInt(10, entretien.getIdEntretien());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de l'entretien a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entretien.setIdEntretien(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de l'entretien a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return entretien;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de l'entretien.", e);
        }
    }

    public void supprimerEntretienParId(int idEntretien) {
        final String sql = "DELETE FROM ENTRETIEN WHERE id_entretien = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEntretien);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun entretien supprimé pour l'ID: " + idEntretien + " (il n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de l'entretien par ID: " + idEntretien, e);
        }
    }

    public void supprimerTousEntretiensPourVehicule(int idVehicule) {
        final String sql = "DELETE FROM ENTRETIEN WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression des entretiens pour le véhicule ID: " + idVehicule, e);
        }
    }


    private EtatVoiture mapResultSetToEtatVoiture(ResultSet rs) throws SQLException {
        EtatVoiture etat = new EtatVoiture();
        etat.setIdEtatVoiture(rs.getInt("id_etat_voiture"));
        etat.setLibEtatVoiture(rs.getString("lib_etat_voiture"));
        return etat;
    }

    public EtatVoiture trouverEtatVoitureParId(int idEtatVoiture) {
        final String sql = "SELECT * FROM ETAT_VOITURE WHERE id_etat_voiture = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEtatVoiture);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEtatVoiture(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'état de voiture par ID: " + idEtatVoiture, e);
        }
        return null;
    }

    public EtatVoiture trouverEtatVoitureParLibelle(String libelle) {
        Objects.requireNonNull(libelle, "Le libellé de l'état voiture ne peut être nul.");
        final String sql = "SELECT * FROM ETAT_VOITURE WHERE lib_etat_voiture = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, libelle);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEtatVoiture(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'état de voiture par libellé: " + libelle, e);
        }
        return null;
    }

    public List<EtatVoiture> trouverTousLesEtatsVoiture() {
        List<EtatVoiture> etats = new ArrayList<>();
        final String sql = "SELECT * FROM ETAT_VOITURE";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                etats.add(mapResultSetToEtatVoiture(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les états de voiture.", e);
        }
        return etats;
    }

    public EtatVoiture sauvegarderEtatVoiture(EtatVoiture etatVoiture) {
        Objects.requireNonNull(etatVoiture, "L'objet EtatVoiture ne peut être nul.");
        final boolean isInsert = etatVoiture.getIdEtatVoiture() == 0;
        final String sql = isInsert ?
                "INSERT INTO ETAT_VOITURE (lib_etat_voiture) VALUES (?)" :
                "UPDATE ETAT_VOITURE SET lib_etat_voiture = ? WHERE id_etat_voiture = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, etatVoiture.getLibEtatVoiture());
            if (!isInsert) {
                pstmt.setInt(2, etatVoiture.getIdEtatVoiture());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de l'état de voiture a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        etatVoiture.setIdEtatVoiture(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de l'état de voiture a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return etatVoiture;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de l'état de voiture.", e);
        }
    }

    public void supprimerEtatVoitureParId(int idEtatVoiture) {
        final String sql = "DELETE FROM ETAT_VOITURE WHERE id_etat_voiture = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEtatVoiture);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun état de voiture supprimé pour l'ID: " + idEtatVoiture + " (il n'existait peut-être pas ou est utilisé).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de l'état de voiture par ID: " + idEtatVoiture + ". Vérifiez s'il est utilisé par des véhicules.", e);
        }
    }

    private FonctionPersonnel mapResultSetToFonctionPersonnel(ResultSet rs) throws SQLException {
        FonctionPersonnel fonction = new FonctionPersonnel();
        fonction.setIdFonction(rs.getInt("id_fonction"));
        fonction.setLibFonction(rs.getString("lib_fonction"));
        return fonction;
    }

    public FonctionPersonnel trouverFonctionPersonnelParId(int idFonction) {
        final String sql = "SELECT * FROM FONCTION WHERE id_fonction = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idFonction);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFonctionPersonnel(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de la fonction personnel par ID: " + idFonction, e);
        }
        return null;
    }

    public List<FonctionPersonnel> trouverToutesLesFonctionsPersonnel() {
        List<FonctionPersonnel> fonctions = new ArrayList<>();
        final String sql = "SELECT * FROM FONCTION";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                fonctions.add(mapResultSetToFonctionPersonnel(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de toutes les fonctions personnel.", e);
        }
        return fonctions;
    }

    public FonctionPersonnel sauvegarderFonctionPersonnel(FonctionPersonnel fonction) {
        Objects.requireNonNull(fonction, "L'objet FonctionPersonnel ne peut être nul.");
        final boolean isInsert = fonction.getIdFonction() == 0;
        final String sql = isInsert ?
                "INSERT INTO FONCTION (lib_fonction) VALUES (?)" :
                "UPDATE FONCTION SET lib_fonction = ? WHERE id_fonction = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, fonction.getLibFonction());
            if (!isInsert) {
                pstmt.setInt(2, fonction.getIdFonction());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de la fonction personnel a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        fonction.setIdFonction(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de la fonction personnel a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return fonction;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de la fonction personnel.", e);
        }
    }

    public void supprimerFonctionPersonnelParId(int idFonction) {
        final String sql = "DELETE FROM FONCTION WHERE id_fonction = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idFonction);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucune fonction personnel supprimée pour l'ID: " + idFonction + " (elle n'existait peut-être pas ou est utilisée).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de la fonction personnel par ID: " + idFonction + ". Vérifiez si elle est utilisée par du personnel.", e);
        }
    }

    private Mission mapResultSetToMission(ResultSet rs) throws SQLException {
        Mission mission = new Mission();
        mission.setIdMission(rs.getInt("id_mission"));
        mission.setIdVehicule(rs.getInt("id_vehicule"));
        mission.setLibMission(rs.getString("lib_mission"));
        mission.setSite(rs.getString("site"));
        Timestamp dateDebutTs = rs.getTimestamp("date_debut_mission");
        if (dateDebutTs != null) mission.setDateDebutMission(dateDebutTs.toLocalDateTime());
        Timestamp dateFinTs = rs.getTimestamp("date_fin_mission");
        if (dateFinTs != null) mission.setDateFinMission(dateFinTs.toLocalDateTime());
        mission.setKmPrevu(rs.getObject("km_prevu", Integer.class));
        mission.setKmReel(rs.getObject("km_reel", Integer.class));
        String statutStr = rs.getString("status");
        if (statutStr != null) mission.setStatus(StatutMission.fromDbValue(statutStr));
        mission.setCoutTotal(rs.getBigDecimal("cout_total"));
        mission.setCircuitMission(rs.getString("circuit_mission"));
        mission.setObservationMission(rs.getString("observation_mission"));
        return mission;
    }

    public Mission trouverMissionParId(int idMission) {
        final String sql = "SELECT * FROM MISSION WHERE id_mission = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idMission);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMission(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de la mission par ID: " + idMission, e);
        }
        return null;
    }

    public List<Mission> trouverMissionsActivesPourVehicule(int idVehicule) {
        List<Mission> missions = new ArrayList<>();
        final String sql = "SELECT * FROM MISSION WHERE id_vehicule = ? AND status IN (?, ?)"; // PLANIFIEE, EN_COURS
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            pstmt.setString(2, StatutMission.PLANIFIEE.getDbValue());
            pstmt.setString(3, StatutMission.EN_COURS.getDbValue());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    missions.add(mapResultSetToMission(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des missions actives/planifiées pour le véhicule ID: " + idVehicule, e);
        }
        return missions;
    }

    public List<Mission> trouverToutesLesMissions() {
        List<Mission> missions = new ArrayList<>();
        final String sql = "SELECT * FROM MISSION";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                missions.add(mapResultSetToMission(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de toutes les missions.", e);
        }
        return missions;
    }

    public Mission sauvegarderMission(Mission mission) {
        Objects.requireNonNull(mission, "L'objet Mission ne peut être nul.");
        final boolean isInsert = mission.getIdMission() == 0;
        final String sql = isInsert ?
                "INSERT INTO MISSION (id_vehicule, lib_mission, site, date_debut_mission, date_fin_mission, km_prevu, km_reel, status, cout_total, circuit_mission, observation_mission) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                "UPDATE MISSION SET id_vehicule = ?, lib_mission = ?, site = ?, date_debut_mission = ?, date_fin_mission = ?, km_prevu = ?, km_reel = ?, status = ?, cout_total = ?, circuit_mission = ?, observation_mission = ? WHERE id_mission = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, mission.getIdVehicule());
            pstmt.setString(2, mission.getLibMission());
            pstmt.setString(3, mission.getSite());
            pstmt.setTimestamp(4, mission.getDateDebutMission() != null ? Timestamp.valueOf(mission.getDateDebutMission()) : null);
            pstmt.setTimestamp(5, mission.getDateFinMission() != null ? Timestamp.valueOf(mission.getDateFinMission()) : null);
            pstmt.setObject(6, mission.getKmPrevu(), Types.INTEGER);
            pstmt.setObject(7, mission.getKmReel(), Types.INTEGER);
            pstmt.setString(8, mission.getStatus() != null ? mission.getStatus().getDbValue() : null);
            pstmt.setBigDecimal(9, mission.getCoutTotal());
            pstmt.setString(10, mission.getCircuitMission());
            pstmt.setString(11, mission.getObservationMission());
            if (!isInsert) {
                pstmt.setInt(12, mission.getIdMission());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de la mission a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        mission.setIdMission(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de la mission a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return mission;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de la mission.", e);
        }
    }

    public void supprimerMissionParId(int idMission) {
        final String sqlDepenses = "DELETE FROM DEPENSE_MISSION WHERE id_mission = ?";
        final String sqlMission = "DELETE FROM MISSION WHERE id_mission = ?";
        Connection conn = null;
        try {
            conn = dbUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtDepenses = conn.prepareStatement(sqlDepenses)) {
                pstmtDepenses.setInt(1, idMission);
                pstmtDepenses.executeUpdate();
            }

            try (PreparedStatement pstmtMission = conn.prepareStatement(sqlMission)) {
                pstmtMission.setInt(1, idMission);
                int affectedRows = pstmtMission.executeUpdate();
                if (affectedRows == 0) {
                    System.out.println("Aucune mission supprimée pour l'ID: " + idMission + " (elle n'existait peut-être pas).");
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ErreurBaseDeDonnees("Erreur lors du rollback de la suppression de la mission: " + idMission, ex);
                }
            }
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de la mission par ID: " + idMission, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    // Ignorer ou logger
                }
            }
        }
    }

    public void supprimerToutesMissionsPourVehicule(int idVehicule) {
        List<Mission> missions = trouverMissionsPourVehicule(idVehicule);
        for (Mission mission : missions) {
            supprimerMissionParId(mission.getIdMission()); // Utilise la méthode qui gère les dépenses
        }
    }

    public List<Mission> trouverMissionsPourVehicule(int idVehicule) {
        List<Mission> missions = new ArrayList<>();
        final String sql = "SELECT * FROM MISSION WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    missions.add(mapResultSetToMission(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération des missions pour le véhicule ID: " + idVehicule, e);
        }
        return missions;
    }


    private Mouvement mapResultSetToMouvement(ResultSet rs) throws SQLException {
        Mouvement mouvement = new Mouvement();
        mouvement.setId(rs.getInt("id"));
        mouvement.setIdSocietaire(rs.getInt("id_societaire"));
        mouvement.setDate(rs.getTimestamp("date").toLocalDateTime());
        mouvement.setType(TypeMouvement.fromDbValue(rs.getString("type")));
        mouvement.setMontant(rs.getBigDecimal("montant"));
        return mouvement;
    }

    public Mouvement trouverMouvementParId(int idMouvement) {
        final String sql = "SELECT * FROM MOUVEMENT WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idMouvement);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMouvement(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du mouvement par ID: " + idMouvement, e);
        }
        return null;
    }

    public List<Mouvement> trouverTousLesMouvements() {
        List<Mouvement> mouvements = new ArrayList<>();
        final String sql = "SELECT * FROM MOUVEMENT";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                mouvements.add(mapResultSetToMouvement(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les mouvements.", e);
        }
        return mouvements;
    }

    public List<Mouvement> trouverMouvementsParSocietaireId(int idSocietaire) {
        List<Mouvement> mouvements = new ArrayList<>();
        final String sql = "SELECT * FROM MOUVEMENT WHERE id_societaire = ? ORDER BY date DESC";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idSocietaire);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mouvements.add(mapResultSetToMouvement(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche des mouvements pour le sociétaire ID: " + idSocietaire, e);
        }
        return mouvements;
    }


    public Mouvement sauvegarderMouvement(Mouvement mouvement) {
        Objects.requireNonNull(mouvement, "L'objet Mouvement ne peut être nul.");
        final boolean isInsert = mouvement.getId() == 0;
        final String sql = isInsert ?
                "INSERT INTO MOUVEMENT (id_societaire, date, type, montant) VALUES (?, ?, ?, ?)" :
                "UPDATE MOUVEMENT SET id_societaire = ?, date = ?, type = ?, montant = ? WHERE id = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, mouvement.getIdSocietaire());
            pstmt.setTimestamp(2, Timestamp.valueOf(mouvement.getDate()));
            pstmt.setString(3, mouvement.getType().getDbValue());
            pstmt.setBigDecimal(4, mouvement.getMontant());
            if (!isInsert) {
                pstmt.setInt(5, mouvement.getId());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde du mouvement a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        mouvement.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde du mouvement a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return mouvement;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde du mouvement.", e);
        }
    }

    public void supprimerMouvementParId(int idMouvement) {
        final String sql = "DELETE FROM MOUVEMENT WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idMouvement);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun mouvement supprimé pour l'ID: " + idMouvement + " (il n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression du mouvement par ID: " + idMouvement, e);
        }
    }

    private Personnel mapResultSetToPersonnel(ResultSet rs) throws SQLException {
        Personnel pers = new Personnel();
        pers.setIdPersonnel(rs.getInt("id_personnel"));
        pers.setIdService(rs.getObject("id_service", Integer.class));
        pers.setIdFonction(rs.getObject("id_fonction", Integer.class));
        pers.setIdVehicule(rs.getObject("id_vehicule", Integer.class)); // FK vers VEHICULES (véhicule de fonction)
        pers.setMatricule(rs.getString("matricule"));
        pers.setNomPersonnel(rs.getString("nom_personnel"));
        pers.setPrenomPersonnel(rs.getString("prenom_personnel"));
        pers.setEmail(rs.getString("email"));
        pers.setTelephone(rs.getString("telephone"));
        pers.setAdresse(rs.getString("adresse"));
        Date dateNaissanceSql = rs.getDate("date_naissance");
        if (dateNaissanceSql != null) pers.setDateNaissance(dateNaissanceSql.toLocalDate());
        String sexeStr = rs.getString("sexe");
        if (sexeStr != null) pers.setSexe(SexePersonnel.fromDbValue(sexeStr));
        Timestamp dateAttributionTs = rs.getTimestamp("date_attribution");
        if (dateAttributionTs != null) pers.setDateAttribution(dateAttributionTs.toLocalDateTime());
        return pers;
    }

    public Personnel trouverPersonnelParId(int idPersonnel) {
        final String sql = "SELECT * FROM PERSONNEL WHERE id_personnel = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idPersonnel);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPersonnel(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du personnel par ID: " + idPersonnel, e);
        }
        return null;
    }

    public List<Personnel> trouverToutLePersonnel() {
        List<Personnel> personnels = new ArrayList<>();
        final String sql = "SELECT * FROM PERSONNEL";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                personnels.add(mapResultSetToPersonnel(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tout le personnel.", e);
        }
        return personnels;
    }

    public Personnel sauvegarderPersonnel(Personnel personnel) {
        Objects.requireNonNull(personnel, "L'objet Personnel ne peut être nul.");
        final boolean isInsert = personnel.getIdPersonnel() == 0;
        final String sql = isInsert ?
                "INSERT INTO PERSONNEL (id_service, id_fonction, id_vehicule, matricule, nom_personnel, prenom_personnel, email, telephone, adresse, date_naissance, sexe, date_attribution) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                "UPDATE PERSONNEL SET id_service = ?, id_fonction = ?, id_vehicule = ?, matricule = ?, nom_personnel = ?, prenom_personnel = ?, email = ?, telephone = ?, adresse = ?, date_naissance = ?, sexe = ?, date_attribution = ? WHERE id_personnel = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, personnel.getIdService(), Types.INTEGER);
            pstmt.setObject(2, personnel.getIdFonction(), Types.INTEGER);
            pstmt.setObject(3, personnel.getIdVehicule(), Types.INTEGER);
            pstmt.setString(4, personnel.getMatricule());
            pstmt.setString(5, personnel.getNomPersonnel());
            pstmt.setString(6, personnel.getPrenomPersonnel());
            pstmt.setString(7, personnel.getEmail());
            pstmt.setString(8, personnel.getTelephone());
            pstmt.setString(9, personnel.getAdresse());
            pstmt.setDate(10, personnel.getDateNaissance() != null ? Date.valueOf(personnel.getDateNaissance()) : null);
            pstmt.setString(11, personnel.getSexe() != null ? personnel.getSexe().getDbValue() : null);
            pstmt.setTimestamp(12, personnel.getDateAttribution() != null ? Timestamp.valueOf(personnel.getDateAttribution()) : null);
            if (!isInsert) {
                pstmt.setInt(13, personnel.getIdPersonnel());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde du personnel a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        personnel.setIdPersonnel(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde du personnel a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return personnel;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde du personnel.", e);
        }
    }

    public void supprimerPersonnelParId(int idPersonnel) {
        // Gérer les dépendances (UTILISATEUR.id_personnel, SOCIETAIRE_COMPTE.id_personnel, AFFECTATION.id_personnel)
        // Option 1: Mettre à NULL dans les tables dépendantes (si la BDD le permet avec ON DELETE SET NULL)
        // Option 2: Interdire la suppression si utilisé (plus sûr par défaut)
        // Option 3: Supprimer en cascade (risqué si non voulu)
        // Ici, nous allons opter pour une vérification simple. Une gestion plus fine serait dans BusinessLogicService.

        final String checkUtilisateurSql = "SELECT COUNT(*) FROM UTILISATEUR WHERE id_personnel = ?";
        final String checkSocietaireSql = "SELECT COUNT(*) FROM SOCIETAIRE_COMPTE WHERE id_personnel = ?";
        final String checkAffectationSql = "SELECT COUNT(*) FROM AFFECTATION WHERE id_personnel = ?";
        final String deletePersonnelSql = "DELETE FROM PERSONNEL WHERE id_personnel = ?";

        try (Connection conn = dbUtil.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtCheckUser = conn.prepareStatement(checkUtilisateurSql);
                 PreparedStatement pstmtCheckSoc = conn.prepareStatement(checkSocietaireSql);
                 PreparedStatement pstmtCheckAff = conn.prepareStatement(checkAffectationSql);
                 PreparedStatement pstmtDelete = conn.prepareStatement(deletePersonnelSql)) {

                pstmtCheckUser.setInt(1, idPersonnel);
                ResultSet rsUser = pstmtCheckUser.executeQuery();
                if (rsUser.next() && rsUser.getInt(1) > 0) {
                    conn.rollback();
                    throw new ErreurLogiqueMetier("Impossible de supprimer le personnel ID " + idPersonnel + ": il est lié à un ou plusieurs utilisateurs.");
                }

                pstmtCheckSoc.setInt(1, idPersonnel);
                ResultSet rsSoc = pstmtCheckSoc.executeQuery();
                if (rsSoc.next() && rsSoc.getInt(1) > 0) {
                    conn.rollback();
                    throw new ErreurLogiqueMetier("Impossible de supprimer le personnel ID " + idPersonnel + ": il est lié à un ou plusieurs comptes sociétaires.");
                }

                pstmtCheckAff.setInt(1, idPersonnel);
                ResultSet rsAff = pstmtCheckAff.executeQuery();
                if (rsAff.next() && rsAff.getInt(1) > 0) {
                    conn.rollback();
                    throw new ErreurLogiqueMetier("Impossible de supprimer le personnel ID " + idPersonnel + ": il est lié à une ou plusieurs affectations.");
                }

                pstmtDelete.setInt(1, idPersonnel);
                int affectedRows = pstmtDelete.executeUpdate();
                if (affectedRows == 0) {
                    System.out.println("Aucun personnel supprimé pour l'ID: " + idPersonnel + " (il n'existait peut-être pas).");
                }
                conn.commit();
            } catch (SQLException eInner) {
                conn.rollback();
                throw eInner;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression du personnel par ID: " + idPersonnel, e);
        }
    }

    private ServiceEntreprise mapResultSetToServiceEntreprise(ResultSet rs) throws SQLException {
        ServiceEntreprise service = new ServiceEntreprise();
        service.setIdService(rs.getInt("id_service"));
        service.setLibService(rs.getString("lib_service"));
        service.setLocalisationService(rs.getString("localisation_service"));
        return service;
    }

    public ServiceEntreprise trouverServiceEntrepriseParId(int idService) {
        final String sql = "SELECT * FROM SERVICE WHERE id_service = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idService);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToServiceEntreprise(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du service entreprise par ID: " + idService, e);
        }
        return null;
    }

    public List<ServiceEntreprise> trouverTousLesServicesEntreprise() {
        List<ServiceEntreprise> services = new ArrayList<>();
        final String sql = "SELECT * FROM SERVICE";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                services.add(mapResultSetToServiceEntreprise(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les services entreprise.", e);
        }
        return services;
    }

    public ServiceEntreprise sauvegarderServiceEntreprise(ServiceEntreprise service) {
        Objects.requireNonNull(service, "L'objet ServiceEntreprise ne peut être nul.");
        final boolean isInsert = service.getIdService() == 0;
        final String sql = isInsert ?
                "INSERT INTO SERVICE (lib_service, localisation_service) VALUES (?, ?)" :
                "UPDATE SERVICE SET lib_service = ?, localisation_service = ? WHERE id_service = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, service.getLibService());
            pstmt.setString(2, service.getLocalisationService());
            if (!isInsert) {
                pstmt.setInt(3, service.getIdService());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde du service entreprise a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        service.setIdService(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde du service entreprise a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return service;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde du service entreprise.", e);
        }
    }

    public void supprimerServiceEntrepriseParId(int idService) {
        final String sql = "DELETE FROM SERVICE WHERE id_service = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idService);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun service entreprise supprimé pour l'ID: " + idService + " (il n'existait peut-être pas ou est utilisé).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression du service entreprise par ID: " + idService + ". Vérifiez s'il est utilisé par du personnel.", e);
        }
    }

    private SocietaireCompte mapResultSetToSocietaireCompte(ResultSet rs) throws SQLException {
        SocietaireCompte compte = new SocietaireCompte();
        compte.setIdSocietaire(rs.getInt("id_societaire"));
        compte.setIdPersonnel(rs.getObject("id_personnel", Integer.class));
        compte.setNom(rs.getString("nom"));
        compte.setNumero(rs.getString("numero"));
        compte.setSolde(rs.getBigDecimal("solde"));
        compte.setEmail(rs.getString("email"));
        compte.setTelephone(rs.getString("telephone"));
        return compte;
    }

    public SocietaireCompte trouverSocietaireCompteParId(int idSocietaire) {
        final String sql = "SELECT * FROM SOCIETAIRE_COMPTE WHERE id_societaire = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idSocietaire);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSocietaireCompte(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du compte sociétaire par ID: " + idSocietaire, e);
        }
        return null;
    }

    public List<SocietaireCompte> trouverTousLesSocietairesComptes() {
        List<SocietaireCompte> comptes = new ArrayList<>();
        final String sql = "SELECT * FROM SOCIETAIRE_COMPTE";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                comptes.add(mapResultSetToSocietaireCompte(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les comptes sociétaires.", e);
        }
        return comptes;
    }

    public SocietaireCompte sauvegarderSocietaireCompte(SocietaireCompte compte) {
        Objects.requireNonNull(compte, "L'objet SocietaireCompte ne peut être nul.");
        final boolean isInsert = compte.getIdSocietaire() == 0;
        final String sql = isInsert ?
                "INSERT INTO SOCIETAIRE_COMPTE (id_personnel, nom, numero, solde, email, telephone) VALUES (?, ?, ?, ?, ?, ?)" :
                "UPDATE SOCIETAIRE_COMPTE SET id_personnel = ?, nom = ?, numero = ?, solde = ?, email = ?, telephone = ? WHERE id_societaire = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, compte.getIdPersonnel(), Types.INTEGER);
            pstmt.setString(2, compte.getNom());
            pstmt.setString(3, compte.getNumero());
            pstmt.setBigDecimal(4, compte.getSolde());
            pstmt.setString(5, compte.getEmail());
            pstmt.setString(6, compte.getTelephone());
            if (!isInsert) {
                pstmt.setInt(7, compte.getIdSocietaire());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde du compte sociétaire a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        compte.setIdSocietaire(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde du compte sociétaire a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return compte;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde du compte sociétaire.", e);
        }
    }

    public void supprimerSocietaireCompteParId(int idSocietaire) {
        // Gérer dépendances: MOUVEMENT, DOCUMENT_SOCIETAIRE, AFFECTATION
        // Pour simplifier ici, on suppose que ON DELETE CASCADE est configuré en BDD pour ces tables
        // ou que la logique métier s'assure de la non-utilisation avant suppression.
        final String sql = "DELETE FROM SOCIETAIRE_COMPTE WHERE id_societaire = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idSocietaire);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun compte sociétaire supprimé pour l'ID: " + idSocietaire + " (il n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression du compte sociétaire par ID: " + idSocietaire + ". Vérifiez les dépendances (mouvements, documents, affectations).", e);
        }
    }

    private Utilisateur mapResultSetToUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(rs.getInt("id"));
        utilisateur.setLogin(rs.getString("login"));
        utilisateur.setHashMdp(rs.getString("hash")); // La colonne est 'hash' dans la BDD
        utilisateur.setRole(RoleUtilisateur.fromDbValue(rs.getString("role")));
        utilisateur.setIdPersonnel(rs.getObject("id_personnel", Integer.class));
        utilisateur.setMfaSecret(rs.getString("mfa_secret"));
        return utilisateur;
    }

    public Utilisateur trouverUtilisateurParLogin(String login) {
        Objects.requireNonNull(login, "Le login ne peut être nul.");
        final String sql = "SELECT * FROM UTILISATEUR WHERE login = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtilisateur(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'utilisateur par login: " + login, e);
        }
        return null;
    }

    public Utilisateur trouverUtilisateurParId(int idUtilisateur) {
        final String sql = "SELECT * FROM UTILISATEUR WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUtilisateur);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtilisateur(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche de l'utilisateur par ID: " + idUtilisateur, e);
        }
        return null;
    }

    public List<Utilisateur> trouverTousLesUtilisateurs() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        final String sql = "SELECT * FROM UTILISATEUR";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                utilisateurs.add(mapResultSetToUtilisateur(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les utilisateurs.", e);
        }
        return utilisateurs;
    }

    public Utilisateur sauvegarderUtilisateur(Utilisateur utilisateur) {
        Objects.requireNonNull(utilisateur, "L'objet Utilisateur ne peut être nul.");
        final boolean isInsert = utilisateur.getId() == 0;
        final String sql = isInsert ?
                "INSERT INTO UTILISATEUR (login, hash, role, id_personnel, mfa_secret) VALUES (?, ?, ?, ?, ?)" :
                "UPDATE UTILISATEUR SET login = ?, hash = ?, role = ?, id_personnel = ?, mfa_secret = ? WHERE id = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, utilisateur.getLogin());
            pstmt.setString(2, utilisateur.getHashMdp());
            pstmt.setString(3, utilisateur.getRole().getDbValue());
            pstmt.setObject(4, utilisateur.getIdPersonnel(), Types.INTEGER);
            pstmt.setString(5, utilisateur.getMfaSecret());
            if (!isInsert) {
                pstmt.setInt(6, utilisateur.getId());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde de l'utilisateur a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        utilisateur.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde de l'utilisateur a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return utilisateur;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde de l'utilisateur.", e);
        }
    }

    public void supprimerUtilisateurParId(int idUtilisateur) {
        final String sql = "DELETE FROM UTILISATEUR WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUtilisateur);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun utilisateur supprimé pour l'ID: " + idUtilisateur + " (il n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression de l'utilisateur par ID: " + idUtilisateur, e);
        }
    }

    private Vehicule mapResultSetToVehicule(ResultSet rs) throws SQLException {
        Vehicule vehicule = new Vehicule();
        vehicule.setIdVehicule(rs.getInt("id_vehicule"));
        vehicule.setIdEtatVoiture(rs.getInt("id_etat_voiture"));
        vehicule.setEnergie(EnergieVehicule.fromDbValue(rs.getString("energie")));
        vehicule.setNumeroChassi(rs.getString("numero_chassi"));
        vehicule.setImmatriculation(rs.getString("immatriculation"));
        vehicule.setMarque(rs.getString("marque"));
        vehicule.setModele(rs.getString("modele"));
        vehicule.setNbPlaces(rs.getObject("nb_places", Integer.class));
        Timestamp dateAcquisitionTs = rs.getTimestamp("date_acquisition");
        if (dateAcquisitionTs != null) vehicule.setDateAcquisition(dateAcquisitionTs.toLocalDateTime());
        Timestamp dateAmmortissementTs = rs.getTimestamp("date_ammortissement");
        if (dateAmmortissementTs != null) vehicule.setDateAmmortissement(dateAmmortissementTs.toLocalDateTime());
        Timestamp dateMiseEnServiceTs = rs.getTimestamp("date_mise_en_service");
        if (dateMiseEnServiceTs != null) vehicule.setDateMiseEnService(dateMiseEnServiceTs.toLocalDateTime());
        vehicule.setPuissance(rs.getObject("puissance", Integer.class));
        vehicule.setCouleur(rs.getString("couleur"));
        vehicule.setPrixVehicule(rs.getBigDecimal("prix_vehicule"));
        vehicule.setKmActuels(rs.getObject("km_actuels", Integer.class));
        Timestamp dateEtatTs = rs.getTimestamp("date_etat");
        if (dateEtatTs != null) vehicule.setDateEtat(dateEtatTs.toLocalDateTime());
        return vehicule;
    }

    public Vehicule trouverVehiculeParId(int idVehicule) {
        final String sql = "SELECT * FROM VEHICULES WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicule(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du véhicule par ID: " + idVehicule, e);
        }
        return null;
    }

    public Vehicule trouverVehiculeParImmatriculation(String immatriculation) {
        Objects.requireNonNull(immatriculation, "L'immatriculation ne peut être nulle.");
        final String sql = "SELECT * FROM VEHICULES WHERE immatriculation = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, immatriculation);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicule(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du véhicule par immatriculation: " + immatriculation, e);
        }
        return null;
    }

    public Vehicule trouverVehiculeParNumeroChassi(String numeroChassi) {
        Objects.requireNonNull(numeroChassi, "Le numéro de châssis ne peut être nul.");
        final String sql = "SELECT * FROM VEHICULES WHERE numero_chassi = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroChassi);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicule(rs);
                }
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la recherche du véhicule par numéro de châssis: " + numeroChassi, e);
        }
        return null;
    }

    public List<Vehicule> trouverTousLesVehicules() {
        List<Vehicule> vehicules = new ArrayList<>();
        final String sql = "SELECT * FROM VEHICULES";
        try (Connection conn = dbUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                vehicules.add(mapResultSetToVehicule(rs));
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la récupération de tous les véhicules.", e);
        }
        return vehicules;
    }

    public Vehicule sauvegarderVehicule(Vehicule vehicule) {
        Objects.requireNonNull(vehicule, "L'objet Vehicule ne peut être nul.");
        final boolean isInsert = vehicule.getIdVehicule() == 0;
        final String sql = isInsert ?
                "INSERT INTO VEHICULES (id_etat_voiture, energie, numero_chassi, immatriculation, marque, modele, nb_places, date_acquisition, date_ammortissement, date_mise_en_service, puissance, couleur, prix_vehicule, km_actuels, date_etat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                "UPDATE VEHICULES SET id_etat_voiture = ?, energie = ?, numero_chassi = ?, immatriculation = ?, marque = ?, modele = ?, nb_places = ?, date_acquisition = ?, date_ammortissement = ?, date_mise_en_service = ?, puissance = ?, couleur = ?, prix_vehicule = ?, km_actuels = ?, date_etat = ? WHERE id_vehicule = ?";

        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, vehicule.getIdEtatVoiture());
            pstmt.setString(2, vehicule.getEnergie().getDbValue());
            pstmt.setString(3, vehicule.getNumeroChassi());
            pstmt.setString(4, vehicule.getImmatriculation());
            pstmt.setString(5, vehicule.getMarque());
            pstmt.setString(6, vehicule.getModele());
            pstmt.setObject(7, vehicule.getNbPlaces(), Types.INTEGER);
            pstmt.setTimestamp(8, vehicule.getDateAcquisition() != null ? Timestamp.valueOf(vehicule.getDateAcquisition()) : null);
            pstmt.setTimestamp(9, vehicule.getDateAmmortissement() != null ? Timestamp.valueOf(vehicule.getDateAmmortissement()) : null);
            pstmt.setTimestamp(10, vehicule.getDateMiseEnService() != null ? Timestamp.valueOf(vehicule.getDateMiseEnService()) : null);
            pstmt.setObject(11, vehicule.getPuissance(), Types.INTEGER);
            pstmt.setString(12, vehicule.getCouleur());
            pstmt.setBigDecimal(13, vehicule.getPrixVehicule());
            pstmt.setObject(14, vehicule.getKmActuels(), Types.INTEGER);
            pstmt.setTimestamp(15, vehicule.getDateEtat() != null ? Timestamp.valueOf(vehicule.getDateEtat()) : null);

            if (!isInsert) {
                pstmt.setInt(16, vehicule.getIdVehicule());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La sauvegarde du véhicule a échoué, aucune ligne affectée.");
            }

            if (isInsert) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        vehicule.setIdVehicule(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("La sauvegarde du véhicule a échoué, aucun ID auto-généré retourné.");
                    }
                }
            }
            return vehicule;
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la sauvegarde du véhicule.", e);
        }
    }

    public void supprimerVehiculeParId(int idVehicule) {
        // La suppression des dépendances (Missions, Entretiens, Affectations, Couvrir)
        // est gérée dans BusinessLogicService avant d'appeler cette méthode,
        // ou devrait être gérée par des contraintes ON DELETE CASCADE dans la BDD.
        // Ici, on supprime juste le véhicule.
        final String sql = "DELETE FROM VEHICULES WHERE id_vehicule = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idVehicule);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Aucun véhicule supprimé pour l'ID: " + idVehicule + " (il n'existait peut-être pas).");
            }
        } catch (SQLException e) {
            throw new ErreurBaseDeDonnees("Erreur lors de la suppression du véhicule par ID: " + idVehicule + ". Vérifiez les dépendances non gérées.", e);
        }
    }
        public List<Vehicule> trouverVehiculesParEtat(int idEtatVoiture) {
            List<Vehicule> vehicules = new ArrayList<>();
            String sql = "SELECT * FROM VEHICULES WHERE id_etat_voiture = ? ORDER BY marque, modele";
            try (Connection conn = dbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idEtatVoiture);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        vehicules.add(extraireVehiculeDepuisResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL lors de la récupération des véhicules par état.", e);
                throw new ErreurBaseDeDonnees("Impossible de récupérer les véhicules par état.", e);
            }
            return vehicules;
        }

        public SocietaireCompte trouverSocietaireCompteParIdPersonnel(int idPersonnel) {
            String sql = "SELECT * FROM SOCIETAIRE_COMPTE WHERE id_personnel = ?";
            try (Connection conn = dbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idPersonnel);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return extraireSocietaireCompteDepuisResultSet(rs);
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL recherche sociétaire par ID Personnel: " + idPersonnel, e);
                throw new ErreurBaseDeDonnees("Impossible de trouver sociétaire par ID Personnel.", e);
            }
            return null; // Peut y avoir plusieurs comptes pour un personnel? Schema dit non.
        }

        public List<DocumentSocietaire> trouverDocumentsParType(TypeDocumentSocietaire typeDoc) {
            List<DocumentSocietaire> documents = new ArrayList<>();
            String sql = "SELECT * FROM DOCUMENT_SOCIETAIRE WHERE type_doc = ? ORDER BY date_upload DESC";
            try (Connection conn = dbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, typeDoc.getDbValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        documents.add(extraireDocumentSocietaireDepuisResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL recherche documents par type: " + typeDoc, e);
                throw new ErreurBaseDeDonnees("Impossible de trouver documents par type.", e);
            }
            return documents;
        }

        public List<DocumentSocietaire> trouverDocumentsParSocietaireEtType(int idSocietaire, TypeDocumentSocietaire typeDoc) {
            List<DocumentSocietaire> documents = new ArrayList<>();
            String sql = "SELECT * FROM DOCUMENT_SOCIETAIRE WHERE id_societaire = ? AND type_doc = ? ORDER BY date_upload DESC";
            try (Connection conn = dbUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idSocietaire);
                pstmt.setString(2, typeDoc.getDbValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        documents.add(extraireDocumentSocietaireDepuisResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL recherche documents pour sociétaire " + idSocietaire + " et type " + typeDoc, e);
                throw new ErreurBaseDeDonnees("Impossible de trouver documents par sociétaire et type.", e);
            }
            return documents;
        }

        public List<DocumentSocietaire> trouverTousLesDocumentsSocietaires() {
            List<DocumentSocietaire> documents = new ArrayList<>();
            String sql = "SELECT * FROM DOCUMENT_SOCIETAIRE ORDER BY id_societaire, date_upload DESC";
            try (Connection conn = dbUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    documents.add(extraireDocumentSocietaireDepuisResultSet(rs));
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL lors de la récupération de tous les documents sociétaires.", e);
                throw new ErreurBaseDeDonnees("Impossible de récupérer tous les documents sociétaires.", e);
            }
            return documents;
        }

        public int compterTousLesVehicules() {
            String sql = "SELECT COUNT(*) FROM VEHICULES";
            try (Connection conn = dbUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL lors du comptage des véhicules.", e);
                throw new ErreurBaseDeDonnees("Impossible de compter les véhicules.", e);
            }
            return 0;
        }

        public int compterMissionsParStatut(StatutMission statut) {
            String sql = "SELECT COUNT(*) FROM MISSION WHERE status = ?";
            try (Connection conn = dbUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, statut.getDbValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL lors du comptage des missions par statut: " + statut, e);
                throw new ErreurBaseDeDonnees("Impossible de compter les missions par statut.", e);
            }
            return 0;
        }

        public int compterEntretiensParStatutOT(StatutOrdreTravail statutOT) {
            String sql = "SELECT COUNT(*) FROM ENTRETIEN WHERE statut_ot = ?";
            try (Connection conn = dbUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, statutOT.getDbValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                LOGGER_PERSISTANCE.log(Level.SEVERE, "Erreur SQL lors du comptage des entretiens par statut OT: " + statutOT, e);
                throw new ErreurBaseDeDonnees("Impossible de compter les entretiens par statut OT.", e);
            }
            return 0;
        }

        // Méthodes d'extraction depuis ResultSet
        private Vehicule extraireVehiculeDepuisResultSet(ResultSet rs) throws SQLException {
            Vehicule v = new Vehicule();
            v.setIdVehicule(rs.getInt("id_vehicule"));
            v.setIdEtatVoiture(rs.getInt("id_etat_voiture"));
            v.setEnergie(EnergieVehicule.fromDbValue(rs.getString("energie")));
            v.setNumeroChassi(rs.getString("numero_chassi"));
            v.setImmatriculation(rs.getString("immatriculation"));
            v.setMarque(rs.getString("marque"));
            v.setModele(rs.getString("modele"));
            v.setNbPlaces(rs.getObject("nb_places", Integer.class));
            Timestamp tsAcquisition = rs.getTimestamp("date_acquisition");
            if (tsAcquisition != null) v.setDateAcquisition(tsAcquisition.toLocalDateTime());
            Timestamp tsAmmortissement = rs.getTimestamp("date_ammortissement");
            if (tsAmmortissement != null) v.setDateAmmortissement(tsAmmortissement.toLocalDateTime());
            Timestamp tsMiseService = rs.getTimestamp("date_mise_en_service");
            if (tsMiseService != null) v.setDateMiseEnService(tsMiseService.toLocalDateTime());
            v.setPuissance(rs.getObject("puissance", Integer.class));
            v.setCouleur(rs.getString("couleur"));
            v.setPrixVehicule(rs.getBigDecimal("prix_vehicule"));
            v.setKmActuels(rs.getObject("km_actuels", Integer.class));
            Timestamp tsEtat = rs.getTimestamp("date_etat");
            if (tsEtat != null) v.setDateEtat(tsEtat.toLocalDateTime());
            return v;
        }

        private Mission extraireMissionDepuisResultSet(ResultSet rs) throws SQLException {
            Mission m = new Mission();
            m.setIdMission(rs.getInt("id_mission"));
            m.setIdVehicule(rs.getInt("id_vehicule"));
            m.setLibMission(rs.getString("lib_mission"));
            m.setSite(rs.getString("site"));
            Timestamp tsDebut = rs.getTimestamp("date_debut_mission");
            if (tsDebut != null) m.setDateDebutMission(tsDebut.toLocalDateTime());
            Timestamp tsFin = rs.getTimestamp("date_fin_mission");
            if (tsFin != null) m.setDateFinMission(tsFin.toLocalDateTime());
            m.setKmPrevu(rs.getObject("km_prevu", Integer.class));
            m.setKmReel(rs.getObject("km_reel", Integer.class));
            m.setStatus(StatutMission.fromDbValue(rs.getString("status")));
            m.setCoutTotal(rs.getBigDecimal("cout_total"));
            m.setCircuitMission(rs.getString("circuit_mission"));
            m.setObservationMission(rs.getString("observation_mission"));
            return m;
        }

        private Affectation extraireAffectationDepuisResultSet(ResultSet rs) throws SQLException {
            Affectation a = new Affectation();
            a.setId(rs.getInt("id"));
            a.setIdVehicule(rs.getInt("id_vehicule"));
            a.setIdPersonnel(rs.getObject("id_personnel", Integer.class));
            a.setIdSocietaire(rs.getObject("id_societaire", Integer.class));
            a.setType(TypeAffectation.fromDbValue(rs.getString("type")));
            a.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
            Timestamp dateFinTs = rs.getTimestamp("date_fin");
            if (dateFinTs != null) {
                a.setDateFin(dateFinTs.toLocalDateTime());
            }
            return a;
        }

        private DepenseMission extraireDepenseMissionDepuisResultSet(ResultSet rs) throws SQLException {
            DepenseMission d = new DepenseMission();
            d.setId(rs.getInt("id"));
            d.setIdMission(rs.getInt("id_mission"));
            d.setNature(NatureDepense.fromDbValue(rs.getString("nature")));
            d.setMontant(rs.getBigDecimal("montant"));
            d.setJustificatif(rs.getString("justificatif"));
            return d;
        }

        private Entretien extraireEntretienDepuisResultSet(ResultSet rs) throws SQLException {
            Entretien e = new Entretien();
            e.setIdEntretien(rs.getInt("id_entretien"));
            e.setIdVehicule(rs.getInt("id_vehicule"));
            Timestamp tsEntree = rs.getTimestamp("date_entree_entr");
            if (tsEntree != null) e.setDateEntreeEntr(tsEntree.toLocalDateTime());
            Timestamp tsSortie = rs.getTimestamp("date_sortie_entr");
            if (tsSortie != null) e.setDateSortieEntr(tsSortie.toLocalDateTime());
            e.setMotifEntr(rs.getString("motif_entr"));
            e.setObservation(rs.getString("observation"));
            e.setCoutEntr(rs.getBigDecimal("cout_entr"));
            e.setLieuEntr(rs.getString("lieu_entr"));
            String typeStr = rs.getString("type");
            if (typeStr != null) e.setType(TypeEntretien.fromDbValue(typeStr));
            String statutOtStr = rs.getString("statut_ot");
            if (statutOtStr != null) e.setStatutOt(StatutOrdreTravail.fromDbValue(statutOtStr));
            return e;
        }

        private SocietaireCompte extraireSocietaireCompteDepuisResultSet(ResultSet rs) throws SQLException {
            SocietaireCompte sc = new SocietaireCompte();
            sc.setIdSocietaire(rs.getInt("id_societaire"));
            sc.setIdPersonnel(rs.getObject("id_personnel", Integer.class));
            sc.setNom(rs.getString("nom"));
            sc.setNumero(rs.getString("numero"));
            sc.setSolde(rs.getBigDecimal("solde"));
            sc.setEmail(rs.getString("email"));
            sc.setTelephone(rs.getString("telephone"));
            return sc;
        }

        private Mouvement extraireMouvementDepuisResultSet(ResultSet rs) throws SQLException {
            Mouvement m = new Mouvement();
            m.setId(rs.getInt("id"));
            m.setIdSocietaire(rs.getInt("id_societaire"));
            m.setDate(rs.getTimestamp("date").toLocalDateTime());
            m.setType(TypeMouvement.fromDbValue(rs.getString("type")));
            m.setMontant(rs.getBigDecimal("montant"));
            return m;
        }

        private DocumentSocietaire extraireDocumentSocietaireDepuisResultSet(ResultSet rs) throws SQLException {
            DocumentSocietaire d = new DocumentSocietaire();
            d.setIdDoc(rs.getInt("id_doc"));
            d.setIdSocietaire(rs.getInt("id_societaire"));
            d.setTypeDoc(TypeDocumentSocietaire.fromDbValue(rs.getString("type_doc")));
            d.setCheminFichier(rs.getString("chemin_fichier"));
            d.setDateUpload(rs.getTimestamp("date_upload").toLocalDateTime());
            return d;
        }

        private Utilisateur extraireUtilisateurDepuisResultSet(ResultSet rs) throws SQLException {
            Utilisateur u = new Utilisateur();
            u.setId(rs.getInt("id"));
            u.setLogin(rs.getString("login"));
            u.setHashMdp(rs.getString("hash"));
            u.setRole(RoleUtilisateur.fromDbValue(rs.getString("role")));
            u.setIdPersonnel(rs.getObject("id_personnel", Integer.class));
            u.setMfaSecret(rs.getString("mfa_secret"));
            return u;
        }

        private Personnel extrairePersonnelDepuisResultSet(ResultSet rs) throws SQLException {
            Personnel p = new Personnel();
            p.setIdPersonnel(rs.getInt("id_personnel"));
            p.setIdService(rs.getObject("id_service", Integer.class));
            p.setIdFonction(rs.getObject("id_fonction", Integer.class));
            p.setIdVehicule(rs.getObject("id_vehicule", Integer.class)); // Peut être null
            p.setMatricule(rs.getString("matricule"));
            p.setNomPersonnel(rs.getString("nom_personnel"));
            p.setPrenomPersonnel(rs.getString("prenom_personnel"));
            p.setEmail(rs.getString("email"));
            p.setTelephone(rs.getString("telephone"));
            p.setAdresse(rs.getString("adresse"));
            Date dateNaissanceSql = rs.getDate("date_naissance");
            if (dateNaissanceSql != null) p.setDateNaissance(dateNaissanceSql.toLocalDate());
            String sexeStr = rs.getString("sexe");
            if (sexeStr != null) p.setSexe(SexePersonnel.fromDbValue(sexeStr));
            Timestamp dateAttributionTs = rs.getTimestamp("date_attribution");
            if (dateAttributionTs != null) p.setDateAttribution(dateAttributionTs.toLocalDateTime());
            return p;
        }


        // Méthodes pour définir les paramètres des PreparedStatement
        private void definirParametresVehiculeStatement(PreparedStatement pstmt, Vehicule v) throws SQLException {
            pstmt.setInt(1, v.getIdEtatVoiture());
            pstmt.setString(2, v.getEnergie().getDbValue());
            pstmt.setString(3, v.getNumeroChassi());
            pstmt.setString(4, v.getImmatriculation());
            pstmt.setString(5, v.getMarque());
            pstmt.setString(6, v.getModele());
            pstmt.setObject(7, v.getNbPlaces());
            pstmt.setTimestamp(8, v.getDateAcquisition() != null ? Timestamp.valueOf(v.getDateAcquisition()) : null);
            pstmt.setTimestamp(9, v.getDateAmmortissement() != null ? Timestamp.valueOf(v.getDateAmmortissement()) : null);
            pstmt.setTimestamp(10, v.getDateMiseEnService() != null ? Timestamp.valueOf(v.getDateMiseEnService()) : null);
            pstmt.setObject(11, v.getPuissance());
            pstmt.setString(12, v.getCouleur());
            pstmt.setBigDecimal(13, v.getPrixVehicule());
            pstmt.setObject(14, v.getKmActuels());
            pstmt.setTimestamp(15, v.getDateEtat() != null ? Timestamp.valueOf(v.getDateEtat()) : Timestamp.valueOf(LocalDateTime.now()));
        }

        private void definirParametresMissionStatement(PreparedStatement pstmt, Mission m) throws SQLException {
            pstmt.setInt(1, m.getIdVehicule());
            pstmt.setString(2, m.getLibMission());
            pstmt.setString(3, m.getSite());
            pstmt.setTimestamp(4, m.getDateDebutMission() != null ? Timestamp.valueOf(m.getDateDebutMission()) : null);
            pstmt.setTimestamp(5, m.getDateFinMission() != null ? Timestamp.valueOf(m.getDateFinMission()) : null);
            pstmt.setObject(6, m.getKmPrevu());
            pstmt.setObject(7, m.getKmReel());
            pstmt.setString(8, m.getStatus().getDbValue());
            pstmt.setBigDecimal(9, m.getCoutTotal());
            pstmt.setString(10, m.getCircuitMission());
            pstmt.setString(11, m.getObservationMission());
        }

        private void definirParametresEntretienStatement(PreparedStatement pstmt, Entretien e) throws SQLException {
            pstmt.setInt(1, e.getIdVehicule());
            pstmt.setTimestamp(2, e.getDateEntreeEntr() != null ? Timestamp.valueOf(e.getDateEntreeEntr()) : null);
            pstmt.setTimestamp(3, e.getDateSortieEntr() != null ? Timestamp.valueOf(e.getDateSortieEntr()) : null);
            pstmt.setString(4, e.getMotifEntr());
            pstmt.setString(5, e.getObservation());
            pstmt.setBigDecimal(6, e.getCoutEntr());
            pstmt.setString(7, e.getLieuEntr());
            pstmt.setString(8, e.getType() != null ? e.getType().getDbValue() : null);
            pstmt.setString(9, e.getStatutOt() != null ? e.getStatutOt().getDbValue() : StatutOrdreTravail.OUVERT.getDbValue());
        }

        private void definirParametresSocietaireCompteStatement(PreparedStatement pstmt, SocietaireCompte sc) throws SQLException {
            pstmt.setObject(1, sc.getIdPersonnel());
            pstmt.setString(2, sc.getNom());
            pstmt.setString(3, sc.getNumero());
            pstmt.setBigDecimal(4, sc.getSolde());
            pstmt.setString(5, sc.getEmail());
            pstmt.setString(6, sc.getTelephone());
        }

        private void definirParametresUtilisateurStatement(PreparedStatement pstmt, Utilisateur u) throws SQLException {
            pstmt.setString(1, u.getLogin());
            pstmt.setString(2, u.getHashMdp());
            pstmt.setString(3, u.getRole().getDbValue());
            pstmt.setObject(4, u.getIdPersonnel());
            pstmt.setString(5, u.getMfaSecret());
        }
    }
