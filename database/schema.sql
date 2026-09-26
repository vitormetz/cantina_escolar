-- Estrutura inicial para MySQL 8 ou superior.
CREATE DATABASE IF NOT EXISTS cantina_escolar
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cantina_escolar;

CREATE TABLE IF NOT EXISTS cliente (
    idcliente INT NOT NULL AUTO_INCREMENT,
    nomecliente VARCHAR(150) NOT NULL,
    nomeresponsavel VARCHAR(150) NOT NULL,
    saldo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    limitesaldo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    emailresponsavel VARCHAR(255) NOT NULL,
    alergias TEXT NULL,
    PRIMARY KEY (idcliente),
    CONSTRAINT ck_cliente_saldo CHECK (saldo >= 0),
    CONSTRAINT ck_cliente_limitesaldo CHECK (limitesaldo >= 0)
);

CREATE TABLE IF NOT EXISTS cardapio (
    id_cardapio INT NOT NULL AUTO_INCREMENT,
    cardapio_json JSON NOT NULL,
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    diasemana TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_cardapio),
    CONSTRAINT ck_cardapio_diasemana CHECK (diasemana BETWEEN 1 AND 7)
);

CREATE TABLE IF NOT EXISTS pedido (
    idpedido INT NOT NULL AUTO_INCREMENT,
    datapedido DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    itempedido JSON NOT NULL,
    antecipado BOOLEAN NOT NULL DEFAULT FALSE,
    idcliente INT NOT NULL,
    retirado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (idpedido),
    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (idcliente) REFERENCES cliente (idcliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

