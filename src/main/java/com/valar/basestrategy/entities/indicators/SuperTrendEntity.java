package com.valar.basestrategy.entities.indicators;

import com.valar.basestrategy.entities.Ohlc;
import lombok.Getter;
import org.ta4j.core.indicators.ATRIndicator;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

public class SuperTrendEntity {
  double ub;
  double lb;
  double m;
  @Getter
  private double st,lastSt;
  double cm1;
//  public int period;
  double stm1;
  ATRIndicator atrIndicatorForSuperTrend;

  @Getter
  private String lastToLastSuperTrendSign="",lastSuperTrendSign="",superTrendSign="";
  private boolean belongsToLeadingCandle;

  public SuperTrendEntity(boolean belongsToLeadingCandle,double multiplier, ATRIndicator atrIndicatorForSuperTrend){
    this.belongsToLeadingCandle = belongsToLeadingCandle;
    m = multiplier;
    this.atrIndicatorForSuperTrend = atrIndicatorForSuperTrend;
  }

  public double getMultiplier(){
    return m;
  }


  public void update(Ohlc ohlc, int parser) {
//    if(period!=1){
//      double doubleParser= (double) parser /period;
//      System.out.println(" Passed parser : "+parser);
//      System.out.println("double Parser : "+doubleParser);
//      System.out.println(period+" currentClose "+ohlc.close);
//      if((period==30 || period==60) && parser>400 && ohlc.time.equalsIgnoreCase("15:29")) {
//        doubleParser=Math.ceil(doubleParser);
//      }
//      parser = (int) doubleParser;
//      System.out.println("Calculated : "+parser);
//      Bar bar = atrIndicatorForSuperTrend.getBarSeries().getBar(parser);
//      ohlc = new Ohlc(bar.getOpenPrice().floatValue(),bar.getHighPrice().floatValue(),
//              bar.getLowPrice().floatValue(), bar.getClosePrice().floatValue());
//      System.out.println("\t\t"+bar.getClosePrice()+" "+bar.getDateName());
//      System.out.println();
//
//    }
    lastToLastSuperTrendSign = lastSuperTrendSign;
    lastSuperTrendSign = superTrendSign;
    lastSt = st;
    double atr = atrIndicatorForSuperTrend.getValue(parser).doubleValue();

    if (atr == 0) {
      cm1 = ohlc.close;
      return;
    }
    double ubb = (ohlc.high + ohlc.low) / 2 + m * atr;
    double lbb = (ohlc.high + ohlc.low) / 2 - m * atr;
    double ubTm1 = ub;
    double lbTm1 = lb;
    if (ub > ubb || ub < cm1) {
      ub = ubb;
    }
    if (lb < lbb || lb > cm1) {
      lb = lbb;
    }
    stm1 = st;
    if (st == ubTm1) {
      if (ohlc.close <= ub) {
        st = ub;
      } else {
        st = lb;
      }
    } else if (st == lbTm1) {
      if (ohlc.close >= lb) {
        st = lb;
      } else {
        st = ub;
      }
    }
    this.cm1 = ohlc.close;

    if(st==ub) superTrendSign = "red";
    else if(st==lb) superTrendSign = "green";
  }

    public String print(LocalDateTime d, double ubb, double lbb) {
    DecimalFormat df = new DecimalFormat("#.##");
    return "SuperTrendEntity{" +
      " Date=" + d +
      ", ubb=" + df.format(ubb) +
      ", lbb=" + df.format(lbb) +
      ", ub=" + df.format(ub) +
      ", lb=" + df.format(lb) +
      ", st=" + df.format(st) +
      ", cm1=" + df.format(cm1) +
      '}';
  }

  public boolean exitShort() {
    return stm1 == ub && st == lb;
  }

  public boolean exitLong() {
    return stm1 == lb && st == ub;
  }

  @Override
  public String toString() {
    return "SuperTrendEntity{" +
            "ub=" + ub +
            ", lb=" + lb +
            ", m=" + m +
            ", st=" + st +
            ", cm1=" + cm1 +
            ", stm1=" + stm1 +
            ", superTrendSign='" + superTrendSign + '\'' +
            '}';
  }
}
