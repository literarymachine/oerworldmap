package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import models.Page;
import org.apache.commons.io.IOUtils;
import org.pegdown.PegDownProcessor;

import play.Logger;
import play.Play;
import play.mvc.Result;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  public static Result get(String aPage) {

    String title = aPage.substring(0, 1).toUpperCase().concat(aPage.substring(1));
    String language = OERWorldMap.mLocale.getLanguage();
    String country = OERWorldMap.mLocale.getCountry();
    String extension = ".md";
    String path = "public/pages/";
    ClassLoader classLoader = Play.application().classloader();
    String titleLocalePath = path.concat(title).concat("_").concat(language).concat("_")
        .concat(country).concat(extension);
    String titleLanguagePath = path.concat(title).concat("_").concat(language).concat(extension);
    String titlePath = path.concat(title).concat(extension);

    Map<String, String> page;
    try {
      page = Page.parse(classLoader.getResourceAsStream(titleLocalePath));
    } catch (NullPointerException | IOException noLocale) {
      try {
        page = Page.parse(classLoader.getResourceAsStream(titleLanguagePath));
      } catch (NullPointerException | IOException noLanguage) {
        try {
          page = Page.parse(classLoader.getResourceAsStream(titlePath));
        } catch (NullPointerException | IOException noPage) {
          return notFound("Page not found");
        }
      }
    }

    return ok(render(page.get("title"), "StaticPage/index.mustache", (Map) page));

  }

}
