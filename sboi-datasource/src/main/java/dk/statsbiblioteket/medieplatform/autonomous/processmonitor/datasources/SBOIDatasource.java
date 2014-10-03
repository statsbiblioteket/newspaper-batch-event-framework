package dk.statsbiblioteket.medieplatform.autonomous.processmonitor.datasources;

import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.CommunicationException;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.Event;
import dk.statsbiblioteket.medieplatform.autonomous.EventAccessor;
import dk.statsbiblioteket.medieplatform.autonomous.Item;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperIDFormatter;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;
import dk.statsbiblioteket.medieplatform.autonomous.PremisManipulatorFactory;
import dk.statsbiblioteket.medieplatform.autonomous.SBOIEventIndex;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Datasource implemented to SBOI */
public class SBOIDatasource implements DataSource {

    private SBOIDatasourceConfiguration configuration;
    private EventAccessor client = null;

    public SBOIDatasource(SBOIDatasourceConfiguration configuration) {
        this.configuration = configuration;
    }

    private synchronized EventAccessor getEventExplorer() {
        try {
            if (client == null) {
                client = new SBOIEventIndex(
                        configuration.getSummaLocation(),
                        new PremisManipulatorFactory(new NewspaperIDFormatter(), PremisManipulatorFactory.TYPE),
                        getDomsEventStorage());
            }
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DomsEventStorage getDomsEventStorage() {
        DomsEventStorageFactory factory = new DomsEventStorageFactory();

        factory.setFedoraLocation(configuration.getDomsLocation());
        factory.setUsername(configuration.getDomsUser());
        factory.setPassword(configuration.getDomsPassword());
        DomsEventStorage domsEventStorage;
        try {
            domsEventStorage = factory.createDomsEventStorage();
        } catch (JAXBException | MalformedURLException | PIDGeneratorException e) {
            throw new RuntimeException(e);
        }

        return domsEventStorage;

    }

    @Override
    public List<Batch> getBatches(boolean includeDetails, Map<String, String> filters) throws
                                                                                       NotWorkingProperlyException {
        try {
            Iterator<Item> batches = getEventExplorer().findItems(includeDetails,
                                                                                   Arrays.asList("Data_Received"),
                                                                                   new ArrayList<String>(),
                                                                                   new ArrayList<String>());
            return iteratorToBatchList(batches);
        } catch (CommunicationException e) {
            throw new NotWorkingProperlyException("Failed to communicate with SBOI", e);
        }

    }

    private List<Batch> iteratorToBatchList(Iterator<Item> batches) {
        ArrayList<Batch> result = new ArrayList<>();
        while (batches.hasNext()) {
            Item item = batches.next();
            if (item instanceof Batch) {
                Batch batch = (Batch) item;
                result.add(batch);
            }
        }
        return result;
    }

    /**
     * Strip details if required from the batched
     *
     * @param batches        the batches to strip
     * @param includeDetails true if details should be stripped
     *
     * @return the batches as a list
     */
    private List<Batch> stripDetails(Iterator<Batch> batches, boolean includeDetails) {
        ArrayList<Batch> result = new ArrayList<>();
        while (batches.hasNext()) {
            Batch batch = batches.next();
            result.add(stripDetails(batch, includeDetails));
        }
        return result;
    }

    /**
     * Strip details on a single batch
     *
     * @param batch          the batch
     * @param includeDetails true if details should be stripped
     *
     * @return the batch
     */
    private Batch stripDetails(Batch batch, boolean includeDetails) {
        if (!includeDetails) {
            List<Event> events = batch.getEventList();
            for (Event event : events) {
                event.setDetails(null);
            }
        }
        return batch;
    }

    @Override
    public Batch getBatch(String batchID, Integer roundTripNumber, boolean includeDetails) throws
                                                                                           NotFoundException,
                                                                                           NotWorkingProperlyException {
        try {
            //TODO look into this cast
            return (Batch) getDomsEventStorage().getBatch(batchID, roundTripNumber);
        } catch (CommunicationException e) {
            throw new NotWorkingProperlyException(e);
        }

    }

    @Override
    public Event getBatchEvent(String batchID, Integer roundTripNumber, String eventID, boolean includeDetails) throws
                                                                                                                NotFoundException,
                                                                                                                NotWorkingProperlyException {
        Batch batch = getBatch(batchID, roundTripNumber, includeDetails);
        for (Event event : batch.getEventList()) {
            if (event.getEventID().equals(eventID)) {
                return event;
            }
        }
        throw new NotFoundException("Event not found");
    }
}
