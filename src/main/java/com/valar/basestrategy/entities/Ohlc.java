package com.valar.basestrategy.entities;

import java.util.ArrayList;
import java.util.List;

import static com.valar.basestrategy.utils.ValarUtils.getInMinutes;

public class Ohlc {
    public String lastLn = "", ln = "", dnt, date, lastDayDate, time;
    public int hr, min;
    public int mins;
    public float open, high, low, close, lastDayClose, lastMinClose, volume;

    public float currentDayHigh=-Float.MAX_VALUE;
    public float   currentDayLow=Float.MAX_VALUE;
    public float prevDayHigh=-Float.MAX_VALUE;
    public float prevDayLow=Float.MAX_VALUE;


    public int volumePeriod;
    public List<Float> volumes = new ArrayList<>();

    public Ohlc(String ln) {
        update(ln);
    }

    public Ohlc(Ohlc ohlc) {
        update(ohlc.ln);
    }

    public Ohlc(float open, float high, float low, float close) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public Ohlc() {}

    public void update(String ln) {
        String[] splits = ln.split(",");
        lastLn = this.ln;
        this.ln = ln;
        dnt = splits[0];

        if (dnt.contains(" ")) {
            String[] dntSplits = dnt.split(" ");
            if (dntSplits.length < 2) {
                System.err.println("Error: No time found in dnt: " + dnt + " | Line: " + ln);
                throw new IllegalArgumentException("No time in date-time field: " + ln);
            }
            date = dntSplits[0];
            time = dntSplits[1];
            String[] timeSplits = time.split(":");
            hr = Integer.parseInt(timeSplits[0]);
            min = Integer.parseInt(timeSplits[1]);
            mins = getInMinutes(hr, min);
        } else {
            date = dnt;
            time = null;
        }
        open = Float.parseFloat(splits[1]);
        high = Float.parseFloat(splits[2]);
        low = Float.parseFloat(splits[3]);
        lastMinClose = close;
        close = Float.parseFloat(splits[4]);



        if ("09:15".equals(time)) {
            prevDayHigh = currentDayHigh;
            prevDayLow = currentDayLow;
            currentDayHigh = high;
            currentDayLow = low;
        }else{
            currentDayHigh = Math.max(currentDayHigh, high);
            currentDayLow  = Math.min(currentDayLow , low);
        }

       if ("15:29".equals(time) && close != 0) {
            lastDayClose = Float.parseFloat(splits[4]);
            lastDayDate = date;
        }

        if (splits.length > 5) {
            volume = Float.parseFloat(splits[5]);
            if (volumePeriod > 0) {
                volumes.add(volume);
                if (volumes.size() > volumePeriod)
                    volumes.remove(0);
            }
        }
    }

    /*
    public Double getVolumesAvg(){
        double volSum=0;
        for(int i=0;i<volumes.size()-1;i++){
            volSum+=volumes.get(i);
        }
        return volSum/volumes.size()-1;
    }
    */

    public void storeVolumesForPeriod(int volumePeriod) {
        this.volumePeriod = volumePeriod;
    }

    public boolean isVolumeConditionSatisfied(float minVolume, float minAvgVolume) {
        if (volumePeriod == 0 || volumes.size() < volumePeriod) return true;
        for (float volume : volumes)
            if (volume < minVolume)
                return false;
        return volumes.stream().mapToDouble(d -> d).average().orElse(0) >= minAvgVolume;
    }

    public String toString() {
        return dnt + " open " + open + " ,high " + high + " ,low " + low + " ,close " + close + " ,lastMinClose " + lastMinClose;
    }
}