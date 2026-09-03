-- Corrige cnes_estabelecimento (só o número) e nome_estabelecimento (nome após " - ")
-- O formato original em atendimento é "9318348 - NOME DO ESTABELECIMENTO"
UPDATE atendimento_norm
SET
    cnes_estabelecimento = (
        SELECT CASE
            WHEN a.cnes_estabelecimento IS NULL OR LOCATE(' - ', a.cnes_estabelecimento) = 0
                THEN a.cnes_estabelecimento
            ELSE TRIM(SUBSTRING(a.cnes_estabelecimento, 1, LOCATE(' - ', a.cnes_estabelecimento) - 1))
        END
        FROM atendimento a WHERE a.id = atendimento_norm.id
    ),
    nome_estabelecimento = (
        SELECT CASE
            WHEN a.cnes_estabelecimento IS NULL OR LOCATE(' - ', a.cnes_estabelecimento) = 0
                THEN NULL
            ELSE TRIM(SUBSTRING(a.cnes_estabelecimento, LOCATE(' - ', a.cnes_estabelecimento) + 3))
        END
        FROM atendimento a WHERE a.id = atendimento_norm.id
    );
