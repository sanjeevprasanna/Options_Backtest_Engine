package com.valar.basestrategy.service;

import com.valar.basestrategy.entities.Ohlc;
import com.valar.basestrategy.entities.TradeEntity;
import com.valar.basestrategy.state.minute.IndexState;
import com.valar.basestrategy.state.minute.State;
import com.valar.basestrategy.tradeAndDayMetrics.DayMetric;
import com.valar.basestrategy.utils.KeyValues;
import com.valar.basestrategy.utils.ExpiryDatesManager;
import com.valar.basestrategy.utils.OptionPremiumLoader;
import org.ta4j.core.Bar;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static com.valar.basestrategy.application.PropertiesReader.properties;
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
    private String prevDate = null;

    public double prevDayADX = -Float.MAX_VALUE;
    private ExpiryDatesManager expiryDatesManager;
    public int currentNotInTrendADX = 0;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy");

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
        try {
            this.expiryDatesManager = new ExpiryDatesManager(properties.getProperty("optionsExpiryDates"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ExpiryDatesManager", e);
        }
    }

    public void setUnSquaredTrades(int unSquaredTrades) {
        this.unSquaredTrades = unSquaredTrades;
    }

    public void iterate(int mins) {
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

            // ENTRY
            if (dayATRConditionSatisfied && entryConditionSatisfied) {
                Ohlc bar = indexState.ohlc;

                //only execute from 2016-2022 for now.
                LocalDate thresholdDate = LocalDate.parse("02-06-16", dateFormatter);
                LocalDate lastOptionsDate = LocalDate.parse("02-06-19", dateFormatter);
                LocalDate barDate = LocalDate.parse(bar.date, dateFormatter);
                if (barDate.isBefore(thresholdDate) || barDate.isAfter(lastOptionsDate)) return;

                double adx = indexState.getAdxVal(kv.adxPeriod, kv.adxSmoothing);
                double plusDi = indexState.getAdxPlusVal(kv.adxPeriod, kv.adxSmoothing);
                double minusDi = indexState.getAdxMinusVal(kv.adxPeriod, kv.adxSmoothing);

                if ("09:15".equals(bar.time)) {
                    prevDayADX = indexState.getPrevDayADX();
                    if (adx < 20) {
                        currentNotInTrendADX++;
                    } else {
                        currentNotInTrendADX = 0;
                    }
                }
                //for setting up the first day prevDayADX
                if (prevDayADX == -Float.MAX_VALUE) {
                    prevDayADX = adx;
                    return;
                }

                if (adx < 20) {
                    return;
                }

                // --- ENTRY: BULLISH SETUP ---
                if (adx > prevDayADX && adx > 25 && plusDi > minusDi) {
                    try {
                        String entryDatetime = indexState.ohlc.date + " " + indexState.ohlc.time;
                        String expirySymbol = expiryDatesManager.getNearestExpiry(entryDatetime);

                        float spot = indexState.ohlc.close;
                        int atmStrike = indexState.getATMStrike(spot, kv.otmStrikeStep);
                        int otmStrike = indexState.getOTMStrike(atmStrike, kv.otmStrikeStep);

                        String year = entryDatetime.substring(6, 8);
                        String atmFile = "BankNifty Options 1min 20160601 to 20231228 Cleaned/20" + year
                                + "/BANKNIFTY" + expirySymbol.substring(9) + atmStrike + "CE.csv";
                        String otmFile = "BankNifty Options 1min 20160601 to 20231228 Cleaned/20" + year
                                + "/BANKNIFTY" + expirySymbol.substring(9) + otmStrike + "CE.csv";

                        float atmPremium, otmPremium;
                        try {
                            atmPremium = OptionPremiumLoader.getPremiumAt(atmFile, entryDatetime);
                            otmPremium = OptionPremiumLoader.getPremiumAt(otmFile, entryDatetime);
                        } catch (Exception ex) {
                            System.out.println("File Missing->" + "Bull" + " " + atmFile + " " + otmFile + " " + entryDatetime);
                            return;
                        }

                        TradeEntity te = new TradeEntity(tradeId, 0, 0, kv, (IndexState) indexState, indexStateMap);
                        te.setBullCallSpread(atmStrike, atmPremium, otmStrike, otmPremium, expirySymbol, kv.lotSize);
                        float netDebit = atmPremium - otmPremium;
                        te.setStopLoss(-kv.setStopLossOptions * netDebit);
                        float maxProfit = (otmStrike - atmStrike) - netDebit;
                        te.setTarget(kv.setTargetPriceOptions * maxProfit);

                        // Set entry indicator values for logging at exit!
                        te.entryAdx = adx;
                        te.entryPlusDi = plusDi;
                        te.entryMinusDi = minusDi;

                        tradeEntities.add(te);
                        tradeId++;
                        parserAtLastTrade = indexState.parser;

                    } catch (Exception ex) {
                        System.err.println("Error during Bull Call entry: " + ex.getMessage());
                    }
                }
                //Bearish Setup
                if (adx > prevDayADX && adx > 25 && minusDi > plusDi) {
                    String entryDatetime = indexState.ohlc.date + " " + indexState.ohlc.time;
                    String expirySymbol = expiryDatesManager.getNearestExpiry(entryDatetime);
                    float spot = indexState.ohlc.close;
                    int step = 100;
                    int atmStrike = indexState.getATMStrike(spot, step);
                    int otmStrike = atmStrike - step;
                    String year = entryDatetime.substring(6, 8);

                    String atmFile = "BankNifty Options 1min 20160601 to 20231228 Cleaned/20" + year +
                            "/BANKNIFTY" + expirySymbol.substring(9) + atmStrike + "PE.csv";
                    String otmFile = "BankNifty Options 1min 20160601 to 20231228 Cleaned/20" + year +
                            "/BANKNIFTY" + expirySymbol.substring(9) + otmStrike + "PE.csv";
                    float atmPremium, otmPremium;
                    try {
                        atmPremium = OptionPremiumLoader.getPremiumAt(atmFile, entryDatetime);
                        otmPremium = OptionPremiumLoader.getPremiumAt(otmFile, entryDatetime);
                    } catch (Exception ex) {
                        System.out.println("File Missing->" + "Bear" + " " + atmFile + " " + otmFile + " " + entryDatetime);
                        return;
                    }
                    TradeEntity te = new TradeEntity(tradeId, 0, 0, kv, (IndexState) indexState, indexStateMap);
                    te.setBearPutSpread(atmStrike, atmPremium, otmStrike, otmPremium, expirySymbol, kv.lotSize);
                    float netDebit = atmPremium - otmPremium;
                    te.setStopLoss(-kv.setStopLossOptions * netDebit);
                    float maxProfit = (atmStrike - otmStrike) - netDebit;
                    te.setTarget(kv.setTargetPriceOptions * maxProfit);

                    // Set entry indicator values for logging at exit!
                    te.entryAdx = adx;
                    te.entryPlusDi = plusDi;
                    te.entryMinusDi = minusDi;

                    tradeEntities.add(te);
                    tradeId++;
                    parserAtLastTrade = indexState.parser;
                }
            }
        }
    }

    public void checkForExitsInEnteredTrades() {
        Ohlc bar = indexState.ohlc;
        LocalDateTime barDateTime = LocalDateTime.parse(bar.date + " " + bar.time, DTF);

        float totalProfitPercent = 0, totalProfit = 0;
        unSquaredTrades = 0;

        for (TradeEntity tradeEntity : tradeEntities) {
            if (tradeEntity.tradeSquared) continue;

            // -------- Expiry-based exit ---------
            boolean isExpiryExit = false;
            try {
                if (tradeEntity.optionLegs != null && !tradeEntity.optionLegs.isEmpty()) {
                    String expirySymbol = tradeEntity.optionLegs.get(0).expirySymbol;
                    LocalDate expiryDate = parseExpiryDate(expirySymbol);
                    LocalDate currentBarDate = LocalDate.parse(bar.date, dateFormatter);
                    if (!currentBarDate.isBefore(expiryDate)) { // currentBarDate >= expiryDate
                        isExpiryExit = true;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error parsing expiry: " + ex.getMessage());
            }
            if (isExpiryExit) {
                tradeEntity.exit("Expiry", "OptionExpiryExit");
                onTradeExit(bar.date, tradeEntity);
                continue;
            }

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
            if (currentNotInTrendADX >= kv.notInTrendDays) {
                tradeEntity.exit("TimeExit", "TimeExit");
                onTradeExit(bar.date, tradeEntity);
            } else if (hitSL) {
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

    // ---- Helper: Parse expiry date from symbol like "BANKNIFTY02JUN16" ----
    private static LocalDate parseExpiryDate(String expirySymbol) {
        // Expects: BANKNIFTY02JUN16 → 02JUN16
        String raw = expirySymbol.substring(9); // e.g., 08JUN17
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("ddMMMyy")
                .toFormatter(Locale.ENGLISH);
        return LocalDate.parse(raw, fmt);
    }
}