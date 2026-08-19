-- =============================================================================
-- V8: Adiciona colunas de status/expiração do carrinho, idempotência do pedido,
--     version do pedido e tabela de outbox para eventos assíncronos.
-- =============================================================================

-- 1. Status do carrinho (ABERTO, CONVERTIDO, EXPIRADO)
ALTER TABLE tb_carrinho
    ADD COLUMN IF NOT EXISTS carrinho_status VARCHAR(255) NOT NULL DEFAULT 'ABERTO';

-- 2. Expiração do carrinho (usado pelo scheduler de liberação de estoque)
ALTER TABLE tb_carrinho
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

-- 3. Chave de idempotência do checkout (impede pedido duplicado por retry)
ALTER TABLE tb_pedido
    ADD COLUMN IF NOT EXISTS checkout_idempotency_key VARCHAR(255);

-- Preenche pedidos antigos com o próprio ID para não violar NOT NULL
UPDATE tb_pedido
SET checkout_idempotency_key = id::text
WHERE checkout_idempotency_key IS NULL;

ALTER TABLE tb_pedido
    ALTER COLUMN checkout_idempotency_key SET NOT NULL;

-- Constraint única por usuário + chave (proteção contra corrida)
CREATE UNIQUE INDEX IF NOT EXISTS uk_pedido_user_idempotency
    ON tb_pedido (user_id, checkout_idempotency_key);

-- 4. Version do pedido (controle otimista de concorrência)
ALTER TABLE tb_pedido
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 5. Tabela de Outbox (caixa de saída transacional para RabbitMQ)
CREATE TABLE IF NOT EXISTS tb_outbox_event (
    id              UUID         NOT NULL PRIMARY KEY,
    aggregate_id    UUID         NOT NULL,
    type_event      VARCHAR(50)  NOT NULL,
    payload         TEXT         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Índice parcial: busca rápida de eventos pendentes
CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON tb_outbox_event (published, created_at)
    WHERE published = FALSE;
