package org.sakaiproject.nakamura.solr;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.solr.IndexingHandler;
import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.ParserConfigurationException;

public class ContentEventListenerSoak {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ContentEventListener.class);
  @Mock
  private SlingRepository repository;
  @Mock
  private Session session;
  
  public static void main(String[] argv) throws IOException, ParserConfigurationException, SAXException, RepositoryException, InterruptedException {
    ContentEventListenerSoak s = new ContentEventListenerSoak();
    s.testContentEventListener();
  }

  public ContentEventListenerSoak() throws IOException, ParserConfigurationException,
      SAXException {
    MockitoAnnotations.initMocks(this);
  }

  public void testContentEventListener() throws IOException, RepositoryException,
      InterruptedException {
    final ContentEventListener contentEventListener = new ContentEventListener();
    contentEventListener.repository = repository;

    contentEventListener.solrServerService = new SolrServerService() {
      
      public String getSolrHome() {
        return "target/solrtest";
      }
      
      public SolrServer getServer() {
        return null;
      }
    };;;
    Map<String, Object> properties = new HashMap<String, Object>();
    Mockito.when(repository.loginAdministrative(null)).thenReturn(session);
    properties.put("dont-run", true);
    contentEventListener.activate(properties);

    IndexingHandler h =  new IndexingHandler() {
      
      public Collection<SolrInputDocument> getDocuments(Session session, Event event) {
        return new ArrayList<SolrInputDocument>();
      }
      
      public Collection<String> getDeleteQueries(Session session, Event event) {
        return new ArrayList<String>();
      }
    };
    contentEventListener.addHandler("test/topic",h);
    Random r = new Random();
    for (int j = 0; j < 100; j++) {
      int e = r.nextInt(10000);
      LOGGER.debug("Adding Events {} ",e);
      for (int i = 0; i < e; i++) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("evnumber", i);
        props.put("a nasty,key", "with a, nasty \n value");
        contentEventListener.handleEvent(new Event("test/topic", props));
      }
      int t = r.nextInt(10000);
      LOGGER.info("Sleeping {} ",t);
      Thread.sleep(t);
    }
    contentEventListener.closeWriter();
    LOGGER.info("Done adding Events ");

    contentEventListener.removeHander("/test/topic", h);

    contentEventListener.deactivate(properties);

    LOGGER.info("Waiting for worker thread ");
    contentEventListener.getQueueDispatcher().join();
    LOGGER.info("Joined worker thread");
  }

}
