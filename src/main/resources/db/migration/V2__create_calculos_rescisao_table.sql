-- Tabela para armazenar o histórico de cálculos de rescisão
CREATE TABLE calculos_rescisao (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tipo_rescisao VARCHAR(50) NOT NULL,
  salario_mensal NUMERIC(19, 2) NOT NULL,
  data_admissao DATE NOT NULL,
  data_desligamento DATE NOT NULL,
  aviso_indenizado BOOLEAN NOT NULL,
  ferias_vencidas_dias INTEGER NOT NULL,
  meses_trabalhados_ano INTEGER NOT NULL,
  saldo_fgts_depositado NUMERIC(19, 2) NOT NULL,
  total_bruto NUMERIC(19, 2) NOT NULL,
  total_descontos NUMERIC(19, 2) NOT NULL,
  total_liquido NUMERIC(19, 2) NOT NULL,
  componentes JSONB NOT NULL,
  pagamento_ate DATE,
  criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

  CONSTRAINT fk_calculos_rescisao_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuarios(id)
    ON DELETE CASCADE
);

-- Índice para otimizar a busca de histórico por usuário
CREATE INDEX idx_calculos_rescisao_usuario_id ON calculos_rescisao(usuario_id);