package com.valar.basestrategy.entities.indicators;

import com.valar.basestrategy.entities.Ohlc;

import java.util.LinkedList;
import java.util.List;


//For Target Condition take previousATR for comparing with high or low as a/c to short or long

public class ATREntity {
    public List<Double> l = new LinkedList();
    private int periodsForATR;
    private double previousClose;
    private double atr;
    public ATREntity(int periodsForATR){
        this.periodsForATR = periodsForATR;
    }

    public int getPeriod(){
        return periodsForATR;
    }

    public void calculateATR(Ohlc ohlc) {
        double tr = calculateTR(ohlc);

        if(l.size()< periodsForATR-1)
            l.add(tr);
        else {
            if (l.size() == periodsForATR-1){
                l.add(tr);
                atr = getAvg();
            }else
                atr = getATRCalculated(tr);
        }
        previousClose = ohlc.close;
    }

    private double calculateTR(Ohlc ohlc) {
        double a = ohlc.high - ohlc.low, b = Math.abs(ohlc.high - previousClose),
                c = Math.abs(ohlc.low - previousClose);

        double d;
        d = a>b?a:b;
        d = d>c?d:c;

        return d;

    }

//	public float calculateAvgTR() {
//		return totTr / KeyStore.periodsForATR;
//	}

    private double getATRCalculated(double tr) {
        if(l.size()!=0)
            l.remove(0);
        l.add(tr);
        return getAvg();
    }

    private double getAvg(){
        double avg=0;
        for(double f:l) {
            avg+=f;
        }
        return avg/ periodsForATR;
    }


    public double getATR(double close){
        if(this.atr==0)
            this.atr = 0.0001 * close;
        return atr;
    }

}

