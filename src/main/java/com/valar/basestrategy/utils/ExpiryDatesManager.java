package com.valar.basestrategy.utils;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExpiryDatesManager {
    // Map sorted by date
    private final TreeMap<LocalDate, String> expiryDateMap = new TreeMap<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

    public ExpiryDatesManager(String csvPath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(csvPath));
        String line;
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\t|,"); // allow tab/csv
            // Parse as LocalDateTime, then take just the date
            LocalDateTime ldt = LocalDateTime.parse(arr[0].trim(), dtf); // Parse "02-06-16 09:15"
            LocalDate date = ldt.toLocalDate();
            String symbol = arr[1].trim();
            expiryDateMap.put(date, symbol);
        }
        br.close();
    }

    public String getNearestExpiry(LocalDate tradeDate) {
        Map.Entry<LocalDate, String> entry = expiryDateMap.ceilingEntry(tradeDate);
        if (entry == null) throw new RuntimeException("No expiry found after date: " + tradeDate);
        return entry.getValue();
    }

    public String getNearestExpiry(String datetime) {
        // "18-11-16 09:15" -> parse as date
        LocalDate date = LocalDate.parse(datetime.split(" ")[0], DateTimeFormatter.ofPattern("dd-MM-yy"));
        return getNearestExpiry(date);
    }
}