-- Carga inicial: o avaliador abre a interface e ja encontra dado na tela,
-- em vez de uma lista vazia. Os CPFs abaixo sao numeros validos pelo modulo 11
-- e nao pertencem a ninguem -- sao os exemplos classicos de teste.
-- Os nomes foram escolhidos para dar previsoes diferentes na nationalize.io.

INSERT INTO pessoa (documento, nome, sobrenome, email, criado_em) VALUES
  ('52998224725', 'Nathaniel', 'Barbosa',  'nathaniel.barbosa@exemplo.com.br', CURRENT_TIMESTAMP),
  ('11144477735', 'Giovanni',  'Ferreira', 'giovanni.ferreira@exemplo.com.br', CURRENT_TIMESTAMP),
  ('12345678909', 'Mariana',   'Souza',    'mariana.souza@exemplo.com.br',     CURRENT_TIMESTAMP),
  ('39053344705', 'Yuki',      'Tanaka',   'yuki.tanaka@exemplo.com.br',       CURRENT_TIMESTAMP);
