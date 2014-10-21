package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.medieplatform.autonomous.Item;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.RunnableComponent;

import java.util.Properties;

public class RunnableTitleRecordRelationsMaintainer implements RunnableComponent<Item> {
    private final Properties properties;
    private final EnhancedFedora eFedora;

    public RunnableTitleRecordRelationsMaintainer(Properties properties, EnhancedFedora eFedora) {

        this.properties = properties;
        this.eFedora = eFedora;
    }

    @Override
    public String getComponentName() {
        return null;
    }

    @Override
    public String getComponentVersion() {
        return null;
    }

    @Override
    public String getEventID() {
        return null;
    }

    @Override
    public void doWorkOnItem(Item item, ResultCollector resultCollector) throws Exception {
        //What should this component do??
        //First, the item is a newspaper object
        //It will trigger if the newspaper object have changed
        //It should find all the edition objects, which correspond to this newspaper and date range
    }
}
