package to.sparks.wosaic;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application configurable properties loaded from wosaic.properties
 *
 * @author gms
 */
public class AppProperties {

    private final static Properties properties = new Properties();

    static {
        try {
            properties.load(ClassLoader.getSystemResourceAsStream("wosaic.properties"));
        } catch (IOException ex) {
            Logger.getLogger(AppProperties.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    static final String config_warning = "set value in wosaic.properties";

    public static String getFlickrApiKey() {
        return properties.getProperty("flickr.apikey", config_warning);
    }

    public static String getFlickrSecret() {
        return properties.getProperty("flickr.secret", config_warning);
    }

    public static String getFacebookApiKey() {
        return properties.getProperty("facebook.apikey", config_warning);
    }

    public static String getFacebookSecret() {
        return properties.getProperty("facebook.secret", config_warning);
    }

}
