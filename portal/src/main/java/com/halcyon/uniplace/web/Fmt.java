package com.halcyon.uniplace.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** USA-02 and USA-04: display formats, available in templates as {@code @fmt}. */
@Component("fmt")
public class Fmt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    public String dateTime(LocalDateTime d) {
        return d == null ? "" : d.format(DATE_TIME);
    }

    public String time(LocalDateTime d) {
        return d == null ? "" : d.toLocalTime().format(TIME);
    }

    public String time(LocalTime t) {
        return t == null ? "" : t.format(TIME);
    }

    public String money(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return "QAR " + df.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public String label(Enum<?> e) {
        if (e == null) {
            return "";
        }
        String s = e.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
