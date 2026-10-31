"""
Local dev seeder — mirrors LocalDataInitializer.java.
Run once after alembic upgrade head:
    python -m scripts.seed_local

Idempotent: exits early if squads already exist.
"""
import sys
import os

# Allow running from backend-py/ directory
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from dotenv import load_dotenv
load_dotenv(".env.local")

import bcrypt as _bcrypt
from app.database import Base, engine, SessionLocal
from app.models.user import AppUser, UserRole
from app.models.lookup import Technology, DeveloperRole, Tag
from app.models.squad import Squad
from app.models.person import Person
from app.models.allocation import Allocation


def seed():
    print("Creating tables if not exist...")
    Base.metadata.create_all(engine)

    db = SessionLocal()
    try:
        if db.query(Squad).count() > 0:
            print("Data already seeded, skipping.")
            return

        print("Seeding lookup tables...")
        technologies = [
            Technology(id=1, name="Angular"),
            Technology(id=2, name="C#"),
            Technology(id=3, name="CloudFormation"),
            Technology(id=4, name="Java"),
            Technology(id=5, name="JavaScript"),
            Technology(id=6, name="Kotlin"),
            Technology(id=7, name="PHP"),
            Technology(id=8, name="React"),
            Technology(id=9, name="TypeScript"),
        ]
        db.add_all(technologies)

        roles = [
            DeveloperRole(id=1, name="Back-end Developer"),
            DeveloperRole(id=2, name="Front-end Developer"),
            DeveloperRole(id=3, name="Platform Engineer"),
            DeveloperRole(id=4, name="Product Manager"),
            DeveloperRole(id=5, name="QA Engineer"),
            DeveloperRole(id=6, name="SDK Developer"),
            DeveloperRole(id=7, name="Sys Admin"),
            DeveloperRole(id=8, name="Tech Lead"),
        ]
        db.add_all(roles)

        tags = [
            Tag(id=1, name="Claude Code Pilot", slug="claude-code-pilot"),
            Tag(id=2, name="Contractor",        slug="contractor"),
            Tag(id=3, name="Missing Role",      slug="missing-role"),
            Tag(id=4, name="On Leave",          slug="on-leave"),
            Tag(id=5, name="Retiring",          slug="retiring"),
        ]
        db.add_all(tags)
        db.flush()

        print("Seeding squads...")
        squads = [
            Squad(id=1,  name="BrandMaster Legacy"),
            Squad(id=2,  name="BrandPortal"),
            Squad(id=3,  name="CircleK"),
            Squad(id=4,  name="Collaboration"),
            Squad(id=5,  name="DAM Keepeek"),
            Squad(id=6,  name="DAM run"),
            Squad(id=7,  name="ImageVault"),
            Squad(id=8,  name="Internal IT"),
            Squad(id=9,  name="Operations"),
            Squad(id=10, name="Peek"),
            Squad(id=11, name="Place"),
            Squad(id=12, name="Plan"),
            Squad(id=13, name="Platform"),
            Squad(id=14, name="Plug-Ins"),
            Squad(id=15, name="Point"),
            Squad(id=16, name="Produce"),
            Squad(id=17, name="Prof Services QA"),
            Squad(id=18, name="Prove"),
            Squad(id=19, name="Resolut"),
            Squad(id=20, name="Tactic"),
        ]
        db.add_all(squads)

        print("Seeding persons...")
        persons = [
            Person(id=1,  name="Bedřich Černošek"),
            Person(id=2,  name="Hans Christian Granum"),
            Person(id=3,  name="Jiří Pejla"),
            Person(id=4,  name="Per Nærland"),
            Person(id=5,  name="Sigmund Fuglestad"),
            Person(id=6,  name="Daniel Marhan"),
            Person(id=7,  name="Martin Hoferek"),
            Person(id=8,  name="Stanislav Juřica"),
            Person(id=9,  name="Gaetan Pierre"),
            Person(id=10, name="Hanane Tigrine"),
            Person(id=11, name="Svetlana Brosset"),
            Person(id=12, name="Tristan Danger"),
            Person(id=13, name="Claire Rousseau"),
            Person(id=14, name="Coralistone Metsa"),
            Person(id=15, name="Gregory Verissimo"),
            Person(id=16, name="Jean-Louis Collet"),
            Person(id=17, name="Kevin Salabert"),
            Person(id=18, name="Dan Handevik"),
            Person(id=19, name="Gordon Hopes"),
            Person(id=20, name="Hany Elsayed"),
            Person(id=21, name="Pierre Baptiste"),
            Person(id=22, name="Tomas Arotiounian"),
            Person(id=23, name="Bénédicte Muller"),
            Person(id=24, name="Benjamin Beaugrard"),
            Person(id=25, name="Geir Martin Alvestad"),
            Person(id=26, name="Harald Gramstad Lie"),
            Person(id=27, name="Petr Zámečník"),
            Person(id=28, name="Titouan Martin-Sevestre"),
            Person(id=29, name="Agustin Loria"),
            Person(id=30, name="Anesca Frappas"),
            Person(id=31, name="Benjamin Chené"),
            Person(id=32, name="Cédric Menou"),
            Person(id=33, name="Ibamar Ba"),
            Person(id=34, name="Maxime Dols"),
            Person(id=35, name="Pauline Rouvel"),
            Person(id=36, name="Valentin Quiblier"),
            Person(id=37, name="Vladimír Kočib"),
            Person(id=38, name="Wendy Simon"),
            Person(id=39, name="Denisa Komárková"),
            Person(id=40, name="Dominik Pollak"),
            Person(id=41, name="Martin Františák"),
            Person(id=42, name="Matúš Kotuľ"),
            Person(id=43, name="Melanie Duffy"),
            Person(id=44, name="Šimon Boškovič"),
            Person(id=45, name="Anders Sagnell"),
            Person(id=46, name="Diego Gonzales"),
            Person(id=47, name="Iva Kavánková"),
            Person(id=48, name="Marek Mitrík"),
            Person(id=49, name="Matěj Mazáč"),
            Person(id=50, name="Radek Janku"),
            Person(id=51, name="Temirkhan Amanzhanov"),
            Person(id=52, name="Tomas Nedved"),
            Person(id=53, name="Jiří Gajdušek"),
            Person(id=54, name="Jiří Valoušek"),
            Person(id=55, name="Jørgen Mølbach"),
            Person(id=56, name="Michal Bernátek"),
            Person(id=57, name="Petr Tihlarik"),
            Person(id=58, name="Sylvain Rosconi"),
            Person(id=59, name="Xavier Rimasson"),
            Person(id=60, name="Samuel Zigo"),
            Person(id=61, name="Jan Sedlář"),
            Person(id=62, name="Marek Jedlinský"),
            Person(id=63, name="Petr Zavadil"),
            Person(id=64, name="Roman Persun"),
            Person(id=65, name="Tomáš Doležal"),
            Person(id=66, name="Alf Magne Kalleland"),
            Person(id=67, name="Daniel Kvam"),
            Person(id=68, name="Ian Watkins"),
            Person(id=69, name="Ioannis Koutsotheodoros"),
            Person(id=70, name="Jan Rašek"),
            Person(id=71, name="Rumelya Borova"),
            Person(id=72, name="Simon Gittins"),
            Person(id=73, name="Sofia Bremin Leth"),
            Person(id=74, name="Tommy Jensen"),
            Person(id=75, name="David Šimák"),
            Person(id=76, name="Josef Širůčka"),
            Person(id=77, name="Roman Brhel"),
        ]
        db.add_all(persons)
        db.flush()

        # Person tags
        from app.models.person import person_tag
        from sqlalchemy import insert
        db.execute(insert(person_tag), [
            {"person_id": 18, "tag_id": 2},
            {"person_id": 30, "tag_id": 2},
            {"person_id": 74, "tag_id": 5},
        ])

        print("Seeding allocations...")
        from datetime import date

        def d(s): return date.fromisoformat(s) if s else None

        allocations = [
            Allocation(id=1,  person_id=1,  squad_id=1,  allocation_percent=10,  public_comment="Legacy: Marketingshop, Chili, TemplateGroups, My Creatives"),
            Allocation(id=2,  person_id=2,  squad_id=1,  allocation_percent=50,  public_comment="Legacy: Marketingshop, Chili, TemplateGroups, My Creatives"),
            Allocation(id=3,  person_id=3,  squad_id=1,  allocation_percent=50,  public_comment="Legacy: Marketingshop, Chili, TemplateGroups, My Creatives"),
            Allocation(id=4,  person_id=4,  squad_id=2,  allocation_percent=50),
            Allocation(id=5,  person_id=5,  squad_id=2,  allocation_percent=50),
            Allocation(id=6,  person_id=6,  squad_id=3,  allocation_percent=100),
            Allocation(id=7,  person_id=7,  squad_id=4,  allocation_percent=100),
            Allocation(id=8,  person_id=8,  squad_id=4,  allocation_percent=100),
            Allocation(id=9,  person_id=9,  squad_id=5,  allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=10, person_id=10, squad_id=5,  allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=11, person_id=11, squad_id=5,  allocation_percent=100),
            Allocation(id=12, person_id=12, squad_id=5,  allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=13, person_id=13, squad_id=6,  allocation_percent=100, public_comment="Fastlane: Back-Office, API, Refront"),
            Allocation(id=14, person_id=14, squad_id=6,  allocation_percent=100, public_comment="Fastlane: Refront, Keepicker"),
            Allocation(id=15, person_id=15, squad_id=6,  allocation_percent=100, public_comment="Fastlane: Back-Office, API"),
            Allocation(id=16, person_id=16, squad_id=6,  allocation_percent=100, public_comment="Support L2/L3"),
            Allocation(id=17, person_id=17, squad_id=6,  allocation_percent=100, public_comment="Lead of Support L2/L3 and Fastlane"),
            Allocation(id=18, person_id=18, squad_id=7,  allocation_percent=20,  public_comment="Contractor. Hourly billing."),
            Allocation(id=19, person_id=19, squad_id=7,  allocation_percent=100, end_date=d("2026-04-30")),
            Allocation(id=20, person_id=20, squad_id=8,  allocation_percent=30),
            Allocation(id=21, person_id=21, squad_id=8,  allocation_percent=30,  public_comment="Internal IT & Security & Device management"),
            Allocation(id=22, person_id=22, squad_id=8,  allocation_percent=100),
            Allocation(id=23, person_id=23, squad_id=9,  allocation_percent=100),
            Allocation(id=24, person_id=24, squad_id=9,  allocation_percent=60),
            Allocation(id=25, person_id=25, squad_id=9,  allocation_percent=100),
            Allocation(id=26, person_id=20, squad_id=9,  allocation_percent=30),
            Allocation(id=27, person_id=26, squad_id=9,  allocation_percent=0),
            Allocation(id=28, person_id=27, squad_id=9,  allocation_percent=50),
            Allocation(id=29, person_id=21, squad_id=9,  allocation_percent=100),
            Allocation(id=30, person_id=28, squad_id=9,  allocation_percent=100),
            Allocation(id=31, person_id=29, squad_id=10, allocation_percent=100),
            Allocation(id=32, person_id=30, squad_id=10, allocation_percent=100, public_comment="External contractor, until 31st March", end_date=d("2026-03-31")),
            Allocation(id=33, person_id=31, squad_id=10, allocation_percent=100),
            Allocation(id=34, person_id=32, squad_id=10, allocation_percent=100),
            Allocation(id=35, person_id=9,  squad_id=10, allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=36, person_id=33, squad_id=10, allocation_percent=100),
            Allocation(id=37, person_id=34, squad_id=10, allocation_percent=100),
            Allocation(id=38, person_id=35, squad_id=10, allocation_percent=100),
            Allocation(id=39, person_id=12, squad_id=10, allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=40, person_id=36, squad_id=10, allocation_percent=33),
            Allocation(id=41, person_id=37, squad_id=10, allocation_percent=100),
            Allocation(id=42, person_id=38, squad_id=10, allocation_percent=100),
            Allocation(id=43, person_id=1,  squad_id=11, allocation_percent=90),
            Allocation(id=44, person_id=39, squad_id=11, allocation_percent=100),
            Allocation(id=45, person_id=40, squad_id=11, allocation_percent=100),
            Allocation(id=46, person_id=41, squad_id=11, allocation_percent=100),
            Allocation(id=47, person_id=42, squad_id=11, allocation_percent=100),
            Allocation(id=48, person_id=43, squad_id=11, allocation_percent=50),
            Allocation(id=49, person_id=44, squad_id=11, allocation_percent=100, public_comment="Matus: not full time"),
            Allocation(id=50, person_id=45, squad_id=12, allocation_percent=50,  public_comment="Leave 31st March", end_date=d("2026-03-31")),
            Allocation(id=51, person_id=46, squad_id=12, allocation_percent=20),
            Allocation(id=52, person_id=47, squad_id=12, allocation_percent=100),
            Allocation(id=53, person_id=48, squad_id=12, allocation_percent=100),
            Allocation(id=54, person_id=49, squad_id=12, allocation_percent=100),
            Allocation(id=55, person_id=50, squad_id=12, allocation_percent=100, public_comment="Also covers Result and Collaboration"),
            Allocation(id=56, person_id=51, squad_id=12, allocation_percent=100),
            Allocation(id=57, person_id=52, squad_id=12, allocation_percent=100),
            Allocation(id=58, person_id=36, squad_id=12, allocation_percent=33),
            Allocation(id=59, person_id=53, squad_id=13, allocation_percent=100, public_comment="Mainly on migration"),
            Allocation(id=60, person_id=54, squad_id=13, allocation_percent=100),
            Allocation(id=61, person_id=55, squad_id=13, allocation_percent=100),
            Allocation(id=62, person_id=56, squad_id=13, allocation_percent=100),
            Allocation(id=63, person_id=57, squad_id=13, allocation_percent=100),
            Allocation(id=64, person_id=27, squad_id=13, allocation_percent=50,  public_comment="Also in Ops Team"),
            Allocation(id=65, person_id=58, squad_id=13, allocation_percent=20,  public_comment="Kpk: Internal DevOps Support, Kpk Cloud, Incident support"),
            Allocation(id=66, person_id=59, squad_id=13, allocation_percent=20,  public_comment="Kpk: Internal DevOps Support, Kpk Cloud, Incident support"),
            Allocation(id=67, person_id=60, squad_id=14, allocation_percent=65,  public_comment="Starting 08.12.2025"),
            Allocation(id=68, person_id=61, squad_id=15, allocation_percent=80),
            Allocation(id=69, person_id=62, squad_id=15, allocation_percent=100),
            Allocation(id=70, person_id=43, squad_id=15, allocation_percent=50),
            Allocation(id=71, person_id=63, squad_id=15, allocation_percent=80),
            Allocation(id=72, person_id=64, squad_id=15, allocation_percent=100),
            Allocation(id=73, person_id=65, squad_id=15, allocation_percent=100),
            Allocation(id=74, person_id=66, squad_id=16, allocation_percent=100),
            Allocation(id=75, person_id=67, squad_id=16, allocation_percent=100),
            Allocation(id=76, person_id=68, squad_id=16, allocation_percent=10),
            Allocation(id=77, person_id=69, squad_id=16, allocation_percent=100),
            Allocation(id=78, person_id=70, squad_id=16, allocation_percent=80),
            Allocation(id=79, person_id=4,  squad_id=16, allocation_percent=50),
            Allocation(id=80, person_id=71, squad_id=16, allocation_percent=100),
            Allocation(id=81, person_id=5,  squad_id=16, allocation_percent=50),
            Allocation(id=82, person_id=72, squad_id=16, allocation_percent=100),
            Allocation(id=83, person_id=73, squad_id=16, allocation_percent=95),
            Allocation(id=84, person_id=74, squad_id=16, allocation_percent=100, public_comment="Retiring summer 2026"),
            Allocation(id=85, person_id=9,  squad_id=17, allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=86, person_id=10, squad_id=17, allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=87, person_id=12, squad_id=17, allocation_percent=33,  public_comment="Automated tests: Gherkin, Bruno and manual tests"),
            Allocation(id=88, person_id=75, squad_id=18, allocation_percent=100),
            Allocation(id=89, person_id=2,  squad_id=18, allocation_percent=50),
            Allocation(id=90, person_id=70, squad_id=18, allocation_percent=20),
            Allocation(id=91, person_id=3,  squad_id=18, allocation_percent=20),
            Allocation(id=92, person_id=76, squad_id=18, allocation_percent=40),
            Allocation(id=93, person_id=45, squad_id=19, allocation_percent=50,  public_comment="Leave 31st March", end_date=d("2026-03-31")),
            Allocation(id=94, person_id=77, squad_id=20, allocation_percent=100, public_comment="Opportunity for ONE Run team?"),
            Allocation(id=95, person_id=73, squad_id=20, allocation_percent=5),
        ]
        db.add_all(allocations)
        db.flush()

        # Allocation → Roles
        from app.models.allocation import allocation_role
        role_data = [
            (1,1),(2,1),(2,8),(3,1),(4,1),(5,1),(6,1),(7,2),(8,1),
            (9,5),(10,5),(11,4),(12,5),
            (13,1),(13,2),(14,2),(15,1),(16,1),(17,8),
            (18,1),(18,2),(19,3),(20,7),(21,7),(22,7),
            (23,7),(24,7),(24,8),(25,7),(26,7),(27,7),(28,7),(29,7),(30,7),
            (31,2),(32,2),(33,1),(33,2),(34,1),(34,2),(34,8),(35,5),(36,1),
            (37,1),(37,2),(37,8),(38,2),(39,5),(40,1),(40,5),(40,8),(41,2),(42,4),
            (43,1),(44,5),(45,2),(46,2),(47,1),(47,8),(48,4),(49,2),
            (50,1),(51,8),(52,5),(53,2),(54,1),(55,4),(56,1),(57,1),(58,1),(58,5),
            (59,3),(60,3),(60,8),(61,3),(62,3),(63,1),(63,3),(64,3),(65,3),(66,3),
            (67,1),(68,2),(68,8),(69,1),(70,4),(71,1),(72,5),(73,2),
            (74,6),(75,6),(76,1),(77,6),(78,5),(79,1),(80,2),(81,1),(82,6),(83,4),(84,1),(84,8),
            (85,5),(86,5),(87,5),
            (88,1),(89,1),(89,8),(90,5),(91,1),(92,5),(93,1),(94,1),(95,4),
        ]
        db.execute(insert(allocation_role), [
            {"allocation_id": a, "developer_role_id": r} for a, r in role_data
        ])

        # Allocation → Technologies
        from app.models.allocation import allocation_technology
        tech_data = [
            (1,4),(1,6),(2,4),(2,6),(3,4),(3,6),(4,2),(5,2),(6,7),(7,1),(8,7),
            (9,9),(10,9),(12,9),
            (13,4),(13,8),(14,7),(14,8),(15,3),(15,4),(16,4),(17,4),(17,5),(17,9),
            (18,2),
            (31,1),(31,8),(32,1),(33,1),(33,8),(34,1),(34,4),(34,7),(34,8),
            (35,9),(36,4),(37,1),(37,4),(37,8),(38,1),(38,8),(39,9),(40,4),(41,1),
            (43,4),(43,6),(44,5),(45,1),(46,1),(47,4),(47,6),(49,1),
            (50,2),(51,2),(52,5),(53,1),(54,2),(56,2),(57,2),(58,4),
            (59,3),(60,3),(60,7),(61,3),(62,3),(62,4),(62,6),(63,3),(63,7),(64,3),
            (65,3),(65,4),(66,3),(66,4),(66,5),(67,2),
            (68,1),(69,7),(71,7),(72,5),(73,1),
            (74,9),(75,9),(77,9),(78,9),(79,2),(80,1),(81,2),(82,9),(84,2),
            (85,9),(86,9),(87,9),
            (88,4),(88,6),(89,4),(89,6),(90,5),(91,4),(91,6),(92,5),(93,2),(94,7),
        ]
        db.execute(insert(allocation_technology), [
            {"allocation_id": a, "technology_id": t} for a, t in tech_data
        ])

        print("Creating admin user...")
        if not db.query(AppUser).filter(AppUser.email == "admin@squadron.local").first():
            db.add(AppUser(
                email="admin@squadron.local",
                password_hash=_bcrypt.hashpw(b"admin123", _bcrypt.gensalt()).decode(),
                user_role=UserRole.ADMIN,
                active=True,
            ))

        db.commit()
        print("Seeding complete.")
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    seed()
