-- Modelo único. As tabelas antigas permanecem como arquivo de conferência, sem API de escrita.
CREATE TABLE jornada_atividade (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
 version BIGINT NOT NULL DEFAULT 0, revisao BIGINT NOT NULL DEFAULT 0,
 titulo VARCHAR(255) NOT NULL, descricao VARCHAR(10000), inicio DATE, prazo DATE, unidade VARCHAR(80),
 fracionavel BOOLEAN NOT NULL DEFAULT FALSE, total NUMERIC(19,4), saldo_inicial NUMERIC(19,4) NOT NULL DEFAULT 0,
 saldo_data DATE, status VARCHAR(30) NOT NULL, prioridade VARCHAR(20), horario TIME, agrupamento VARCHAR(255),
 origem VARCHAR(20), origem_id BIGINT, percentual_legado DOUBLE PRECISION,
 UNIQUE(origem,origem_id), CHECK(total IS NULL OR total>0), CHECK(saldo_inicial>=0),
 CHECK(total IS NULL OR saldo_inicial<=total)
);
CREATE INDEX idx_jornada_usuario ON jornada_atividade(user_id);
CREATE TABLE jornada_dia (
 atividade_id BIGINT NOT NULL REFERENCES jornada_atividade(id) ON DELETE CASCADE,
 dia VARCHAR(20) NOT NULL, PRIMARY KEY(atividade_id,dia)
);
CREATE TABLE jornada_progresso (
 id BIGSERIAL PRIMARY KEY, atividade_id BIGINT NOT NULL REFERENCES jornada_atividade(id) ON DELETE CASCADE,
 request_id UUID NOT NULL, data DATE NOT NULL, modo VARCHAR(20) NOT NULL, quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>=0),
 observacao VARCHAR(4000), motivacao INTEGER CHECK(motivacao BETWEEN 1 AND 5), UNIQUE(atividade_id,request_id)
);
CREATE INDEX idx_jornada_historico ON jornada_progresso(atividade_id,data,id);
CREATE TABLE jornada_material (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
 atividade_id BIGINT REFERENCES jornada_atividade(id) ON DELETE SET NULL,
 version BIGINT NOT NULL DEFAULT 0, titulo VARCHAR(255) NOT NULL, url VARCHAR(2048) NOT NULL, descricao VARCHAR(4000)
);
CREATE INDEX idx_material_usuario ON jornada_material(user_id);
-- Em instalação nova esta tabela ainda não foi criada pelo Hibernate.
CREATE TABLE IF NOT EXISTS "tb-estudos" (
 id BIGSERIAL PRIMARY KEY, nome VARCHAR(255), duracao_min INTEGER NOT NULL DEFAULT 0,
 data DATE, notas VARCHAR(255), materia_id BIGINT
);
ALTER TABLE "tb-estudos" ADD COLUMN atividade_id BIGINT REFERENCES jornada_atividade(id) ON DELETE CASCADE;
ALTER TABLE sessao_estudo ADD COLUMN atividade_id BIGINT REFERENCES jornada_atividade(id) ON DELETE CASCADE;
ALTER TABLE sessao_estudo ALTER COLUMN materia_id DROP NOT NULL;
CREATE INDEX idx_estudo_atividade ON "tb-estudos"(atividade_id);
CREATE INDEX idx_sessao_atividade ON sessao_estudo(atividade_id);
ALTER TABLE "tb-estudos" ALTER COLUMN notas TYPE VARCHAR(10000);
ALTER TABLE email_log DROP CONSTRAINT IF EXISTS email_log_tipo_check;
DO $$
BEGIN
 IF to_regclass('tb_tarefas') IS NOT NULL THEN
  INSERT INTO jornada_atividade(user_id,titulo,prazo,horario,prioridade,unidade,total,saldo_inicial,saldo_data,status,origem,origem_id)
  SELECT t.user_id,COALESCE(t.titulo,'Atividade sem título'),t.prazo,t.horario,t.prioridade,'unidades',1,
    CASE WHEN t.status='Concluida' THEN 1 ELSE 0 END,
    (CURRENT_TIMESTAMP AT TIME ZONE 'America/Cuiaba')::date,
    CASE t.status WHEN 'Concluida' THEN 'CONCLUIDA' WHEN 'Cancelada' THEN 'CANCELADA' WHEN 'Ativa' THEN 'EM_ANDAMENTO' ELSE 'NAO_INICIADA' END,
    'TAREFA',t.id FROM tb_tarefas t JOIN tb_user u ON u.id=t.user_id;
 END IF;
 IF to_regclass('tb_metas') IS NOT NULL THEN
  INSERT INTO jornada_atividade(user_id,titulo,descricao,prazo,unidade,fracionavel,total,saldo_inicial,saldo_data,status,origem,origem_id,percentual_legado)
  SELECT m.user_id,COALESCE(m.titulo,'Atividade sem título'),m.descricao,m.prazo,'%',TRUE,
    CASE WHEN m.prograsso BETWEEN 0 AND 100 THEN 100 ELSE NULL END,
    CASE WHEN m.prograsso BETWEEN 0 AND 100 THEN round(m.prograsso::numeric,4) ELSE 0 END,
    (CURRENT_TIMESTAMP AT TIME ZONE 'America/Cuiaba')::date,
    CASE WHEN m.prograsso=100 THEN 'CONCLUIDA' WHEN m.status='Cancelada' THEN 'CANCELADA'
      WHEN m.prograsso>0 OR m.status='Ativa' THEN 'EM_ANDAMENTO' ELSE 'NAO_INICIADA' END,
    'META',m.id,m.prograsso FROM tb_metas m JOIN tb_user u ON u.id=m.user_id;
 END IF;
 IF to_regclass('tb_materia') IS NOT NULL THEN
  INSERT INTO jornada_atividade(user_id,titulo,descricao,status,origem,origem_id)
  SELECT m.user_id,COALESCE(m.nome,'Atividade sem título'),m.descricao,'EM_ANDAMENTO','MATERIA',m.id
  FROM tb_materia m JOIN tb_user u ON u.id=m.user_id;
  INSERT INTO jornada_dia(atividade_id,dia)
  SELECT DISTINCT a.id,d.dia_semana FROM materia_dias_semana d JOIN jornada_atividade a ON a.origem='MATERIA' AND a.origem_id=d.materia_id;
  UPDATE sessao_estudo s SET atividade_id=a.id FROM jornada_atividade a WHERE a.origem='MATERIA' AND a.origem_id=s.materia_id;
  UPDATE "tb-estudos" e SET atividade_id=a.id FROM jornada_atividade a WHERE a.origem='MATERIA' AND a.origem_id=e.materia_id;
  -- Falhar a migration inteira se algum vínculo com matéria migrada não tiver sido preservado.
  IF EXISTS(SELECT 1 FROM sessao_estudo s JOIN jornada_atividade a ON a.origem='MATERIA' AND a.origem_id=s.materia_id WHERE s.atividade_id IS DISTINCT FROM a.id)
    OR EXISTS(SELECT 1 FROM "tb-estudos" e JOIN jornada_atividade a ON a.origem='MATERIA' AND a.origem_id=e.materia_id WHERE e.atividade_id IS DISTINCT FROM a.id) THEN
    RAISE EXCEPTION 'Falha ao preservar estudos na migração de Jornadas';
  END IF;
 END IF;
 -- Evita reenviar no mesmo dia os lembretes de tarefas que já foram entregues antes da migração.
 INSERT INTO email_log(usuario_id,tipo,referencia_id,data_referencia,enviado_em)
 SELECT l.usuario_id,'ATIVIDADE_DIA',a.id,l.data_referencia,l.enviado_em
 FROM email_log l JOIN jornada_atividade a ON a.origem='TAREFA' AND a.origem_id=l.referencia_id AND a.user_id=l.usuario_id
 WHERE l.tipo='TAREFA_DIA';
END $$;
