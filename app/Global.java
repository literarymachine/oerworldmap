import play.Application;
import play.GlobalSettings;
import play.Logger;
import services.ElasticsearchConfig;

public class Global extends GlobalSettings {

  private static ElasticsearchConfig esConfig = new ElasticsearchConfig();

  @Override
  public void onStart(Application app) {
    Logger.info("oerworldmap has started");
    Logger.info("Elasticsearch config: " + esConfig.toString());
  }

  @Override
  public void onStop(Application app) {
    Logger.info("oerworldmap shutdown...");
  }

}
