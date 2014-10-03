package dk.statsbiblioteket.medieplatform.autonomous;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

public class TestingComponent extends AbstractRunnableComponent {

    private ArrayList<Item> items;

    protected TestingComponent(Properties properties) {
        super(properties);
    }


    @Override
    public String getEventID() {
        return "Data_Archived";
    }


    @Override
    public void doWorkOnItem(Item batch, ResultCollector resultCollector) throws Exception {
        System.out.println("working");
    }

    public EventTrigger getEventTrigger() {
        return new EventTrigger() {
            @Override
            public Iterator<Item> getTriggeredItems(Collection<String> pastSuccessfulEvents,
                                                              Collection<String> pastFailedEvents,
                                                              Collection<String> futureEvents)
                    throws CommunicationException {
                return items.iterator();
            }

            @Override
            public Iterator<Item> getTriggeredItems(Collection<String> pastSuccessfulEvents,
                                                              Collection<String> pastFailedEvents,
                                                              Collection<String> futureEvents,
                                                              Collection<Item> itemCollection) throws
                                                                                                    CommunicationException {
                return itemCollection.iterator();
            }
        };
    }

    public EventStorer getEventStorer() {
        return new EventStorer() {
            @Override
            public Date addEventToItem(Item item, String agent, Date timestamp,
                                        String details, String eventType, boolean outcome)
                    throws CommunicationException {
                return addEvent(item.getFullID(), timestamp, details, eventType, outcome);
            }

            private Date addEvent(String fullId, Date timestamp, String details, String eventType,
                                  boolean outcome) {
                for (Item item : items) {
                    if (item.getFullID().equals(fullId)) {
                        Event event = new Event();
                        event.setDate(timestamp);
                        event.setEventID(eventType);
                        event.setSuccess(outcome);
                        event.setDetails(details);
                        item.getEventList().add(event);
                    }
                }
                return new Date();
            }


            @Override
            public int triggerWorkflowRestartFromFirstFailure(Item item, int maxTries,
                                                              long waitTime, String eventId)
                    throws CommunicationException, NotFoundException {
                return 0;
            }

            @Override
            public int triggerWorkflowRestartFromFirstFailure(Item item, int maxTries,
                                                              long waitTime)
                    throws CommunicationException, NotFoundException {
                return 0;
            }
        };
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public Item getItem(String itemFullID) {
        for (Item batch : items) {
            if (batch.getFullID().equals(itemFullID)){
                return batch;
            }
        }
        return null;
    }
}
