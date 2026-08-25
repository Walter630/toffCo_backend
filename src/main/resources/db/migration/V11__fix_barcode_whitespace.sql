-- Corrige o regex da V9 que não funcionou no PostgreSQL (barra dupla não é interpretada).
UPDATE tb_produto
SET codigo_barras = regexp_replace(codigo_barras, '\s+', '', 'g')
WHERE codigo_barras IS NOT NULL
  AND codigo_barras <> regexp_replace(codigo_barras, '\s+', '', 'g');
