package controllers;

import models.Commit;
import models.TripleCommit;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Result;

import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * Created by fo on 21.07.16.
 */
public class Sparql extends OERWorldMap  {

  public static Result query(String q) {

    String form = "<h1 class=\"modal-title\">SPARQL</h1>\n" +
      "\n" +
      "    <form method=\"get\" action=\"/sparql\">\n" +
      "        <textarea style=\"width: 100%\" name=\"q\">" + q + "</textarea>\n" +
      "        <pre>\n" +
      (StringUtils.isEmpty(q) ? "" : StringEscapeUtils.escapeHtml4(mBaseRepository.sparql(q))) +
      "        </pre>\n" +
      "        <input type=\"submit\">\n" +
      "    </form>";

    return ok(form).as("text/html");

  }

  public static Result update(String insert, String delete, String where) {

    String updateForm = "<form method=\"get\" action=\"/sparql/update\">\n" +
      "<label>Delete<input style=\"width:100%\" name=\"delete\" value=\"" + delete + "\"></label>\n" +
      "<label>Insert<input style=\"width:100%\" name=\"insert\" value=\"" + insert + "\"></label>\n" +
      "<label>Where<input style=\"width:100%\" name=\"where\" value=\"" + where + "\"></label>\n" +
      "<input type=\"submit\">" +
      "</form>";

    String patchForm = "<form method=\"post\" action=\"/sparql/patch\">\n" +
      "<textarea style=\"width:100%; height:50%;\" name=\"diff\">" +
      mBaseRepository.update(insert, delete, where) +
      "</textarea>\n" +
      "<input type=\"submit\">" +
      "</form>";

    return ok(updateForm + patchForm).as("text/html");

  }

  public static Result patch() throws IOException {

    String diffString = request().body().asFormUrlEncoded().get("diff")[0];

    Commit.Diff diff = TripleCommit.Diff.fromString(diffString);
    TripleCommit.Header header = new TripleCommit.Header(getMetadata().get(TripleCommit.Header.AUTHOR_HEADER),
      ZonedDateTime.parse(getMetadata().get(TripleCommit.Header.DATE_HEADER)));

    Commit commit = new TripleCommit(header, diff);

    mBaseRepository.commit(commit);

    return ok(commit.toString());

  }


}
