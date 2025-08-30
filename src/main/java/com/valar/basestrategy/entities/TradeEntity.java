package com.valar.basestrategy.entities;

import com.valar.basestrategy.state.day.IndexDayState;
import com.valar.basestrategy.state.day.DayState;
import com.valar.basestrategy.state.minute.IndexState;
import com.valar.basestrategy.state.minute.State;
import com.valar.basestrategy.tradeAndDayMetrics.TradeMetric;
import com.valar.basestrategy.utils.KeyValues;
import com.valar.basestrategy.utils.PrintAttribs;
import com.valar.basestrategy.utils.OptionPremiumLoader;
import com.valar.basestrategy.utils.PrintWriters;

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
    public String entryDate, entryTime, exitDate, exitTime;
    public int entryIndex, exitIndex;
    public double entryClose, exitClose;
    public double entryAdx, entryPlusDi, entryMinusDi;  // Store indicators at entry
    public double exitAdx, exitPlusDi, exitMinusDi;     // (optional, can fill if desired)
    private double dayAtrPercent, dayAtrPercentage;

    // For minute-based
    private IndexState indexState;
    private Map<Integer, IndexState> indexStateMap;

    //for tracking ema,rsi,pivot in orderinfo
    public double entryEma, entryRsi, entryPivot;
    public double exitEma, exitRsi, exitPivot;
    public double hhv, llv;
    public float pdh, pdl, cdh, cdl;
    public List<TradeAttrib> tradeAttribs = new ArrayList<>(2);

    private ProfitMetric profit = new ProfitMetric();
    private ProfitMetric profitPercent = new ProfitMetric();
    private ProfitMetric profitPercentBN = new ProfitMetric();
    public ProfitMetric profitWithCost = new ProfitMetric();
    public ProfitMetric profitPercentWithCost = new ProfitMetric();

    public TradeMetric overAllTradeMetric = new TradeMetric();
    private List<TradeMetric> tradeMetrics = new ArrayList<>();

    public double stopLoss;
    public double target;

    // Options
    public List<OptionLeg> optionLegs = new ArrayList<>();

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
        this.entryIndex = indexState.parser;
        this.entryClose = indexState.ohlc.close;
        this.dayAtrPercent = dayAtrPercent;
        this.dayAtrPercentage = dayAtrPercentage;
        loadAttribs(indexState, kv.tradeType.charAt(0), false);
    }

    // ----------- Core Methods for P&L, Entry, Exit ----------- //

    public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }
    public void setTarget(double target) { this.target = target; }

    public void addOptionLeg(OptionLeg leg) { optionLegs.add(leg); }

    // Helper for BULL CALL SPREAD
    public void setBullCallSpread(int atmStrike, float atmPremium, int otmStrike, float otmPremium,
                                  String expiry, int qty) {
        addOptionLeg(new OptionLeg(atmStrike, "CE", expiry, qty, "BUY", atmPremium));
        addOptionLeg(new OptionLeg(otmStrike, "CE", expiry, qty, "SELL", otmPremium));
    }
    public void setBearPutSpread(int atmStrike, float atmPremium, int otmStrike, float otmPremium,
                                 String expiry, int qty) {
        addOptionLeg(new OptionLeg(atmStrike, "PE", expiry, qty, "BUY", atmPremium));
        addOptionLeg(new OptionLeg(otmStrike, "PE", expiry, qty, "SELL", otmPremium));
    }

    public void setTrade(double entryEma, double entryRsi, double entryPivot, float pdh, float pdl, float cdh, float cdl, double hhv, double llv) {
        this.entryEma = entryEma;
        this.entryRsi = entryRsi;
        this.entryPivot = entryPivot;
        this.pdh = pdh;
        this.pdl = pdl;
        this.cdh = cdh;
        this.cdl = cdl;
        this.hhv = hhv;
        this.llv = llv;
    }

    // --- On EXIT: Record exit context, premiums, P&L, and log to CSV --- //
    public void exit(String reason, String reasonInfo) {
        if (this.tradeSquared) return;
        exitTrade(reason, reasonInfo);
    }

    private void exitTrade(String reason, String reasonInfo) {
        this.tradeSquared = true;
        if (indexState != null && indexState.ohlc != null) {
            this.exitDate = indexState.ohlc.date;
            this.exitTime = indexState.ohlc.time;
            this.exitIndex = indexState.parser;
            this.exitClose = indexState.ohlc.close;
        } else {
            this.exitDate = "";
            this.exitTime = "";
            this.exitIndex = -1;
            this.exitClose = 0.0;
        }
        if (indexState != null && kv != null) {
            this.exitEma = indexState.getEmaVal(kv.emaPeriod);
            this.exitRsi = indexState.getRsiVal(kv.rsiPeriod);
            this.exitPivot = indexState.pivotsInitialized ? (Float) ((IndexState) indexState).pp : Double.NaN;
        }
        for (OptionLeg leg : optionLegs) {
            try {
                String year = exitDate.substring(6, 8);
                String file = String.format(
                        "BankNifty Options 1min 20160601 to 20231228 Cleaned/20%s/BANKNIFTY%s%d%s.csv",
                        year,
                        leg.expirySymbol.substring(9),
                        leg.strike,
                        leg.optionType);
                String dnt = exitDate + " " + exitTime;
                leg.exitPremium = com.valar.basestrategy.utils.OptionPremiumLoader.getPremiumAt(file, dnt);
            } catch (Exception ex) {
                leg.exitPremium = Float.NaN;
            }
        }
        float totalEntryDebit = 0, totalExitDebit = 0, totalEntryCost = 0, totalExitCost = 0;
        for (OptionLeg leg : optionLegs) {
            float absEntry = leg.entryPremium * leg.quantity;
            float absExit = (!Float.isNaN(leg.exitPremium) ? leg.exitPremium : leg.entryPremium) * leg.quantity;
            float entryCost = (absEntry / 100.0f) * kv.costPercent;
            float exitCost = (absExit / 100.0f) * kv.costPercent;
            if (leg.action.equals("BUY")) {
                totalEntryDebit += absEntry;
                totalExitDebit += absExit;
                totalEntryCost += entryCost;
                totalExitCost += exitCost;
            } else {
                totalEntryDebit -= absEntry;
                totalExitDebit -= absExit;
                totalEntryCost += entryCost;
                totalExitCost += exitCost;
            }
        }
        float netProfit = totalExitDebit - totalEntryDebit;
        float netProfitWithCost = (totalExitDebit - totalExitCost) - (totalEntryDebit + totalEntryCost);
        float profitPercent = (totalEntryDebit != 0) ? (netProfit / Math.abs(totalEntryDebit)) * 100f : 0;
        float profitPercentWithCost = (totalEntryDebit != 0) ? (netProfitWithCost / Math.abs(totalEntryDebit)) * 100f : 0;
        this.holdingPeriod = (exitIndex > entryIndex) ? (exitIndex - entryIndex) : 1;

        // --- Inject option P&L into overAllTradeMetric so that DayMetric/OverallMetric get correct P&L! ---
        this.overAllTradeMetric.profit = new com.valar.basestrategy.tradeAndDayMetrics.ProfitLossMetric();
        this.overAllTradeMetric.profitWithCost = new com.valar.basestrategy.tradeAndDayMetrics.ProfitLossMetric();
        this.overAllTradeMetric.profitPercent = new com.valar.basestrategy.tradeAndDayMetrics.ProfitLossMetric();
        this.overAllTradeMetric.profitPercentWithCost = new com.valar.basestrategy.tradeAndDayMetrics.ProfitLossMetric();
        this.overAllTradeMetric.totalTrades = 1;
        this.overAllTradeMetric.profit.add(netProfit);
        this.overAllTradeMetric.profitWithCost.add(netProfitWithCost);
        this.overAllTradeMetric.profitPercent.add(profitPercent);
        this.overAllTradeMetric.profitPercentWithCost.add(profitPercentWithCost);

        // CSV logging for options
        if (optionLegs.size() == 2) {
            PrintWriters.writeOptionsTradeExit(
                    indexState.name, entryDate, entryTime, entryIndex, entryClose,
                    entryAdx, entryPlusDi, entryMinusDi,
                    optionLegs.get(0).strike, optionLegs.get(0).entryPremium, optionLegs.get(0).exitPremium,
                    optionLegs.get(1).strike, optionLegs.get(1).entryPremium, optionLegs.get(1).exitPremium,
                    optionLegs.get(0).expirySymbol, getStrategyName(),
                    optionLegs.get(0).quantity,
                    exitDate, exitTime, exitIndex, exitClose,
                    netProfit, profitPercent, netProfitWithCost, profitPercentWithCost,
                    holdingPeriod, exitClose
            );
        }
        updateOverAll();
    }

    // Helper to guess strategy string for log
    private String getStrategyName() {
        if (optionLegs.size() == 2) {
            if (optionLegs.get(0).optionType.equals("CE") && optionLegs.get(0).action.equals("BUY")) return "BULL_CALL_SPREAD";
            if (optionLegs.get(0).optionType.equals("PE") && optionLegs.get(0).action.equals("BUY")) return "BEAR_PUT_SPREAD";
        }
        return "CUSTOM_STRAT";
    }

    // =========== Rest of your methods unchanged (macros, etc) =========== //

    class ProfitMetric {
        public float currentProfit;
        public float profitBooked;
        public float maxProfit = Float.NEGATIVE_INFINITY;
        public float getTotalProfit() { return currentProfit + profitBooked; }
        public void resetCurrentProfit() { currentProfit = 0; }
    }

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
        if (!optionLegs.isEmpty()) {
            return;
        }
        for (TradeMetric tradeMetric : tradeMetrics)
            if (tradeMetric.exitOhlc != null)
                overAllTradeMetric.updateOverAll(tradeMetric);
    }

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

    // OptionLeg Inner Class
    public static class OptionLeg {
        public int strike;
        public String optionType; // "CE" or "PE"
        public String expirySymbol;
        public int quantity;      // e.g. 15/25 (lot size)
        public String action;     // "BUY" or "SELL"
        public float entryPremium;
        public float exitPremium = Float.NaN;
        public OptionLeg(int strike, String optionType, String expirySymbol, int quantity, String action, float entryPremium) {
            this.strike = strike;
            this.optionType = optionType;
            this.expirySymbol = expirySymbol;
            this.quantity = quantity;
            this.action = action;
            this.entryPremium = entryPremium;
        }
    }
}