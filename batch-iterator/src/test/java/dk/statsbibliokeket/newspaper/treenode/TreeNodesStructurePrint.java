package dk.statsbibliokeket.newspaper.treenode;

import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.AttributeParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.NodeBeginsParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.NodeEndParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.ParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.eventhandlers.EventRunner;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.eventhandlers.TreeEventHandler;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.filesystem.transforming.TransformingIteratorForFileSystems;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TreeNodesStructurePrint {

    @Test(groups = "integrationTest", enabled = false)
    public void printStructure()
            throws
            IOException {

        String pathToTestBatch =
                System.getProperty("integration.test.newspaper.testdata") + "/small-test-batch/B400022028241-RT1/";


        EventRunner eventRunner = new EventRunner(new TransformingIteratorForFileSystems(new File(pathToTestBatch),
                                                                                         "\\.",
                                                                                         ".*\\.jp2",
                                                                                         ".md5"));


        final TreeNodeState nodeState = new TreeNodeState();


        eventRunner.runEvents(Arrays.<TreeEventHandler>asList(
                                            new TreeEventHandler() {

                                                private String printEvent(ParsingEvent event,
                                                                          String type)
                                                        throws
                                                        IOException {

                                                    switch (event.getType()) {
                                                        case NodeBegin:
                                                            return "<" + type + " name=\"" + event.getName() + "\">";
                                                        case NodeEnd:
                                                            return "</" + type + ">";
                                                        case Attribute:
                                                            if (event instanceof AttributeParsingEvent) {
                                                                AttributeParsingEvent attributeParsingEvent =
                                                                        (AttributeParsingEvent) event;
                                                                return "<" + "attribute" + " name=\"" + event.getName()
                                                                       + "\" checksum=\"" + attributeParsingEvent
                                                                        .getChecksum() + "\" />";
                                                            }

                                                        default:
                                                            return event.toString();
                                                    }
                                                }

                                                @Override
                                                public void handleNodeBegin(NodeBeginsParsingEvent event) {
                                                    try {
                                                        nodeState.handleNodeBegin(event);
                                                        TreeNode currentNode = nodeState.getCurrentNode();
                                                        System.out
                                                              .println(printEvent(event, currentNode.getType().name()));
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }

                                                @Override
                                                public void handleNodeEnd(NodeEndParsingEvent event) {
                                                    try {
                                                        TreeNode currentNode = nodeState.getCurrentNode();
                                                        nodeState.handleNodeEnd(event);
                                                        System.out
                                                              .println(printEvent(event, currentNode.getType().name()));
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }

                                                @Override
                                                public void handleAttribute(AttributeParsingEvent event) {
                                                    try {

                                                        System.out.println(printEvent(event, "attribute"));
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }

                                                @Override
                                                public void handleFinish() {
                                                    //To change body of implemented methods use
                                                    // File | Settings | File Templates.
                                                }
                                            }

                                           ), null);


    }
}
