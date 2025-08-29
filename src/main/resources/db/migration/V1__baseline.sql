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

CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE usuarios (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  senha_hash VARCHAR(255) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE usuarios_roles (
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  PRIMARY KEY(usuario_id, role_id)
);
