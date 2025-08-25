package com.valar.basestrategy.entities.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.adx.ADXIndicator;

public class ADXEntity {

    private final ADXIndicator adxIndicator;

    public ADXEntity(BarSeries series, int period, int smoothingPeriod) {
        this.adxIndicator = new ADXIndicator(series, period, smoothingPeriod);
    }


    public double getADXValue(int index) {
        if (index < 0 || index > adxIndicator.getBarSeries().getBarCount()-1) {
            return Double.NaN;
        }
        return adxIndicator.getValue(index).doubleValue();
    }


    public double[] getADXLookbackValues(int index, int lookback) {
        if (index < 0 || lookback <= 0) {
            throw new IllegalArgumentException("Invalid index or lookback value");
        }
        double[] adxValues = new double[lookback];
        for (int i = 0; i <lookback; i++) {
            int lookbackIndex = index - i;
            if (lookbackIndex >= 0) {
                adxValues[i] = getADXValue(lookbackIndex);
            } else {
                adxValues[i] = Double.NaN; // For cases where lookback exceeds available bars
            }
        }
        return adxValues;
    }

    /**
     * Get the average ADX value for a specific lookback range
     *
     * @param index    The current bar index
     * @param lookback The number of bars to look back
     * @return The average ADX value for the lookback range
     */
    public double getAverageADX(int index, int lookback) {

        double[] adxValues = getADXLookbackValues(index, lookback);
       double currentAdx = adxValues[0];
       double previousAdx = adxValues[adxValues.length-1];

       for(int i=adxValues.length-1;i>0;i--){
           previousAdx= Math.min(previousAdx,adxValues[i]);
       }

//        System.out.println(Arrays.toString(adxValues));
//        System.out.println("Previous Adx: " + previousAdx);

//        System.out.println("Previous Adx "+previousAdx);

       return (currentAdx - previousAdx)/previousAdx*100;
    }


    /**
     * Get the maximum ADX value for a specific lookback range
     *
     * @param index    The current bar index
     * @param lookback The number of bars to look back
     * @return The maximum ADX value for the lookback range
     */
    public double getMaxADX(int index, int lookback) {
        double[] adxValues = getADXLookbackValues(index, lookback);
        double max = Double.NEGATIVE_INFINITY;

        for (double value : adxValues) {
            if (!Double.isNaN(value) && value > max) {
                max = value;
            }
        }

        return max == Double.NEGATIVE_INFINITY ? Double.NaN : max;
    }

    /**
     * Get the minimum ADX value for a specific lookback range
     *
     * @param index    The current bar index
     * @param lookback The number of bars to look back
     * @return The minimum ADX value for the lookback range
     */
    public double getMinADX(int index, int lookback) {
        double[] adxValues = getADXLookbackValues(index, lookback);
        double min = Double.POSITIVE_INFINITY;

        for (double value : adxValues) {
            if (!Double.isNaN(value) && value < min) {
                min = value;
            }
        }

        return min == Double.POSITIVE_INFINITY ? Double.NaN : min;
    }
}
