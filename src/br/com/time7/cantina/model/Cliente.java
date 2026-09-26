package br.com.time7.cantina.model;

import java.math.BigDecimal;

/**
 * Representa um registro da tabela cliente.
 *
 * BigDecimal é usado nos valores monetários para corresponder às colunas
 * DECIMAL(10,2) e evitar erros de arredondamento do tipo double.
 */
public class Cliente {
    private final int idCliente;
    private final String nomeCliente;
    private final String nomeResponsavel;
    private final BigDecimal saldo;
    private final BigDecimal limiteSaldo;
    private final String emailResponsavel;
    private final String alergias;

    public Cliente(
            int idCliente,
            String nomeCliente,
            String nomeResponsavel,
            BigDecimal saldo,
            BigDecimal limiteSaldo,
            String emailResponsavel,
            String alergias) {
        this.idCliente = idCliente;
        this.nomeCliente = nomeCliente;
        this.nomeResponsavel = nomeResponsavel;
        this.saldo = saldo;
        this.limiteSaldo = limiteSaldo;
        this.emailResponsavel = emailResponsavel;
        this.alergias = alergias;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getLimiteSaldo() {
        return limiteSaldo;
    }

    public String getEmailResponsavel() {
        return emailResponsavel;
    }

    public String getAlergias() {
        return alergias;
    }

    /** O combo da tela de pedidos identifica o cliente somente pelo nome. */
    @Override
    public String toString() {
        return nomeCliente;
    }
}
