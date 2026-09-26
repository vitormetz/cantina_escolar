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
    UNIQUE KEY uk_cliente_nome (nomecliente),
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
    -- Garante que cada cliente tenha somente um pedido ainda não retirado.
    -- Pedidos concluídos geram NULL e deixam de participar da restrição UNIQUE.
    cliente_pendente INT GENERATED ALWAYS AS (
        CASE WHEN retirado = FALSE THEN idcliente ELSE NULL END
    ) STORED,
    PRIMARY KEY (idpedido),
    UNIQUE KEY uk_pedido_cliente_pendente (cliente_pendente),
    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (idcliente) REFERENCES cliente (idcliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);
