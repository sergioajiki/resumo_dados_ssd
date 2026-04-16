-- V12 — Tabela de diagnóstico de sincronização
-- Armazena contagens de campos por sync para auditoria e diagnóstico.
-- Consultar: SELECT * FROM SYNC_DIAGNOSTICO ORDER BY DT_SYNC DESC
CREATE TABLE sync_diagnostico (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    dt_sync       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tabela        VARCHAR(50)  NOT NULL,
    campo         VARCHAR(50)  NOT NULL,
    total         BIGINT       NOT NULL,
    nao_nulos     BIGINT       NOT NULL,
    percentual    DECIMAL(5,1) NOT NULL
);
