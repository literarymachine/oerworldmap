package models;

import org.apache.commons.io.IOUtils;
import org.pegdown.PegDownProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fo on 26.07.16.
 */
public class Page {

  private static PegDownProcessor pegDownProcessor = new PegDownProcessor();

  /**
   * Parse a page into front matter and body
   *
   * Adopted from http://stackoverflow.com/questions/11770077/parsing-yaml-front-matter-in-java
   * @param aInputStream The InputStream to parse
   * @return A map of front matter key values and page content
   * @throws IOException
   */
  public static Map<String, String> parse(InputStream aInputStream) throws IOException {

    Map<String, String> page = new HashMap<>();

    BufferedReader br = new BufferedReader(new InputStreamReader(aInputStream));

    // detect YAML front matter
    String line = br.readLine();
    while (line.isEmpty()) line = br.readLine();
    if (!line.matches("[-]{3,}")) { // use at least three dashes
      throw new IllegalArgumentException("No YAML Front Matter");
    }
    final String delimiter = line;

    // scan YAML front matter
    line = br.readLine();
    while (!line.equals(delimiter)) {
      String[] entry = line.split(":");
      page.put(entry[0].trim(), entry[1].trim());
      line = br.readLine();
    }

    page.put("body", pegDownProcessor.markdownToHtml(IOUtils.toString(br)));

    return page;

  }

}
