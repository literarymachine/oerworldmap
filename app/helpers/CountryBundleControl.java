package helpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by fo on 27.07.16.
 */
public class CountryBundleControl extends ResourceBundle.Control {

  @Override
  public List<Locale> getCandidateLocales(String baseName, Locale locale) {
    return Arrays.asList(locale);
  }

  @Override
  public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
    throws IllegalAccessException, InstantiationException, IOException {

    return new ResourceBundle() {

      @Override
      protected Object handleGetObject(String key) {
        return new String(new Locale(key.toLowerCase(), key).getDisplayCountry(locale).getBytes(),
          StandardCharsets.ISO_8859_1);
      }

      @Override
      public Enumeration<String> getKeys() {
        return Collections.enumeration(Arrays.asList(Locale.getISOCountries()));
      }

    };

  }

}
