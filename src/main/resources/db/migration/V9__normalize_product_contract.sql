-- Contrato canônico de produto: código de barras String, status oficial,
-- estoque inteiro não negativo e campos de apresentação persistidos.

ALTER TABLE tb_produto
    ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS tb_produto_images
(
    produto_id UUID NOT NULL REFERENCES tb_produto (id) ON DELETE CASCADE,
    image_url  VARCHAR(1000) NOT NULL
);

-- Remove espaços somente no valor persistido do código; zeros à esquerda são preservados.
UPDATE tb_produto
SET codigo_barras = regexp_replace(codigo_barras, '\\s+', '', 'g')
WHERE codigo_barras IS NOT NULL;

-- Migração explícita dos nomes antigos conhecidos para o enum canônico.
UPDATE tb_produto SET status = 'DISPONIVEL' WHERE status = 'ATIVO';
UPDATE tb_produto SET status = 'SEM_ESTOQUE' WHERE status = 'ESGOTADO';
UPDATE tb_produto SET status = 'EM_BREVE' WHERE status = 'INDISPONIVEL';
UPDATE tb_produto SET status = 'EM_PRODUCAO' WHERE status IN ('EM_PRODUÇAO', 'EM_PRODUÇÃO');
UPDATE tb_produto SET status = 'DISPONIVEL' WHERE status IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tb_produto
        WHERE status NOT IN ('DISPONIVEL', 'SEM_ESTOQUE', 'EM_PRODUCAO', 'EM_BREVE', 'PRE_VENDA')
    ) THEN
        RAISE EXCEPTION 'Existem produtos com status fora da lista oficial';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM tb_produto
        WHERE estoque IS NULL OR estoque < 0 OR estoque <> trunc(estoque)
    ) THEN
        RAISE EXCEPTION 'Existem produtos com estoque inválido';
    END IF;
END $$;

ALTER TABLE tb_produto
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE tb_produto
    ADD CONSTRAINT ck_tb_produto_status_oficial
    CHECK (status IN ('DISPONIVEL', 'SEM_ESTOQUE', 'EM_PRODUCAO', 'EM_BREVE', 'PRE_VENDA'));

ALTER TABLE tb_produto
    ADD CONSTRAINT ck_tb_produto_estoque_nao_negativo_inteiro
    CHECK (estoque >= 0 AND estoque = trunc(estoque));

CREATE INDEX IF NOT EXISTS idx_tb_produto_codigo_barras
    ON tb_produto (codigo_barras);
