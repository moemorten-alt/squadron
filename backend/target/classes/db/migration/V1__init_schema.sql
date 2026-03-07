-- Squadron schema

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    user_role     VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE squad (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE person (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    active      BOOLEAN   NOT NULL DEFAULT TRUE,
    admin_note  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE technology (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE developer_role (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tag (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE person_tag (
    person_id BIGINT NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    tag_id    BIGINT NOT NULL REFERENCES tag(id)    ON DELETE CASCADE,
    PRIMARY KEY (person_id, tag_id)
);

CREATE TABLE allocation (
    id                 BIGSERIAL PRIMARY KEY,
    person_id          BIGINT  NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    squad_id           BIGINT  NOT NULL REFERENCES squad(id)  ON DELETE CASCADE,
    allocation_percent INTEGER NOT NULL CHECK (allocation_percent >= 0 AND allocation_percent <= 100),
    public_comment     TEXT,
    admin_note         TEXT,
    start_date         DATE,
    end_date           DATE,
    active             BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE allocation_role (
    allocation_id   BIGINT NOT NULL REFERENCES allocation(id)    ON DELETE CASCADE,
    developer_role_id BIGINT NOT NULL REFERENCES developer_role(id) ON DELETE CASCADE,
    PRIMARY KEY (allocation_id, developer_role_id)
);

CREATE TABLE allocation_technology (
    allocation_id BIGINT NOT NULL REFERENCES allocation(id)  ON DELETE CASCADE,
    technology_id BIGINT NOT NULL REFERENCES technology(id)  ON DELETE CASCADE,
    PRIMARY KEY (allocation_id, technology_id)
);

CREATE INDEX idx_allocation_person ON allocation(person_id);
CREATE INDEX idx_allocation_squad  ON allocation(squad_id);
CREATE INDEX idx_allocation_active ON allocation(active);
CREATE INDEX idx_person_active     ON person(active);
