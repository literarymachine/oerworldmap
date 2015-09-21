package services;

import models.Record;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fo
 */
public class AggregationProvider {

  private static String resource_field = Record.RESOURCEKEY + ".location.address.addressCountry";
  private static String mentions_field = Record.RESOURCEKEY + ".mentions.location.address.addressCountry";
  private static String provider_field = Record.RESOURCEKEY + ".provider.location.address.addressCountry";

  public static AggregationBuilder<?> getTypeAggregation() {
    return AggregationBuilders.terms("about.@type").field("about.@type");
  }

  public static AggregationBuilder<?> getLocationAggregation() {
    return AggregationBuilders.terms("about.location.address.addressCountry")
        .field("about.location.address.addressCountry");
  }

  public static AggregationBuilder<?> getServiceLanguageAggregation() {
    return AggregationBuilders.terms("about.availableChannel.availableLanguage")
        .field("about.availableChannel.availableLanguage");
  }

  public static AggregationBuilder<?> getFieldOfEducationAggregation() {
    return AggregationBuilders.terms("about.audience.@id")
        .field("about.audience.@id");
  }

  public static AggregationBuilder<?> getByCountryAggregation() {
    return AggregationBuilders
        .terms("by_country").script("doc['"
            + resource_field + "'].values + doc['" + mentions_field + "'].values  + doc['"
            + provider_field + "'].values").size(0)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.existsFilter(Record.RESOURCEKEY + ".countryChampionFor")));
  }

  public static AggregationBuilder<?> getForCountryAggregation(String id) {
    return AggregationBuilders
        .terms("by_country").script("doc['"
            + resource_field + "'].values + doc['" + mentions_field + "'].values  + doc['"
            + provider_field + "'].values").include(id).size(0)
        .subAggregation(AggregationBuilders.terms("by_type").field("about.@type").minDocCount(0))
        .subAggregation(AggregationBuilders
            .filter("champions")
            .filter(FilterBuilders.existsFilter(Record.RESOURCEKEY + ".countryChampionFor")));
  }

}
