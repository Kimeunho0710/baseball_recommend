-- ============================================================
-- V1: 초기 스키마 (기존 ddl-auto:update 로 생성되던 스키마의 baseline)
--  * 기존 운영 DB에는 flyway.baseline-on-migrate=true 로 인해 적용되지 않고 baseline 처리됨
--  * 신규 DB(AWS RDS 등)에는 이 스크립트가 그대로 실행됨
-- ============================================================

CREATE TABLE team (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255) NOT NULL,
    city            VARCHAR(255) NOT NULL,
    stadium         VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    characteristics TEXT         NOT NULL,
    primary_color   VARCHAR(255) NOT NULL,
    logo_url        VARCHAR(255),
    founded_year    INTEGER,
    championships   INTEGER,
    mascot          VARCHAR(255),
    manager         VARCHAR(255),
    beginner_guide  TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_team_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE player (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    team_id       BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    position      VARCHAR(255) NOT NULL,
    jersey_number INTEGER,
    description   TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_player_team FOREIGN KEY (team_id) REFERENCES team (id)
) ENGINE=InnoDB;

CREATE TABLE member (
    id         BIGINT                NOT NULL AUTO_INCREMENT,
    email      VARCHAR(100)          NOT NULL,
    password   VARCHAR(255)          NOT NULL,
    nickname   VARCHAR(50)           NOT NULL,
    role       ENUM('USER','ADMIN')  NOT NULL,
    created_at DATETIME(6)           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE refresh_token (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_member UNIQUE (member_id),
    CONSTRAINT uk_refresh_token_token  UNIQUE (token),
    CONSTRAINT fk_refresh_token_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;

CREATE TABLE survey_result (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    answers    TEXT        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE recommend_result (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    survey_result_id        BIGINT      NOT NULL,
    team_id                 BIGINT      NOT NULL,
    member_id               BIGINT,
    reason                  TEXT        NOT NULL,
    top3json                TEXT,
    fan_profile             VARCHAR(50),
    fan_profile_description TEXT,
    created_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recommend_result_team   FOREIGN KEY (team_id)   REFERENCES team (id),
    CONSTRAINT fk_recommend_result_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;

CREATE INDEX idx_recommend_result_member_created ON recommend_result (member_id, created_at);

CREATE TABLE standing (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    season       INTEGER      NOT NULL,
    team_rank    INTEGER      NOT NULL,
    team_name    VARCHAR(255) NOT NULL,
    games        INTEGER      NOT NULL,
    wins         INTEGER      NOT NULL,
    losses       INTEGER      NOT NULL,
    draws        INTEGER      NOT NULL,
    win_rate     DOUBLE       NOT NULL,
    games_behind VARCHAR(255),
    streak       VARCHAR(255),
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_standing_season_team ON standing (season, team_name);

CREATE TABLE game (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    game_date  VARCHAR(255) NOT NULL,
    game_time  VARCHAR(255),
    away_team  VARCHAR(255) NOT NULL,
    home_team  VARCHAR(255) NOT NULL,
    away_score INTEGER,
    home_score INTEGER,
    stadium    VARCHAR(255),
    completed  BIT          NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_game_date_away_home UNIQUE (game_date, away_team, home_team)
) ENGINE=InnoDB;

CREATE INDEX idx_game_date ON game (game_date);
