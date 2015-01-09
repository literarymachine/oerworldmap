package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.*;
import models.Resource;
import services.ResourceRepository;
import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.impl.NQuadRDFParser;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.InputStream;
import com.github.jknack.handlebars.*;

public class ResourceIndex extends Controller {

  private static ResourceRepository resourceRepository = new ResourceRepository();

  public static Result get() throws IOException, JsonLdError {
    Object jsonObject = getJsonForID("JsonLdTest", "person");
    JsonNode jsonNode = new ObjectMapper().convertValue(jsonObject, JsonNode.class);

    Handlebars handlebars = new Handlebars();
    handlebars.registerHelper("json", Jackson2Helper.INSTANCE);

    Context ctx = Context
      .newBuilder(jsonNode)
      .resolver(JsonNodeValueResolver.INSTANCE)
      .build();
    Template template = handlebars.compileInline("Hello {{givenName}} {{familyName}}!");
    return ok(template.apply(ctx));
  }

  public static Result post() {
    JsonNode json = request().body().asJson();
    if (json == null) {
      return badRequest("Expecting Json data");
    } else {
      return ok(json);
    }
  }

  private static Object getJsonLdFrameFor(String type) throws IOException {
    String root = play.Play.application().path().getAbsolutePath();
    String fileName = type + ".json";
    InputStream inputStream = new FileInputStream(root + "/public/frames/" + fileName);
    LinkedHashMap frame = (LinkedHashMap)JsonUtils.fromInputStream(inputStream);
    frame.put("@context", getJsonLdContextFor(type));
    return frame;
  }

  private static String getJsonLdContextFor(String type) {
    String fileName = type + ".json";
    return routes.Assets.at("contexts/" + fileName).absoluteURL(request());
  }

  private static Object getJsonForID(String id, String type) throws IOException, JsonLdError {
    String root = play.Play.application().path().getAbsolutePath();
    String path = root + "/tests/data/JsonLdTest" + ".nt";
    String nt = new String(Files.readAllBytes(Paths.get(path)));
    NQuadRDFParser parser = new NQuadRDFParser();
    Object jsonObject = JsonLdProcessor.fromRDF(nt, parser);
    Object frame = getJsonLdFrameFor("person");
    JsonLdOptions options = new JsonLdOptions();
    Map compact = JsonLdProcessor.frame(jsonObject, frame, options);
    ArrayList valueObjects = (ArrayList)compact.get("@graph");
    Map valueObject = new LinkedHashMap();
    valueObject.put("@context", getJsonLdContextFor(type));
    valueObject.putAll((Map)valueObjects.get(0));
    return valueObject;
  }

}

