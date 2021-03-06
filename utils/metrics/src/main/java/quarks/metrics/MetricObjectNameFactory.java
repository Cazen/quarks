/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015, 2016 
*/
package quarks.metrics;

import java.util.Hashtable;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.ObjectNameFactory;

import quarks.oplet.OpletContext;

/**
 * A factory of metric {@code ObjectName} instances. 
 * <p>
 * The implementation relies on unique metric names generated by
 * {@link OpletContext#uniquify(String)} to
 * successfully parse the job and oplet id.
 */
public class MetricObjectNameFactory implements ObjectNameFactory {
    /** Prefix of all metric types. */
    public static final String TYPE_PREFIX = "metric";
    /** The {@code name} property key. */  
    public static final String KEY_NAME = "name";
    /** The {@code type} property key. */  
    public static final String KEY_TYPE = "type";
    /** The {@code jobId} property key. */  
    public static final String KEY_JOBID = "jobId";
    /** The {@code opId} (oplet id) property key. */  
    public static final String KEY_OPID = "opId";

    /*
     * This implementation avoids creating a dependency on the embedded 
     * runtime package by declaring local strings for the expected job and 
     * oplet id prefixes parsed from the metric name, which must be equal 
     * to those declared by the embedded runtime (see EtiaoJob.ID_PREFIX and 
     * Invocation.ID_PREFIX).
     */
    /** The prefix of the job id as serialized in the metric name. */    
    public static final String PREFIX_JOBID = "JOB_"; // Must be equal to EtiaoJob.PREFIX_ID
    /** The prefix of the oplet id as serialized in the metric name. */
    public static final String PREFIX_OPID = "OP_";  // Must be equal to Invocation.PREFIX_ID

    /**
     * Creates a JMX {@code ObjectName} from the given domain, metric type, 
     * and metric name.
     * <p>
     * If the metric name is an ObjectName pattern, or has a format which does
     * not correspond to a valid ObjectName, this implementation attempts to 
     * create an ObjectName using the quoted metric name instead. 
     * 
     * @param type the value of the "type" key property in the object name, 
     *      which represents the type of metric.
     * @param domain the domain part of the object name.
     * @param name the value of the "name" key property in the object name,
     *      which represents the metric name.
     * @throws RuntimeException wrapping a MalformedObjectNameException if 
     *      the implementation cannot create a valid object name.
     */
    @Override
    public ObjectName createName(String type, String domain, String name) {
        Hashtable<String,String> table = new Hashtable<String,String>();
        table.put(KEY_TYPE, TYPE_PREFIX + "." + type);
        table.put(KEY_NAME, name);
        addKeyProperties(name, table);
        try {
            ObjectName objectName = new ObjectName(domain, table);
            if (objectName.isPattern()) {
                table.put(KEY_NAME, ObjectName.quote(name));
                objectName = new ObjectName(domain, table);
            }
            return objectName;
        } catch (MalformedObjectNameException e) {
            try {
                table.put(KEY_NAME, ObjectName.quote(name));
                return new ObjectName(domain, table);
            } catch (MalformedObjectNameException e1) {
                // TODO slf4j logger.warn("Unable to register {} {} {}", type, domain, name, e1);
                throw new RuntimeException(e1);
            }
        }
    }

    /**
     * Extracts job and oplet identifier values from the specified buffer and 
     * adds the {@link #KEY_JOBID} and {@link #KEY_OPID} key properties to the 
     * specified properties map.  
     * <p>
     * Assumes that the job and oplet identifiers are concatenated (possibly with 
     * other strings as well) using '.' as a separator.
     * 
     * @param buf contains serialized job and oplet identifiers separated
     * @param properties key property map
     */
    protected void addKeyProperties(String buf, Map<String,String> properties) {
        addKeyProperty(buf, PREFIX_JOBID, KEY_JOBID, properties);
        addKeyProperty(buf, PREFIX_OPID, KEY_OPID, properties);
    }

    private void addKeyProperty(String buf, String prefix, String key, Map<String,String> properties) {
        String value = tokenStartingWith(buf, prefix);
        if (value != null) {
            properties.put(key, value);
        }
    }

    private static String tokenStartingWith(String buf, String prefix) {
        int start = buf.indexOf("." + prefix);
        String value = null;
        if (start != -1) {
            int end = buf.indexOf('.', start+1);
            if (end == -1) 
                end = buf.length();
            value = buf.substring(start+1, end);
        }
        return value;
    }
}
