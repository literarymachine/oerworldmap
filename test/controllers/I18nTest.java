package controllers;

import static org.junit.Assert.*;

import helpers.CountryBundleControl;
import helpers.LanguageBundleControl;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by fo on 27.07.16.
 */
public class I18nTest {

  @Test
  public void testResourceBundleControl() {

    Locale testLocale = new Locale("de", "DE");

    ResourceBundle languageBundle = ResourceBundle.getBundle("languages", testLocale, new LanguageBundleControl());
    assertEquals(Arrays.asList(Locale.getISOLanguages()), Collections.list(languageBundle.getKeys()));

    ResourceBundle countryBundle = ResourceBundle.getBundle("countries", testLocale, new CountryBundleControl());
    assertEquals(Arrays.asList(Locale.getISOCountries()), Collections.list(countryBundle.getKeys()));

  }

}
