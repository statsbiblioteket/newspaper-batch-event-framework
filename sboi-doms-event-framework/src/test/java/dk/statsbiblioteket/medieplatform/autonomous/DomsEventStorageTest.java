package dk.statsbiblioteket.medieplatform.autonomous;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.ChecksumType;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DomsEventStorageTest {

    public static final String BATCH_ID = "400022028241";
    public static final int ROUND_TRIP_NUMBER = 1;

    @Test
    public void testAddEventToBatch3() throws Exception {
        ArrayList<String> log = new ArrayList<>();
        FedoraMockupEmpty fedora = new FedoraMockupEmpty(log);

        DomsEventStorage doms = new DomsEventStorage(
                fedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);

        doms.addEventToBatch(
                BATCH_ID, ROUND_TRIP_NUMBER + 1, "agent", new Date(0), "Details here", "Data_Received", true);

        Assert.assertEquals(8, log.size());
        for (String s : log) {
            Assert.assertNotSame(s, AbstractFedoraMockup.UNEXPECTED_METHOD);
            System.out.println(s);
        }

    }

    @Test
    public void testAddEventToBatch1() throws Exception {
        ArrayList<String> log = new ArrayList<>();
        FedoraMockupEmpty fedora = new FedoraMockupEmpty(log);

        DomsEventStorage doms = new DomsEventStorage(
                fedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);

        doms.addEventToBatch(BATCH_ID, ROUND_TRIP_NUMBER, "agent", new Date(0), "Details here", "Data_Received", true);

        Assert.assertEquals(8, log.size());
        for (String s : log) {
            Assert.assertNotSame(s, AbstractFedoraMockup.UNEXPECTED_METHOD);
            System.out.println(s);
        }

    }

    @Test
    public void testAddEventToBatch2() throws Exception {
        ArrayList<String> log = new ArrayList<>();
        FedoraMockupEmpty fedora = new FedoraMockupBatchNoRoundTripObject(log);
        DomsEventStorage doms = new DomsEventStorage(
                fedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);

        doms.addEventToBatch(BATCH_ID, ROUND_TRIP_NUMBER, "agent", new Date(0), "Details here", "Data_Received", true);

        Assert.assertEquals(log.size(), 3);
        for (String s : log) {
            Assert.assertNotSame(s, AbstractFedoraMockup.UNEXPECTED_METHOD);
            System.out.println(s);
        }

    }

    @Test
    public void testCreateBatchRoundTrip() throws Exception {
        ArrayList<String> log = new ArrayList<>();
        FedoraMockupEmpty fedora = new FedoraMockupEmpty(log);
        DomsEventStorage doms = new DomsEventStorage(
                fedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);
        doms.createBatchRoundTrip(BATCH_ID, ROUND_TRIP_NUMBER);
        Assert.assertEquals(log.size(), 6);
        for (String s : log) {
            Assert.assertNotSame(s, AbstractFedoraMockup.UNEXPECTED_METHOD);
            System.out.println(s);
        }

    }

    /**
     * Tests the case where we remove all events after the first error.
     *
     * @throws Exception
     */
    @Test
    public void testTriggerWorkflowRestartFromError() throws Exception {
        EnhancedFedora enhancedFedora = mock(EnhancedFedora.class);
        ObjectProfile objectProfile = new ObjectProfile();
        objectProfile.setObjectLastModifiedDate(new Date());
        when(enhancedFedora.getObjectProfile(anyString(), anyLong())).thenReturn(objectProfile);
        PremisManipulator manipulator = getPremisManipulator();
        when(
                enhancedFedora.getXMLDatastreamContents(
                        anyString(), anyString(), anyLong())).thenReturn(manipulator.toXML());
        ArrayList<String> pids = new ArrayList<>();
        pids.add("uuid:thepid");
        when(enhancedFedora.findObjectFromDCIdentifier(anyString())).thenReturn(pids);

        //The following call means that the 1st attempt to reset the events throws an exception, but the second attempt
        //is successful
        doThrow(new ConcurrentModificationException()).doReturn(new Date()).when(enhancedFedora).modifyDatastreamByValue(
                anyString(),
                anyString(),
                any(ChecksumType.class),
                anyString(),
                any(byte[].class),
                any((new ArrayList<String>()).getClass()),
                anyString(),
                anyString(),
                anyLong());

        DomsEventStorage doms = new DomsEventStorage(
                enhancedFedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);
        //Make the call
        int eventsRemoved = doms.triggerWorkflowRestartFromFirstFailure("foo", 3, 10, 10L);
        assertEquals(eventsRemoved, 6);
        //The captor is used to capture the modified datastream so it can be examined to see if it has been
        //correctly modfied
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

        //There should be two calls to each of these methods, once for each attempt
        verify(enhancedFedora, times(2)).modifyDatastreamByValue(
                anyString(),
                anyString(),
                any(ChecksumType.class),
                anyString(),
                captor.capture(),
                any((new ArrayList<String>()).getClass()),
                anyString(),
                anyString(),
                anyLong());

        //The failed events should have been stripped from the xml
        String newXml = new String(captor.getValue());
        assertTrue(newXml.contains("e1"));
        assertTrue(newXml.contains("e2"));
        assertFalse(newXml.contains("e3"));
        assertFalse(newXml.contains("e4"));
        assertFalse(newXml.contains("e5"));
        assertFalse(newXml.contains("e6"));
        assertFalse(newXml.contains("e7"));
        assertFalse(newXml.contains("e8"));
    }

    /**
     * Tests the case where we remove all events after a given event.
     *
     * @throws Exception
     */
    @Test
    public void testTriggerWorkflowRestartFromNamedEvent() throws Exception {
        EnhancedFedora enhancedFedora = mock(EnhancedFedora.class);
        ObjectProfile objectProfile = new ObjectProfile();
        objectProfile.setObjectLastModifiedDate(new Date());
        when(enhancedFedora.getObjectProfile(anyString(), anyLong())).thenReturn(objectProfile);
        PremisManipulator manipulator = getPremisManipulator();
        when(
                enhancedFedora.getXMLDatastreamContents(
                        anyString(), anyString(), anyLong())).thenReturn(manipulator.toXML());
        ArrayList<String> pids = new ArrayList<>();
        pids.add("uuid:thepid");
        when(enhancedFedora.findObjectFromDCIdentifier(anyString())).thenReturn(pids);

        //The following call means that the 1st attempt to reset the events throws an exception, but the second attempt
        //is successful
        doThrow(new ConcurrentModificationException()).doReturn(new Date()).when(enhancedFedora).modifyDatastreamByValue(
                anyString(),
                anyString(),
                any(ChecksumType.class),
                anyString(),
                any(byte[].class),
                any((new ArrayList<String>()).getClass()),
                anyString(),
                anyString(),
                anyLong());

        DomsEventStorage doms = new DomsEventStorage(
                enhancedFedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);
        //Make the call
        int eventsRemoved = doms.triggerWorkflowRestartFromFirstFailure("foo", 3, 10, 10L, "e5");
        assertEquals(eventsRemoved, 4);
        //The captor is used to capture the modified datastream so it can be examined to see if it has been
        //correctly modfied
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

        //There should be two calls to each of these methods, once for each attempt
        verify(enhancedFedora, times(2)).modifyDatastreamByValue(
                anyString(),
                anyString(),
                any(ChecksumType.class),
                anyString(),
                captor.capture(),
                any((new ArrayList<String>()).getClass()),
                anyString(),
                anyString(),
                anyLong());

        //The failed events should have been stripped from the xml
        String newXml = new String(captor.getValue());
        assertTrue(newXml.contains("e1"));
        assertTrue(newXml.contains("e2"));
        assertTrue(newXml.contains("e3"));
        assertTrue(newXml.contains("e4"));
        assertFalse(newXml.contains("e5"));
        assertFalse(newXml.contains("e6"));
        assertFalse(newXml.contains("e7"));
        assertFalse(newXml.contains("e8"));
    }

    /**
     * This tests that if the attempt to modify the EVENTS datastream keeps failing then the code will give up after
     * the specified number of attempts.
     *
     * @throws Exception
     */
    @Test
    public void testTriggerWorkflowRestartFails() throws Exception {
        EnhancedFedora enhancedFedora = mock(EnhancedFedora.class);
        ObjectProfile objectProfile = new ObjectProfile();
        objectProfile.setObjectLastModifiedDate(new Date());
        when(enhancedFedora.getObjectProfile(anyString(), anyLong())).thenReturn(objectProfile);
        PremisManipulator manipulator = getPremisManipulator();
        when(
                enhancedFedora.getXMLDatastreamContents(
                        anyString(), anyString(), anyLong())).thenReturn(manipulator.toXML());
        ArrayList<String> pids = new ArrayList<>();
        pids.add("uuid:thepid");
        when(enhancedFedora.findObjectFromDCIdentifier(anyString())).thenReturn(pids);

        //This ensure that the call always throws the exception
        doThrow(new ConcurrentModificationException()).when(enhancedFedora).modifyDatastreamByValue(
                anyString(),
                anyString(),
                any(ChecksumType.class),
                anyString(),
                any(byte[].class),
                any((new ArrayList<String>()).getClass()),
                anyString(),
                anyString(),
                anyLong());

        DomsEventStorage doms = new DomsEventStorage(
                enhancedFedora,
                new NewspaperIDFormatter(),
                PremisManipulatorFactory.TYPE,
                DomsEventStorageFactory.BATCH_TEMPLATE,
                DomsEventStorageFactory.ROUND_TRIP_TEMPLATE,
                DomsEventStorageFactory.HAS_PART,
                DomsEventStorageFactory.EVENTS);
        final int MAX_ATTEMPTS = 10;
        try {
            int eventsRemoved = doms.triggerWorkflowRestartFromFirstFailure("foo", 3, MAX_ATTEMPTS, 10L);
            fail("Should have thrown a " + CommunicationException.class.getSimpleName());
        } catch (CommunicationException e) {
            //expected
        }
        verify(enhancedFedora, times(MAX_ATTEMPTS)).modifyDatastreamByValue(
                anyString(),
                anyString(),
                any(ChecksumType.class),
                anyString(),
                any(byte[].class),
                any((new ArrayList<String>()).getClass()),
                anyString(),
                anyString(),
                anyLong());
    }

    /**
     * Get a Premis object with some successful and some failed events.
     *
     * @return
     * @throws JAXBException
     */
    private PremisManipulator getPremisManipulator() throws JAXBException {
        PremisManipulatorFactory factory = new PremisManipulatorFactory(
                new NewspaperIDFormatter(), PremisManipulatorFactory.TYPE);
        PremisManipulator manipulator = factory.createInitialPremisBlob(BATCH_ID, ROUND_TRIP_NUMBER);
        manipulator = manipulator.addEvent("me", new Date(100), "details here", "e1", true);
        manipulator = manipulator.addEvent("me", new Date(200), "details here", "e2", true);
        manipulator = manipulator.addEvent("me", new Date(300), "details here", "e3", false);
        manipulator = manipulator.addEvent("me", new Date(400), "details here", "e4", true);
        manipulator = manipulator.addEvent("me", new Date(500), "details here", "e5", true);
        manipulator = manipulator.addEvent("me", new Date(600), "details here", "e6", false);
        manipulator = manipulator.addEvent("me", new Date(700), "details here", "e7", true);
        manipulator = manipulator.addEvent("me", new Date(800), "details here", "e8", true);
        return manipulator;
    }

}