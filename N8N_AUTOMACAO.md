# Automação ToffCo + n8n

Este documento é o mapa da automação. Pense no backend como o personagem que
controla o inventário e as regras do jogo; o n8n é o copiloto que observa os
eventos e executa tarefas externas.

## 1. Regra principal da arquitetura

O backend continua sendo responsável por:

- estado da conversa;
- deduplicação de mensagens;
- fila e status do atendimento;
- envio de mensagens pela Evolution API;
- regras de pedido, pagamento e estoque.

O n8n fica responsável por:

- notificações;
- CRM, planilhas e tickets;
- lembretes e SLA;
- relatórios;
- integrações externas;
- classificação e resumo com IA.

O n8n não deve alterar o Redis diretamente e não deve assumir o controle da
conversa sem chamar uma API protegida do backend.

## 2. Primeiro nível: criar o webhook de eventos

No `.env` do backend, configure:

```env
N8N_ALERT_WEBHOOK_URL=https://SEU_N8N/webhook/toffco-events
N8N_WEBHOOK_SECRET=um-segredo-grande-e-aleatorio
N8N_REVIEW_MODE=false
# webhook é simples; rabbitmq é recomendado quando o n8n consumir a fila
N8N_TRANSPORT=webhook
```

No n8n:

1. Crie um workflow chamado `ToffCo - Event Router`.
2. Adicione um nó **Webhook**.
3. Use método `POST`.
4. Use o caminho `toffco-events`.
5. Configure autenticação por header:
   - nome: `X-N8N-Webhook-Secret`;
   - valor: exatamente o mesmo valor de `N8N_WEBHOOK_SECRET`.
6. Ative o workflow e copie a URL de produção para o `.env`.
7. Reinicie o backend depois de alterar o `.env`.

Para produção, depois que o workflow HTTP estiver validado, você pode trocar
`N8N_TRANSPORT=rabbitmq`. Nesse modo o backend publica na fila durável
`n8n.automation.events` do RabbitMQ e o n8n deve usar um nó **RabbitMQ Trigger**
com a exchange `toffco.exchange` e a routing key `n8n.automation`.

Essa opção é mais resistente a reinícios: o evento permanece no RabbitMQ até
ser consumido. Não use os dois transportes ao mesmo tempo, pois isso produzirá
duas execuções para cada evento.

O corpo recebido pelo n8n tem este formato:

```json
{
  "eventId": "attendance-assigned:5511999999999",
  "type": "HUMAN_ATTENDANCE_ASSIGNED",
  "source": "toffco-backend",
  "occurredAt": "2026-08-01T14:00:00Z",
  "data": {
    "number": "5511999999999",
    "attendantNumber": "553488560330"
  }
}
```

## 3. Segundo nível: impedir mensagens duplicadas no n8n

Depois do Webhook, coloque um nó **Data Store** ou Redis para guardar
`eventId` por 24 horas.

Fluxo:

1. Ler `eventId`.
2. Verificar se já existe.
3. Se existir, encerrar o workflow.
4. Se não existir, salvar o `eventId`.
5. Continuar para o roteador.

Isso é importante porque webhooks podem ser repetidos por rede instável,
reinício ou retry do provedor.

## 4. Terceiro nível: roteador de eventos

Adicione um nó **Switch** usando o campo `type`.

### `HUMAN_ATTENDANCE_REQUESTED`

Use para:

- criar ou atualizar um ticket;
- registrar número, assunto e descrição;
- avisar Slack, e-mail ou painel;
- iniciar um contador de SLA;
- colocar o atendimento em uma fila do CRM.

### `HUMAN_ATTENDANCE_ASSIGNED`

Use para:

- registrar qual atendente assumiu;
- parar o alerta de “aguardando atendente”;
- iniciar o contador de tempo de atendimento.

### `HUMAN_ATTENDANCE_RESOLVED`

Use para:

- fechar o ticket;
- registrar duração;
- enviar pesquisa de satisfação;
- gerar uma tarefa de pós-venda.

### `WHATSAPP_SEND_FAILURE`

Use para:

- registrar o número afetado;
- contar falhas por janela de tempo;
- avisar o responsável apenas depois de um limite, por exemplo 3 falhas em
  5 minutos;
- abrir incidente quando a Evolution API estiver indisponível.

### `WHATSAPP_CIRCUIT_OPEN`

Use para:

- enviar um alerta de indisponibilidade;
- criar incidente;
- aguardar alguns minutos;
- consultar novamente o endpoint de health.

Não faça um loop mandando mensagens a cada tentativa bloqueada. O circuit
breaker existe justamente para impedir esse comportamento.

### `BOT_RESPONSE_REVIEW`

Ative `N8N_REVIEW_MODE=true` somente em testes. Use o evento para:

- salvar mensagem recebida e resposta do bot;
- permitir revisão humana;
- identificar respostas ruins;
- montar uma base para melhorar o fluxo.

Em produção, mantenha `false` até o workflow de revisão estar pronto.

## 5. Fluxo recomendado para atendimento humano

Monte estes nós:

```text
Webhook
  -> Deduplicação por eventId
  -> Switch por type
  -> Criar/atualizar ticket
  -> Notificar equipe
  -> Registrar SLA
```

O n8n pode notificar a equipe, mas a alteração oficial do atendimento deve
continuar passando pelos endpoints do backend:

- atribuir: `POST /api/webhook/whatsapp/queue/{clientNumber}/assign`;
- finalizar: `POST /api/webhook/whatsapp/queue/{clientNumber}/release`.

Esses endpoints devem ser chamados pelo n8n com autenticação de serviço, não
com uma URL pública sem proteção.

## 6. Próximas automações de alto valor

### Vendas

Quando o cliente demonstrar interesse:

1. registrar lead;
2. salvar produto ou categoria;
3. salvar quantidade e cidade;
4. classificar quente/morno/frio;
5. criar tarefa para vendas;
6. fazer apenas um follow-up controlado.

### Pedido e pagamento

Consumir eventos do backend para:

- pedido criado;
- pagamento aprovado;
- pagamento recusado;
- pedido separado;
- nota fiscal emitida;
- pedido enviado.

O n8n deve notificar e integrar. Não deve decidir sozinho se um pagamento é
válido.

### Odoo e estoque

Automatizar:

- alerta de estoque baixo;
- falha de sincronização;
- NF-e autorizada ou rejeitada;
- divergência entre produto do site e Odoo.

### Relatórios

Um workflow diário pode consolidar:

- atendimentos abertos e resolvidos;
- tempo médio até o primeiro atendimento;
- falhas da Evolution;
- quantidade de mensagens duplicadas bloqueadas;
- conversões e pedidos.

## 7. Regras para a automação não virar um problema

- Todo evento precisa de `eventId`.
- Todo workflow precisa de deduplicação.
- Todo envio deve ter limite de tentativas.
- Não enviar mensagem promocional sem consentimento.
- Não deixar o n8n chamar o próprio webhook em um loop.
- Não expor a chave da Evolution API no n8n sem necessidade.
- Não fazer o usuário esperar o n8n para receber a resposta principal.
- Registrar falha e permitir reprocessamento manual.
- Para eventos críticos, preferir `N8N_TRANSPORT=rabbitmq` em vez de webhook.

## 8. Ordem de implementação

1. Testar o webhook `toffco-events` com `BOT_RESPONSE_REVIEW`.
2. Criar o roteador e a deduplicação.
3. Implementar o fluxo de atendimento humano.
4. Implementar SLA e alertas de falha.
5. Adicionar eventos de pedido e pagamento.
6. Adicionar follow-up com limite e opt-in.
7. Adicionar IA somente depois de os fluxos determinísticos estarem estáveis.
