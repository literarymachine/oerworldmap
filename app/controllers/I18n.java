package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.Countries;
import helpers.CountryBundleControl;
import helpers.LanguageBundleControl;
import helpers.Languages;
import org.apache.commons.lang3.StringEscapeUtils;
import play.mvc.Result;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class I18n extends OERWorldMap {

  public static Result get() {

    Map<String, Object> i18n = new HashMap<>();
    ResourceBundle messageBundle = ResourceBundle.getBundle("messages", OERWorldMap.mLocale);
    ResourceBundle languageBundle = ResourceBundle.getBundle("languages", OERWorldMap.mLocale,
      new LanguageBundleControl());
    ResourceBundle countryBundle = ResourceBundle.getBundle("countries", OERWorldMap.mLocale,
      new CountryBundleControl());

    i18n.put("messages", resourceBundleToMap(messageBundle));
    i18n.put("countries", resourceBundleToMap(countryBundle));
    i18n.put("languages", resourceBundleToMap(languageBundle));

    String countryMap = new ObjectMapper().convertValue(i18n, JsonNode.class).toString();
    return ok("i18nStrings = ".concat(countryMap)).as("application/javascript");

  }

  private static Map<String, String> resourceBundleToMap(ResourceBundle aResourceBundle) {

    Map<String, String> map = new HashMap<>();
    Enumeration<String> keys = aResourceBundle.getKeys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      try {
        String value =  StringEscapeUtils.unescapeJava(new String(aResourceBundle.getString(key)
          .getBytes("ISO-8859-1"), "UTF-8"));
        map.put(key, value);
      } catch (UnsupportedEncodingException e) {
        map.put(key, aResourceBundle.getString(key));
      }
    }

    return map;

  }

}
