INSERT INTO promotion (id, nom) VALUES (1, 'Promotion Démonstration');
INSERT INTO etudiant (id, nom, promotion_id) VALUES
    (1, 'Amina Njoya', 1),
    (2, 'Boris Ndzié', 1),
    (3, 'Chloé Ndzi', 1);
ALTER TABLE promotion ALTER COLUMN id RESTART WITH 2;
ALTER TABLE etudiant ALTER COLUMN id RESTART WITH 4;
