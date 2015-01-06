package models;

import play.data.validation.Constraints;
import java.util.Map;
import java.util.HashMap;

public class Resource {

    @Constraints.Required
    public String context;

    @Constraints.Required
    public String id;

    private Map<String, String> properties = new HashMap<String, String>();

    public void set(String property, String value) {
        properties.put(property, value);
    }

    public String get(String property) {
        return properties.get(property);
    }

}

