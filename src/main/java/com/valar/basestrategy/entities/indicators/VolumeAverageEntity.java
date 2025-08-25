package com.valar.basestrategy.entities.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;

public class VolumeAverageEntity {

    private final VolumeIndicator volumeIndicator;
    private final SMAIndicator averageVolumeIndicator;

    public VolumeAverageEntity(BarSeries series, int period) {
        this.volumeIndicator = new VolumeIndicator(series);
        this.averageVolumeIndicator = new SMAIndicator(volumeIndicator, period);
    }

    public double getAverageVolume(int index) {
        if (index < 0 || index > averageVolumeIndicator.getBarSeries().getBarCount()-1) {
            return Double.NaN;
        }
        return averageVolumeIndicator.getValue(index-1).doubleValue();
    }
    public double getCurrentVolume(int index) {
        if (index < 0 || index > volumeIndicator.getBarSeries().getBarCount()-1) {
            return Double.NaN;
        }
        return volumeIndicator.getValue(index).doubleValue();
    }
}
