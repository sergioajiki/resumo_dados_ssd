package br.gov.ms.saude.ssd.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Serviço responsável pela transformação (T do ETL) de valores brutos do Qlik Sense
 * para tipos Java fortemente tipados.
 *
 * <p>O Qlik Sense retorna todos os valores como strings ou números genéricos.
 * Este serviço converte cada tipo para o equivalente Java correto, tratando
 * nulos, strings vazias e formatos inesperados de forma defensiva.</p>
 *
 * <p>Componentes internos:</p>
 * <ul>
 *   <li><strong>DateParser</strong> — converte strings de data para {@link LocalDate}</li>
 *   <li><strong>AgeCalculator</strong> — calcula faixa etária a partir de {@link LocalDate}</li>
 *   <li><strong>TimeConverter</strong> — converte horário numérico para {@link LocalTime}</li>
 *   <li><strong>NullHandler</strong> — normaliza nulos e strings vazias/"-"/null-texto</li>
 * </ul>
 */
@Service
public class FieldTransformerService {

    private static final Logger log = LoggerFactory.getLogger(FieldTransformerService.class);

    /** Formatos de data aceitos pela fonte Qlik (tentados em ordem). */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    /** Formatos de timestamp aceitos. */
    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );

    // -------------------------------------------------------------------------
    // DateParser
    // -------------------------------------------------------------------------

    /**
     * Converte uma string de data para {@link LocalDate}.
     *
     * <p>Tenta os formatos em {@code DATE_FORMATTERS} sequencialmente.
     * Retorna {@code null} se a entrada for nula, vazia, ou não corresponder
     * a nenhum formato conhecido. Erros de parse são logados em DEBUG.</p>
     *
     * @param value string de data vinda do Qlik (ex: "15/03/2025", "2025-03-15")
     * @return {@link LocalDate} parseada, ou {@code null} se inválida
     */
    public LocalDate parseDate(Object value) {
        String str = asString(value);
        if (str == null) return null;

        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(str, fmt);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato
            }
        }

        log.debug("Não foi possível parsear data: '{}'", str);
        return null;
    }

    /**
     * Converte uma string de timestamp para {@link LocalDateTime}.
     *
     * <p>Se o valor contiver apenas data (sem hora), delega para {@link #parseDate(Object)}
     * e adiciona meia-noite como hora.</p>
     *
     * @param value string de timestamp vinda do Qlik
     * @return {@link LocalDateTime} parseado, ou {@code null} se inválido
     */
    public LocalDateTime parseDateTime(Object value) {
        String str = asString(value);
        if (str == null) return null;

        for (DateTimeFormatter fmt : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(str, fmt);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato
            }
        }

        // Tenta como data pura e converte para LocalDateTime com hora 00:00
        LocalDate d = parseDate(value);
        if (d != null) return d.atStartOfDay();

        log.debug("Não foi possível parsear datetime: '{}'", str);
        return null;
    }

    // -------------------------------------------------------------------------
    // AgeCalculator
    // -------------------------------------------------------------------------

    /**
     * Calcula a faixa etária de um paciente a partir da data de nascimento.
     *
     * <p>Faixas utilizadas: "0-17", "18-29", "30-59", "60+".
     * Usa a data de hoje como referência de cálculo da idade.</p>
     *
     * @param dtNascimento data de nascimento do paciente; {@code null} retorna {@code null}
     * @return string da faixa etária correspondente, ou {@code null} se a data for nula
     */
    public String calcularFaixaEtaria(LocalDate dtNascimento) {
        if (dtNascimento == null) return null;

        int idade = LocalDate.now().getYear() - dtNascimento.getYear();
        // Ajuste se ainda não completou aniversário este ano
        if (LocalDate.now().getDayOfYear() < dtNascimento.getDayOfYear()) {
            idade--;
        }

        if (idade < 0) return null;
        if (idade <= 17) return "0-17";
        if (idade <= 29) return "18-29";
        if (idade <= 59) return "30-59";
        return "60+";
    }

    // -------------------------------------------------------------------------
    // TimeConverter
    // -------------------------------------------------------------------------

    /**
     * Converte o campo numérico {@code HR_AGENDAMENTO} do Qlik para {@link LocalTime}.
     *
     * <p>O Qlik armazena horários como fração decimal do dia (ex: 0.5 = 12:00,
     * 0.75 = 18:00). Também aceita inteiros representando minutos desde meia-noite
     * ou strings no formato "HH:mm".</p>
     *
     * @param value valor do campo HR_AGENDAMENTO da fonte
     * @return {@link LocalTime} correspondente, ou {@code null} se inválido
     */
    public LocalTime convertTime(Object value) {
        if (value == null) return null;

        // String no formato HH:mm ou HH:mm:ss
        if (value instanceof String str && !str.isBlank()) {
            try {
                if (str.contains(":")) {
                    return LocalTime.parse(str.length() == 5 ? str + ":00" : str);
                }
            } catch (DateTimeParseException ignored) {
                // tenta interpretação numérica
            }
        }

        // Numérico: fração do dia (0.0 = 00:00, 0.5 = 12:00, 1.0 = 24:00)
        try {
            double fraction = Double.parseDouble(value.toString());
            if (fraction >= 0 && fraction < 1) {
                int totalMinutes = (int) (fraction * 24 * 60);
                return LocalTime.of(totalMinutes / 60, totalMinutes % 60);
            }
            // Inteiro representando minutos desde meia-noite
            int minutes = (int) fraction;
            if (minutes < 1440) {
                return LocalTime.of(minutes / 60, minutes % 60);
            }
        } catch (NumberFormatException ignored) {
            // não é numérico
        }

        log.debug("Não foi possível converter horário: '{}'", value);
        return null;
    }

    // -------------------------------------------------------------------------
    // NullHandler
    // -------------------------------------------------------------------------

    /**
     * Normaliza um valor Object para String, tratando representações de nulo
     * usadas pelo Qlik Sense.
     *
     * <p>Retorna {@code null} para: {@code null}, string vazia, "-", "null", "N/A",
     * "NA", "S/I". Retorna a string com espaços eliminados para valores válidos.</p>
     *
     * @param value valor bruto vindo do Qlik
     * @return string normalizada, ou {@code null} se representar ausência de valor
     */
    public String asString(Object value) {
        if (value == null) return null;
        String str = value.toString().strip();
        if (str.isEmpty() || str.equals("-") || str.equalsIgnoreCase("null")
                || str.equalsIgnoreCase("N/A") || str.equalsIgnoreCase("NA")
                || str.equalsIgnoreCase("S/I")) {
            return null;
        }
        // Campos concatenados do Qlik que ficam só com separadores quando a fonte é vazia
        // Ex: ENDERECO_COMPLETO = "," ou ", , ," quando rua/num/bairro são nulos
        if (str.chars().allMatch(c -> c == ',' || c == ' ' || c == '/' || c == '-')) {
            return null;
        }
        return str;
    }

    /**
     * Normaliza um valor de CNS, retornando {@code null} para strings all-zeros
     * usadas pelo Qlik como sentinela de "CNS desconhecido".
     *
     * @param value valor bruto do campo CNS_PACIENTE
     * @return CNS normalizado, ou {@code null} se ausente ou sentinela
     */
    public String asCns(Object value) {
        String str = asString(value);
        if (str == null) return null;
        // Strings compostas só de zeros são sentinelas Qlik para "CNS desconhecido"
        if (!str.isEmpty() && str.chars().allMatch(c -> c == '0')) return null;
        return str;
    }

    /**
     * Converte um valor Object para Long, retornando {@code null} em caso de falha.
     *
     * @param value valor a converter (Number, String, etc.)
     * @return valor como Long, ou {@code null} se inválido
     */
    public Long asLong(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Number n) return n.longValue();
            String str = value.toString().strip();
            if (str.isEmpty()) return null;
            // Remove decimais se presentes (ex: "12345.0" do Qlik)
            if (str.contains(".")) str = str.substring(0, str.indexOf('.'));
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            log.debug("Não foi possível converter para Long: '{}'", value);
            return null;
        }
    }

    /**
     * Converte um valor Object para Integer, retornando {@code null} em caso de falha.
     *
     * @param value valor a converter
     * @return valor como Integer, ou {@code null} se inválido
     */
    public Integer asInteger(Object value) {
        Long l = asLong(value);
        return l == null ? null : l.intValue();
    }
}
