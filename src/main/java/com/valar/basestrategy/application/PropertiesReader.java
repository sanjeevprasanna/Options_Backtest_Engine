package com.valar.basestrategy.application;

import java.io.FileReader;
import java.util.Properties;

public class PropertiesReader {

    public static Properties properties;
    static{
        try {
            FileReader reader = new FileReader("BaseStrategy.properties");
            properties = new Properties();
            properties.load(reader);
        }catch (Exception e){e.printStackTrace();}
    }

}
