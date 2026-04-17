-- id_digsaude_ref e id_jornd_ref: o Qlik retorna chaves compostas (ex: 14_fev_2025_DEODÁPOLIS),
-- não IDs numéricos. mes também é texto (ex: "fev"), não número.
ALTER TABLE link ALTER COLUMN id_digsaude_ref VARCHAR(100);
ALTER TABLE link ALTER COLUMN id_jornd_ref    VARCHAR(100);
ALTER TABLE link ALTER COLUMN mes             VARCHAR(10);
