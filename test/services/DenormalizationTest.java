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

  private static HashSet idType = new HashSet<>(Arrays.asList(JsonLdConstants.ID, JsonLdConstants.TYPE));
  private static HashSet idTypeName =
      new HashSet<>(Arrays.asList(JsonLdConstants.TYPE, JsonLdConstants.ID, "name"));

  public List<Resource> denormalize(Resource aResource, ResourceRepository aSource) {

    List<Resource> result = new ArrayList<>();
    Resource updatedResource = (Resource) aResource.clone();

    for (Map.Entry<String, Object> entry : aResource.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      boolean isList = true;
      if (!(value instanceof List)) {
        value = new ArrayList<>(Arrays.asList(value));
        isList = false;
      }

      System.out.println("Processing values for " + key);
      //List<Object> values = new ArrayList<>();
      for (Object v : (List) value) {
        if (v instanceof Resource) {
          if (((Resource) v).keySet().equals(idType)) {
            // Handle link: add backlink, set value to embed view
            System.out.println("Found linked resource " + v);
            try {
              Resource referenced = aSource.getResource(((Resource) v).get(JsonLdConstants.ID).toString());
              updatedResource.put(key, referenced);
              System.out.println("Found referenced resource " + referenced);
              String inverse = inverseRelations.get(key);
              // Todo: Merge, not put!
              referenced.put(inverse, getEmbedView(updatedResource));
              result.add(referenced);
              //values.add(getEmbedView((Resource) v));
            } catch (IOException e) {
              Logger.error(e.toString());
            }
          } else {
            // Handle new / modified resource: add backlink, replace existing / create new resource, set value to embed view
            System.out.println("Found new / modified resource " + v);
            String inverse = inverseRelations.get(key);
            // Todo: Merge, not put!
            ((Resource) v).put(inverse, getEmbedView(aResource));
            result.add((Resource) v);
            updatedResource.put(key, v);
            //values.add(getEmbedView((Resource) v));
          }
        } else {
          // Handle literal value
          System.out.println("Found literal " + v);
          updatedResource.put(key, v);
          //values.add(v);
        }
      }

      /*if (isList) {
        aResource.put(key, values);
      } else {
        aResource.put(key, values.get(0));
      }*/

    }

    result.add(updatedResource);
    return result;

  }

  public Resource getEmbedView(Resource aResource) {
    System.out.println("Getting embed view for " + aResource);
    Resource embedView = new Resource(aResource.get(JsonLdConstants.TYPE).toString(),
        aResource.get(JsonLdConstants.ID).toString());
    for (Map.Entry<String, Object> entry : aResource.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Resource) {
        embedView.put(key, getLinkView((Resource) value));
      } else if (value instanceof List) {
        List<Object> values = new ArrayList<>();
        for (Object v : (List) value) {
          if (v instanceof Resource) {
            values.add(getLinkView((Resource) v));
          } else {
            values.add(v);
          }
        }
        embedView.put(key, values);
      } else {
        embedView.put(key, value);
      }
    }
    System.out.println("Returning embed view for " + embedView);
    return embedView;
  }

  public Resource getLinkView(Resource aResource) {
    System.out.println("Getting link view for " + aResource);
    Resource linkView = new Resource(aResource.get(JsonLdConstants.TYPE).toString(),
        aResource.get(JsonLdConstants.ID).toString());
    Set<String> keys = aResource.keySet();
    for (String key : keys) {
      if (idTypeName.contains(key)) {
        linkView.put(key, aResource.get(key));
      }
    }
    System.out.println("Returning link view for " + linkView);
    return linkView;
  }

  @Test
  public void testDenormalizeNewResource() throws IOException {
    ResourceRepository resourceRepository = new MockResourceRepository();
    InputStream in = ClassLoader.getSystemResourceAsStream("DenormalizationTest/testDenormalizeNewResource.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource resource = Resource.fromJson(json);
    for (Resource r : denormalize(resource, resourceRepository)) {
      System.out.println(r);
      resourceRepository.addResource(r);
    }
  }

  @Test
  public void testDenormalizeLinkedResource() throws IOException {
    ResourceRepository resourceRepository = new MockResourceRepository();
    Resource linkedResource = new Resource("Person", "1");
    linkedResource.put("name", "Hans Wurst");
    linkedResource.put("email", "foo@bar.de");
    resourceRepository.addResource(linkedResource);
    InputStream in = ClassLoader.getSystemResourceAsStream("DenormalizationTest/testDenormalizeLinkedResource.json");
    String json = IOUtils.toString(in, "UTF-8");
    Resource resource = Resource.fromJson(json);
    for (Resource r : denormalize(resource, resourceRepository)) {
      System.out.println(r);
      resourceRepository.addResource(r);
    }
  }

}
