-- Permite um refresh token por aparelho/dispositivo conectado.
-- A coluna token continua com unicidade própria para identificar cada sessão.
DO $$
DECLARE
    constraint_name text;
BEGIN
    -- Remove qualquer constraint UNIQUE legada que cubra apenas user_id,
    -- independentemente do nome gerado pelo Hibernate.
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = to_regclass('tb_refresh_token')
          AND c.contype = 'u'
          AND c.conkey = ARRAY[
              (
                  SELECT a.attnum
                  FROM pg_attribute a
                  WHERE a.attrelid = c.conrelid
                    AND a.attname = 'user_id'
                    AND NOT a.attisdropped
              )
          ]::smallint[]
    LOOP
        EXECUTE format(
            'ALTER TABLE tb_refresh_token DROP CONSTRAINT %I',
            constraint_name
        );
    END LOOP;
END $$;
