package com.valar.basestrategy.utils;
import java.io.*;

public class OptionPremiumLoader {
    // Reads option premium at target datetime from file
    // Assumes headers: datetime,open,high,low,close,volume
    public static float getPremiumAt(String optionFile, String datetime) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(optionFile));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(datetime)) {
                String[] arr = line.split("\t|,");
                return Float.parseFloat(arr[4]); // close price
            }
        }
        br.close();
        throw new RuntimeException("Datetime " + datetime + " not found in " + optionFile);
    }
}