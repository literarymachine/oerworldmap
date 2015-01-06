package services;

import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import models.Resource;

public class ResourceRepository {

    private HashMap<String, Resource> sink = new HashMap<String, Resource>();

    public void addResource(Resource resource) {
        sink.put(resource.id, resource);
    }

    public Collection query() {
        return sink.values();
    }

}
