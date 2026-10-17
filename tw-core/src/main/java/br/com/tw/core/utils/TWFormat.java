package br.com.tw.core.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TWFormat {
    // Array com as letras de grandeza (adicionado até Quintilhões)
    private static final String[] SUFIXOS = new String[]{"", "K", "M", "B", "T", "Q", "QQ", "S", "SS"};

    // Força o formato PT-BR (ponto para milhares, vírgula para decimais)
    private static final DecimalFormat FORMATADOR = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

    public static String formatarDinheiro(double valor) {
        if (valor < 1000) {
            return FORMATADOR.format(valor);
        }

        int index = 0;
        while (valor >= 1000 && index < SUFIXOS.length - 1) {
            valor /= 1000;
            index++;
        }

        // Remove .00 se o número for exato e já tiver sufixo para ficar ainda mais limpo (Opcional)
        String resultado = FORMATADOR.format(valor);
        if (resultado.endsWith(",00")) {
            resultado = resultado.substring(0, resultado.length() - 3);
        }

        return resultado + SUFIXOS[index];
    }
}