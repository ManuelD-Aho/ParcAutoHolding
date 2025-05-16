-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Hôte : db:3306
-- Généré le : ven. 16 mai 2025 à 05:24
-- Version du serveur : 8.3.0
-- Version de PHP : 8.2.27

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `ParcAuto`
--

-- --------------------------------------------------------

--
-- Structure de la table `AFFECTATION`
--

CREATE TABLE `AFFECTATION` (
                               `id` int NOT NULL,
                               `id_vehicule` int NOT NULL,
                               `id_personnel` int DEFAULT NULL,
                               `id_societaire` int DEFAULT NULL,
                               `type` enum('Credit5Ans','Mission') NOT NULL,
                               `date_debut` datetime NOT NULL,
                               `date_fin` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `ASSURANCE`
--

CREATE TABLE `ASSURANCE` (
                             `num_carte_assurance` int NOT NULL,
                             `date_debut_assurance` datetime DEFAULT NULL,
                             `date_fin_assurance` datetime DEFAULT NULL,
                             `agence` varchar(100) DEFAULT NULL,
                             `cout_assurance` decimal(12,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `COUVRIR`
--

CREATE TABLE `COUVRIR` (
                           `id_vehicule` int NOT NULL,
                           `num_carte_assurance` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `DEPENSE_MISSION`
--

CREATE TABLE `DEPENSE_MISSION` (
                                   `id` int NOT NULL,
                                   `id_mission` int NOT NULL,
                                   `nature` enum('Carburant','FraisAnnexes') NOT NULL,
                                   `montant` decimal(12,2) NOT NULL,
                                   `justificatif` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `DOCUMENT_SOCIETAIRE`
--

CREATE TABLE `DOCUMENT_SOCIETAIRE` (
                                       `id_doc` int NOT NULL,
                                       `id_societaire` int NOT NULL,
                                       `type_doc` enum('CarteGrise','Assurance','ID','Permis') NOT NULL,
                                       `chemin_fichier` varchar(255) NOT NULL,
                                       `date_upload` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `ENTRETIEN`
--

CREATE TABLE `ENTRETIEN` (
                             `id_entretien` int NOT NULL,
                             `id_vehicule` int NOT NULL,
                             `date_entree_entr` datetime DEFAULT NULL,
                             `date_sortie_entr` datetime DEFAULT NULL,
                             `motif_entr` varchar(100) DEFAULT NULL,
                             `observation` text,
                             `cout_entr` decimal(12,2) DEFAULT NULL,
                             `lieu_entr` varchar(70) DEFAULT NULL,
                             `type` enum('Preventif','Correctif') DEFAULT NULL,
                             `statut_ot` enum('Ouvert','EnCours','Cloture') DEFAULT 'Ouvert'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `ETAT_VOITURE`
--

CREATE TABLE `ETAT_VOITURE` (
                                `id_etat_voiture` int NOT NULL,
                                `lib_etat_voiture` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `ETAT_VOITURE`
--

INSERT INTO `ETAT_VOITURE` (`id_etat_voiture`, `lib_etat_voiture`) VALUES
                                                                       (5, 'Attribuer'),
                                                                       (1, 'Disponible'),
                                                                       (4, 'En entretien'),
                                                                       (2, 'En mission'),
                                                                       (3, 'Hors Service'),
                                                                       (6, 'Panne');

-- --------------------------------------------------------

--
-- Structure de la table `FONCTION`
--

CREATE TABLE `FONCTION` (
                            `id_fonction` int NOT NULL,
                            `lib_fonction` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `MISSION`
--

CREATE TABLE `MISSION` (
                           `id_mission` int NOT NULL,
                           `id_vehicule` int NOT NULL,
                           `lib_mission` varchar(100) DEFAULT NULL,
                           `site` varchar(100) DEFAULT NULL,
                           `date_debut_mission` datetime DEFAULT NULL,
                           `date_fin_mission` datetime DEFAULT NULL,
                           `km_prevu` int DEFAULT NULL,
                           `km_reel` int DEFAULT NULL,
                           `status` enum('Planifiee','EnCours','Cloturee') DEFAULT 'Planifiee',
                           `cout_total` decimal(12,2) DEFAULT NULL,
                           `circuit_mission` varchar(200) DEFAULT NULL,
                           `observation_mission` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déclencheurs `MISSION`
--
DELIMITER $$
CREATE TRIGGER `trg_mission_cloturee` AFTER UPDATE ON `MISSION` FOR EACH ROW BEGIN
    IF NEW.status = 'Cloturee' AND OLD.status <> 'Cloturee' THEN
    UPDATE VEHICULES
    SET km_actuels = NEW.km_reel
    WHERE id_vehicule = NEW.id_vehicule;
END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `MOUVEMENT`
--

CREATE TABLE `MOUVEMENT` (
                             `id` int NOT NULL,
                             `id_societaire` int NOT NULL,
                             `date` datetime NOT NULL,
                             `type` enum('Depot','Retrait','Mensualite') NOT NULL,
                             `montant` decimal(12,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `PERSONNEL`
--

CREATE TABLE `PERSONNEL` (
                             `id_personnel` int NOT NULL,
                             `id_service` int DEFAULT NULL,
                             `id_fonction` int DEFAULT NULL,
                             `id_vehicule` int DEFAULT NULL,
                             `matricule` varchar(20) DEFAULT NULL,
                             `nom_personnel` varchar(50) DEFAULT NULL,
                             `prenom_personnel` varchar(100) DEFAULT NULL,
                             `email` varchar(100) NOT NULL,
                             `telephone` varchar(15) DEFAULT NULL,
                             `adresse` varchar(150) DEFAULT NULL,
                             `date_naissance` date DEFAULT NULL,
                             `sexe` enum('M','F') DEFAULT NULL,
                             `date_attribution` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `SERVICE`
--

CREATE TABLE `SERVICE` (
                           `id_service` int NOT NULL,
                           `lib_service` varchar(30) DEFAULT NULL,
                           `localisation_service` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `SOCIETAIRE_COMPTE`
--

CREATE TABLE `SOCIETAIRE_COMPTE` (
                                     `id_societaire` int NOT NULL,
                                     `id_personnel` int DEFAULT NULL,
                                     `nom` varchar(80) NOT NULL,
                                     `numero` varchar(20) NOT NULL,
                                     `solde` decimal(12,2) NOT NULL DEFAULT '0.00',
                                     `email` varchar(100) DEFAULT NULL,
                                     `telephone` varchar(15) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `UTILISATEUR`
--

CREATE TABLE `UTILISATEUR` (
                               `id` int NOT NULL,
                               `login` varchar(50) NOT NULL,
                               `hash` varchar(255) NOT NULL,
                               `role` enum('U1','U2','U3','U4') NOT NULL,
                               `id_personnel` int DEFAULT NULL,
                               `mfa_secret` varchar(32) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Déchargement des données de la table `UTILISATEUR`
--

INSERT INTO `UTILISATEUR` (`id`, `login`, `hash`, `role`, `id_personnel`, `mfa_secret`) VALUES
                                                                                            (1, 'u1', '4upduFvZCn8tfcrB$mJ2ShUKO9hEVdayIcOQIrzJGpr3t0WNf+oLOoQ3/FSk=', 'U1', NULL, NULL),
                                                                                            (2, 'u2', 'Q0dtPGENPvC8UwBb$yTS+sALqMlCV0pt5V9erQjWHOLZVdIukzuORy92UXls=', 'U2', NULL, NULL),
                                                                                            (3, 'u3', 'Slun3Kds502Orhc5$9wxCH8bcel0RPXT2ryXX0M9CuOO8IeXNPzPBQmXA5qA=', 'U3', NULL, NULL),
                                                                                            (4, 'admin', '43IHYSGk7hmCeoh8$2at/Et2ROOLKSlELlbzqgnigneZNCGMsFNJNbXmBqMs=', 'U4', NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `VEHICULES`
--

CREATE TABLE `VEHICULES` (
                             `id_vehicule` int NOT NULL,
                             `id_etat_voiture` int NOT NULL,
                             `energie` enum('Diesel','Essence','Électrique','Hybride') NOT NULL DEFAULT 'Diesel',
                             `numero_chassi` varchar(20) DEFAULT NULL,
                             `immatriculation` varchar(20) DEFAULT NULL,
                             `marque` varchar(30) DEFAULT NULL,
                             `modele` varchar(30) DEFAULT NULL,
                             `nb_places` int DEFAULT NULL,
                             `date_acquisition` datetime DEFAULT NULL,
                             `date_ammortissement` datetime DEFAULT NULL,
                             `date_mise_en_service` datetime DEFAULT NULL,
                             `puissance` int DEFAULT NULL,
                             `couleur` varchar(20) DEFAULT NULL,
                             `prix_vehicule` decimal(12,2) DEFAULT NULL,
                             `km_actuels` int DEFAULT NULL,
                             `date_etat` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Doublure de structure pour la vue `v_TCO`
-- (Voir ci-dessous la vue réelle)
--
CREATE TABLE `v_TCO` (
                         `couts_totaux` decimal(36,2)
    ,`id_vehicule` int
    ,`km_actuels` int
);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `AFFECTATION`
--
ALTER TABLE `AFFECTATION`
    ADD PRIMARY KEY (`id`),
  ADD KEY `id_vehicule` (`id_vehicule`),
  ADD KEY `id_personnel` (`id_personnel`),
  ADD KEY `id_sociétaire` (`id_societaire`);

--
-- Index pour la table `ASSURANCE`
--
ALTER TABLE `ASSURANCE`
    ADD PRIMARY KEY (`num_carte_assurance`),
  ADD KEY `idx_fin_assurance` (`date_fin_assurance`);

--
-- Index pour la table `COUVRIR`
--
ALTER TABLE `COUVRIR`
    ADD PRIMARY KEY (`id_vehicule`,`num_carte_assurance`),
  ADD KEY `num_carte_assurance` (`num_carte_assurance`);

--
-- Index pour la table `DEPENSE_MISSION`
--
ALTER TABLE `DEPENSE_MISSION`
    ADD PRIMARY KEY (`id`),
  ADD KEY `id_mission` (`id_mission`);

--
-- Index pour la table `DOCUMENT_SOCIETAIRE`
--
ALTER TABLE `DOCUMENT_SOCIETAIRE`
    ADD PRIMARY KEY (`id_doc`),
  ADD KEY `id_sociétaire` (`id_societaire`);

--
-- Index pour la table `ENTRETIEN`
--
ALTER TABLE `ENTRETIEN`
    ADD PRIMARY KEY (`id_entretien`),
  ADD KEY `id_vehicule` (`id_vehicule`),
  ADD KEY `idx_sortie_entr` (`date_sortie_entr`);

--
-- Index pour la table `ETAT_VOITURE`
--
ALTER TABLE `ETAT_VOITURE`
    ADD PRIMARY KEY (`id_etat_voiture`),
  ADD UNIQUE KEY `lib_etat_voiture` (`lib_etat_voiture`);

--
-- Index pour la table `FONCTION`
--
ALTER TABLE `FONCTION`
    ADD PRIMARY KEY (`id_fonction`);

--
-- Index pour la table `MISSION`
--
ALTER TABLE `MISSION`
    ADD PRIMARY KEY (`id_mission`),
  ADD KEY `id_vehicule` (`id_vehicule`);

--
-- Index pour la table `MOUVEMENT`
--
ALTER TABLE `MOUVEMENT`
    ADD PRIMARY KEY (`id`),
  ADD KEY `id_sociétaire` (`id_societaire`);

--
-- Index pour la table `PERSONNEL`
--
ALTER TABLE `PERSONNEL`
    ADD PRIMARY KEY (`id_personnel`),
  ADD UNIQUE KEY `matricule` (`matricule`),
  ADD KEY `id_service` (`id_service`),
  ADD KEY `id_fonction` (`id_fonction`);

--
-- Index pour la table `SERVICE`
--
ALTER TABLE `SERVICE`
    ADD PRIMARY KEY (`id_service`);

--
-- Index pour la table `SOCIETAIRE_COMPTE`
--
ALTER TABLE `SOCIETAIRE_COMPTE`
    ADD PRIMARY KEY (`id_societaire`),
  ADD UNIQUE KEY `numero` (`numero`),
  ADD KEY `id_personnel` (`id_personnel`);

--
-- Index pour la table `UTILISATEUR`
--
ALTER TABLE `UTILISATEUR`
    ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `login` (`login`),
  ADD KEY `id_personnel` (`id_personnel`);

--
-- Index pour la table `VEHICULES`
--
ALTER TABLE `VEHICULES`
    ADD PRIMARY KEY (`id_vehicule`),
  ADD UNIQUE KEY `numero_chassi` (`numero_chassi`),
  ADD UNIQUE KEY `immatriculation` (`immatriculation`),
  ADD KEY `id_etat_voiture` (`id_etat_voiture`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `AFFECTATION`
--
ALTER TABLE `AFFECTATION`
    MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `ASSURANCE`
--
ALTER TABLE `ASSURANCE`
    MODIFY `num_carte_assurance` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `DEPENSE_MISSION`
--
ALTER TABLE `DEPENSE_MISSION`
    MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `DOCUMENT_SOCIETAIRE`
--
ALTER TABLE `DOCUMENT_SOCIETAIRE`
    MODIFY `id_doc` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `ENTRETIEN`
--
ALTER TABLE `ENTRETIEN`
    MODIFY `id_entretien` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `ETAT_VOITURE`
--
ALTER TABLE `ETAT_VOITURE`
    MODIFY `id_etat_voiture` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `FONCTION`
--
ALTER TABLE `FONCTION`
    MODIFY `id_fonction` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `MISSION`
--
ALTER TABLE `MISSION`
    MODIFY `id_mission` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `MOUVEMENT`
--
ALTER TABLE `MOUVEMENT`
    MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `PERSONNEL`
--
ALTER TABLE `PERSONNEL`
    MODIFY `id_personnel` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `SERVICE`
--
ALTER TABLE `SERVICE`
    MODIFY `id_service` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `SOCIETAIRE_COMPTE`
--
ALTER TABLE `SOCIETAIRE_COMPTE`
    MODIFY `id_societaire` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `UTILISATEUR`
--
ALTER TABLE `UTILISATEUR`
    MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `VEHICULES`
--
ALTER TABLE `VEHICULES`
    MODIFY `id_vehicule` int NOT NULL AUTO_INCREMENT;

-- --------------------------------------------------------

--
-- Structure de la vue `v_TCO`
--
DROP TABLE IF EXISTS `v_TCO`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `v_TCO`  AS SELECT `v`.`id_vehicule` AS `id_vehicule`, ((coalesce(sum(distinct `e`.`cout_entr`),0) + coalesce(sum(distinct `a`.`cout_assurance`),0)) + coalesce(sum(distinct `m`.`cout_total`),0)) AS `couts_totaux`, `v`.`km_actuels` AS `km_actuels` FROM ((((`VEHICULES` `v` left join `ENTRETIEN` `e` on((`e`.`id_vehicule` = `v`.`id_vehicule`))) left join `COUVRIR` `cv` on((`cv`.`id_vehicule` = `v`.`id_vehicule`))) left join `ASSURANCE` `a` on((`a`.`num_carte_assurance` = `cv`.`num_carte_assurance`))) left join `MISSION` `m` on((`m`.`id_vehicule` = `v`.`id_vehicule`))) GROUP BY `v`.`id_vehicule` ;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `AFFECTATION`
--
ALTER TABLE `AFFECTATION`
    ADD CONSTRAINT `AFFECTATION_ibfk_1` FOREIGN KEY (`id_vehicule`) REFERENCES `VEHICULES` (`id_vehicule`),
  ADD CONSTRAINT `AFFECTATION_ibfk_2` FOREIGN KEY (`id_personnel`) REFERENCES `PERSONNEL` (`id_personnel`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `AFFECTATION_ibfk_3` FOREIGN KEY (`id_societaire`) REFERENCES `SOCIETAIRE_COMPTE` (`id_societaire`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `COUVRIR`
--
ALTER TABLE `COUVRIR`
    ADD CONSTRAINT `COUVRIR_ibfk_1` FOREIGN KEY (`id_vehicule`) REFERENCES `VEHICULES` (`id_vehicule`),
  ADD CONSTRAINT `COUVRIR_ibfk_2` FOREIGN KEY (`num_carte_assurance`) REFERENCES `ASSURANCE` (`num_carte_assurance`);

--
-- Contraintes pour la table `DEPENSE_MISSION`
--
ALTER TABLE `DEPENSE_MISSION`
    ADD CONSTRAINT `DEPENSE_MISSION_ibfk_1` FOREIGN KEY (`id_mission`) REFERENCES `MISSION` (`id_mission`) ON DELETE CASCADE;

--
-- Contraintes pour la table `DOCUMENT_SOCIETAIRE`
--
ALTER TABLE `DOCUMENT_SOCIETAIRE`
    ADD CONSTRAINT `DOCUMENT_SOCIETAIRE_ibfk_1` FOREIGN KEY (`id_societaire`) REFERENCES `SOCIETAIRE_COMPTE` (`id_societaire`) ON DELETE CASCADE;

--
-- Contraintes pour la table `ENTRETIEN`
--
ALTER TABLE `ENTRETIEN`
    ADD CONSTRAINT `ENTRETIEN_ibfk_1` FOREIGN KEY (`id_vehicule`) REFERENCES `VEHICULES` (`id_vehicule`);

--
-- Contraintes pour la table `MISSION`
--
ALTER TABLE `MISSION`
    ADD CONSTRAINT `MISSION_ibfk_1` FOREIGN KEY (`id_vehicule`) REFERENCES `VEHICULES` (`id_vehicule`);

--
-- Contraintes pour la table `MOUVEMENT`
--
ALTER TABLE `MOUVEMENT`
    ADD CONSTRAINT `MOUVEMENT_ibfk_1` FOREIGN KEY (`id_societaire`) REFERENCES `SOCIETAIRE_COMPTE` (`id_societaire`) ON DELETE CASCADE;

--
-- Contraintes pour la table `PERSONNEL`
--
ALTER TABLE `PERSONNEL`
    ADD CONSTRAINT `PERSONNEL_ibfk_1` FOREIGN KEY (`id_service`) REFERENCES `SERVICE` (`id_service`),
  ADD CONSTRAINT `PERSONNEL_ibfk_2` FOREIGN KEY (`id_fonction`) REFERENCES `FONCTION` (`id_fonction`);

--
-- Contraintes pour la table `SOCIETAIRE_COMPTE`
--
ALTER TABLE `SOCIETAIRE_COMPTE`
    ADD CONSTRAINT `SOCIETAIRE_COMPTE_ibfk_1` FOREIGN KEY (`id_personnel`) REFERENCES `PERSONNEL` (`id_personnel`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `UTILISATEUR`
--
ALTER TABLE `UTILISATEUR`
    ADD CONSTRAINT `UTILISATEUR_ibfk_1` FOREIGN KEY (`id_personnel`) REFERENCES `PERSONNEL` (`id_personnel`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `VEHICULES`
--
ALTER TABLE `VEHICULES`
    ADD CONSTRAINT `VEHICULES_ibfk_1` FOREIGN KEY (`id_etat_voiture`) REFERENCES `ETAT_VOITURE` (`id_etat_voiture`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
