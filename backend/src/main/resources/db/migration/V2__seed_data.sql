-- ============================================================
-- Seed: lookup tables
-- ============================================================

INSERT INTO technology (id, name) VALUES
(1, 'Angular'), (2, 'C#'), (3, 'CloudFormation'), (4, 'Java'),
(5, 'JavaScript'), (6, 'Kotlin'), (7, 'PHP'), (8, 'React'),
(9, 'TypeScript');

INSERT INTO developer_role (id, name) VALUES
(1, 'Back-end Developer'), (2, 'Front-end Developer'), (3, 'Platform Engineer'),
(4, 'Product Manager'),   (5, 'QA Engineer'),         (6, 'SDK Developer'),
(7, 'Sys Admin'),         (8, 'Tech Lead');

INSERT INTO tag (id, name, slug) VALUES
(1, 'Claude Code Pilot',  'claude-code-pilot'),
(2, 'Contractor',         'contractor'),
(3, 'Missing Role',       'missing-role'),
(4, 'On Leave',           'on-leave'),
(5, 'Retiring',           'retiring');

-- ============================================================
-- Seed: squads
-- ============================================================

INSERT INTO squad (id, name) VALUES
(1,  'BrandMaster Legacy'),
(2,  'BrandPortal'),
(3,  'CircleK'),
(4,  'Collaboration'),
(5,  'DAM Keepeek'),
(6,  'DAM run'),
(7,  'ImageVault'),
(8,  'Internal IT'),
(9,  'Operations'),
(10, 'Peek'),
(11, 'Place'),
(12, 'Plan'),
(13, 'Platform'),
(14, 'Plug-Ins'),
(15, 'Point'),
(16, 'Produce'),
(17, 'Prof Services QA'),
(18, 'Prove'),
(19, 'Resolut'),
(20, 'Tactic');

-- ============================================================
-- Seed: persons (77 unique people)
-- ============================================================

INSERT INTO person (id, name) VALUES
(1,  'Bedřich Černošek'),
(2,  'Hans Christian Granum'),
(3,  'Jiří Pejla'),
(4,  'Per Nærland'),
(5,  'Sigmund Fuglestad'),
(6,  'Daniel Marhan'),
(7,  'Martin Hoferek'),
(8,  'Stanislav Juřica'),
(9,  'Gaetan Pierre'),
(10, 'Hanane Tigrine'),
(11, 'Svetlana Brosset'),
(12, 'Tristan Danger'),
(13, 'Claire Rousseau'),
(14, 'Coralistone Metsa'),
(15, 'Gregory Verissimo'),
(16, 'Jean-Louis Collet'),
(17, 'Kevin Salabert'),
(18, 'Dan Handevik'),
(19, 'Gordon Hopes'),
(20, 'Hany Elsayed'),
(21, 'Pierre Baptiste'),
(22, 'Tomas Arotiounian'),
(23, 'Bénédicte Muller'),
(24, 'Benjamin Beaugrard'),
(25, 'Geir Martin Alvestad'),
(26, 'Harald Gramstad Lie'),
(27, 'Petr Zámečník'),
(28, 'Titouan Martin-Sevestre'),
(29, 'Agustin Loria'),
(30, 'Anesca Frappas'),
(31, 'Benjamin Chené'),
(32, 'Cédric Menou'),
(33, 'Ibamar Ba'),
(34, 'Maxime Dols'),
(35, 'Pauline Rouvel'),
(36, 'Valentin Quiblier'),
(37, 'Vladimír Kočib'),
(38, 'Wendy Simon'),
(39, 'Denisa Komárková'),
(40, 'Dominik Pollak'),
(41, 'Martin Františák'),
(42, 'Matúš Kotuľ'),
(43, 'Melanie Duffy'),
(44, 'Šimon Boškovič'),
(45, 'Anders Sagnell'),
(46, 'Diego Gonzales'),
(47, 'Iva Kavánková'),
(48, 'Marek Mitrík'),
(49, 'Matěj Mazáč'),
(50, 'Radek Janku'),
(51, 'Temirkhan Amanzhanov'),
(52, 'Tomas Nedved'),
(53, 'Jiří Gajdušek'),
(54, 'Jiří Valoušek'),
(55, 'Jørgen Mølbach'),
(56, 'Michal Bernátek'),
(57, 'Petr Tihlarik'),
(58, 'Sylvain Rosconi'),
(59, 'Xavier Rimasson'),
(60, 'Samuel Zigo'),
(61, 'Jan Sedlář'),
(62, 'Marek Jedlinský'),
(63, 'Petr Zavadil'),
(64, 'Roman Persun'),
(65, 'Tomáš Doležal'),
(66, 'Alf Magne Kalleland'),
(67, 'Daniel Kvam'),
(68, 'Ian Watkins'),
(69, 'Ioannis Koutsotheodoros'),
(70, 'Jan Rašek'),
(71, 'Rumelya Borova'),
(72, 'Simon Gittins'),
(73, 'Sofia Bremin Leth'),
(74, 'Tommy Jensen'),
(75, 'David Šimák'),
(76, 'Josef Širůčka'),
(77, 'Roman Brhel');

-- Tags for specific persons
INSERT INTO person_tag (person_id, tag_id) VALUES
(18, 2),   -- Dan Handevik: Contractor
(30, 2),   -- Anesca Frappas: Contractor (ext)
(74, 5);   -- Tommy Jensen: Retiring

-- ============================================================
-- Seed: allocations
-- id, person_id, squad_id, allocation_percent, public_comment, end_date
-- ============================================================

INSERT INTO allocation (id, person_id, squad_id, allocation_percent, public_comment, end_date) VALUES
-- BrandMaster Legacy
(1,  1,  1, 10,  'Legacy: Marketingshop, Chili, TemplateGroups, My Creatives', NULL),
(2,  2,  1, 50,  'Legacy: Marketingshop, Chili, TemplateGroups, My Creatives', NULL),
(3,  3,  1, 50,  'Legacy: Marketingshop, Chili, TemplateGroups, My Creatives', NULL),
-- BrandPortal
(4,  4,  2, 50,  NULL, NULL),
(5,  5,  2, 50,  NULL, NULL),
-- CircleK
(6,  6,  3, 100, NULL, NULL),
-- Collaboration
(7,  7,  4, 100, NULL, NULL),
(8,  8,  4, 100, NULL, NULL),
-- DAM Keepeek
(9,  9,  5, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
(10, 10, 5, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
(11, 11, 5, 100, NULL, NULL),
(12, 12, 5, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
-- DAM run
(13, 13, 6, 100, 'Fastlane: Back-Office, API, Refront', NULL),
(14, 14, 6, 100, 'Fastlane: Refront, Keepicker', NULL),
(15, 15, 6, 100, 'Fastlane: Back-Office, API', NULL),
(16, 16, 6, 100, 'Support L2/L3', NULL),
(17, 17, 6, 100, 'Lead of Support L2/L3 and Fastlane', NULL),
-- ImageVault
(18, 18, 7, 20,  'Contractor. Hourly billing.', NULL),
(19, 19, 7, 100, NULL, '2026-04-30'),
-- Internal IT
(20, 20, 8, 30,  NULL, NULL),
(21, 21, 8, 30,  'Internal IT & Security & Device management', NULL),
(22, 22, 8, 100, NULL, NULL),
-- Operations
(23, 23, 9, 100, NULL, NULL),
(24, 24, 9, 60,  NULL, NULL),
(25, 25, 9, 100, NULL, NULL),
(26, 20, 9, 30,  NULL, NULL),
(27, 26, 9, 0,   NULL, NULL),
(28, 27, 9, 50,  NULL, NULL),
(29, 21, 9, 100, NULL, NULL),
(30, 28, 9, 100, NULL, NULL),
-- Peek
(31, 29, 10, 100, NULL, NULL),
(32, 30, 10, 100, 'External contractor, until 31st March', '2026-03-31'),
(33, 31, 10, 100, NULL, NULL),
(34, 32, 10, 100, NULL, NULL),
(35, 9,  10, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
(36, 33, 10, 100, NULL, NULL),
(37, 34, 10, 100, NULL, NULL),
(38, 35, 10, 100, NULL, NULL),
(39, 12, 10, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
(40, 36, 10, 33,  NULL, NULL),
(41, 37, 10, 100, NULL, NULL),
(42, 38, 10, 100, NULL, NULL),
-- Place
(43, 1,  11, 90,  NULL, NULL),
(44, 39, 11, 100, NULL, NULL),
(45, 40, 11, 100, NULL, NULL),
(46, 41, 11, 100, NULL, NULL),
(47, 42, 11, 100, NULL, NULL),
(48, 43, 11, 50,  NULL, NULL),
(49, 44, 11, 100, 'Matus: not full time', NULL),
-- Plan
(50, 45, 12, 50,  'Leave 31st March', '2026-03-31'),
(51, 46, 12, 20,  NULL, NULL),
(52, 47, 12, 100, NULL, NULL),
(53, 48, 12, 100, NULL, NULL),
(54, 49, 12, 100, NULL, NULL),
(55, 50, 12, 100, 'Also covers Result and Collaboration', NULL),
(56, 51, 12, 100, NULL, NULL),
(57, 52, 12, 100, NULL, NULL),
(58, 36, 12, 33,  NULL, NULL),
-- Platform
(59, 53, 13, 100, 'Mainly on migration', NULL),
(60, 54, 13, 100, NULL, NULL),
(61, 55, 13, 100, NULL, NULL),
(62, 56, 13, 100, NULL, NULL),
(63, 57, 13, 100, NULL, NULL),
(64, 27, 13, 50,  'Also in Ops Team', NULL),
(65, 58, 13, 20,  'Kpk: Internal DevOps Support, Kpk Cloud, Incident support', NULL),
(66, 59, 13, 20,  'Kpk: Internal DevOps Support, Kpk Cloud, Incident support', NULL),
-- Plug-Ins
(67, 60, 14, 65,  'Starting 08.12.2025', NULL),
-- Point
(68, 61, 15, 80,  NULL, NULL),
(69, 62, 15, 100, NULL, NULL),
(70, 43, 15, 50,  NULL, NULL),
(71, 63, 15, 80,  NULL, NULL),
(72, 64, 15, 100, NULL, NULL),
(73, 65, 15, 100, NULL, NULL),
-- Produce
(74, 66, 16, 100, NULL, NULL),
(75, 67, 16, 100, NULL, NULL),
(76, 68, 16, 10,  NULL, NULL),
(77, 69, 16, 100, NULL, NULL),
(78, 70, 16, 80,  NULL, NULL),
(79, 4,  16, 50,  NULL, NULL),
(80, 71, 16, 100, NULL, NULL),
(81, 5,  16, 50,  NULL, NULL),
(82, 72, 16, 100, NULL, NULL),
(83, 73, 16, 95,  NULL, NULL),
(84, 74, 16, 100, 'Retiring summer 2026', NULL),
-- Prof Services QA
(85, 9,  17, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
(86, 10, 17, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
(87, 12, 17, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL),
-- Prove
(88, 75, 18, 100, NULL, NULL),
(89, 2,  18, 50,  NULL, NULL),
(90, 70, 18, 20,  NULL, NULL),
(91, 3,  18, 20,  NULL, NULL),
(92, 76, 18, 40,  NULL, NULL),
-- Resolut
(93, 45, 19, 50,  'Leave 31st March', '2026-03-31'),
-- Tactic
(94, 77, 20, 100, 'Opportunity for ONE Run team?', NULL),
(95, 73, 20, 5,   NULL, NULL);

-- ============================================================
-- Allocation → Developer Roles
-- ============================================================

INSERT INTO allocation_role (allocation_id, developer_role_id) VALUES
-- BrandMaster Legacy
(1, 1),(2, 1),(2, 8),(3, 1),
-- BrandPortal
(4, 1),(5, 1),
-- CircleK
(6, 1),
-- Collaboration
(7, 2),(8, 1),
-- DAM Keepeek
(9, 5),(10, 5),(11, 4),(12, 5),
-- DAM run
(13, 1),(13, 2),(14, 2),(15, 1),(16, 1),(17, 8),
-- ImageVault
(18, 1),(18, 2),(19, 3),
-- Internal IT
(20, 7),(21, 7),(22, 7),
-- Operations
(23, 7),(24, 7),(24, 8),(25, 7),(26, 7),(27, 7),(28, 7),(29, 7),(30, 7),
-- Peek
(31, 2),(32, 2),(33, 1),(33, 2),(34, 1),(34, 2),(34, 8),
(35, 5),(36, 1),(37, 1),(37, 2),(37, 8),(38, 2),(39, 5),
(40, 1),(40, 5),(40, 8),(41, 2),(42, 4),
-- Place
(43, 1),(44, 5),(45, 2),(46, 2),(47, 1),(47, 8),(48, 4),(49, 2),
-- Plan
(50, 1),(51, 8),(52, 5),(53, 2),(54, 1),(55, 4),(56, 1),(57, 1),(58, 1),(58, 5),
-- Platform
(59, 3),(60, 3),(60, 8),(61, 3),(62, 3),(63, 1),(63, 3),(64, 3),(65, 3),(66, 3),
-- Plug-Ins
(67, 1),
-- Point
(68, 2),(68, 8),(69, 1),(70, 4),(71, 1),(72, 5),(73, 2),
-- Produce
(74, 6),(75, 6),(76, 1),(77, 6),(78, 5),(79, 1),(80, 2),(81, 1),(82, 6),(83, 4),(84, 1),(84, 8),
-- Prof Services QA
(85, 5),(86, 5),(87, 5),
-- Prove
(88, 1),(89, 1),(89, 8),(90, 5),(91, 1),(92, 5),
-- Resolut
(93, 1),
-- Tactic
(94, 1),(95, 4);

-- ============================================================
-- Allocation → Technologies
-- ============================================================

INSERT INTO allocation_technology (allocation_id, technology_id) VALUES
-- BrandMaster Legacy (Java=4, Kotlin=6)
(1, 4),(1, 6),(2, 4),(2, 6),(3, 4),(3, 6),
-- BrandPortal (C#=2)
(4, 2),(5, 2),
-- CircleK (PHP=7)
(6, 7),
-- Collaboration (Angular=1, PHP=7)
(7, 1),(8, 7),
-- DAM Keepeek (TypeScript=9)
(9, 9),(10, 9),(12, 9),
-- DAM run
(13, 4),(13, 8),(14, 7),(14, 8),(15, 3),(15, 4),(16, 4),(17, 4),(17, 5),(17, 9),
-- ImageVault (C#=2)
(18, 2),
-- Peek
(31, 1),(31, 8),(32, 1),(33, 1),(33, 8),(34, 1),(34, 4),(34, 7),(34, 8),
(35, 9),(36, 4),(37, 1),(37, 4),(37, 8),(38, 1),(38, 8),(39, 9),
(40, 4),(41, 1),
-- Place (Angular=1, Java=4, Kotlin=6, JavaScript=5)
(43, 4),(43, 6),(44, 5),(45, 1),(46, 1),(47, 4),(47, 6),(49, 1),
-- Plan (C#=2, JavaScript=5, Angular=1, Java=4)
(50, 2),(51, 2),(52, 5),(53, 1),(54, 2),(56, 2),(57, 2),(58, 4),
-- Platform (CloudFormation=3, PHP=7, Java=4, Kotlin=6, JavaScript=5)
(59, 3),(60, 3),(60, 7),(61, 3),(62, 3),(62, 4),(62, 6),(63, 3),(63, 7),(64, 3),
(65, 3),(65, 4),(66, 3),(66, 4),(66, 5),
-- Plug-Ins (C#=2)
(67, 2),
-- Point (Angular=1, PHP=7, JavaScript=5)
(68, 1),(69, 7),(71, 7),(72, 5),(73, 1),
-- Produce (TypeScript=9, C#=2, Angular=1)
(74, 9),(75, 9),(77, 9),(78, 9),(79, 2),(80, 1),(81, 2),(82, 9),(84, 2),
-- Prof Services QA (TypeScript=9)
(85, 9),(86, 9),(87, 9),
-- Prove (Java=4, Kotlin=6, JavaScript=5)
(88, 4),(88, 6),(89, 4),(89, 6),(90, 5),(91, 4),(91, 6),(92, 5),
-- Resolut (C#=2)
(93, 2),
-- Tactic (PHP=7)
(94, 7);

-- ============================================================
-- Seed: default admin user
-- Password: admin123  (bcrypt hash)
-- CHANGE THIS before deploying to production!
-- ============================================================

INSERT INTO app_user (email, password_hash, user_role) VALUES
('admin@squadron.local', '$2a$12$LjbIb7lSOnx7mrVCjJMgxuqLJYM2WOoKzv4RGY0MLMkNTsIJsUSqC', 'ADMIN');

-- Reset sequences to avoid PK conflicts when inserting new records
SELECT setval('technology_id_seq',     (SELECT MAX(id) FROM technology));
SELECT setval('developer_role_id_seq', (SELECT MAX(id) FROM developer_role));
SELECT setval('tag_id_seq',            (SELECT MAX(id) FROM tag));
SELECT setval('squad_id_seq',          (SELECT MAX(id) FROM squad));
SELECT setval('person_id_seq',         (SELECT MAX(id) FROM person));
SELECT setval('allocation_id_seq',     (SELECT MAX(id) FROM allocation));
SELECT setval('app_user_id_seq',       (SELECT MAX(id) FROM app_user));
