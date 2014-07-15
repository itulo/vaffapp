CREATE TABLE regions (rowid INT PRIMARY KEY, name TEXT);
INSERT INTO regions VALUES (1, 'Molise');
INSERT INTO regions VALUES (2, 'Valle d''Aosta');
INSERT INTO regions VALUES (3, 'Piemonte');
INSERT INTO regions VALUES (4, 'Lombardia');
INSERT INTO regions VALUES (5, 'Trentino Alto Adige');
INSERT INTO regions VALUES (6, 'Friuli Venezia Giulia');
INSERT INTO regions VALUES (7, 'Veneto');
INSERT INTO regions VALUES (8, 'Emilia-Romagna');
INSERT INTO regions VALUES (9, 'Liguria');
INSERT INTO regions VALUES (10, 'Toscana');
INSERT INTO regions VALUES (11, 'Lazio');
INSERT INTO regions VALUES (12, 'Umbria');
INSERT INTO regions VALUES (13, 'Marche');
INSERT INTO regions VALUES (14, 'Abruzzo');
INSERT INTO regions VALUES (15, 'Campania');
INSERT INTO regions VALUES (16, 'Puglia');
INSERT INTO regions VALUES (17, 'Basilicata');
INSERT INTO regions VALUES (18, 'Calabria');
INSERT INTO regions VALUES (19, 'Sicilia');
INSERT INTO regions VALUES (20, 'Sardegna');


-- no PK, default PK is rowid, which is autoincremented
CREATE TABLE insults (insult TEXT, desc TEXT, region INT,
FOREIGN KEY(region) REFERENCES regions(rowid));

-- needed by Android
CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO "android_metadata" VALUES ('en_US');

-- to keep track of db versions
CREATE TABLE version (ver INT);
INSERT INTO version VALUES (0);
