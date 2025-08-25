package com.valar.basestrategy.entities.indicators;


import com.valar.basestrategy.entities.Ohlc;

/**
 * This is a generic class to store VWAP intermediate state to calculate future values.
 */
public class VWAPEntity {
    private double volumePriceTotal;
    private double sumVolume;
    private double vwap;private String date;

    public void setVWAP(Ohlc ohlc) {

        if(date==null || !date.equals(ohlc.date)) {
            sumVolume=0;
            volumePriceTotal = 0;
            date = ohlc.date;
        }
        double typical = (ohlc.high + ohlc.low + ohlc.close) / 3;
        sumVolume += ohlc.volume;
        volumePriceTotal += typical * ohlc.volume;
        vwap = volumePriceTotal / sumVolume;
    }

    public double getVwap(){
        return vwap;
    }
}