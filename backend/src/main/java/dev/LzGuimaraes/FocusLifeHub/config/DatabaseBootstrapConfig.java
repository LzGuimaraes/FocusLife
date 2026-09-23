package dev.LzGuimaraes.FocusLifeHub.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Garante o schema-base ANTES de o Flyway aplicar as migrations versioneadas.
 *
 * ── O PROBLEMA ──────────────────────────────────────────────────────────────
 * O Flyway roda antes do Hibernate. Em um banco NOVO, o
 * {@code V18__auth_security_hardening.sql} faz {@code ALTER TABLE tb_user ...}
 * sem guard de existência de tabela — e como quem cria tb_user é o Hibernate
 * (depois), a subida falhava com:
 *     ERROR: relation "tb_user" does not exist
 * Ou seja: instalação nova simplesmente não subia.
 *
 * ── POR QUE NÃO EDITAR O V18 ────────────────────────────────────────────────
 * O V18 já está aplicado em bancos existentes. Alterar o arquivo mudaria o
 * checksum e o Flyway recusaria a subida ("Migration checksum mismatch"),
 * exigindo `flyway repair` manual. Esta estratégia resolve o banco novo sem
 * tocar em nenhum banco existente: o DDL é idempotente
 * (CREATE TABLE IF NOT EXISTS) e não faz nada quando a tabela já existe.
 *
 * ── POR QUE FlywayMigrationStrategy (e não um Callback) ─────────────────────
 * Callbacks do Flyway deixaram de ser um recurso livre no Flyway 11 (o OSS
 * ignora callbacks que não são "royalty-free"), então eles não eram sequer
 * executados. `FlywayMigrationStrategy` é o ponto de extensão estável do
 * Spring Boot: ele envolve a chamada de migração.
 *
 * ── CONVENÇÃO PARA NOVAS MIGRATIONS ─────────────────────────────────────────
 * Prefira sempre: (a) CREATE TABLE/INDEX IF NOT EXISTS; ou (b) checagem em
 * information_schema antes de ALTER/DROP (padrão de V3/V5/V6). Só use este
 * bootstrap se a tabela for criada pelo Hibernate E a migration precisar dela
 * — nesse caso, acrescente a tabela ao DDL abaixo.
 */
@Configuration
public class DatabaseBootstrapConfig {

    /**
     * DDL idempotente das tabelas-base que as migrations versioneadas
     * referenciam. Hoje só `tb_user` (usada pelo V18).
     */
    private static final String[] SCHEMA_BASE = {
            """
            CREATE TABLE IF NOT EXISTS tb_user (
                id                               BIGSERIAL    PRIMARY KEY,
                nome                             VARCHAR(255),
                email                            VARCHAR(255),
                password                         VARCHAR(255),
                enabled                          BOOLEAN      DEFAULT FALSE,
                activation_code                  VARCHAR(255),
                activation_code_expiration       TIMESTAMP WITH TIME ZONE,
                reset_password_token             VARCHAR(255),
                reset_password_token_expiration  TIMESTAMP WITH TIME ZONE,
                token_version                    INTEGER      DEFAULT 0,
                role                             VARCHAR(255)
            )
            """
    };

    @Bean
    public FlywayMigrationStrategy garantirSchemaBaseAntesDeMigrar(DataSource dataSource) {
        return flyway -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            for (String ddl : SCHEMA_BASE) {
                jdbc.execute(ddl);
            }
            flyway.migrate();
        };
    }
}
