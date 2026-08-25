DO $$
BEGIN
    -- Garante NOT NULL
    ALTER TABLE tb_carrinho ALTER COLUMN user_id SET NOT NULL;

    -- Cria constraint unique apenas se não existir na tabela correta
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE c.conname = 'uk_tb_carrinho_user_id'
          AND t.relname = 'tb_carrinho'
    ) THEN
        ALTER TABLE tb_carrinho ADD CONSTRAINT uk_tb_carrinho_user_id UNIQUE (user_id);
    END IF;
END $$;
