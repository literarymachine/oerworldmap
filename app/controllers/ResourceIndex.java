package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import models.Resource;
import services.ResourceRepository;

public class ResourceIndex extends Controller {

    private static ResourceRepository resourceRepository = new ResourceRepository();

    public static Result get() {
        Resource resource = new Resource();
        resource.id = "foo";
        resource.context = "bar";
        resource.set("baz", "bam");
        resourceRepository.addResource(resource);
        return ok(Json.toJson(resourceRepository.query()));
    }

    public static Result post() {
        JsonNode json = request().body().asJson();
        if (json == null) {
            return badRequest("Expecting Json data");
        } else {
            return ok(json);
        }
    }

}

