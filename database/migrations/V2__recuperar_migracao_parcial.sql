-- Use somente quando a primeira execução da V2 antiga parou no segundo ALTER.
USE cantina_escolar;

UPDATE pedido
SET dia_cardapio = WEEKDAY(datapedido) + 1
WHERE dia_cardapio IS NULL;

UPDATE pedido SET pago = TRUE;

UPDATE pedido p
JOIN cliente c ON c.idcliente = p.idcliente
SET p.saldo_apos = c.saldo
WHERE p.saldo_apos IS NULL;

ALTER TABLE pedido
    MODIFY COLUMN dia_cardapio TINYINT UNSIGNED NOT NULL,
    MODIFY COLUMN saldo_apos DECIMAL(10,2) NOT NULL,
    ADD CONSTRAINT ck_pedido_dia_cardapio CHECK (dia_cardapio BETWEEN 1 AND 7),
    ADD CONSTRAINT ck_pedido_saldo_apos CHECK (saldo_apos >= 0);

CREATE TABLE pedido_ativo (
    idcliente INT NOT NULL,
    PRIMARY KEY (idcliente),
    CONSTRAINT fk_pedido_ativo_cliente
        FOREIGN KEY (idcliente) REFERENCES cliente (idcliente)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

INSERT INTO pedido_ativo (idcliente)
SELECT idcliente FROM pedido WHERE retirado = FALSE;

CREATE TABLE pedido_item (
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

INSERT INTO pedido_item (idpedido, nome, preco_unitario, quantidade)
SELECT p.idpedido, antigo.nome, antigo.preco, antigo.quantidade
FROM pedido p
JOIN JSON_TABLE(
    IF(JSON_VALID(p.itempedido), p.itempedido, JSON_OBJECT('itens', JSON_ARRAY())),
    '$.itens[*]' COLUMNS (
        nome VARCHAR(150) PATH '$.nome',
        preco DECIMAL(10,2) PATH '$.preco',
        quantidade INT PATH '$.quantidade'
    )
) AS antigo
WHERE antigo.nome IS NOT NULL
  AND antigo.preco > 0
  AND antigo.quantidade BETWEEN 1 AND 99;

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
