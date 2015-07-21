package models;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;

import services.BaseRepository;
import services.ElasticsearchClient;
import services.ElasticsearchConfig;

public abstract class ResourceTestBase {

  // protected static final Map<String, Resource> mResources = new HashMap<>();
  private static final String defaultConfigFile = "conf/test.conf";
  private static final ElasticsearchConfig esConfig = new ElasticsearchConfig(defaultConfigFile);
  private static final Settings settings = ImmutableSettings.settingsBuilder().build();
  private static final Client client = nodeBuilder().settings(settings)
      .clusterName(esConfig.getCluster()).local(true).node().client();
  private static final Path fileRepoPath = Paths.get(System.getProperty("java.io.tmpdir")
      + File.separator + "ResourceTest");
  protected static BaseRepository mRepo = null;

  static {
    // setup repos and elasticsearch
    if (!fileRepoPath.toFile().exists()) {
      fileRepoPath.toFile().mkdirs();
    }
    try {
      // delete Elasticsearch index if still existing from earlier test runs
      if (client.admin().indices().prepareExists(esConfig.getIndex()).execute().actionGet().isExists()){
        client.admin().indices().delete(new DeleteIndexRequest(esConfig.getIndex())).actionGet();
      }
      // setup Elasticsearch new (again)
      client.admin().indices().prepareCreate(esConfig.getIndex()).setSource(esConfig.getIndexConfigString()).execute().actionGet();
      client.admin().indices().refresh(new RefreshRequest(esConfig.getIndex())).actionGet();
      client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(5000);
      mRepo = new BaseRepository(new ElasticsearchClient(client, esConfig), fileRepoPath);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
