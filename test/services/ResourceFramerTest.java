package services;

import static org.junit.Assert.assertEquals;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import helpers.JsonTest;
import models.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by fo on 10.06.16.
 */
public class ResourceFramerTest implements JsonTest {

  @Test
  public void testResourceFramer() throws IOException {

    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, "ResourceFramerTest/testResourceFramer.IN.1.nt", Lang.NTRIPLES);
    Resource actual = ResourceFramer.resourceFromModel(model, "info:carol");
    Resource expected = getResourceFromJsonFile("ResourceFramerTest/testResourceFramer.OUT.1.json");
    assertEquals(expected, actual);

  }

}
