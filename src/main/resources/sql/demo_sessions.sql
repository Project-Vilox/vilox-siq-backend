CREATE TABLE IF NOT EXISTS demo_sessions (
    id           CHAR(36)     PRIMARY KEY,
    viaje_id     CHAR(36)     NOT NULL,
    imei         VARCHAR(50)  NOT NULL,
    route_points JSON         NOT NULL,
    cursor       INT          NOT NULL DEFAULT 0,
    interval_sec INT          NOT NULL DEFAULT 30,
    status       VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
