/* Tilbakemelding */
CREATE TABLE IF NOT EXISTS tilbakemelding(
    _id TEXT DEFAULT einnsyn_id('tilb'),
    _external_id TEXT,
    _created TIMESTAMP DEFAULT now(),
    _updated TIMESTAMP DEFAULT now(),
    tilbakemelding_id INT,
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
CREATE UNIQUE INDEX IF NOT EXISTS tilbakemelding_id_idx ON tilbakemelding (_id);
