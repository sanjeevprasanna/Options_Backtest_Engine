package com.valar.basestrategy.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.valar.basestrategy.application.PropertiesReader.properties;
import static com.valar.basestrategy.utils.ValarUtils.*;

public class KeyValues {
    public String ln;
    public float costPercent,hpCostPercent;
    public int sno, indexType, startTime, cutOffTime, endTime, candlePeriod;
    public int maxOverlap,tradeGap;

    public String tradeType;
    private int iter;
    public boolean invalidStartTime,positional;
    public float atrFrom,atrTo=1000;
    public final int emaPeriod;
    public  final int rsiPeriod;
    public final boolean usePivots ;
    public final int rsiLong,rsiShort;
    public KeyValues(String ln) {
        this.ln = ln;
        String[] splits = ln.split(",");
        sno = Integer.parseInt(splits[iter++]);
        indexType = Integer.parseInt(splits[iter++]);
        tradeType = splits[iter++];
        costPercent = Float.parseFloat(splits[iter++]);
        hpCostPercent = Float.parseFloat(splits[iter++]);
        startTime = getInMinutes(splits[iter++]);
        cutOffTime = getInMinutes(splits[iter++]);
        endTime = getInMinutes(splits[iter++]);
        positional = Boolean.parseBoolean(splits[iter++]);
        candlePeriod = Integer.parseInt(splits[iter++]);
        emaPeriod = Integer.parseInt(splits[iter++]);
        rsiPeriod = Integer.parseInt(splits[iter++]);
        usePivots = Boolean.parseBoolean(splits[iter++]);
        maxOverlap = Integer.parseInt(splits[iter++]);
        tradeGap = Integer.parseInt(splits[iter++]);
        rsiLong = Integer.parseInt(splits[iter++]);
        rsiShort = Integer.parseInt(splits[iter++]);
    }

    public static List<String> getTimeFrames(int timeFrame) {
        int marketClosingTime = getInMinutes(15, 29);

        List<String> timeFrames = new ArrayList<>();
        int hr = 9, min = 15;
        String lastSavedTime = null;
        int periodCounter = 0;
        String time;
        while (hr < 15 || min < 31) {
            if (hr < 10) time = "0" + hr + ":";
            else time = hr + ":";
            if (min < 10) time += "0" + min;
            else time += min;

            periodCounter++;
            if ((periodCounter - 1) % timeFrame == 0) {
                if (lastSavedTime != null) timeFrames.add(lastSavedTime);
                lastSavedTime = time;
            }

            min++;
            if (min > 59) {
                hr++;
                min = 0;
            }
        }

        if (!lastSavedTime.equalsIgnoreCase(timeFrames.get(timeFrames.size() - 1)) &&
                getInMinutes(lastSavedTime) <= marketClosingTime) timeFrames.add(lastSavedTime);

        return timeFrames;
    }

    private String getCorrectTimeForm(String s) {
        int h = Integer.parseInt(s.split(":")[0]);
        if (h < 10 && !s.startsWith("0"))
            s = "0" + s;
        return s;
    }

    @Override
    public String toString() {
        return "KeyValues{" +
                "costPercent=" + costPercent +
                ", startTime=" + startTime +
                ", sno=" + sno +
                ", cutOffTime=" + cutOffTime +
                ", endTime=" + endTime +
                ", iter=" + iter +
                ", invalidStartTime=" + invalidStartTime +
                '}';
    }

    public static void setIndexAttribs(int BNOrN) {
        if (BNOrN == 0) {
            indexFile = properties.getProperty("bankNifty1Min");
            indexDayFile = properties.getProperty("bankNiftyDay");
            tradingDatesFile = properties.getProperty("bankNiftyTradingDates");
            expiryDatesFile = properties.getProperty("bankNiftyExpiryDates");
            strikePlus = 100;
            dataAvailableFromYr = 16;
            optionsDataPath = properties.getProperty("BNOptionFilesPath");
            try {
                indexLines = Files.readAllLines(Paths.get(indexFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } /*else {
            indexFile = properties.getProperty("nifty1Min");
            indexDayFile = properties.getProperty("niftyDay");
            tradingDatesFile = properties.getProperty("niftyTradingDates");
            expiryDatesFile = properties.getProperty("niftyExpiryDates");
            strikePlus = 50;
            dataAvailableFromYr = 19;
            optionsDataPath = properties.getProperty("NiftyOptionFilesPath");
            try {
                indexLines = Files.readAllLines(Paths.get(indexFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
//        listOfFiles = getAllFilesOfFolder("");
    }
}
