package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.medieplatform.autonomous.*;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewspaperIndex {
    private static final String FIELD_NAME_TITLE_AVIS_ID = "newspapr_title_avisID";
    private static final String FIELD_NAME_TITLE_START_DATE = "newspapr_title_startDate";
    private static final String FIELD_NAME_TITLE_END_DATE = "newspapr_title_endDate";
    private static final String FIELD_NAME_EDITION_AVIS_ID = "newspapr_edition_avisID";
    private static final String FIELD_NAME_EDITION_DATE_ISSUED = "newspapr_edition_dateIssued";
    HttpSolrServer summaSearch;

    /**
     * Constructor
     *
     * @param summaSearch Solr server to use for searching
     */
    public NewspaperIndex(HttpSolrServer summaSearch) {
        this.summaSearch = summaSearch;
    }

    /**
     * Get all editions that match given newspaper object ("titelpost") and date range
     *
     * @param avisID The newspaper ID, for example "adresseavisen1759", identifying the newspaper object to be matched
     * @param startDate The start of the date range to be matched
     * @param endDate The end of the date range to be matched
     * @return A list of the editions that match given newspaper object ("titelpost") and date range
     */
    public List<Item> getEditions(String avisID, String startDate, String endDate) {
        String queryString;
        final int rows = 10;
        int start = 0;

        // Construct query string TODO
        queryString = toQueryString(pastSuccessfulEvents, pastFailedEvents, futureEvents, items);

        try {
            SolrQuery query = new SolrQuery();
            query.setQuery(queryString);
            query.setRows(rows);  // Fetch size. Do not go over 1000 unless you specify fields to fetch not including content_text
            query.setStart(start);
            // IMPORTANT! Only use facets if needed.
            query.set("facet", "false");  // Very important. Must overwrite to false. Facets are very slow and expensive.

            query.setFields(FIELD_NAME_TITLE_AVIS_ID, FIELD_NAME_TITLE_START_DATE, FIELD_NAME_TITLE_END_DATE,
                    FIELD_NAME_EDITION_AVIS_ID, FIELD_NAME_EDITION_DATE_ISSUED);

            query.addSort(SBOIEventIndex.SORT_DATE, SolrQuery.ORDER.asc);

            QueryResponse response = summaSearch.query(query);
            SolrDocumentList results = response.getResults();
            List<T> hits = new ArrayList<>();
            for (SolrDocument result : results) {
                T hit;
                String uuid = result.getFirstValue(SBOIEventIndex.UUID).toString();
                if (!details) {  // No details, so we can retrieve everything from Summa
                    final ByteArrayInputStream inputStream
                            = new ByteArrayInputStream(result.getFirstValue(SBOIEventIndex.PREMIS_NO_DETAILS)
                            .toString()
                            .getBytes());
                    hit = premisManipulatorFactory.createFromBlob(inputStream).toItem();
                    hit.setDomsID(uuid);
                } else {//Details requested so go to DOMS
                    hit = domsEventStorage.getItemFromDomsID(uuid);
                }

                hits.add(hit);
            }
            items = hits.iterator();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return null; // TODO
    }



    protected static String spaced(String string) {
        return " " + string.trim() + " ";
    }

    protected static String quoted(String string) {
        return "\"" + string + "\"";
    }

    protected String toQueryString(Collection<String> pastSuccessfulEvents, Collection<String> pastFailedEvents,
                                   Collection<String> futureEvents, Collection<T> items) {
        String base = spaced(RECORD_BASE);

        String itemsString = "";

        if (items != null) {
            itemsString = getResultRestrictions(items);
        }

        List<String> events = new ArrayList<>();

        if (pastSuccessfulEvents != null) {
            for (String successfulPastEvent : pastSuccessfulEvents) {
                events.add(spaced("+" + SUCCESSEVENT + ":" + quoted(successfulPastEvent)));
            }
        }
        if (pastFailedEvents != null) {
            for (String failedPastEvent : pastFailedEvents) {
                events.add(spaced("+" + FAILEVENT + ":" + quoted(failedPastEvent)));
            }
        }
        if (futureEvents != null) {
            for (String futureEvent : futureEvents) {
                events.add(spaced("-" + SUCCESSEVENT + ":" + quoted(futureEvent)));
                events.add(spaced("-" + FAILEVENT + ":" + quoted(futureEvent)));
            }
        }

        return base + itemsString + anded(events);
    }

    protected String getResultRestrictions(Collection<T> items) {
        String itemsString;
        StringBuilder batchesString = new StringBuilder();
        batchesString.append(" AND ( ");

        boolean first = true;
        for (Item item : items) {
            if (first) {
                first = false;
            } else {
                batchesString.append(" OR ");
            }
            batchesString.append(" ( ");

            batchesString.append("+").append(UUID).append(":\"").append(item.getDomsID()).append("\"");

            batchesString.append(" ) ");
        }
        batchesString.append(" ) ");

        itemsString = batchesString.toString();
        return itemsString;
    }
}
