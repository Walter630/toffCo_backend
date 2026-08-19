SELECT user_id, COUNT(*)
FROM tb_carrinho
GROUP BY user_id
HAVING COUNT(*) > 1;

ALTER TABLE tb_carrinho
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE tb_carrinho
    ADD CONSTRAINT uk_tb_carrinho_user_id
        UNIQUE (user_id);
