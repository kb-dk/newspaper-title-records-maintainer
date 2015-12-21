package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsItemFactory;
import dk.statsbiblioteket.medieplatform.autonomous.Item;
import dk.statsbiblioteket.medieplatform.autonomous.SolrJConnector;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.testng.Assert.assertTrue;

public class NewspaperIndexIT {

    private String summaLocation;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();


        props.load(new FileInputStream(pathToProperties));
        summaLocation = props.getProperty(ConfigConstants.AUTONOMOUS_SBOI_URL);

    }

    @Test(groups = "externalTest")
    public void testGetNewspapers() throws Exception {
        NewspaperIndex newspaperIndex = new NewspaperIndex(new SolrJConnector(summaLocation).getSolrServer(),
                                                           new DomsItemFactory());
        List<Item> newspapers = newspaperIndex.getEditions("berlingsketidende", "1749-01-03", "1762-01-01");
        assertTrue(newspapers.size() > 1);
    }

    @Test(groups = "externalTest")
    public void testGetNewspapersNoEndDate() throws Exception {
        NewspaperIndex newspaperIndex = new NewspaperIndex(new SolrJConnector(summaLocation).getSolrServer(),
                new DomsItemFactory());
        List<Item> newspapers = newspaperIndex.getEditions("berlingsketidende", "1749-01-03", "");
        assertTrue(newspapers.size() > 1);
    }

    @Test(groups = "externalTest")
    public void testGetNewspapersNoStartDate() throws Exception {
        NewspaperIndex newspaperIndex = new NewspaperIndex(new SolrJConnector(summaLocation).getSolrServer(),
                new DomsItemFactory());
        List<Item> newspapers = newspaperIndex.getEditions("berlingsketidende", "", "1762-01-01");
        assertTrue(newspapers.size() > 1);
    }

    @Test(groups = "externalTest")
    public void testGetNewspapersNoDates() throws Exception {
        NewspaperIndex newspaperIndex = new NewspaperIndex(new SolrJConnector(summaLocation).getSolrServer(),
                new DomsItemFactory());
        List<Item> newspapers = newspaperIndex.getEditions("berlingsketidende", "", "");
        assertTrue(newspapers.size() > 1);
    }

    @Test(groups = "externalTest")
    public void testGetNewspapersWildcardEquivalent() throws Exception {
        NewspaperIndex newspaperIndex = new NewspaperIndex(new SolrJConnector(summaLocation).getSolrServer(),
                new DomsItemFactory());
        List<Item> newspapersWildcard = newspaperIndex.getEditions("berlingsketidende", "1749-01-03", "");
        List<Item> newspapersFuture = newspaperIndex.getEditions("berlingsketidende", "1749-01-03", "2515-01-01");
        assertThat(newspapersWildcard, containsInAnyOrder(newspapersFuture.toArray()));
    }



}