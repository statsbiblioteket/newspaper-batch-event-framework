package dk.statsbiblioteket.medieplatform.autonomous;

import java.util.Date;

/**
 * Interface for storing result of an event.
 */
public interface EventStorer<T extends Item> {

    /**
     * Append an event to a item in doms.
     *
     * @param item            the item
     * @param agent           the agent of the event
     * @param timestamp       the timestamp of the event
     * @param details         details about the OUTCOME of the event
     * @param eventType       the type of event, from a controlled list
     * @param outcome         true if the event was a success, false otherwise
     *
     * @throws CommunicationException if communication with doms failed
     */
    Date appendEventToItem(T item, String agent, Date timestamp, String details,
                         String eventType, boolean outcome) throws CommunicationException, NotFoundException;

    /**
     * Prepend an event to a item in doms.
     *
     * @param item            the item
     * @param agent           the agent of the event
     * @param timestamp       the timestamp of the event
     * @param details         details about the OUTCOME of the event
     * @param eventType       the type of event, from a controlled list
     * @param outcome         true if the event was a success, false otherwise
     *
     * @throws CommunicationException if communication with doms failed
     */
    Date prependEventToItem(T item, String agent, Date timestamp, String details,
                         String eventType, boolean outcome) throws CommunicationException, NotFoundException;

    /**
     * Remove one or more events from an item. If an event with a given type exists more than once in the item's eventList, remove all entries
     *
     * @param item      the item
     * @param eventType the type of event, from a controlled list
     *
     * @return the number of events removed
     * @throws CommunicationException if communication with doms failed
     */
    int removeEventFromItem(T item, String eventType) throws CommunicationException, NotFoundException;



    /**
     * This method
     * i) reads the EVENTS datastream
     * ii) Calculates the number of events to be removed from the EVENTS datastream starting with the
     * given eventId
     * iii) if there are no events to be removed it just returns zero, otherwise it
     * iv) makes a backup of the EVENTS datastream for this batch round trip
     * v) writes the modfied EVENTS datastream back to DOMS
     *
     * It is the job of implementers of this method to ensure that it correctly handles the possibility of concurrent
     * modification ie. that the datastream may have changed again between being read and being written. (In which case
     * one should return to step i).
     *
     * @param eventId         The eventId of the of the earliest event to be removed
     *
     * @return the number of events removed.
     * @throws CommunicationException
     */
    int triggerWorkflowRestartFromFirstFailure(T item, String eventId) throws CommunicationException, NotFoundException;

    /**
     * This method
     * i) reads the EVENTS datastream
     * ii) Calculates the number of events to be removed from the EVENTS datastream starting with the
     * earliest failure
     * iii) if there are no events to be removed it just returns zero, otherwise it
     * iv) makes a backup of the EVENTS datastream for this batch round trip
     * v) writes the modfied EVENTS datastream back to DOMS
     *
     * It is the job of implementers of this method to ensure that it correctly handles the possibility of concurrent
     * modification ie. that the datastream may have changed again between being read and being written. (In which case
     * one should return to step i).
     *
     * @return the number of events removed.
     * @throws CommunicationException
     */
    int triggerWorkflowRestartFromFirstFailure(T item) throws CommunicationException, NotFoundException;
}
