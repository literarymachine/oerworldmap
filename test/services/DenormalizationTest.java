package services;

import helpers.JsonLdConstants;
import models.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import play.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author fo
 */
public class DenormalizationTest {

  private static Map<String, String> inverseRelations = new HashMap<>();
  static {
    inverseRelations.put("author", "authorOf");
    inverseRelations.put("authorOf", "author");
  }

  private static HashSet idOnly = new HashSet<String>(Arrays.asList(JsonLdConstants.ID));
  private static HashSet idTypeName =
      new HashSet<String>(Arrays.asList(JsonLdConstants.TYPE, JsonLdConstants.ID, "name"));

  public List<Resource> denormalize(Resource aResource) {

    List<Resource> result = new ArrayList<>();
    result.add(aResource);
    for (Map.Entry<String, Object> entry : aResource.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (!(value instanceof List)) {
        value = new ArrayList<>(Arrays.asList(value));
      }

      System.out.println("Processing values for " + key);
      for (Object v : (List) value) {
        if (v instanceof Resource) {
          if (((Resource) v).keySet().equals(idOnly)) {
            // Handle link
            System.out.println("Found linked resource " + v);
          } else {
            // Handle embedded resource
            System.out.println("Found embedded resource " + v);
          }
        } else {
          // Handle literal value
          System.out.println("Found literal " + v);
        }
      }

    }

    return result;

  }

  public Resource getEmbedView(Resource aResource) {
    for (Map.Entry<String, Object> entry : aResource.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Resource) {
        aResource.put(key, getLinkView((Resource) value));
      } else if (value instanceof List) {
        List<Object> values = new ArrayList<>();
        for (Object v : (List) value) {
          if (v instanceof Resource) {
            values.add(getLinkView((Resource) v));
          } else {
            values.add(v);
          }
        }
        aResource.put(key, values);
      }
    }
    return aResource;
  }

  public Resource getLinkView(Resource aResource) {
    Set<String> keys = aResource.keySet();
    for (String key : keys) {
      if (! idTypeName.contains(key)) {
        keys.remove(key);
      }
    }
    return aResource;
  }

  @Test
  public void testDenormalizeNewResource() throws IOException {
    ResourceRepository resourceRepository = new MockResourceRepository();
    InputStream in = ClassLoader.getSystemResourceAsStream("DenormalizationTest/testDenormalizeNewResource.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource resource = Resource.fromJson(json);
    denormalize(resource);
    System.out.println("Embed view " + getEmbedView(resource));
    resourceRepository.addResource(resource);
  }

}
