ALTER TABLE tb_produto
    ADD COLUMN IF NOT EXISTS peso INTEGER,
    ADD COLUMN IF NOT EXISTS diametro NUMERIC(10, 2);

ALTER TABLE tb_produto
    ALTER COLUMN description TYPE TEXT
    USING description::text;
