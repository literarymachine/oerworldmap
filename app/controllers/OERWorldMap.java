package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.MarkdownHelper;
import com.typesafe.config.ConfigFactory;
import helpers.JSONForm;
import models.TripleCommit;
import org.apache.commons.io.IOUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.TemplateLoader;

import helpers.HandlebarsHelpers;
import helpers.ResourceTemplateLoader;
import helpers.UniversalFunctions;
import models.Resource;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.Logger;
import play.Play;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import play.twirl.api.Html;
import services.AccountService;
import services.AggregationProvider;
import services.QueryContext;
import services.repository.BaseRepository;

/**
 * @author fo
 */
@With(Authorized.class)
public abstract class OERWorldMap extends Controller {

  Configuration mConf;
  protected BaseRepository mBaseRepository;
  Locale mLocale;
  AccountService mAccountService;
  ResourceBundle mMessages;
  ResourceBundle mEmails;
  QueryContext mQueryContext;
  Resource mUser;

  public OERWorldMap() {

    // Configuration
    if (Play.isTest()){
      mConf = new Configuration(ConfigFactory.parseFile(new File("conf/test.conf")).resolve());
    } else {
      mConf = new Configuration(ConfigFactory.parseFile(new File("conf/application.conf")).resolve());
    }

    // Repository
    try {
      mBaseRepository = new BaseRepository(mConf.underlying());
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to create Respository", ex);
    }

    // Localization
    if (mConf.getBoolean("i18n.enabled")) {
      List<Lang> acceptLanguages = request().acceptLanguages();
      if (acceptLanguages.size() > 0) {
        mLocale = acceptLanguages.get(0).toLocale();
      } else {
        mLocale = new Locale("en", "US");
      }
    } else {
      mLocale = new Locale("en", "US");
    }

    // Messages
    mMessages = ResourceBundle.getBundle("messages", this.mLocale);
    mEmails = ResourceBundle.getBundle("emails", this.mLocale);

    // Account service
    mAccountService = new AccountService(
      new File(mConf.getString("user.token.dir")),
      new File(mConf.getString("ht.passwd")),
      new File(mConf.getString("ht.groups")),
      new File(mConf.getString("ht.profiles")),
      new File(mConf.getString("ht.permissions")));
    mAccountService.setApache2Ctl(mConf.getString("ht.apache2ctl.restart"));

    // Current mUser
    List<String> roles = new ArrayList<>();
    roles.add("guest");
    /*
    String profileId = mAccountService.getProfileId(ctx().request().username());
    if (!StringUtils.isEmpty(profileId)) {
      roles.add("authenticated");
      mUser = getRepository().getResource(profileId);
    }
    */
    mQueryContext = new QueryContext(roles);

    HandlebarsHelpers.setController(this);

  }

  //TODO: is this right here? how else to implement?
  public String getLabel(String aId) {

    Resource resource = mBaseRepository.getResource(aId);
    if (null == resource) {
      return aId;
    }
    Object name = resource.get("name");
    if (name instanceof ArrayList) {
      // Return requested language
      for (Object n : ((ArrayList) name)) {
        if (n instanceof Resource) {
          String language = ((Resource) n).getAsString("@language");
          if (language.equals(this.mLocale.getLanguage())) {
            return ((Resource) n).getAsString("@value");
          }
        }
      }
      // Return English if requested language is not available
      for (Object n : ((ArrayList) name)) {
        if (n instanceof Resource) {
          String language = ((Resource) n).getAsString("@language");
          if (language.equals("en")) {
            return ((Resource) n).getAsString("@value");
          }
        }
      }
      // Return first available if English is not available
      for (Object n : ((ArrayList) name)) {
        if (n instanceof Resource) {
          return ((Resource) n).getAsString("@value");
        }
      }
    }

    return aId;
  }

  protected Html render(String pageTitle, String templatePath, Map<String, Object> scope,
      List<Map<String, Object>> messages) {
    Map<String, Object> mustacheData = new HashMap<>();
    mustacheData.put("scope", scope);
    mustacheData.put("mMessages", messages);
    mustacheData.put("user", mUser);
    mustacheData.put("username", ctx().request().username());
    mustacheData.put("pageTitle", pageTitle);
    mustacheData.put("template", templatePath);
    mustacheData.put("config", mConf.asMap());
    mustacheData.put("templates", getClientTemplates());


    try {
      if (scope != null) {
        Resource globalAggregation = mBaseRepository.aggregate(AggregationProvider.getByCountryAggregation(0));
        scope.put("globalAggregation", globalAggregation);
      }
    } catch (IOException e) {
      Logger.error("Could not add global statistics", e);
    }

    boolean mayAdd = (mUser != null) && (mAccountService.getGroups(ctx().request().username()).contains("admin")
      || mAccountService.getGroups(ctx().request().username()).contains("editor"));
    boolean mayAdminister = (mUser != null) && mAccountService.getGroups(ctx().request().username()).contains("admin");
    Map<String, Object> permissions = new HashMap<>();
    permissions.put("add", mayAdd);
    permissions.put("administer", mayAdminister);
    mustacheData.put("permissions", permissions);

    TemplateLoader loader = new ResourceTemplateLoader();
    loader.setPrefix("public/mustache");
    loader.setSuffix("");
    Handlebars handlebars = new Handlebars(loader);

    handlebars.registerHelpers(StringHelpers.class);

    handlebars.registerHelper("obfuscate", new Helper<String>() {
      public CharSequence apply(String string, Options options) {
        return UniversalFunctions.getHtmlEntities(string);
      }
    });

    try {
      handlebars.registerHelpers(new File("public/javascripts/helpers.js"));
    } catch (Exception e) {
      Logger.error(e.toString());
    }

    handlebars.registerHelpers(new HandlebarsHelpers());

    try {
      handlebars.registerHelpers(new File("public/javascripts/helpers/shared.js"));
    } catch (Exception e) {
      Logger.error(e.toString());
    }

    try {
      handlebars.registerHelpers(new File("public/javascripts/handlebars.form-helpers.js"));
    } catch (Exception e) {
      Logger.error(e.toString());
    }

    handlebars.registerHelper("md", new MarkdownHelper());

    try {
      Template template = handlebars.compile("main.mustache");
      return Html.apply(template.apply(mustacheData));
    } catch (IOException e) {
      Logger.error(e.toString());
      return null;
    }

  }

  protected Html render(String pageTitle, String templatePath, Map<String, Object> scope) {
    return render(pageTitle, templatePath, scope, null);
  }

  protected Html render(String pageTitle, String templatePath) {
    return render(pageTitle, templatePath, null, null);
  }

  protected BaseRepository getRepository() {
    return mBaseRepository;
  }

  protected AccountService getAccountService() {
    return mAccountService;
  }

  /**
   * Get resource from JSON body or form data
   * @return The JSON node
   */
  protected JsonNode getJsonFromRequest() {

    JsonNode jsonNode = ctx().request().body().asJson();
    if (jsonNode == null) {
      Map<String, String[]> formUrlEncoded = ctx().request().body().asFormUrlEncoded();
      if (formUrlEncoded != null) {
        jsonNode = JSONForm.parseFormData(formUrlEncoded, true);
      }
    }
    return jsonNode;

  }

  /**
   * Get metadata suitable for record provinence
   *
   * @return Map containing current mUser in author and current time in date field.
   */
  protected Map<String, String> getMetadata() {

    Map<String, String> metadata = new HashMap<>();
    if (!StringUtils.isEmpty(ctx().request().username())) {
      metadata.put(TripleCommit.Header.AUTHOR_HEADER, ctx().request().username());
    } else {
      metadata.put(TripleCommit.Header.AUTHOR_HEADER, "System");
    }
    metadata.put(TripleCommit.Header.DATE_HEADER, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    return metadata;

  }


  private String getClientTemplates() {

    final List<String> templates = new ArrayList<>();
    final String dir = "public/mustache/ClientTemplates/";
    final ClassLoader classLoader = Play.application().classloader();

    String[] paths = new String[0];
    try {
      paths = getResourceListing(dir, classLoader);
    } catch (URISyntaxException | IOException e) {
      Logger.error(e.toString());
    }

    for (String path : paths) {
      try {
        String template = "<script id=\"".concat(path).concat("\" type=\"text/mustache\">\n");
        InputStream templateStream = classLoader.getResourceAsStream(dir + path);
        if (templateStream == null){
          templateStream = new FileInputStream(dir + path);
        }
        template = template.concat(IOUtils.toString(templateStream));
        templateStream.close();
        template = template.concat("</script>\n\n");
        templates.add(template);
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }

    return String.join("\n", templates);

  }

  /**
   * List directory contents for a resource folder. Not recursive. This is
   * basically a brute-force implementation. Works for regular files and also
   * JARs.
   *
   * Adapted from http://www.uofr.net/~greg/java/get-resource-listing.html
   *
   * @param path
   *          Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException
   * @throws IOException
   */
  private String[] getResourceListing(String path, ClassLoader classLoader)
      throws URISyntaxException, IOException {

    URL dirURL = classLoader.getResource(path);

    if (dirURL == null) {
      return new File(play.Play.application().path().getAbsolutePath().concat("/").concat(path))
          .list();
    } else if (dirURL.getProtocol().equals("jar")) {
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip
                                                                                     // out
                                                                                     // only
                                                                                     // the
                                                                                     // JAR
                                                                                     // file
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
      Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
      Set<String> result = new HashSet<>(); // avoid duplicates in case it
                                                  // is a subdirectory
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.startsWith(path)) { // filter according to the path
          String entry = name.substring(path.length());
          int checkSubdir = entry.indexOf("/");
          if (checkSubdir >= 0) {
            // if it is a subdirectory, we just return the directory name
            entry = entry.substring(0, checkSubdir);
          }
          if (!entry.equals("")) {
            result.add(entry);
          }
        }
      }
      jar.close();
      return result.toArray(new String[result.size()]);
    } else if (dirURL.getProtocol().equals("file")) {
      return new File(dirURL.getFile()).list();
    }

    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);

  }

}
