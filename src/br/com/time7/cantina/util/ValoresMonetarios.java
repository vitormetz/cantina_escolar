package br.com.time7.cantina.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Regras comuns aos valores que serão gravados como DECIMAL(10,2). */
public final class ValoresMonetarios {
    public static final BigDecimal MAXIMO = new BigDecimal("99999999.99");

    private static final Pattern NUMERO_SIMPLES =
            Pattern.compile("[0-9]+(?:[.,][0-9]{1,2})?");
    private static final Pattern NUMERO_BRASILEIRO =
            Pattern.compile("[1-9][0-9]{0,2}(?:\\.[0-9]{3})+,[0-9]{1,2}");

    private ValoresMonetarios() {
        // Classe utilitária: não precisa ser instanciada.
    }

    /**
     * Aceita 10,50, 10.50 e 1.234,56. Campo vazio mantém o padrão 0,00.
     * O agrupamento por milhar exige a vírgula decimal para evitar ambiguidade:
     * 1.234 sozinho poderia significar um decimal com três casas.
     */
    public static BigDecimal converter(String texto, String campo) {
        if (texto == null) {
            throw new IllegalArgumentException(campo + " não foi informado.");
        }
        String valor = texto.trim();
        if (valor.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (NUMERO_BRASILEIRO.matcher(valor).matches()) {
            valor = valor.replace(".", "").replace(',', '.');
        } else if (NUMERO_SIMPLES.matcher(valor).matches()) {
            valor = valor.replace(',', '.');
        } else {
            throw new IllegalArgumentException(campo
                    + " deve ser um valor positivo ou zero com até duas casas decimais"
                    + " (ex.: 10,50 ou 1.234,56).");
        }
        return validar(new BigDecimal(valor), campo);
    }

    /** Normaliza para duas casas sem arredondar ou perder centavos. */
    public static BigDecimal validar(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " não foi informado.");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
        if (valor.compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException(campo
                    + " não pode ultrapassar 99.999.999,99 (DECIMAL(10,2)).");
        }
        try {
            // Zeros extras são permitidos em objetos (10.500), mas não se arredonda 10.501.
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException excecao) {
            throw new IllegalArgumentException(campo
                    + " deve ter no máximo duas casas decimais.", excecao);
        }
    }
}
