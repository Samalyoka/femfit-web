package com.femfit.service.impl;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Locale;

/**
 * Formats dates as "21 Jun 2026" / "21 июн. 2026" / "21 мау. 2026"
 * (day, abbreviated month, year) without relying on the JVM's built-in
 * locale data for month names.
 *
 * <p><b>Why this exists:</b> Java's standard library has no built-in
 * {@code DateFormatSymbols} for Kazakh under the {@code "kz"} locale code
 * used by this app's language switcher (the correct ISO 639-1 code is
 * {@code "kk"}, and even that isn't guaranteed to have complete CLDR data
 * on every JDK build). {@code #temporals.format(date, 'd MMM yyyy')} in
 * Thymeleaf therefore silently falls back to English month abbreviations
 * for Kazakh, even though the day/year are correct and Russian works
 * fine. This service sidesteps the JVM locale data entirely: month names
 * come from {@code messages_*.properties} (the same translation files
 * already used for everything else), so Kazakh is exactly as reliable as
 * any other label in the app.</p>
 *
 * <p>Exposed to Thymeleaf templates as the {@code @dateFormatter} bean
 * (see {@code th:text="${@dateFormatter.format(someDate)}"}).</p>
 */
@Service("dateFormatter")
public class LocalizedDateFormatter {

    private static final String[] MONTH_KEYS = {
            "date.month.jan", "date.month.feb", "date.month.mar", "date.month.apr",
            "date.month.may", "date.month.jun", "date.month.jul", "date.month.aug",
            "date.month.sep", "date.month.oct", "date.month.nov", "date.month.dec"
    };

    private static final String[] WEEKDAY_KEYS = {
            "date.weekday.mon", "date.weekday.tue", "date.weekday.wed", "date.weekday.thu",
            "date.weekday.fri", "date.weekday.sat", "date.weekday.sun"
    };

    private final MessageSource messageSource;

    public LocalizedDateFormatter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Formats using the current request's locale (from
     * {@link LocaleContextHolder}). Accepts {@link LocalDate} or
     * {@link LocalDateTime} (or any {@link Temporal} exposing
     * {@code getDayOfMonth()}/{@code getMonthValue()}/{@code getYear()}).
     *
     * @param date the date to format; {@code null} returns an empty string
     * @return e.g. "21 Jun 2026", or "21 мау. 2026" for Kazakh
     */
    public String format(Temporal date) {
        return format(date, LocaleContextHolder.getLocale());
    }

    /**
     * Formats using an explicit locale — useful for batch/report contexts
     * outside a web request where {@link LocaleContextHolder} isn't set.
     *
     * @param date   the date to format; {@code null} returns an empty string
     * @param locale the locale to format month names in
     * @return e.g. "21 Jun 2026"
     */
    public String format(Temporal date, Locale locale) {
        if (date == null) {
            return "";
        }
        int day = extractDay(date);
        int month = extractMonth(date);
        int year = extractYear(date);

        String monthLabel = messageSource.getMessage(MONTH_KEYS[month - 1], null, locale);
        return day + " " + monthLabel + " " + year;
    }

    /**
     * Formats as month + year only, e.g. "Jun 2026" / "июн. 2026" / "мау. 2026".
     * Used for member registration date display.
     *
     * @param date the date to format; {@code null} returns an empty string
     * @return e.g. "Jun 2026"
     */
    public String formatMonthYear(Temporal date) {
        if (date == null) {
            return "";
        }
        Locale locale = LocaleContextHolder.getLocale();
        int month = extractMonth(date);
        int year = extractYear(date);
        String monthLabel = messageSource.getMessage(MONTH_KEYS[month - 1], null, locale);
        return monthLabel + " " + year;
    }

    /**
     * Formats a class schedule datetime as "Mon, 21 Jun · 14:00" with
     * all locale-sensitive parts (weekday and month abbreviation) from
     * i18n keys rather than JVM locale data.
     *
     * @param dateTime the scheduled datetime; {@code null} returns an empty string
     * @return e.g. "Mon, 21 Jun · 14:00" or "Дс, 21 мау. · 14:00" for Kazakh
     */
    public String formatSchedule(Temporal dateTime) {
        if (dateTime == null) {
            return "";
        }
        Locale locale = LocaleContextHolder.getLocale();
        int day = extractDay(dateTime);
        int month = extractMonth(dateTime);
        int hour = extractHour(dateTime);
        int minute = extractMinute(dateTime);
        int dowIndex = extractDayOfWeekIndex(dateTime);  // 0=Mon … 6=Sun

        String weekday = messageSource.getMessage(WEEKDAY_KEYS[dowIndex], null, locale);
        String monthLabel = messageSource.getMessage(MONTH_KEYS[month - 1], null, locale);
        return weekday + ", " + day + " " + monthLabel + " · "
                + String.format("%02d:%02d", hour, minute);
    }

    private int extractDay(Temporal t) {
        if (t instanceof LocalDate d) return d.getDayOfMonth();
        if (t instanceof LocalDateTime d) return d.getDayOfMonth();
        throw new IllegalArgumentException("Unsupported temporal type: " + t.getClass());
    }

    private int extractMonth(Temporal t) {
        if (t instanceof LocalDate d) return d.getMonthValue();
        if (t instanceof LocalDateTime d) return d.getMonthValue();
        throw new IllegalArgumentException("Unsupported temporal type: " + t.getClass());
    }

    private int extractYear(Temporal t) {
        if (t instanceof LocalDate d) return d.getYear();
        if (t instanceof LocalDateTime d) return d.getYear();
        throw new IllegalArgumentException("Unsupported temporal type: " + t.getClass());
    }

    private int extractHour(Temporal t) {
        if (t instanceof LocalDateTime d) return d.getHour();
        throw new IllegalArgumentException("No hour in: " + t.getClass());
    }

    private int extractMinute(Temporal t) {
        if (t instanceof LocalDateTime d) return d.getMinute();
        throw new IllegalArgumentException("No minute in: " + t.getClass());
    }

    /** Returns 0 for Monday … 6 for Sunday (matching WEEKDAY_KEYS array). */
    private int extractDayOfWeekIndex(Temporal t) {
        if (t instanceof LocalDateTime d) return d.getDayOfWeek().getValue() - 1;
        if (t instanceof LocalDate d) return d.getDayOfWeek().getValue() - 1;
        throw new IllegalArgumentException("No day-of-week in: " + t.getClass());
    }
}
