-- Adiciona a coluna para armazenar o nome do empregado em cada cálculo.
-- A coluna pode ser nula para ser compatível com os registros já existentes.
ALTER TABLE calculos_rescisao ADD COLUMN nome_empregado VARCHAR(255);