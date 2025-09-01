-- Adiciona a coluna 'username', que deve ser única para cada usuário.
-- A coluna pode ser nula para não quebrar os usuários já existentes.
ALTER TABLE usuarios ADD COLUMN username VARCHAR(50) UNIQUE;