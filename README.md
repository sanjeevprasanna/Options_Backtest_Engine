# RSI Crossover Positional Back-Test 

A Java-8, Maven-built framework that back-tests a **RSI–EMA crossover strategy** (with optional long/short & pivot support) and produces day-wise / trade-wise performance CSVs.

##  **Key Features**
| Area | Highlights |
|------|------------|
| **Markets** | Bank Nifty (minute & day), stocks & options – easily extendable |
| **Trade Types** | Long / Short positional, overlap control, forced Thursday exit |
| **Indicators** | 20-EMA, RSI-14, classic floor pivots (PP / R1 R2 / S1 S2) |
| **Strategy Logic** |<br>• *Entry* – `RSI > 60 && Close > EMA && High > PDH` (long)  <br>• *Entry* – `RSI < 40 && Close < EMA && Low < PDL` (short)  <br>• *Targets* – nearest pivot resistance  <br>• *Stop-loss* – nearest pivot support |
| **Day-Exit Rules** | SL, target, retracement, inactivity & Thursday 15:15 |
| **Reports** | `OrderInfo`, `DayWise`, `OverAllDetails` – all CSV |
| **Config-Driven** | Change everything in `BaseStrategy.properties` |


##  **Data Flow (high-level)**
```
ValarTrade  ──▶  Strategy  ──▶  StrategyImpl  ──▶  TradeEntity
   │              │                │                │
   │              │                │                └─▶ TradeMetric (OrderInfo)
   │              │                └─▶ DayMetric (DayWise)
   │              └─▶ OverAllMetric (OverallInfo)
   └─▶ (creates keystore batches & hands them to Strategy)

           ┌─────────── State hierarchy ────────────┐
           │ IndexState │ StockState │ OptionState │
           └─────────────────────────────────────────┘
```
---

##  **Input Files**
| File                   | Purpose                         | Minimal Headers                     |
|------------------------|---------------------------------|-------------------------------------|
| 1-min Index / Stock    | Primary OHLCV stream            | `Date Time,Open,High,Low,Close,Volume` |
| Daily Index / Stock    | ATR %, pivot calculations       | same fields (EOD)                   |
| `keystore.csv`         | Strategy parameter grid         | see sample below                    |
| `instruments.txt`      | List of symbols to back-test    | one per line                        |


### `keystore.csv` Example

Keystore.csv example
```
sno,indexType,tradeType,costPercent,hpCostPercent,startTime,cutOffTime,endTime,positional,candlePeriod,emaPeriod,rsiPeriod,usePivots,maxOverlap
1,0,l,0.05,0.01,09:15,15:10,15:29,true,1,20,14,true,1000
```

## **Output CSV Headers**
<details>
<summary><code>Outputs/OrderInfo[overAll].csv</code></summary>

S.no,Symbol,Date,ID,Holding Period,TradeType,Event,EntryDate,EntryTime,EntryClose,
EntryEMA,EntryRSI,EntryPivot,
Event,ExitDate,ExitTime,ExitClose,
ExitEMA,ExitRSI,ExitPivot,
Reason,ReasonInfo,Profit,Profit%,tradeMaxProfit,ProfitWith(Cost),Profit%With(Cost),
DayAtrPercentile,DayAtrPercent,candlesWaited,IndexCloseAtExit,HoldingCost
</details>

<details>
<summary><code>Outputs/DayWise[overAll].csv</code></summary>
sno,date,TotalTrades,profit,profit%,ProfitWithcost,Profit%WithCost
</details>

<details>
<summary><code>Outputs/overAllDetails[serialWise].csv</code></summary>
</details>


## Quickstart

# 1. clone
```git clone https://github.com/sanjeevprasanna/RSI-Crossover-Positional.git
cd RSI-Crossover-Positional
```
# 2. build
```mvn clean package dependency:copy-dependencies```

# 3. edit BaseStrategy.properties to match your file paths & params
```$EDITOR BaseStrategy.properties```

# 4. run back-test
```java -cp "target/BaseStrategy_*.jar:target/dependency/*" \com.valar.basestrategy.application.ValarTrade```

## How It Works

1. **ValarTrade**  
    Reads `keystore.csv`, groups rows into batches (`runKeystores`) & kicks off Strategy.

2. **Strategy**  
    Loads `IndexState` (1 min) + `IndexDayState` (daily) for each symbol.  
    Iterates day-by-day, spawning `StrategyImpl` per keystore.

3. **StrategyImpl**  
    Per keystore logic:  
    &nbsp;&nbsp;• Evaluate trade exits first, aggregate P&L, check day-level exits.  
    &nbsp;&nbsp;• If day still open, evaluate entry conditions → create `TradeEntity`.

4. **TradeEntity**  
    Manages one trade: monitors target, stop, Thursday 15:15; when squared, hands data to `TradeMetric`.

5. **Metrics chain**  
    `TradeMetric` ➜ `DayMetric` ➜ `OverAllMetric` ➜ CSV writers.

6. **State hierarchy**  
    (Index / Stock / Option) supplies live & historical OHLC + indicator values.
# Options_Backtest_Engine
# Options_Backtest_Engine
# Options_Backtest_Engine
