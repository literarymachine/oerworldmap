package services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import helpers.JsonLdConstants;
import org.apache.jena.riot.Lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.rdf.model.Model;
import models.Resource;
import play.Logger;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceFramer {

  public static Resource resourceFromModel(Model aModel, String aId) throws IOException {

    ByteArrayOutputStream unframed = new ByteArrayOutputStream();
    aModel.write(unframed, Lang.JSONLD.getName());
    Object jsonObject = JsonUtils.fromString(unframed.toString());
    JsonLdOptions options = new JsonLdOptions();
    options.setEmbed(true);
    Map<String, Object> context = new ObjectMapper().readValue(new File("public/json/context.json"),
      new TypeReference<HashMap<String, Object>>() {
      });

    try {
      Object flattened = JsonLdProcessor.flatten(jsonObject, context, options);
      JsonNode jsonNode = new ObjectMapper().valueToTree(flattened);
      Resource resource = Resource.fromJson(full(aId, (ArrayNode) jsonNode.get(JsonLdConstants.GRAPH)));
      if (resource != null) {
        resource.put(JsonLdConstants.CONTEXT, "http://schema.org/");
      }
      return resource;
    } catch (JsonLdError e) {
      Logger.error("Could not read model", e);
    }

    return null;

  }

  public static List<Resource> flatten(Resource resource) throws IOException {

    Object jsonObject = JsonUtils.fromString(resource.toString());
    JsonLdOptions options = new JsonLdOptions();
    options.setEmbed(true);
    Map<String, Object> context = new ObjectMapper().readValue(new File("public/json/context.json"),
      new TypeReference<HashMap<String, Object>>() {
      });

    try {
      Object flattened = JsonLdProcessor.flatten(jsonObject, context, options);
      ArrayNode graphs = (ArrayNode) new ObjectMapper().valueToTree(flattened).get(JsonLdConstants.GRAPH);
      List<Resource> resources = new ArrayList<>();
      for (JsonNode graph : graphs) {
        if (graph.hasNonNull(JsonLdConstants.ID)) {
          Resource value = Resource.fromJson(full(graph.get(JsonLdConstants.ID).textValue(), graphs));
          value.put(JsonLdConstants.CONTEXT, "http://schema.org/");
          resources.add(value);
        }
      }
      return resources;
    } catch (JsonLdError e) {
      Logger.error("Could not flatten", e);
    }

    return null;

  }

  private static ObjectNode full(String aId, ArrayNode aGraphs) {

    ObjectNode graph = getGraph(aId, aGraphs);
    if (graph == null) {
      return null;
    }
    ObjectNode result = new ObjectNode(JsonNodeFactory.instance);

    Iterator<Map.Entry<String,JsonNode>> it = graph.fields();
    while(it.hasNext()) {
      Map.Entry<String,JsonNode> entry = it.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();
      //System.out.println("\nKey:" + key + "\nValue:" + value);
      if (value.isValueNode()) {
        result.put(key, value);
      } else if (value.isObject()) {
        if (value.hasNonNull(JsonLdConstants.ID)) {
          String id = value.get(JsonLdConstants.ID).textValue();
          if (id.startsWith("_:")) {
            ObjectNode objectNode = full(id, aGraphs);
            if (objectNode != null) {
              result.put(key, objectNode);
            }
          } else {
            ObjectNode objectNode = embed(id, aGraphs);
            if (objectNode != null) {
              result.put(key, objectNode);
            }
          }
        } else if (! value.isNull()){
          result.put(key, value);
        }
      } else if(value.isArray()) {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        for (JsonNode arrayValue : value) {
          if (arrayValue.isValueNode()) {
            arrayNode.add(arrayValue);
          } else {
            if (arrayValue.hasNonNull(JsonLdConstants.ID)) {
              String id = arrayValue.get(JsonLdConstants.ID).textValue();
              if (id.startsWith("_:")) {
                ObjectNode objectNode = full(id, aGraphs);
                if (objectNode != null) {
                  arrayNode.add(objectNode);
                }
              } else {
                ObjectNode objectNode = embed(id, aGraphs);
                if (objectNode != null) {
                  arrayNode.add(objectNode);
                }
              }
            } else {
              arrayNode.add(arrayValue);
            }
          }
        }
        result.put(key, arrayNode);
      }
    }

    return result;

  }

  private static ObjectNode embed(String aId, ArrayNode aGraphs) {

    ObjectNode graph = getGraph(aId, aGraphs);
    if (graph == null) {
      return null;
    }

    ObjectNode result = new ObjectNode(JsonNodeFactory.instance);

    Iterator<Map.Entry<String,JsonNode>> it = graph.fields();
    while(it.hasNext()) {
      Map.Entry<String,JsonNode> entry = it.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();
      if (value.isValueNode()) {
        result.put(key, value);
      } else if (value.isObject()) {
        if (value.hasNonNull(JsonLdConstants.ID)) {
          String id = value.get(JsonLdConstants.ID).textValue();
          if (id.startsWith("_:")) {
            ObjectNode objectNode = full(id, aGraphs);
            if (objectNode != null) {
              result.put(key, objectNode);
            }
          } else {
            ObjectNode objectNode = link(id, aGraphs);
            if (objectNode != null) {
              result.put(key, objectNode);
            }
          }
        } else {
          result.put(key, value);
        }
      } else if(value.isArray()) {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        for (JsonNode arrayValue : value) {
          if (arrayValue.isValueNode()) {
            arrayNode.add(arrayValue);
          } else {
            if (arrayValue.hasNonNull(JsonLdConstants.ID)) {
              String id = arrayValue.get(JsonLdConstants.ID).textValue();
              if (id.startsWith("_:")) {
                ObjectNode objectNode = full(id, aGraphs);
                if (objectNode != null) {
                  arrayNode.add(objectNode);
                }
              } else {
                ObjectNode objectNode = link(id, aGraphs);
                if (objectNode != null) {
                  arrayNode.add(objectNode);
                }
              }
            } else {
              arrayNode.add(arrayValue);
            }
          }
        }
        result.put(key, arrayNode);
      }
    }

    return result;

  }

  private static ObjectNode link(String aId, ArrayNode aGraphs) {

    ObjectNode graph = getGraph(aId, aGraphs);
    if (graph == null) {
      return null;
    }

    ObjectNode result = new ObjectNode(JsonNodeFactory.instance);

    String[] linkProperties = new String[] {
      "@id", "@type", "name"
    };

    for (String linkProperty : linkProperties) {
      if (graph.hasNonNull(linkProperty)) {
        result.put(linkProperty, graph.get(linkProperty));
      }
    }

    return result;

  }

  private static ObjectNode getGraph(String aId, ArrayNode aGraphs) {

    if (aGraphs == null) {
      return null;
    }

    ObjectNode graph = null;

    for (JsonNode jsonNode : aGraphs) {
      if (jsonNode.hasNonNull(JsonLdConstants.ID) && aId.equals(jsonNode.get(JsonLdConstants.ID).textValue())) {
        graph = (ObjectNode) jsonNode;
        break;
      }
    }

    if (graph != null && graph.hasNonNull(JsonLdConstants.ID)
        && graph.get(JsonLdConstants.ID).textValue().startsWith("_:")) {
      graph.remove(JsonLdConstants.ID);
    }

    return graph;

  }


}
