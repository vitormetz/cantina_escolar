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
    dia_cardapio TINYINT UNSIGNED NOT NULL,
    pago BOOLEAN NOT NULL DEFAULT FALSE,
    retirado BOOLEAN NOT NULL DEFAULT FALSE,
    email_enviado BOOLEAN NOT NULL DEFAULT FALSE,
    email_erro VARCHAR(500) NULL,
    saldo_apos DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (idpedido),
    CONSTRAINT ck_pedido_dia_cardapio CHECK (dia_cardapio BETWEEN 1 AND 7),
    CONSTRAINT ck_pedido_saldo_apos CHECK (saldo_apos >= 0),
    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (idcliente) REFERENCES cliente (idcliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- A PK garante um único pedido ativo por cliente. Os gatilhos abaixo mantêm
-- esta tabela automaticamente e evitam a limitação do MySQL entre coluna
-- gerada e a FK com ON UPDATE CASCADE existente em pedido.
CREATE TABLE IF NOT EXISTS pedido_ativo (
    idcliente INT NOT NULL,
    PRIMARY KEY (idcliente),
    CONSTRAINT fk_pedido_ativo_cliente
        FOREIGN KEY (idcliente) REFERENCES cliente (idcliente)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS pedido_item (
    id_pedido_item INT NOT NULL AUTO_INCREMENT,
    idpedido INT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    quantidade INT UNSIGNED NOT NULL,
    PRIMARY KEY (id_pedido_item),
    KEY idx_pedido_item_pedido (idpedido),
    CONSTRAINT ck_pedido_item_preco CHECK (preco_unitario > 0),
    CONSTRAINT ck_pedido_item_quantidade CHECK (quantidade BETWEEN 1 AND 99),
    CONSTRAINT fk_pedido_item_pedido
        FOREIGN KEY (idpedido) REFERENCES pedido (idpedido)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

DELIMITER //
CREATE TRIGGER trg_pedido_ativo_inserir
AFTER INSERT ON pedido
FOR EACH ROW
BEGIN
    IF NEW.retirado = FALSE THEN
        INSERT INTO pedido_ativo (idcliente) VALUES (NEW.idcliente);
    END IF;
END//

CREATE TRIGGER trg_pedido_ativo_atualizar
AFTER UPDATE ON pedido
FOR EACH ROW
BEGIN
    IF OLD.retirado = FALSE
            AND (NEW.retirado = TRUE OR OLD.idcliente <> NEW.idcliente) THEN
        DELETE FROM pedido_ativo WHERE idcliente = OLD.idcliente;
    END IF;
    IF NEW.retirado = FALSE
            AND (OLD.retirado = TRUE OR OLD.idcliente <> NEW.idcliente) THEN
        INSERT INTO pedido_ativo (idcliente) VALUES (NEW.idcliente);
    END IF;
END//

CREATE TRIGGER trg_pedido_ativo_excluir
AFTER DELETE ON pedido
FOR EACH ROW
BEGIN
    IF OLD.retirado = FALSE THEN
        DELETE FROM pedido_ativo WHERE idcliente = OLD.idcliente;
    END IF;
END//
DELIMITER ;
