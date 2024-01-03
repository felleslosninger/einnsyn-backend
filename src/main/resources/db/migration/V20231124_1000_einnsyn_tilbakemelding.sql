/* Tilbakemelding */
CREATE SEQUENCE IF NOT EXISTS tilbakemelding_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS tilbakemelding(
    tilbakemelding_id INT PRIMARY KEY DEFAULT nextval('tilbakemelding_seq'),
    _id TEXT DEFAULT einnsyn_id('tbm'),
    _external_id TEXT,
    _created TIMESTAMP DEFAULT now(),
    _updated TIMESTAMP DEFAULT now(),
    journalenhet_id UUID,
    lock_version BIGINT NOT NULL DEFAULT 1,
    message_from_user TEXT,
    path TEXT,
    referer TEXT,
    user_agent TEXT,
    screen_height INT,
    screen_width INT,
    doc_height INT,
    doc_width INT,
    win_height INT,
    win_width INT,
    scroll_x INT,
    scroll_y INT,
    user_satisfied BOOLEAN,
    handled_by_admin BOOLEAN,
    admin_comment TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS tilbakemelding_tilbakemelding_id_idx ON tilbakemelding (tilbakemelding_id);
CREATE UNIQUE INDEX IF NOT EXISTS tilbakemelding_id_idx ON tilbakemelding (_id);