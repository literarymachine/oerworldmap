package controllers;

import java.util.Map;

import play.mvc.Result;

/**
 * @author fo
 */
public class StaticPage extends OERWorldMap {

  public static Result get(String aPage) {

    Map<String, String> page = mPageProvider.getPage(aPage, OERWorldMap.mLocale);
    if (page == null) {
      return notFound("Page not found");
    } else {
      return ok(render(page.get("title"), "StaticPage/index.mustache", (Map) page));
    }

  }

}
