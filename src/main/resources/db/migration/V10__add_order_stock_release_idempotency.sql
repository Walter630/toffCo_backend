-- Registra a baixa de estoque do pedido para impedir descontos duplicados.
ALTER TABLE tb_pedido
    ADD COLUMN IF NOT EXISTS stock_released BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stock_released_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS stock_release_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_pedido_stock_release_id
    ON tb_pedido (stock_release_id)
    WHERE stock_release_id IS NOT NULL;

-- Pedidos legados: só marca como baixado os que já foram entregues ou enviados,
-- pois é seguro assumir que esses já tiveram o estoque descontado no fluxo anterior.
-- Pedidos em outros estados ficam como FALSE para que o admin decida.
UPDATE tb_pedido
SET stock_released = TRUE,
    stock_released_at = COALESCE(data_atualizacao, data_criacao),
    stock_release_id = gen_random_uuid()
WHERE stock_released = FALSE
  AND status IN ('ENTREGUE', 'ENVIADO', 'PRONTO');
