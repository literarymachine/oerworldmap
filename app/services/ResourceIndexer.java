package services;

import models.Commit;
import models.GraphHistory;
import models.Record;
import models.Resource;
import models.TripleCommit;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.Lock;
import play.Logger;
import services.repository.TriplestoreRepository;
import services.repository.Writable;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by fo on 23.03.16.
 */
public class ResourceIndexer {

  private Model mDb;
  private Writable mTargetRepo;
  private GraphHistory mGraphHistory;
  private AccountService mAccountService;
  private String mContextUrl;

  private final static String GLOBAL_QUERY_TEMPLATE =
    "SELECT DISTINCT ?s WHERE { ?s a [] }";

  private final static String SCOPE_QUERY_TEMPLATE =
    "SELECT DISTINCT ?s1 WHERE { ?s1 ?p1 <%1$s> }";

  private final static String LINK_COUNT_QUERY_TEMPLATE =
    "SELECT (count(?p) as ?link_count) WHERE { _:s ?p <%1$s> }";

  private final static String LIKE_COUNT_QUERY_TEMPLATE =
    "SELECT (count(distinct ?like) as ?like_count) WHERE { ?like a <http://schema.org/LikeAction> . ?like <http://schema.org/object> <%1$s> . }";

  private final static String LIGHTHOUSE_COUNT_QUERY_TEMPLATE =
    "SELECT (count(distinct ?lighthouse) as ?lighthouse_count) WHERE { ?lighthouse a <http://schema.org/LighthouseAction> . ?lighthouse <http://schema.org/object> <%1$s> . }";

  public ResourceIndexer(Model aDb, Writable aTargetRepo, GraphHistory aGraphHistory, AccountService aAccountService,
                         String aContextUrl) {
    mDb = aDb;
    mTargetRepo = aTargetRepo;
    mGraphHistory = aGraphHistory;
    mAccountService = aAccountService;
    mContextUrl = aContextUrl;
  }

  /**
   * Extracts resources that need to be indexed from a triple diff
   *
   * @param aDiff The diff from which to extract resources
   * @return The list of resources touched by the diff
   */
  private Set<String> getScope(Commit.Diff aDiff) {

    Set<String> commitScope = new HashSet<>();
    Set<String> indexScope = new HashSet<>();

    if (aDiff.getLines().isEmpty()) {
      return commitScope;
    }

    for (Commit.Diff.Line line : aDiff.getLines()) {
      RDFNode subject = ((TripleCommit.Diff.Line) line).stmt.getSubject();
      RDFNode object = ((TripleCommit.Diff.Line) line).stmt.getObject();
      if (subject.isURIResource()) {
        commitScope.add(subject.toString());
      }
      if (object.isURIResource()) {
        commitScope.add(object.toString());
      }
    }

    indexScope.addAll(commitScope);
    indexScope.addAll(getScope(commitScope));

    Logger.debug("Indexing scope is " + indexScope);

    return indexScope;
  }

  /**
   * Queries the triple store for related resources that must also be indexed
   *
   * @param aIds The list of resources for which to find related resources
   * @return The list of related resources
   */
  private Set<String> getScope(Set<String> aIds) {

    Set<String> indexScope = new HashSet<>();
    mDb.enterCriticalSection(Lock.READ);
    try {
      for (String id : aIds) {
        indexScope.addAll(getScope(id));
      }
    } finally {
      mDb.leaveCriticalSection();
    }

    return indexScope;
  }

  /**
   * Queries the triple store for related resources that must also be indexed
   *
   * @param aId A resource for which to find related resources
   * @return The list of related resources
   */
  private Set<String> getScope(String aId) {

    Set<String> indexScope = new HashSet<>();
    String query = String.format(SCOPE_QUERY_TEMPLATE, aId);
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(query), mDb)) {
      ResultSet rs = queryExecution.execSelect();
      while (rs.hasNext()) {
        QuerySolution qs = rs.next();
        if (qs.contains("s1")) {
          indexScope.add(qs.get("s1").toString());
        }
        if (qs.contains("s2")) {
          indexScope.add(qs.get("s2").toString());
        }
      }
    } catch (QueryParseException e) {
      Logger.error("Failed to execute query " + query, e);
    }

    return indexScope;
  }

  /**
   * Queries the triple store for all resources to be indexed
   *
   * @return The list of typed resources
   */
  public Set<String> getScope() {

    Set<String> indexScope = new HashSet<>();
    String query = GLOBAL_QUERY_TEMPLATE;
    try (QueryExecution queryExecution = QueryExecutionFactory
      .create(QueryFactory.create(query), mDb)) {
      ResultSet rs = queryExecution.execSelect();
      while (rs.hasNext()) {
        QuerySolution qs = rs.next();
        if (qs.contains("s") && qs.get("s").isURIResource()) {
          indexScope.add(qs.get("s").toString());
        }
      }
    } catch (QueryParseException e) {
      Logger.error("Failed to execute query " + query, e);
    }

    Logger.debug("Indexing scope" + indexScope.toString());

    return indexScope;
  }

  public Resource getResource(String aId) {

    try {
      Resource resource = ResourceFramer.resourceFromModel(TriplestoreRepository.getExtendedDescription(aId, mDb),
        aId, mContextUrl);
      if (resource != null) {
        return resource;
      }
    } catch (IOException e) {
      Logger.error("Could not create resource from model", e);
    }

    return null;
  }

  private Set<Resource> getResources(String aId) {

    Set<Resource> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope(aId);
    for (String id : idsToIndex) {
      resourcesToIndex.add(getResource(id));
    }

    return resourcesToIndex;
  }

  private Set<Resource> getResources(Commit.Diff aDiff) {

    Set<Resource> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope(aDiff);
    for (String id : idsToIndex) {
      resourcesToIndex.add(getResource(id));
    }

    return resourcesToIndex;
  }

  public Set<Resource> getResources() {

    Set<Resource> resourcesToIndex = new HashSet<>();
    Set<String> idsToIndex = this.getScope();
    for (String id : idsToIndex) {
      resourcesToIndex.add(getResource(id));
    }

    return resourcesToIndex;
  }

  public void index(Resource aResource) {

    if (aResource.hasId()) {
      try {
        Map<String, String> metadata = new HashMap<>();
        if (mGraphHistory != null && mAccountService != null) {
          List<Commit> history = mGraphHistory.log(aResource.getId());
          metadata.put(Record.CONTRIBUTOR, mAccountService.getProfileId(history.get(0).getHeader().getAuthor()));
          metadata.put(Record.AUTHOR,
            mAccountService.getProfileId(history.get(history.size() - 1).getHeader().getAuthor()));
          metadata.put(Record.DATE_MODIFIED, history.get(0).getHeader().getTimestamp()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
          metadata
            .put(Record.DATE_CREATED, history.get(history.size() - 1).getHeader().getTimestamp()
              .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        mDb.enterCriticalSection(Lock.READ);
        try {
          try (QueryExecution queryExecution = QueryExecutionFactory
            .create(QueryFactory.create(String.format(LINK_COUNT_QUERY_TEMPLATE, aResource.getId())), mDb)) {
            metadata.put(Record.LINK_COUNT, queryExecution.execSelect().next().get("link_count").asLiteral().getString());
          }
          try (QueryExecution queryExecution = QueryExecutionFactory
            .create(QueryFactory.create(String.format(LIKE_COUNT_QUERY_TEMPLATE, aResource.getId())), mDb)) {
            metadata.put(Record.LIKE_COUNT, queryExecution.execSelect().next().get("like_count").asLiteral().getString());
          }
          try (QueryExecution queryExecution = QueryExecutionFactory
            .create(QueryFactory.create(String.format(LIGHTHOUSE_COUNT_QUERY_TEMPLATE, aResource.getId())), mDb)) {
            metadata.put(Record.LIGHTHOUSE_COUNT, queryExecution.execSelect().next().get("lighthouse_count").asLiteral().getString());
          }
        } finally {
          mDb.leaveCriticalSection();
        }
        mTargetRepo.addResource(aResource, metadata);
      } catch (IndexOutOfBoundsException | IOException e) {
        Logger.error("Could not index resource", e);
      }
    }
  }

  public void index(Set<Resource> aResources) {

    long startTime = System.nanoTime();
    for (Resource resource : aResources) {
      if (resource != null) {
        index(resource);
      }
    }
    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1000000000;
    Logger.debug("Done indexing, took ".concat(Long.toString(duration)).concat(" sec."));
  }

  public void index(Commit.Diff aDiff) {

    Set<Resource> denormalizedResources = getResources(aDiff);
    index(denormalizedResources);
  }

  public void index(String aId) {

    Set<Resource> denormalizedResources;

    if (aId.equals("*")) {
      denormalizedResources = getResources();
    } else {
      denormalizedResources = getResources(aId);
    }

    index(denormalizedResources);
  }
}
