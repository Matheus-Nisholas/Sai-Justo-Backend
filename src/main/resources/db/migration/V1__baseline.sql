CREATE TABLE calculos_rescisao (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tipo_rescisao VARCHAR(40) NOT NULL,
  salario_mensal NUMERIC(15,2) NOT NULL,
  data_admissao DATE NOT NULL,
  data_desligamento DATE NOT NULL,
  aviso_indenizado BOOLEAN NOT NULL,
  ferias_vencidas_dias INT NOT NULL,
  meses_trabalhados_ano INT NOT NULL,
  saldo_fgts_depositado NUMERIC(15,2) NOT NULL,
  total_bruto NUMERIC(15,2) NOT NULL,
  total_descontos NUMERIC(15,2) NOT NULL,
  total_liquido NUMERIC(15,2) NOT NULL,
  componentes JSONB NOT NULL,
  pagamento_ate DATE,
  criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_calculos_usuario_criado_em
ON calculos_rescisao(usuario_id, criado_em DESC);
