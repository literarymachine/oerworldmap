package controllers;

import helpers.UniversalFunctions;
import models.Record;
import models.Resource;
import models.ResourceList;
import play.Configuration;
import play.Environment;
import play.mvc.Result;
import services.AggregationProvider;
import services.QueryContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author fo
 */
public class CountryIndex extends OERWorldMap {

  @Inject
  public CountryIndex(Configuration aConf, Environment aEnv) {
    super(aConf, aEnv);
  }

  public Result read(String id) throws IOException {

    if (! UniversalFunctions.resourceBundleToMap(ResourceBundle
      .getBundle("iso3166-1-alpha-2", getLocale()))
      .containsKey(id.toUpperCase())) {
      return notFound("Not found");
    }

    QueryContext queryContext = getQueryContext();

    queryContext.setFetchSource(new String[]{
      "about.@id", "about.@type", "about.name", "about.alternateName", "about.location", "about.image",
      "about.provider.@id", "about.provider.@type", "about.provider.name", "about.provider.location",
      "about.participant.@id", "about.participant.@type", "about.participant.name", "about.participant.location",
      "about.agent.@id", "about.agent.@type", "about.agent.name", "about.agent.location",
      "about.mentions.@id", "about.mentions.@type", "about.mentions.name", "about.mentions.location",
      "about.mainEntity.@id", "about.mainEntity.@type", "about.mainEntity.name", "about.mainEntity.location"
    });

    Resource countryAggregation = mBaseRepository.aggregate(AggregationProvider.getForCountryAggregation(id.toUpperCase(), 0));

    Map<String, List<String>> filters = new HashMap<>();
    // FIXME: update mapping of countryChampionFor to not analyzed, use upper case here
    filters.put(Record.RESOURCE_KEY + ".countryChampionFor", Arrays.asList(id.toLowerCase()));
    ResourceList champions = mBaseRepository.query("*", 0, 9999, null, filters);
    ResourceList reports = mBaseRepository.query(
      "about.keywords:\"countryreport:".concat(id.toUpperCase()).concat("\""), 0, 10, null, null);

    filters.clear();
    filters.put(Record.RESOURCE_KEY + ".location.address.addressCountry", Arrays.asList(id.toUpperCase()));
    ResourceList resources = mBaseRepository.query("*", 0, 9999, null, filters, queryContext);

    Map<String, Object> scope = new HashMap<>();
    String countryName = ResourceBundle.getBundle("iso3166-1-alpha-2", getLocale())
      .getString(id.toUpperCase());

    scope.put("alpha-2", id.toUpperCase());
    scope.put("name", countryName);
    scope.put("champions", champions.getItems());
    scope.put("reports", reports.getItems());
    scope.put("resources", resources.toResource());
    scope.put("countryAggregation", countryAggregation);

    if (ctx().request().accepts("text/html")) {
      return ok(render(countryName, "CountryIndex/read.mustache", scope));
    } else {
      return ok(resources.toResource().toString()).as("application/json");
    }

  }

}
