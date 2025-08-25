package com.valar.basestrategy.service;

import com.valar.basestrategy.entities.Ohlc;
import com.valar.basestrategy.entities.TradeEntity;
import com.valar.basestrategy.state.minute.IndexState;
import com.valar.basestrategy.state.minute.State;
import com.valar.basestrategy.tradeAndDayMetrics.DayMetric;
import com.valar.basestrategy.utils.KeyValues;
import org.ta4j.core.Bar;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Math.max;

public class StrategyImpl {
    private int tradeId;
    private float dayMaxProfit, dayMaxProfitPercent;
    public KeyValues kv;
    public boolean dayExited;
    private int unSquaredTrades;
    private final List<TradeEntity> tradeEntities = new ArrayList<>();
    private final State indexState;
    private final List<Map<String, DayMetric>> dayMetricsMapList;
    private double dayAtrPercent, dayAtrPercentage;
    private boolean dayATRConditionSatisfied, candlePeriodBelongsToDay;
    private final Map<Integer, IndexState> indexStateMap;
    private final Map<String, Double> dayAtrMap, dayAtrMapPercentage;
    private int parserAtLastTrade;
    private String lastAtrCheckeAtDate = "";
    //private int chk = 0;
    // Pivot tracking fields
    private String prevDate = null;
    //private List<Ohlc> prevDayBars = new ArrayList<>();

   private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");
    private static final DateTimeFormatter INPUT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");
    private static final DateTimeFormatter OUTPUT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public StrategyImpl(
            boolean candlePeriodBelongsToDay,
            Map<Integer, IndexState> indexStateMap,
            KeyValues kv,
            Map<String, Double> dayAtrMap,
            Map<String, Double> dayAtrMapPercentage,
            State indexState,
            Map<String, DayMetric> dayMetricsMap,
            Map<String, DayMetric> stockDayMetricsMap
    ) {
        this.indexStateMap = indexStateMap;
        this.kv = kv;
        this.indexState = indexState;
        this.dayAtrMap = dayAtrMap;
        this.dayAtrMapPercentage = dayAtrMapPercentage;
        this.dayMetricsMapList = new ArrayList<>(Arrays.asList(dayMetricsMap, stockDayMetricsMap));
        this.candlePeriodBelongsToDay = candlePeriodBelongsToDay;
    }

    public void setUnSquaredTrades(int unSquaredTrades) {
        this.unSquaredTrades = unSquaredTrades;
    }

    public void iterate(int mins) {
        String currDate = indexState.ohlc.date;

        // PIVOT LOGIC


        //prevDayBars- holds Entire days minute ohlc
        /*if (prevDate != null && !prevDate.equals(currDate)) {
            if (!prevDayBars.isEmpty()) {
                float hi = Float.NEGATIVE_INFINITY, lo = Float.POSITIVE_INFINITY, cls = 0;
                for (Ohlc bar : prevDayBars) {
                    hi = max(hi, bar.high);
                    lo = Math.min(lo, bar.low);
                    cls = bar.close;
                }
                indexState.computePivots(hi, lo, cls);
                indexState.pivotsInitialized = true;
            } else {
                indexState.pivotsInitialized = false;
            }
            prevDayBars.clear();
        }
        prevDate = currDate;
        prevDayBars.add(new Ohlc(indexState.ohlc));*/

        if (prevDate != null && !prevDate.equals(currDate)) {
            float high = indexState.ohlc.prevDayHigh;
            float low = indexState.ohlc.prevDayLow;
            float close = indexState.ohlc.lastDayClose;
            indexState.computePivots(high, low, close);
            indexState.pivotsInitialized = true;

        }
        /*System.out.println("DEBUG: Pivots for " + currDate + " | prevDayHigh=" + indexState.ohlc.prevDayHigh
                + " prevDayLow=" + indexState.ohlc.prevDayLow
                + " lastDayClose=" + indexState.ohlc.lastDayClose);
        */
        prevDate = currDate;

        if ((mins >= kv.startTime || candlePeriodBelongsToDay)
                && !lastAtrCheckeAtDate.equals(indexState.ohlc.date)) {
            lastAtrCheckeAtDate = indexState.ohlc.date;
            if (!dayAtrMap.containsKey(indexState.ohlc.date)) {
                if (!kv.positional) dayExited = true;
                return;
            }
            dayAtrPercent = dayAtrMap.get(indexState.ohlc.date);
            dayAtrPercentage = dayAtrMapPercentage.get(indexState.ohlc.date);
            dayATRConditionSatisfied = dayAtrPercent >= 0
                    && (dayAtrPercent >= kv.atrFrom && dayAtrPercent <= kv.atrTo);
            if (!dayATRConditionSatisfied && !kv.positional) {
                dayExited = true;
                return;
            }
        }

        if (mins >= kv.startTime || candlePeriodBelongsToDay) {
            checkForExitsInEnteredTrades();

            boolean entryConditionSatisfied = indexState.ohlc.mins >= kv.startTime
                    && indexState.ohlc.mins <= kv.cutOffTime
                    && (kv.maxOverlap == 0 || unSquaredTrades < kv.maxOverlap)
                    && indexState.parser - parserAtLastTrade >= kv.tradeGap;
                    //&& chk < 10000;
            //chk++;
            // ENTRY
            if (dayATRConditionSatisfied && entryConditionSatisfied && indexState.pivotsInitialized) {
                if (mins >= kv.startTime && kv.rsiPeriod != 0 && kv.positional) {
                    runOptionalLogic(entryConditionSatisfied);
                }
            }
        }
    }

    private void runOptionalLogic(boolean entryConditionSatisfied) {
        Ohlc bar = indexState.ohlc;
        int curIdx = indexState.parser;
        indexState.loadIndicators(kv.emaPeriod, kv.rsiPeriod);

        double emaVal = indexState.getEmaVal(kv.emaPeriod);
        double rsiVal = indexState.getRsiVal(kv.rsiPeriod);

        // last 10-candle high/low
        float high10 = Float.NEGATIVE_INFINITY, low10 = Float.POSITIVE_INFINITY;
        for (int i = max(0, curIdx - 10); i < curIdx; ++i) {
            Bar o = indexState.series.getBar(i);
            high10 = max(high10, o.getHighPrice().floatValue());
            low10 = Math.min(low10, o.getLowPrice().floatValue());
            //System.out.println("chkr"+" " +o.getBeginTime()+" "+o.getEndTime()+" ");
        }

        //String inputDateTime = indexState.ohlc.date + " " + indexState.ohlc.time;
        //String formattedDateTime;
        //LocalDateTime ldt = LocalDateTime.parse(inputDateTime, INPUT_DATE_FMT);
        //formattedDateTime = ldt.format(OUTPUT_DATE_FMT);
        //System.out.println(indexState.parser + "  " + formattedDateTime);

        // ENTRY: LONG
        if (kv.usePivots) {
            if (bar.high>bar.prevDayHigh && rsiVal > kv.rsiLong && bar.close > emaVal  && kv.tradeType.equals("l")) {
                TradeEntity trade = new TradeEntity(tradeId, 0, 0, kv, (IndexState) indexState, indexStateMap);
                trade.setTrade(emaVal,rsiVal,(indexState.pivotsInitialized ? ((float) indexState.pp) : Float.NaN), bar.prevDayHigh,bar.prevDayLow, bar.high,bar.low,high10,low10);
                trade.setStopLoss(indexState.nearestBelow(bar.close,low10));//support-SL
                trade.setTarget(indexState.nearestAbove(bar.close, high10));//resistance-TP
                tradeEntities.add(trade);
                tradeId++;
                parserAtLastTrade = indexState.parser;
            }
            // ENTRY: SHORT
            if ( bar.low<bar.prevDayLow && rsiVal < kv.rsiShort && bar.close < emaVal &&  kv.tradeType.equals("s")) {
                TradeEntity trade = new TradeEntity(tradeId, 0, 0, kv, (IndexState) indexState, indexStateMap);
                trade.setTrade(emaVal,rsiVal,(indexState.pivotsInitialized ? ((float) indexState.pp) : Float.NaN), bar.prevDayHigh,bar.prevDayLow, bar.high,bar.low,high10,low10);
                trade.setStopLoss(indexState.nearestAbove(bar.close, high10));
                trade.setTarget(indexState.nearestBelow(bar.close,low10));
                tradeEntities.add(trade);
                tradeId++;
                parserAtLastTrade = indexState.parser;
            }
        }
    }

    /*public void checkForExitsInEnteredTrades() {
        Ohlc bar = indexState.ohlc;
        LocalDateTime barDateTime = LocalDateTime.parse(bar.date + " " + bar.time, DTF);

        float totalProfitPercent = 0, totalProfit = 0;
        unSquaredTrades = 0;
        for (TradeEntity tradeEntity : tradeEntities) {
            if (tradeEntity.tradeSquared) continue;

            char lOrS = tradeEntity.tradeAttribs.get(0).lOrS;
            boolean forceExit = (barDateTime.getDayOfWeek() == DayOfWeek.THURSDAY && bar.mins >= (15 * 60 + 15));
            boolean hitSL = false, hitTarget = false;

            if (lOrS == 'l') {
                hitSL = (bar.low <= tradeEntity.stopLoss);
                hitTarget = (bar.high >= tradeEntity.target);
            } else if (lOrS == 's') {
                hitSL = (bar.high >= tradeEntity.stopLoss);
                hitTarget = (bar.low <= tradeEntity.target);
            }

           if (hitSL || hitTarget || forceExit) {
                tradeEntity.forceExit();
                onTradeExit(bar.date, tradeEntity);
            }

            if (!tradeEntity.tradeSquared) unSquaredTrades++;
            totalProfitPercent += tradeEntity.getTotalProfitPercent();
            totalProfit += tradeEntity.getTotalProfit();
        }
        dayMaxProfit = Float.max(dayMaxProfit, totalProfit);
        dayMaxProfitPercent = Float.max(dayMaxProfitPercent, totalProfitPercent);
    }*/

    public void checkForExitsInEnteredTrades() {
        Ohlc bar = indexState.ohlc;
        LocalDateTime barDateTime = LocalDateTime.parse(bar.date + " " + bar.time, DTF);

        float totalProfitPercent = 0, totalProfit = 0;
        unSquaredTrades = 0;

        for (TradeEntity tradeEntity : tradeEntities) {
            if (tradeEntity.tradeSquared) continue;

            char lOrS = tradeEntity.tradeAttribs.get(0).lOrS;
            boolean forceExit = (barDateTime.getDayOfWeek() == DayOfWeek.THURSDAY && bar.mins >= (15 * 60 + 15));
            boolean hitSL = false, hitTarget = false;

            if (lOrS == 'l') {
                hitSL = (bar.low <= tradeEntity.stopLoss);
                hitTarget = (bar.high >= tradeEntity.target);
            } else if (lOrS == 's') {
                hitSL = (bar.high >= tradeEntity.stopLoss);
                hitTarget = (bar.low <= tradeEntity.target);
            }

            if (hitSL) {
                tradeEntity.exit("StopLoss", "SL-hit");
                onTradeExit(bar.date, tradeEntity);
            } else if (hitTarget) {
                tradeEntity.exit("Target", "TP-hit");
                onTradeExit(bar.date, tradeEntity);
            } else if (forceExit) {
                tradeEntity.exit("ForceExit", "TimeExit");
                onTradeExit(bar.date, tradeEntity);
            }

            if (!tradeEntity.tradeSquared) unSquaredTrades++;
            totalProfitPercent += tradeEntity.getTotalProfitPercent();
            totalProfit += tradeEntity.getTotalProfit();
        }
        dayMaxProfit = Float.max(dayMaxProfit, totalProfit);
        dayMaxProfitPercent = Float.max(dayMaxProfitPercent, totalProfitPercent);
    }

    public void onTradeExit(String date, TradeEntity tradeEntity) {
        try {
            dayMetricsMapList.parallelStream().forEach(dayMetricsMap -> {
                DayMetric dayMetric;
                if (dayMetricsMap.containsKey(date)) dayMetric = dayMetricsMap.get(date);
                else {
                    dayMetric = new DayMetric(date, kv.costPercent, tradeEntity.indexCloseAtEntry, kv.sno);
                    dayMetricsMap.put(date, dayMetric);
                }
                dayMetric.updateMetric(tradeEntity.overAllTradeMetric, dayMaxProfit, dayMaxProfitPercent);
            });
        } catch (Exception e) {
            System.out.println(dayMetricsMapList);
            e.printStackTrace();
            System.out.println(dayMetricsMapList);
            System.exit(1);
        }
    }
}