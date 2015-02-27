package org.apache.jackrabbit.oak.plugins.document.gc;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import javax.jcr.Repository;
import javax.management.AttributeList;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.StringValueExp;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.jackrabbit.core.data.FileDataStore;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.blob.datastore.DataStoreBlobStore;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.util.MongoConnection;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.spi.blob.BlobStore;
import org.apache.jackrabbit.oak.spi.whiteboard.WhiteboardExecutor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

public class Test2 {
	private static final long MB = 1024 * 1024;

	@Test
	public void test0() throws Throwable {
		/*
		 * -Dcom.sun.management.jmxremote.port=8999
		 * -Dcom.sun.management.jmxremote.ssl=false
		 * -Dcom.sun.management.jmxremote.authenticate=false
		 */
		String hostname = "localhost:8999";
		String serviceUrl = String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", hostname);
		JMXConnector conn = JMXConnectorFactory.connect(new JMXServiceURL(serviceUrl));
		Object o = conn.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
		CompositeData data = (CompositeData) o;
		Long used = (Long) data.get("used");
		System.out.println(used/1024);
	}

	@Test
	public void test1() throws Throwable {
		registerNodeStore();
	}

	private void registerNodeStore() throws Throwable {
		String uri = "mongodb://localhost:27017/oak";
		String db = "oak";

		int offHeapCache = 0;
		int cacheSize = 256;
		int changesSize = 256;
		int blobCacheSize = 16;

		DocumentMK.Builder mkBuilder = new DocumentMK.Builder()
				.memoryCacheSize(cacheSize * MB).offHeapCacheSize(
						offHeapCache * MB);

		mkBuilder.setBlobStore(getBlobStore("bin/oak_fileDataStore/"
				+ Test2.class.getSimpleName(), "blob"));

		MongoClientOptions.Builder builder = MongoConnection
				.getDefaultBuilder();
		MongoClientURI mongoURI = new MongoClientURI(uri, builder);
		MongoClient client = new MongoClient(mongoURI);
		DB mongoDB = client.getDB(db);

		mkBuilder.setMaxReplicationLag(TimeUnit.HOURS.toSeconds(6),
				TimeUnit.SECONDS);
		mkBuilder.setMongoDB(mongoDB, changesSize, blobCacheSize);

		mkBuilder.setExecutor(executor);
		mk = mkBuilder.open();

		Oak oak = new Oak(mk.getNodeStore()).with(mbeanServer);

		Jcr jcr = new Jcr(oak);
		Repository repository = jcr.createRepository();

		DocumentNodeStore mns = mk.getNodeStore();

		test14();

		System.out.println("hoge");
	}

	private void test14() throws Throwable {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectInstance> instances = mbs.queryMBeans(null,null);

		Stack<ObjectInstance> s = new Stack<ObjectInstance>();
		s.addAll(instances);

		while (!s.isEmpty()) {
			ObjectInstance current = s.pop();
			System.out.println("Class Name:\t" + current.getClassName());
			Object o = mbs.getAttribute(current.getObjectName(), null);

//			Set<ObjectInstance> chileds = mbs.queryMBeans(current.getObjectName(), null);
//			s.addAll(chileds);
		}
	}

	private void test13() throws Throwable {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectInstance> instances = mbs.queryMBeans(null,
				Query.isInstanceOf(new StringValueExp(QueryEngineSettings.class.getName())));

		Iterator<ObjectInstance> iterator = instances.iterator();
		while (iterator.hasNext()) {
			ObjectInstance instance = iterator.next();
			System.out.println("MBean Found:");
			System.out.println("Class Name:\t" + instance.getClassName());
			System.out.println("Object Name:\t" + instance.getObjectName());
			System.out.println("****************************************");
		}
	}

	private void test12() throws Throwable {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Object o = mbs.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
		CompositeData data = (CompositeData) o;
		System.out.println(data);
	}

	private void test11() throws Throwable {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

		Set<ObjectInstance> instances = mbs.queryMBeans(null,null);

		Iterator<ObjectInstance> iterator = instances.iterator();
		while (iterator.hasNext()) {
			ObjectInstance instance = iterator.next();
			System.out.println("MBean Found:");
			System.out.println("Class Name:\t" + instance.getClassName());
			System.out.println("Object Name:\t" + instance.getObjectName());

			Set<ObjectInstance> i2 = mbs.queryMBeans(instance.getObjectName(), null);
			Iterator<ObjectInstance> it2 = i2.iterator();
			while (it2.hasNext()) {
				ObjectInstance ins2 = it2.next();
				System.out.println(ins2);
			}
//			AttributeList al = mbs.getAttributes(instance.getObjectName(), null);
//			for (Object o : al) {
//				System.out.println(o);
//			}


			System.out.println("****************************************");
		}
	}

	private void test10() throws Throwable {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName(
				"com.example.mxbeans:type=QueueSampler");
		RepositoryManagementMBean proxy = JMX.newMXBeanProxy(mbs, name,
				RepositoryManagementMBean.class);
		CompositeData o = proxy.startDataStoreGC(false);
		System.out.println(o);
	}

	private BlobStore getBlobStore(String baseDir, String storeName) {
		FileDataStore fileDS = new FileDataStore();
		fileDS.setMinRecordLength(4092);
		File storeDir = new File(baseDir, storeName);
		fileDS.init(storeDir.getAbsolutePath());
		BlobStore bs = new DataStoreBlobStore(fileDS, true, 100);
		return bs;
	}

	private static final Logger LOG = LoggerFactory.getLogger(Test2.class);

	public static final String PROP_BLOB_GC_MAX_AGE = "blobGcMaxAgeInSecs";

	public static final String PROP_VER_GC_MAX_AGE = "versionGcMaxAgeInSecs";

	private DocumentMK mk;
	private WhiteboardExecutor executor;

	private MBeanServer mbeanServer = ManagementFactory
			.getPlatformMBeanServer();

	static abstract class Finder {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		abstract void found(ObjectInstance instance);
		public void traverse(Set<ObjectInstance> instances) {
			Stack<ObjectInstance> processeeStack = new Stack<ObjectInstance>();
			processeeStack.addAll(instances);
			while (!processeeStack.isEmpty()) {
				ObjectInstance current = processeeStack.pop();
				found(current);
				Set<ObjectInstance> chileds = mbs.queryMBeans(current.getObjectName(), null);
				processeeStack.addAll(chileds);
			}
		}
	}
}
