CREATE TABLE IF NOT EXISTS search_result
(
    `from`          INTEGER,
    `to`            INTEGER,
    dijkstraLength  INTEGER,
    chLength        INTEGER,
    dijkstra_weight REAL,
    ch_weight       REAL,
    dijkstraTime    INTEGER,
    chTime          INTEGER
);
CREATE TABLE IF NOT EXISTS contraction_result
(
    `timestamp`       INTEGER,
    `method`          TEXT,
    contractionTime   INTEGER,
    shortcutsInserted INTEGER
);
