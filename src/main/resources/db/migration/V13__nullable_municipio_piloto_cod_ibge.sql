-- MUN_PILOTO no Qlik não expõe campo de código IBGE; tornando nullable para não bloquear ETL.
ALTER TABLE municipio_piloto ALTER COLUMN cod_ibge VARCHAR(7) NULL;
