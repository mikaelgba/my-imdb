CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Cadastra um EDITOR inicial
WITH inserted_editor AS (
INSERT INTO users (id, email, password_hash, role, active)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'editor@seuprojeto.com',
    crypt('Editor@123456', gen_salt('bf', 12)),
    'EDITOR',
    TRUE
    )
ON CONFLICT (email) DO NOTHING
    RETURNING id
    )
INSERT INTO user_profiles (user_id, full_name, bio)
SELECT id, 'Editor Padrão', 'Conta inicial de editor'
FROM inserted_editor;

-- 2. Cadastra um USER comum inicial
WITH inserted_user AS (
INSERT INTO users (id, email, password_hash, role, active)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'usuario@seuprojeto.com',
    crypt('Usuario@123456', gen_salt('bf', 12)),
    'USER',
    TRUE
    )
ON CONFLICT (email) DO NOTHING
    RETURNING id
    )
INSERT INTO user_profiles (user_id, full_name, bio)
SELECT id, 'Usuário Padrão', 'Conta inicial de usuário'
FROM inserted_user;