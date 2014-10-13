package dk.statsbiblioteket.medieplatform.hadoop;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

/**
 * Generic hadoop reduce job which has access to a DOMS instance.
 */
@SuppressWarnings("deprecation")//Credentials
public abstract class AbstractDomsReducer extends Reducer<Text, Text, Text, Text>  {
    public static final String HADOOP_SAVER_DATASTREAM = "hadoop.saver.doms.datastream";

    private static Logger log = Logger.getLogger(AbstractDomsReducer.class);
    protected EnhancedFedora fedora;
    protected String batchID = null;
    protected String datastreamName;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        fedora = createFedoraClient(context);
        batchID = context.getConfiguration().get(ConfigConstants.BATCH_ID);
        datastreamName = context.getConfiguration().get(HADOOP_SAVER_DATASTREAM);
    }

    /**
     * Get the fedora client
     *
     * @param context the hadoop context
     *
     * @return the fedora client
     * @throws java.io.IOException
     */
    @SuppressWarnings("deprecation")//Credentials
    protected EnhancedFedora createFedoraClient(Context context) throws IOException {
        try {
            Configuration conf = context.getConfiguration();

            String username = conf.get(ConfigConstants.DOMS_USERNAME);
            String password = conf.get(ConfigConstants.DOMS_PASSWORD);
            String domsUrl = conf.get(ConfigConstants.DOMS_URL);
            return new EnhancedFedoraImpl(
                    new Credentials(username, password), domsUrl, null, null);
        } catch (JAXBException e) {
            throw new IOException(e);
        } catch (PIDGeneratorException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reduce method which can access DOMS via the EnhancedFedora interface.
     *
     * @param key     the input filename
     * @param values  the corresponding values generated by the final mapper
     * @param context the task context
     *
     * @throws java.io.IOException  Any checked exception that is not an InterruptedException
     * @throws InterruptedException from Hadoop
     */
    @Override
    protected abstract void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException;

    /**
     * Get the doms pid from the filename
     *
     * @param key the filename
     *
     * @return the doms pid
     */
    protected String getDomsPid(Text key) throws BackendInvalidCredsException, BackendMethodFailedException {

        String filePath = translate(key.toString());
        String path = "path:" + filePath;
        List<String> hits = fedora.findObjectFromDCIdentifier(path);
        if (hits.isEmpty()) {

            throw new RuntimeException("Failed to look up doms object for DC identifier '" + path + "'");
        } else {
            if (hits.size() > 1) {
                log.warn("Found multipe pids for dc identifier '" + path + "', using the first one '" + hits.get(0) + "'");
            }
            return hits.get(0);
        }

    }

    /**
     * Translate the filename back to the original path as stored in doms
     *
     * @param file the filename
     *
     * @return the original path
     */
    protected String translate(String file) {
        return file.substring(file.indexOf(batchID)).replaceAll("_", "/");
    }



}
