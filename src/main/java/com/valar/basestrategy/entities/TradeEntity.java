package com.valar.basestrategy.entities;

import com.valar.basestrategy.state.day.IndexDayState;
import com.valar.basestrategy.state.day.DayState;
import com.valar.basestrategy.state.minute.IndexState;
import com.valar.basestrategy.state.minute.State;
import com.valar.basestrategy.tradeAndDayMetrics.TradeMetric;
import com.valar.basestrategy.utils.KeyValues;
import com.valar.basestrategy.utils.PrintAttribs;

import java.util.*;

import static com.valar.basestrategy.service.Strategy.runTill;

public class TradeEntity {
    public float indexCloseAtEntry;
    public PrintAttribs printAttribs = new PrintAttribs();
    private KeyValues kv;
    public boolean canEnter;
    public int tradeId;
    public boolean tradeSquared;
    private Ohlc indexohlc;
    public int holdingPeriod, entryParser = -1;
    private String entryDate, entryTime;
    private double dayAtrPercent, dayAtrPercentage;

    // For minute-based
    private IndexState indexState;
    private Map<Integer, IndexState> indexStateMap;

    // For day-based
    //private IndexDayState indexDayState;
    //private Map<Integer, IndexDayState> indexDayStateMap;


    //for tracking ema,rsi,pivot in orderinfo
    public double entryEma, entryRsi, entryPivot;
    public double exitEma, exitRsi, exitPivot;
    public double hhv, llv;
    public float pdh,pdl,cdh,cdl;
    public List<TradeAttrib> tradeAttribs = new ArrayList<>(2);

    public void setTrade(double entryEma, double entryRsi, double entryPivot,float pdh,float pdl,float cdh,float cdl,double hhv,double llv) {
        this.entryEma=entryEma;
        this.entryRsi=entryRsi;
        this.entryPivot=entryPivot;
        this.pdh=pdh;
        this.pdl=pdl;
        this.cdh=cdh;
        this.cdl=cdl;
        this.hhv=hhv;
        this.llv=llv;
    }



    private ProfitMetric profit = new ProfitMetric(),
            profitPercent = new ProfitMetric(),
            profitPercentBN = new ProfitMetric(),
            profitWithCost = new ProfitMetric(),
            profitPercentWithCost = new ProfitMetric();

    public TradeMetric overAllTradeMetric = new TradeMetric();
    private List<TradeMetric> tradeMetrics = new ArrayList<>();

    public double stopLoss;
    public double target;


    // Constructor for minute-based (optional) trading
    public TradeEntity(int tradeId, double dayAtrPercent, double dayAtrPercentage, KeyValues kv,
                       IndexState indexState, Map<Integer, IndexState> indexStateMap) {
        this.tradeId = tradeId;
        this.kv = kv;
        this.indexState = indexState;
        this.indexStateMap = indexStateMap;
        this.indexohlc = indexState.ohlc;
        this.entryParser = indexState.parser;
        this.entryDate = indexState.ohlc.date;
        this.entryTime = indexState.ohlc.time;
        this.dayAtrPercent = dayAtrPercent;
        this.dayAtrPercentage = dayAtrPercentage;
        loadAttribs(indexState, kv.tradeType.charAt(0), false);
    }

    // Constructor for day-based (positional) trading
    /*public TradeEntity(int tradeId, double dayAtrPercent, double dayAtrPercentage, KeyValues kv,
                       IndexDayState indexDayState, Map<Integer, IndexDayState> indexDayStateMap) {
        this.tradeId = tradeId;
        this.kv = kv;
        this.indexDayState = indexDayState;
        this.indexDayStateMap = indexDayStateMap;
        this.indexohlc = indexDayState.ohlc;
        this.entryDate = indexDayState.ohlc.date;
        this.entryTime = indexDayState.ohlc.time;
        this.dayAtrPercent = dayAtrPercent;
        this.dayAtrPercentage = dayAtrPercentage;
        loadAttribs(indexDayState, kv.tradeType.charAt(0), false);
    }*/

    // Unified method - TradeAttrib for both minute/day state types
    private TradeEntity loadAttribs(Object os, char tradeType, boolean isReload) {
        indexCloseAtEntry = indexohlc.close;
        printAttribs.setVariablesAtEntry(dayAtrPercent, dayAtrPercentage, 0);

        if (isReload) tradeAttribs.clear();

        if (os instanceof State) {
            tradeAttribs.add(new TradeAttrib((State) os, tradeType));
        } else if (os instanceof IndexDayState) {
            tradeAttribs.add(new TradeAttrib((IndexDayState) os, tradeType));
        } else if (os instanceof DayState) {
            tradeAttribs.add(new TradeAttrib((DayState) os, tradeType));
        } else {
            throw new IllegalArgumentException("Unsupported state type: " + os.getClass());
        }
        canEnter = true;
        return this;
    }
    public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }
    public void setTarget(double target) { this.target = target; }

    public void forceExit() {
        if (!this.tradeSquared) {
            exitTrade("ForceExit", "TimeExit");
            //System.out.println("Forced exit");
            this.stopLoss = Double.NaN;
            this.target   = Double.NaN;
        }
    }

    class ProfitMetric {
        public float currentProfit;
        public float profitBooked;
        public float maxProfit = Float.NEGATIVE_INFINITY;
        public float getTotalProfit() { return currentProfit + profitBooked; }
        public void resetCurrentProfit() { currentProfit = 0; }
    }

    //  Both minute and day state types
    public class TradeAttrib {
        Object os;
        public char lOrS;
        float entryPrice;
        public Ohlc ohlcAtEntry, ohlc;
        public TradeMetric tradeMetric;

        public TradeAttrib(State os, char lOrS) {
            this.os = os;
            this.lOrS = lOrS;
            setState(os);
        }
        public TradeAttrib(IndexDayState os, char lOrS) {
            this.os = os;
            this.lOrS = lOrS;
            setDayState(os);
        }
        public TradeAttrib(DayState os, char lOrS) {
            this.os = os;
            this.lOrS = lOrS;
            setDayState(os);
        }

        private void setState(State os) {
            this.ohlc = os.ohlc;
            this.entryPrice = ohlc.close;
            this.ohlcAtEntry = new Ohlc(ohlc.ln);
            this.tradeMetric = new TradeMetric(new Ohlc(os.ohlc), lOrS);
            tradeMetrics.add(tradeMetric);
        }
        private void setDayState(DayState os) {
            this.ohlc = os.ohlc;
            this.entryPrice = ohlc.close;
            this.ohlcAtEntry = new Ohlc(ohlc.ln);
            this.tradeMetric = new TradeMetric(new Ohlc(os.ohlc), lOrS);
            tradeMetrics.add(tradeMetric);
        }

        public float getProfit() {
            if (lOrS == 'l') return (ohlc.close - entryPrice);
            else return (entryPrice - ohlc.close);
        }

        public float getProfitPercent() {
            if (lOrS == 'l') return (ohlc.close - entryPrice) / entryPrice * 100;
            else return (entryPrice - ohlc.close) / entryPrice * 100;
        }

        public float getProfitWithCost() {
            float entryPriceCost = (entryPrice / 100) * kv.costPercent;
            float exitPriceCost = (ohlc.close / 100) * kv.costPercent;
            if (lOrS == 'l') return ((ohlc.close - exitPriceCost) - (entryPrice + entryPriceCost));
            else return ((entryPrice - entryPriceCost) - (ohlc.close + exitPriceCost));
        }

        public float getProfitPercentWithCost() {
            float entryPriceCost = (entryPrice / 100) * kv.costPercent;
            float exitPriceCost = (ohlc.close / 100) * kv.costPercent;
            if (lOrS == 'l')
                return (((ohlc.close - exitPriceCost) - (entryPrice + entryPriceCost)) / (entryPrice + entryPriceCost) * 100);
            else
                return (((entryPrice - entryPriceCost) - (ohlc.close + exitPriceCost)) / (entryPrice - entryPriceCost) * 100);
        }

        public float getProfitPercentBN() {
            if (lOrS == 'l') return (ohlc.close - entryPrice) / indexohlc.close * 100;
            else return (entryPrice - ohlc.close) / indexohlc.close * 100;
        }

        public String[] getInfo() {
            return new String[]{ohlcAtEntry.dnt + "," + ohlcAtEntry.close};
        }
    }

    public float getBNInTermsOfDistance(float distancePercent, char cOrP) {
        float indexClose = indexohlc.close;
        if (cOrP == 'c') indexClose = indexClose + (distancePercent / 100 * indexClose);
        else indexClose = indexClose - (distancePercent / 100 * indexClose);

        return indexClose;
    }

    public void updateProfit() {
        profit.resetCurrentProfit();
        profitPercent.resetCurrentProfit();
        profitPercentBN.resetCurrentProfit();
        profitWithCost.resetCurrentProfit();
        profitPercentWithCost.resetCurrentProfit();

        for (TradeAttrib tradeAttrib : tradeAttribs) {
            profit.currentProfit += tradeAttrib.getProfit();
            profitPercent.currentProfit += tradeAttrib.getProfitPercent();
            profitPercentBN.currentProfit += tradeAttrib.getProfitPercentBN();
            profitWithCost.currentProfit += tradeAttrib.getProfitWithCost();
            profitPercentWithCost.currentProfit += tradeAttrib.getProfitPercentWithCost();
            profit.maxProfit = Math.max(profit.maxProfit, profitPercent.currentProfit);
        }
    }

    public float getTotalProfitPercent() { return profitPercent.getTotalProfit(); }
    public float getTotalProfit() { return profit.getTotalProfit(); }

    public boolean checkExitAndIsToBeExited() {
        updateProfit();
        if ((!kv.positional && indexohlc.mins >= kv.endTime)
                || (indexState != null && indexState.finished)
                || runTill.equals(indexohlc.dnt)) {
            exitTrade("EndTime", "ExitTime " + indexohlc.time);
        }
        return tradeSquared;
    }

    private void updateOverAll() {
        for (TradeMetric tradeMetric : tradeMetrics)
            if (tradeMetric.exitOhlc != null)
                overAllTradeMetric.updateOverAll(tradeMetric);
    }

    private void shiftTrade(TradeAttrib ta, String reasonInfo) {
       /* ta.tradeMetric.calculateOverAllMetricsAndPrint(
                profitPercent.currentProfit, profitWithCost.getTotalProfit(),
                profitPercentWithCost.getTotalProfit(), "", reasonInfo, tradeId,
                holdingPeriod, kv, new Ohlc(ta.ohlc), indexohlc.close,
                "", printAttribs, profit.maxProfit);*/
        ta.tradeMetric.calculateOverAllMetricsAndPrint(
                ta.getProfitPercent(), ta.getProfitWithCost(),
                ta.getProfitPercentWithCost(), "", reasonInfo, tradeId,
                holdingPeriod, kv, new Ohlc(ta.ohlc), indexohlc.close,
                "", printAttribs, profit.maxProfit,
                entryEma, entryRsi, entryPivot,
                exitEma, exitRsi, exitPivot
        );
        profit.profitBooked += ta.getProfit();
        profitPercent.profitBooked += ta.getProfitPercent();
        profitPercentBN.profitBooked += ta.getProfitPercentBN();
        profitWithCost.profitBooked += ta.getProfitWithCost();
        profitPercentWithCost.profitBooked += ta.getProfitPercentWithCost();
    }

    private void exitTrade(String reason, String reasonInfo) {
        // For day-based, entryParser use -1 as fallback
        if (entryParser != -1 && indexState != null)
            holdingPeriod = indexState.parser - entryParser;
        else
            holdingPeriod = 0;

        // Capture exit EMA/RSI/Pivot
        if (indexState != null && kv != null) {
            this.exitEma = indexState.getEmaVal(kv.emaPeriod);
            this.exitRsi = indexState.getRsiVal(kv.rsiPeriod);
            this.exitPivot = indexState.pivotsInitialized ? (Float)((IndexState)indexState).pp : Double.NaN;
        }

        for (TradeAttrib ta : tradeAttribs) {
            Ohlc exitOhlc = new Ohlc(ta.ohlc);
            /*ta.tradeMetric.calculateOverAllMetricsAndPrint(
                    ta.getProfitPercent(), ta.getProfitWithCost(),
                    ta.getProfitPercentWithCost(), reason, reasonInfo, tradeId,
                    holdingPeriod, kv, exitOhlc, indexohlc.close, "", printAttribs, profit.maxProfit
            );*/
            ta.tradeMetric.calculateOverAllMetricsAndPrint(
                    ta.getProfitPercent(),
                    ta.getProfitWithCost(),
                    ta.getProfitPercentWithCost(),
                    reason,
                    reasonInfo,
                    tradeId,
                    holdingPeriod,
                    kv,
                    exitOhlc,
                    indexohlc.close,
                    "", // option, if used
                    printAttribs,
                    profit.maxProfit,
                    entryEma,
                    entryRsi,
                    entryPivot,
                    pdh,
                    cdh,
                    hhv,
                    pdl,
                    cdl,
                    llv,
                    exitEma,
                    exitRsi,
                    exitPivot
            );
        }
        updateOverAll();
        tradeSquared = true;
    }

    public void exit(String reason, String reasonInfo) {
        if (!this.tradeSquared) {
            this.exitTrade(reason, reasonInfo);
        }
    }
}