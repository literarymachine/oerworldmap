package models;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class ResourceAddInformationTest extends ResourceTestBase {

  @Test
  public void testPersonWithArticleWithoutId() {
    final Resource person = Resource
        .fromJson("{\"@type\":\"Person\",\"@id\":\"person00002\",\"workLocation\":{\"@type\":\"Place\",\"@id\":\"afcad4ac-9c27-44fe-b7c6-489b7a19cc27\",\"address\":{\"@type\":\"PostalAddress\",\"@id\":\"44c95321-b734-4bd8-b41b-5fb58352b03f\",\"addressCountry\":\"IT\"}},\"email\":\"e553ac652d8b349f6b3f603aaed06bf40b72dd2d3c59ea41a1cb4b83ea3e70c9\"}");
    person.put("mbox_sha1sum", "encryptedEmailAddressPerson00002");
    person.remove("email");
    person.put("authorOf", "article00001");
    try {
      mRepo.addResource(person);

    } catch (IOException e) {
      e.printStackTrace();
    }
    Assert
        .assertEquals(
            Resource
                .fromJson("{\"workLocation\":{\"address\":{\"addressCountry\":\"IT\",\"@type\":\"PostalAddress\"},\"@type\":\"Place\"},\"@type\":\"Person\",\"authorOf\":\"article00001\",\"mbox_sha1sum\":\"encryptedEmailAddressPerson00002\",\"@id\":\"person00002\"}"),
            mRepo.getResource("person00002"));
    Assert.assertNull(mRepo.getResource("article00001"));
  }
  
  @Test
  public void testPersonWithArticleWithId() {
    final Resource person = Resource
        .fromJson("{\"@type\":\"Person\",\"@id\":\"person00003\",\"workLocation\":{\"@type\":\"Place\",\"@id\":\"afcad4ac-9c38-44fe-b7c6-489b7a19cc27\",\"address\":{\"@type\":\"PostalAddress\",\"@id\":\"55c95321-b734-4bd8-b41b-5fb58352b03f\",\"addressCountry\":\"FR\"}},\"email\":\"e663ac652d8b349f6b3f603aaed06bf40b72dd2d3c59ea41a1cb4b83ea3e70c9\"}");
    person.put("mbox_sha1sum", "encryptedEmailAddressPerson00003");
    person.remove("email");
    Resource article = Resource.fromJson("{\"@type\":\"Article\",\"@id\":\"article00002\", \"name\":\"Highly interesting\", \"articleBody\":\"...and so on.\"}");
    article.put("creator", person);
    // person.put("authorOf", article); TODO: currently leads to stack overflow (circling references)
    try {
      mRepo.addResource(person);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // TODO: Assert.....
  }
}
