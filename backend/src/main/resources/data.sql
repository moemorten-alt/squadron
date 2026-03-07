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
-- Seed: persons
-- ============================================================

INSERT INTO person (id, name, active) VALUES
(1,  'Bedřich Černošek',        TRUE),
(2,  'Hans Christian Granum',   TRUE),
(3,  'Jiří Pejla',              TRUE),
(4,  'Per Nærland',             TRUE),
(5,  'Sigmund Fuglestad',       TRUE),
(6,  'Daniel Marhan',           TRUE),
(7,  'Martin Hoferek',          TRUE),
(8,  'Stanislav Juřica',        TRUE),
(9,  'Gaetan Pierre',           TRUE),
(10, 'Hanane Tigrine',          TRUE),
(11, 'Svetlana Brosset',        TRUE),
(12, 'Tristan Danger',          TRUE),
(13, 'Claire Rousseau',         TRUE),
(14, 'Coralistone Metsa',       TRUE),
(15, 'Gregory Verissimo',       TRUE),
(16, 'Jean-Louis Collet',       TRUE),
(17, 'Kevin Salabert',          TRUE),
(18, 'Dan Handevik',            TRUE),
(19, 'Gordon Hopes',            TRUE),
(20, 'Hany Elsayed',            TRUE),
(21, 'Pierre Baptiste',         TRUE),
(22, 'Tomas Arotiounian',       TRUE),
(23, 'Bénédicte Muller',        TRUE),
(24, 'Benjamin Beaugrard',      TRUE),
(25, 'Geir Martin Alvestad',    TRUE),
(26, 'Harald Gramstad Lie',     TRUE),
(27, 'Petr Zámečník',           TRUE),
(28, 'Titouan Martin-Sevestre', TRUE),
(29, 'Agustin Loria',           TRUE),
(30, 'Anesca Frappas',          TRUE),
(31, 'Benjamin Chené',          TRUE),
(32, 'Cédric Menou',            TRUE),
(33, 'Ibamar Ba',               TRUE),
(34, 'Maxime Dols',             TRUE),
(35, 'Pauline Rouvel',          TRUE),
(36, 'Valentin Quiblier',       TRUE),
(37, 'Vladimír Kočib',          TRUE),
(38, 'Wendy Simon',             TRUE),
(39, 'Denisa Komárková',        TRUE),
(40, 'Dominik Pollak',          TRUE),
(41, 'Martin Františák',        TRUE),
(42, 'Matúš Kotuľ',             TRUE),
(43, 'Melanie Duffy',           TRUE),
(44, 'Šimon Boškovič',          TRUE),
(45, 'Anders Sagnell',          TRUE),
(46, 'Diego Gonzales',          TRUE),
(47, 'Iva Kavánková',           TRUE),
(48, 'Marek Mitrík',            TRUE),
(49, 'Matěj Mazáč',             TRUE),
(50, 'Radek Janku',             TRUE),
(51, 'Temirkhan Amanzhanov',    TRUE),
(52, 'Tomas Nedved',            TRUE),
(53, 'Jiří Gajdušek',           TRUE),
(54, 'Jiří Valoušek',           TRUE),
(55, 'Jørgen Mølbach',          TRUE),
(56, 'Michal Bernátek',         TRUE),
(57, 'Petr Tihlarik',           TRUE),
(58, 'Sylvain Rosconi',         TRUE),
(59, 'Xavier Rimasson',         TRUE),
(60, 'Samuel Zigo',             TRUE),
(61, 'Jan Sedlář',              TRUE),
(62, 'Marek Jedlinský',         TRUE),
(63, 'Petr Zavadil',            TRUE),
(64, 'Roman Persun',            TRUE),
(65, 'Tomáš Doležal',           TRUE),
(66, 'Alf Magne Kalleland',     TRUE),
(67, 'Daniel Kvam',             TRUE),
(68, 'Ian Watkins',             TRUE),
(69, 'Ioannis Koutsotheodoros', TRUE),
(70, 'Jan Rašek',               TRUE),
(71, 'Rumelya Borova',          TRUE),
(72, 'Simon Gittins',           TRUE),
(73, 'Sofia Bremin Leth',       TRUE),
(74, 'Tommy Jensen',            TRUE),
(75, 'David Šimák',             TRUE),
(76, 'Josef Širůčka',           TRUE),
(77, 'Roman Brhel',             TRUE);

INSERT INTO person_tag (person_id, tag_id) VALUES
(18, 2), (30, 2), (74, 5);

-- ============================================================
-- Seed: allocations
-- ============================================================

INSERT INTO allocation (id, person_id, squad_id, allocation_percent, public_comment, end_date, active) VALUES
(1,  1,  1, 10,  'Legacy: Marketingshop, Chili, TemplateGroups, My Creatives', NULL, TRUE),
(2,  2,  1, 50,  'Legacy: Marketingshop, Chili, TemplateGroups, My Creatives', NULL, TRUE),
(3,  3,  1, 50,  'Legacy: Marketingshop, Chili, TemplateGroups, My Creatives', NULL, TRUE),
(4,  4,  2, 50,  NULL, NULL, TRUE),
(5,  5,  2, 50,  NULL, NULL, TRUE),
(6,  6,  3, 100, NULL, NULL, TRUE),
(7,  7,  4, 100, NULL, NULL, TRUE),
(8,  8,  4, 100, NULL, NULL, TRUE),
(9,  9,  5, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(10, 10, 5, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(11, 11, 5, 100, NULL, NULL, TRUE),
(12, 12, 5, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(13, 13, 6, 100, 'Fastlane: Back-Office, API, Refront', NULL, TRUE),
(14, 14, 6, 100, 'Fastlane: Refront, Keepicker', NULL, TRUE),
(15, 15, 6, 100, 'Fastlane: Back-Office, API', NULL, TRUE),
(16, 16, 6, 100, 'Support L2/L3', NULL, TRUE),
(17, 17, 6, 100, 'Lead of Support L2/L3 and Fastlane', NULL, TRUE),
(18, 18, 7, 20,  'Contractor. Hourly billing.', NULL, TRUE),
(19, 19, 7, 100, NULL, '2026-04-30', TRUE),
(20, 20, 8, 30,  NULL, NULL, TRUE),
(21, 21, 8, 30,  'Internal IT & Security & Device management', NULL, TRUE),
(22, 22, 8, 100, NULL, NULL, TRUE),
(23, 23, 9, 100, NULL, NULL, TRUE),
(24, 24, 9, 60,  NULL, NULL, TRUE),
(25, 25, 9, 100, NULL, NULL, TRUE),
(26, 20, 9, 30,  NULL, NULL, TRUE),
(27, 26, 9, 0,   NULL, NULL, TRUE),
(28, 27, 9, 50,  NULL, NULL, TRUE),
(29, 21, 9, 100, NULL, NULL, TRUE),
(30, 28, 9, 100, NULL, NULL, TRUE),
(31, 29, 10, 100, NULL, NULL, TRUE),
(32, 30, 10, 100, 'External contractor, until 31st March', '2026-03-31', TRUE),
(33, 31, 10, 100, NULL, NULL, TRUE),
(34, 32, 10, 100, NULL, NULL, TRUE),
(35, 9,  10, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(36, 33, 10, 100, NULL, NULL, TRUE),
(37, 34, 10, 100, NULL, NULL, TRUE),
(38, 35, 10, 100, NULL, NULL, TRUE),
(39, 12, 10, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(40, 36, 10, 33,  NULL, NULL, TRUE),
(41, 37, 10, 100, NULL, NULL, TRUE),
(42, 38, 10, 100, NULL, NULL, TRUE),
(43, 1,  11, 90,  NULL, NULL, TRUE),
(44, 39, 11, 100, NULL, NULL, TRUE),
(45, 40, 11, 100, NULL, NULL, TRUE),
(46, 41, 11, 100, NULL, NULL, TRUE),
(47, 42, 11, 100, NULL, NULL, TRUE),
(48, 43, 11, 50,  NULL, NULL, TRUE),
(49, 44, 11, 100, 'Matus: not full time', NULL, TRUE),
(50, 45, 12, 50,  'Leave 31st March', '2026-03-31', TRUE),
(51, 46, 12, 20,  NULL, NULL, TRUE),
(52, 47, 12, 100, NULL, NULL, TRUE),
(53, 48, 12, 100, NULL, NULL, TRUE),
(54, 49, 12, 100, NULL, NULL, TRUE),
(55, 50, 12, 100, 'Also covers Result and Collaboration', NULL, TRUE),
(56, 51, 12, 100, NULL, NULL, TRUE),
(57, 52, 12, 100, NULL, NULL, TRUE),
(58, 36, 12, 33,  NULL, NULL, TRUE),
(59, 53, 13, 100, 'Mainly on migration', NULL, TRUE),
(60, 54, 13, 100, NULL, NULL, TRUE),
(61, 55, 13, 100, NULL, NULL, TRUE),
(62, 56, 13, 100, NULL, NULL, TRUE),
(63, 57, 13, 100, NULL, NULL, TRUE),
(64, 27, 13, 50,  'Also in Ops Team', NULL, TRUE),
(65, 58, 13, 20,  'Kpk: Internal DevOps Support, Kpk Cloud, Incident support', NULL, TRUE),
(66, 59, 13, 20,  'Kpk: Internal DevOps Support, Kpk Cloud, Incident support', NULL, TRUE),
(67, 60, 14, 65,  'Starting 08.12.2025', NULL, TRUE),
(68, 61, 15, 80,  NULL, NULL, TRUE),
(69, 62, 15, 100, NULL, NULL, TRUE),
(70, 43, 15, 50,  NULL, NULL, TRUE),
(71, 63, 15, 80,  NULL, NULL, TRUE),
(72, 64, 15, 100, NULL, NULL, TRUE),
(73, 65, 15, 100, NULL, NULL, TRUE),
(74, 66, 16, 100, NULL, NULL, TRUE),
(75, 67, 16, 100, NULL, NULL, TRUE),
(76, 68, 16, 10,  NULL, NULL, TRUE),
(77, 69, 16, 100, NULL, NULL, TRUE),
(78, 70, 16, 80,  NULL, NULL, TRUE),
(79, 4,  16, 50,  NULL, NULL, TRUE),
(80, 71, 16, 100, NULL, NULL, TRUE),
(81, 5,  16, 50,  NULL, NULL, TRUE),
(82, 72, 16, 100, NULL, NULL, TRUE),
(83, 73, 16, 95,  NULL, NULL, TRUE),
(84, 74, 16, 100, 'Retiring summer 2026', NULL, TRUE),
(85, 9,  17, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(86, 10, 17, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(87, 12, 17, 33,  'Automated tests: Gherkin, Bruno and manual tests', NULL, TRUE),
(88, 75, 18, 100, NULL, NULL, TRUE),
(89, 2,  18, 50,  NULL, NULL, TRUE),
(90, 70, 18, 20,  NULL, NULL, TRUE),
(91, 3,  18, 20,  NULL, NULL, TRUE),
(92, 76, 18, 40,  NULL, NULL, TRUE),
(93, 45, 19, 50,  'Leave 31st March', '2026-03-31', TRUE),
(94, 77, 20, 100, 'Opportunity for ONE Run team?', NULL, TRUE),
(95, 73, 20, 5,   NULL, NULL, TRUE);

-- ============================================================
-- Allocation → Roles
-- ============================================================

INSERT INTO allocation_role (allocation_id, developer_role_id) VALUES
(1, 1),(2, 1),(2, 8),(3, 1),
(4, 1),(5, 1),
(6, 1),
(7, 2),(8, 1),
(9, 5),(10, 5),(11, 4),(12, 5),
(13, 1),(13, 2),(14, 2),(15, 1),(16, 1),(17, 8),
(18, 1),(18, 2),(19, 3),
(20, 7),(21, 7),(22, 7),
(23, 7),(24, 7),(24, 8),(25, 7),(26, 7),(27, 7),(28, 7),(29, 7),(30, 7),
(31, 2),(32, 2),(33, 1),(33, 2),(34, 1),(34, 2),(34, 8),
(35, 5),(36, 1),(37, 1),(37, 2),(37, 8),(38, 2),(39, 5),
(40, 1),(40, 5),(40, 8),(41, 2),(42, 4),
(43, 1),(44, 5),(45, 2),(46, 2),(47, 1),(47, 8),(48, 4),(49, 2),
(50, 1),(51, 8),(52, 5),(53, 2),(54, 1),(55, 4),(56, 1),(57, 1),(58, 1),(58, 5),
(59, 3),(60, 3),(60, 8),(61, 3),(62, 3),(63, 1),(63, 3),(64, 3),(65, 3),(66, 3),
(67, 1),
(68, 2),(68, 8),(69, 1),(70, 4),(71, 1),(72, 5),(73, 2),
(74, 6),(75, 6),(76, 1),(77, 6),(78, 5),(79, 1),(80, 2),(81, 1),(82, 6),(83, 4),(84, 1),(84, 8),
(85, 5),(86, 5),(87, 5),
(88, 1),(89, 1),(89, 8),(90, 5),(91, 1),(92, 5),
(93, 1),
(94, 1),(95, 4);

-- ============================================================
-- Allocation → Technologies
-- ============================================================

INSERT INTO allocation_technology (allocation_id, technology_id) VALUES
(1, 4),(1, 6),(2, 4),(2, 6),(3, 4),(3, 6),
(4, 2),(5, 2),
(6, 7),
(7, 1),(8, 7),
(9, 9),(10, 9),(12, 9),
(13, 4),(13, 8),(14, 7),(14, 8),(15, 3),(15, 4),(16, 4),(17, 4),(17, 5),(17, 9),
(18, 2),
(31, 1),(31, 8),(32, 1),(33, 1),(33, 8),(34, 1),(34, 4),(34, 7),(34, 8),
(35, 9),(36, 4),(37, 1),(37, 4),(37, 8),(38, 1),(38, 8),(39, 9),
(40, 4),(41, 1),
(43, 4),(43, 6),(44, 5),(45, 1),(46, 1),(47, 4),(47, 6),(49, 1),
(50, 2),(51, 2),(52, 5),(53, 1),(54, 2),(56, 2),(57, 2),(58, 4),
(59, 3),(60, 3),(60, 7),(61, 3),(62, 3),(62, 4),(62, 6),(63, 3),(63, 7),(64, 3),
(65, 3),(65, 4),(66, 3),(66, 4),(66, 5),
(67, 2),
(68, 1),(69, 7),(71, 7),(72, 5),(73, 1),
(74, 9),(75, 9),(77, 9),(78, 9),(79, 2),(80, 1),(81, 2),(82, 9),(84, 2),
(85, 9),(86, 9),(87, 9),
(88, 4),(88, 6),(89, 4),(89, 6),(90, 5),(91, 4),(91, 6),(92, 5),
(93, 2),
(94, 7);

-- Admin user is created programmatically by LocalDataInitializer (BCrypt hash generated at runtime)

-- Advance H2 identity sequences past the seeded IDs so new inserts don't conflict
ALTER TABLE technology     ALTER COLUMN id RESTART WITH 10;
ALTER TABLE developer_role ALTER COLUMN id RESTART WITH 9;
ALTER TABLE tag            ALTER COLUMN id RESTART WITH 6;
ALTER TABLE squad          ALTER COLUMN id RESTART WITH 21;
ALTER TABLE person         ALTER COLUMN id RESTART WITH 78;
ALTER TABLE allocation     ALTER COLUMN id RESTART WITH 96;
