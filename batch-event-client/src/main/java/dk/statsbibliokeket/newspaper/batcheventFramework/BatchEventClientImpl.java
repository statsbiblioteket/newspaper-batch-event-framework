package dk.statsbibliokeket.newspaper.batcheventFramework;

import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.CommunicationException;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventClient;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventClientFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperIDFormatter;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;
import dk.statsbiblioteket.medieplatform.autonomous.PremisManipulatorFactory;
import org.slf4j.Logger;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * Delegating BatchEventClientImpl
 *
 * @see SBOIClientImpl
 * @see DomsEventClientFactory
 */
public class BatchEventClientImpl implements BatchEventClient {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(BatchEventClientImpl.class);
    private String summaLocation;
    private String domsUrl;
    private String domsUser;
    private String domsPass;
    private String urlToPidGen;

    private SBOIInterface sboiClient;
    private DomsEventClient domsEventClient;

    public BatchEventClientImpl(String summaLocation, String domsUrl, String domsUser, String domsPass,
                                String urlToPidGen) {
        this.summaLocation = summaLocation;
        this.domsUrl = domsUrl;
        this.domsUser = domsUser;
        this.domsPass = domsPass;
        this.urlToPidGen = urlToPidGen;
    }

    private SBOIInterface getSboiClient() {
        try {
            if (sboiClient == null) {
                sboiClient = new SBOIClientImpl(
                        summaLocation,
                        new PremisManipulatorFactory(new NewspaperIDFormatter(), PremisManipulatorFactory.TYPE),
                        getDomsEventClient());
            }
            return sboiClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DomsEventClient getDomsEventClient() {
        if (domsEventClient == null) {
            DomsEventClientFactory factory = new DomsEventClientFactory();
            factory.setPidGeneratorLocation(urlToPidGen);
            factory.setPassword(domsPass);
            factory.setUsername(domsUser);
            factory.setFedoraLocation(domsUrl);
            try {
                domsEventClient = factory.createDomsEventClient();
            } catch (JAXBException | MalformedURLException | PIDGeneratorException e) {
                throw new RuntimeException(e);
            }
        }
        return domsEventClient;
    }

    @Override
    public void addEventToBatch(String batchId, int roundTripNumber, String agent, Date timestamp, String details,
                                String eventType, boolean outcome) throws CommunicationException {
        getDomsEventClient().addEventToBatch(batchId, roundTripNumber, agent, timestamp, details, eventType, outcome);
    }

    @Override
    public String createBatchRoundTrip(String batchId, int roundTripNumber) throws CommunicationException {
        return getDomsEventClient().createBatchRoundTrip(batchId, roundTripNumber);
    }

    @Override
    public Batch getBatch(String batchId, Integer roundTripNumber, boolean details) throws
                                                                                    CommunicationException,
                                                                                    NotFoundException {
        return getSboiClient().getBatch(batchId, roundTripNumber, details);
    }

    @Override
    public Batch getBatch(String batchId, Integer roundTripNumber) throws CommunicationException, NotFoundException {
        return getDomsEventClient().getBatch(batchId, roundTripNumber);
    }


    @Override
    public Batch getBatch(String domsID) throws CommunicationException, NotFoundException {
        return getDomsEventClient().getBatch(domsID);
    }

    @Override
    public int triggerWorkflowRestartFromFirstFailure(String batchId, int roundTripNumber, int maxAttempts,
                                                      long waitTime, String eventId) throws
                                                                                     CommunicationException,
                                                                                     NotFoundException {
        return getDomsEventClient().triggerWorkflowRestartFromFirstFailure(
                batchId, roundTripNumber, maxAttempts, waitTime, eventId);
    }

    @Override
    public int triggerWorkflowRestartFromFirstFailure(String batchId, int roundTripNumber, int maxTries,
                                                      long waitTime) throws CommunicationException, NotFoundException {
        return getDomsEventClient().triggerWorkflowRestartFromFirstFailure(
                batchId, roundTripNumber, maxTries, waitTime);
    }

    @Override
    public Iterator<Batch> getBatches(boolean details, List<String> pastSuccessfulEvents, List<String> pastFailedEvents,
                                      List<String> futureEvents) throws CommunicationException {
        return getSboiClient().getBatches(details, pastSuccessfulEvents, pastFailedEvents, futureEvents);

    }

    @Override
    public Iterator<Batch> getCheckedBatches(boolean details, List<String> pastSuccessfulEvents,
                                             List<String> pastFailedEvents, List<String> futureEvents) throws
                                                                                                       CommunicationException {
        return getSboiClient().getCheckedBatches(details, pastSuccessfulEvents, pastFailedEvents, futureEvents);
    }
}
