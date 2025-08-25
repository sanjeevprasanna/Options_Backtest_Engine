package com.valar.basestrategy.entities.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.HashMap;
import java.util.Map;

public class BollingerBand {
    private BollingerBandsUpperIndicator bbu;
    private BollingerBandsLowerIndicator bbl;
    private BollingerBandsMiddleIndicator bbm;
    private final Map<BBType,Double> bbValues = new HashMap<>(),bbLastMinValues = new HashMap<>();

    public enum BBType{
        UB,MB,LB
    }

    public BollingerBand(BarSeries series, int bbPeriod, int bbSd){
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        StandardDeviationIndicator sdi = new StandardDeviationIndicator(closePriceIndicator,bbPeriod);
        bbm = new BollingerBandsMiddleIndicator(new SMAIndicator(closePriceIndicator,bbPeriod));
        Num myNum = DecimalNum.valueOf(bbSd);

        bbu = new BollingerBandsUpperIndicator(bbm, sdi,myNum);
        bbl = new BollingerBandsLowerIndicator(bbm, sdi, myNum);
    }

    public Map<BBType,Double> getBbValues(boolean isLastMin,int parser){
        Map<BBType,Double> values = bbValues;
        if(isLastMin)values = bbLastMinValues;
        values.put(BBType.UB,bbu.getValue(parser).doubleValue());
        values.put(BBType.MB,bbm.getValue(parser).doubleValue());
        values.put(BBType.LB,bbl.getValue(parser).doubleValue());
        return values;
    }

    public String getBbValuesInString(int parser){
        return bbu.getValue(parser)+";"+
        bbm.getValue(parser)+";"+
        bbl.getValue(parser).doubleValue();
    }
}