package dk.statsbiblioteket.medieplatform.autonomous;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the {@link EventTrigger} interface using SBOI summa index and DOMS.
 * Uses soap, json and xml to query the summa instance for batches, and REST to get batch details from DOMS.
 */
public class SBOIEventIndex<T extends Item> implements EventTrigger<T> {

    public static final String SUCCESSEVENT = "success_event";
    public static final String FAILEVENT = "fail_event";
    public static final String RECORD_BASE = "recordBase:doms_sboiCollection";
    public static final String ROUND_TRIP_NO = "round_trip_no";
    public static final String BATCH_ID = "batch_id";
    public static final String UUID = "round_trip_uuid";
    public static final String PREMIS_NO_DETAILS = "premis_no_details";

    private static Logger log = org.slf4j.LoggerFactory.getLogger(SBOIEventIndex.class);
    private final PremisManipulatorFactory<T> premisManipulatorFactory;
    private DomsEventStorage<T> domsEventStorage;
    private final HttpSolrServer summaSearch;

    public SBOIEventIndex(String summaLocation, PremisManipulatorFactory<T> premisManipulatorFactory,
                          DomsEventStorage<T> domsEventStorage) throws MalformedURLException {
        this.premisManipulatorFactory = premisManipulatorFactory;
        this.domsEventStorage = domsEventStorage;
        summaSearch = new SolrJConnector(summaLocation).getSolrServer();

    }

    public static String anded(List<String> events) {
        StringBuilder result = new StringBuilder();
        for (String event : events) {
            result.append(" AND ").append(event);
        }
        return result.toString();
    }


    @Override
    public Iterator<T> getTriggeredItems(Collection<String> pastSuccessfulEvents,
                                            Collection<String> pastFailedEvents, Collection<String> futureEvents) throws CommunicationException {
        return getTriggeredItems(pastSuccessfulEvents, pastFailedEvents, futureEvents, null);
    }

    @Override
    public Iterator<T> getTriggeredItems(Collection<String> pastSuccessfulEvents,
                                            Collection<String> pastFailedEvents, Collection<String> futureEvents,
                                            Collection<T> batches) throws CommunicationException {
        Iterator<T> sboiBatches = search(false, pastSuccessfulEvents, pastFailedEvents, futureEvents,batches);
        ArrayList<T> result = new ArrayList<>();
        while (sboiBatches.hasNext()) {
            T next = sboiBatches.next();
            T instead;
            try {
                instead = domsEventStorage.getItemFromDomsID(next.getDomsID());
            } catch (NotFoundException ignored) {
                continue;
            }
            if (match(instead, pastSuccessfulEvents, pastFailedEvents, futureEvents)) {
                result.add(instead);
            }
        }
        return result.iterator();

    }

    /**
     * Check that the item matches the requirements expressed in the three lists
     *
     * @param item                the item to check
     * @param pastSuccessfulEvents events that must be success
     * @param pastFailedEvents     events that must be failed
     * @param futureEvents         events that must not be there
     *
     * @return true if the item match all requirements
     */
    private boolean match(Item item, Collection<String> pastSuccessfulEvents, Collection<String> pastFailedEvents,
                          Collection<String> futureEvents) {
        Set<String> successEvents = new HashSet<>();
        Set<String> failEvents = new HashSet<>();
        for (Event event : item.getEventList()) {
            if (event.isSuccess()) {
                successEvents.add(event.getEventID());
            } else {
                failEvents.add(event.getEventID());
            }
        }
        return successEvents.containsAll(pastSuccessfulEvents) && failEvents.containsAll(pastFailedEvents) && Collections
                .disjoint(futureEvents, successEvents) && Collections.disjoint(futureEvents, failEvents);
    }


    /**
     * Perform a search for items matching the given criteria
     *
     * @param pastSuccessfulEvents Events that the batch must have sucessfully experienced
     * @param pastFailedEvents     Events that the batch must have experienced, but which failed
     * @param futureEvents         Events that the batch must not have experienced
     * @param items                if not null, the resulting iterator will only contain items from this set. If the
     *                             items is empty, the result will be empty.
     *
     * @return An iterator over the found items
     * @throws CommunicationException if the communication failed
     */
    public Iterator<T> search(boolean details, Collection<String> pastSuccessfulEvents, Collection<String> pastFailedEvents,
                              Collection<String> futureEvents, Collection<T> items) throws CommunicationException {
        try {

            SolrQuery query = new SolrQuery();
            query.setQuery(toQueryString(pastSuccessfulEvents, pastFailedEvents, futureEvents, items));
            query.setRows(1000); //Fetch size. Do not go over 1000 unless you specify fields to fetch which does not include content_text
            //IMPORTANT!Only use facets if needed.
            query.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
            query.setFields(UUID,PREMIS_NO_DETAILS);
            QueryResponse response = summaSearch.query(query);
            SolrDocumentList results = response.getResults();
            List<T> hits = new ArrayList<>();
            for (SolrDocument result : results) {
                T hit;
                String uuid = result.getFieldValue(UUID).toString();
                if (!details) { //no details, so we can retrieve everything from Summa
                    hit
                            = premisManipulatorFactory.createFromBlob(new ByteArrayInputStream(result.getFirstValue(PREMIS_NO_DETAILS)
                                                                                                     .toString()
                                                                                                     .getBytes()))
                                                      .toItem();
                    hit.setDomsID(uuid);
                } else {//Details requested so go to DOMS
                    hit = domsEventStorage.getItemFromDomsID(uuid);
                }

                hits.add(hit);
            }
            return hits.iterator();
        } catch (Exception e){
            log.warn("Caught Unknown Exception", e);
            throw new CommunicationException(e);
        }
    }

    protected static String commaSeparate(String... elements) {
        StringBuilder result = new StringBuilder();
        for (String element : elements) {
            if (element == null) {
                continue;
            }
            if (result.length() != 0) {
                result.append(",");
            }
            result.append(element);
        }
        return result.toString();
    }

    protected String getPremisFieldName(boolean details) {
        if (details) {
            return "";
        } else {
            return PREMIS_NO_DETAILS;
        }
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
        batchesString.append(" ( ");

        boolean first = true;
        for (Item item : items) {
            if (first) {
                first = false;
            } else {
                batchesString.append(" OR ");
            }
            batchesString.append(" ( ");

            batchesString.append(UUID).append(":").append(item.getDomsID());

            batchesString.append(" ) ");
        }
        batchesString.append(" ) ");

        itemsString = batchesString.toString();
        return itemsString;
    }
}
