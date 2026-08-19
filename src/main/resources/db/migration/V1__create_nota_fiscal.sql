-- =============================================================================
-- Tabela de Notas Fiscais
--
-- Registra cada NF-e emitida pelo Odoo para um pedido.
-- O ciclo de vida (status) é gerenciado pelo OdooInvoiceService.
--
-- Relação: tb_pedido (1) ──── (0..1) tb_nota_fiscal
-- =============================================================================

CREATE TABLE IF NOT EXISTS tb_nota_fiscal
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Relacionamento com o pedido (1 pedido = no máximo 1 nota fiscal)
    pedido_id        UUID         NOT NULL UNIQUE
        REFERENCES tb_pedido (id) ON DELETE RESTRICT,

    -- ID da fatura no Odoo (account.move). Preenchido após createInvoice().
    odoo_invoice_id  BIGINT,

    -- Número legível da nota gerado pelo Odoo (ex: "INV/2025/00001").
    numero_nota      VARCHAR(50),

    -- Chave de acesso de 44 dígitos retornada pela SEFAZ.
    chave_acesso     VARCHAR(50),

    -- URLs dos documentos fiscais retornadas pelo Odoo.
    url_danfe        VARCHAR(500),
    url_xml          VARCHAR(500),

    -- Mensagem de erro preenchida quando status = 'ERRO'.
    mensagem_erro    VARCHAR(1000),

    -- Status do ciclo de vida: PENDENTE | EMITIDA | AUTORIZADA | ERRO | CANCELADA
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',

    -- Timestamps gerenciados pelo Hibernate (@CreationTimestamp / @UpdateTimestamp)
    data_criacao     TIMESTAMP    NOT NULL DEFAULT now(),
    data_atualizacao TIMESTAMP    NOT NULL DEFAULT now()
);

-- Índice para buscas rápidas pelo ID do Odoo (usadas no processamento do webhook)
CREATE INDEX IF NOT EXISTS idx_nota_fiscal_odoo_invoice_id
    ON tb_nota_fiscal (odoo_invoice_id);

-- Índice para buscas por status (útil para dashboards e monitoramento)
CREATE INDEX IF NOT EXISTS idx_nota_fiscal_status
    ON tb_nota_fiscal (status);

-- Adiciona coluna CPF na tabela de usuários (necessária para emissão de NF-e)
ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(14) UNIQUE;