package org.apache.jackrabbit.oak.plugins.document.gc;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.jackrabbit.core.data.FileDataStore;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.osgi.ObserverTracker;
import org.apache.jackrabbit.oak.plugins.blob.BlobGC;
import org.apache.jackrabbit.oak.plugins.blob.BlobGCMBean;
import org.apache.jackrabbit.oak.plugins.blob.BlobGarbageCollector;
import org.apache.jackrabbit.oak.plugins.blob.datastore.DataStoreBlobStore;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStoreService;
import org.apache.jackrabbit.oak.plugins.document.util.MongoConnection;
import org.apache.jackrabbit.oak.plugins.identifier.ClusterRepositoryInfo;
import org.apache.jackrabbit.oak.spi.blob.BlobStore;
import org.apache.jackrabbit.oak.spi.blob.GarbageCollectableBlobStore;
import org.apache.jackrabbit.oak.spi.commit.Observable;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.spi.state.RevisionGC;
import org.apache.jackrabbit.oak.spi.state.RevisionGCMBean;
import org.apache.jackrabbit.oak.spi.whiteboard.DefaultWhiteboard;
import org.apache.jackrabbit.oak.spi.whiteboard.Registration;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.jackrabbit.oak.spi.whiteboard.WhiteboardExecutor;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closer;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

public class Test1 {
	private static DocumentNodeStoreService このクラスの一部をコピー;

	private static final long MB = 1024 * 1024;

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
				+ Test1.class.getSimpleName(), "blob"));

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

		Oak oak;
		{
			MBeanServer mbeanServer = ManagementFactory
					.getPlatformMBeanServer();
			oak = new Oak(mk.getNodeStore()).with(mbeanServer);
		}

		DocumentNodeStore mns = mk.getNodeStore();
		whiteboard = createWhiteboad(mns);
		DocumentNodeStore ds = mk.getNodeStore();
		ClusterRepositoryInfo.createId(ds);
		registerJMXBeans(ds);

		System.out.println("hoge");
	}

	private BlobStore getBlobStore(String baseDir, String storeName) {
		FileDataStore fileDS = new FileDataStore();
		fileDS.setMinRecordLength(4092);
		File storeDir = new File(baseDir, storeName);
		fileDS.init(storeDir.getAbsolutePath());
		BlobStore bs = new DataStoreBlobStore(fileDS, true, 100);
		return bs;
	}

	private void registerJMXBeans(final DocumentNodeStore store) {
		executor = new WhiteboardExecutor();
		executor.start(whiteboard);

		if (store.getBlobStore() instanceof GarbageCollectableBlobStore) {
			BlobGarbageCollector gc = new BlobGarbageCollector() {
				@Override
				public void collectGarbage(boolean sweep) throws Exception {
					store.createBlobGarbageCollector(blobGcMaxAgeInSecs,
							ClusterRepositoryInfo.getId(mk.getNodeStore()))
							.collectGarbage(sweep);
				}
			};
			registrations.add(registerMBean(whiteboard, BlobGCMBean.class,
					new BlobGC(gc, executor), BlobGCMBean.TYPE,
					"Document node store blob garbage collection"));
		}

		RevisionGC revisionGC = new RevisionGC(new Runnable() {
			@Override
			public void run() {
				store.getVersionGarbageCollector().gc(versionGcMaxAgeInSecs,
						TimeUnit.SECONDS);
			}
		}, executor);
		registrations.add(registerMBean(whiteboard, RevisionGCMBean.class,
				revisionGC, RevisionGCMBean.TYPE,
				"Document node store revision garbage collection"));
	}

	private static final Logger LOG = LoggerFactory.getLogger(Test1.class);

	private static final long DEFAULT_BLOB_GC_MAX_AGE = TimeUnit.HOURS
			.toSeconds(24);
	public static final String PROP_BLOB_GC_MAX_AGE = "blobGcMaxAgeInSecs";
	private long blobGcMaxAgeInSecs = DEFAULT_BLOB_GC_MAX_AGE;

	private static final long DEFAULT_VER_GC_MAX_AGE = TimeUnit.DAYS
			.toSeconds(1);
	public static final String PROP_VER_GC_MAX_AGE = "versionGcMaxAgeInSecs";
	private long versionGcMaxAgeInSecs = DEFAULT_VER_GC_MAX_AGE;

	private final List<Registration> registrations = new ArrayList<Registration>();
	private DocumentMK mk;
	private WhiteboardExecutor executor;

	private static final AtomicLong COUNTER = new AtomicLong();

	public static <T> Registration registerMBean(Whiteboard whiteboard,
			Class<T> iface, T bean, String type, String name) {
		try {
			Hashtable<String, String> table = new Hashtable<String, String>();
			table.put("type", ObjectName.quote(type));
			table.put("name", ObjectName.quote(name));
			table.put("id", String.valueOf(COUNTER.incrementAndGet()));
			return whiteboard.register(iface, bean, ImmutableMap.of(
					"jmx.objectname", new ObjectName(
							"org.apache.jackrabbit.oak", table)));
		} catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private MBeanServer mbeanServer = ManagementFactory
			.getPlatformMBeanServer();

	private Whiteboard whiteboard;

	private Whiteboard createWhiteboad(final NodeStore store) {
		return new DefaultWhiteboard() {
			@Override
			public <T> Registration register(Class<T> type, T service,
					Map<?, ?> properties) {
				final Registration registration = super.register(type, service,
						properties);

				final Closer observerSubscription = Closer.create();
				Future<?> future = null;
				if (type == Runnable.class) {
					Runnable runnable = (Runnable) service;
					Long period = getValue(properties, "scheduler.period",
							Long.class);
					if (period != null) {
						Boolean concurrent = getValue(properties,
								"scheduler.concurrent", Boolean.class,
								Boolean.FALSE);
						if (concurrent) {
							future = getScheduledExecutor()
									.scheduleAtFixedRate(runnable, period,
											period, TimeUnit.SECONDS);
						} else {
							future = getScheduledExecutor()
									.scheduleWithFixedDelay(runnable, period,
											period, TimeUnit.SECONDS);
						}
					}
				} else if (type == Observer.class
						&& store instanceof Observable) {
					observerSubscription.register(((Observable) store)
							.addObserver((Observer) service));
				}

				ObjectName objectName = null;
				Object name = properties.get("jmx.objectname");
				if (mbeanServer != null && name != null) {
					try {
						if (name instanceof ObjectName) {
							objectName = (ObjectName) name;
						} else {
							objectName = new ObjectName(String.valueOf(name));
						}
						mbeanServer.registerMBean(service, objectName);
					} catch (JMException e) {
						// ignore
					}
				}

				final Future<?> f = future;
				final ObjectName on = objectName;
				return new Registration() {
					@Override
					public void unregister() {
						if (f != null) {
							f.cancel(false);
						}
						if (on != null) {
							try {
								mbeanServer.unregisterMBean(on);
							} catch (JMException e) {
								// ignore
							}
						}
						try {
							observerSubscription.close();
						} catch (IOException e) {
							LOG.warn(
									"Unexpected IOException while unsubscribing observer",
									e);
						}

						registration.unregister();
					}
				};
			}
		};
	}

	private static <T> T getValue(Map<?, ?> properties, String name,
			Class<T> type) {
		return getValue(properties, name, type, null);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getValue(Map<?, ?> properties, String name,
			Class<T> type, T def) {
		Object value = properties.get(name);
		if (type.isInstance(value)) {
			return (T) value;
		} else {
			return def;
		}
	}

	private ScheduledExecutorService scheduledExecutor;

	private synchronized ScheduledExecutorService getScheduledExecutor() {
		if (scheduledExecutor == null) {
			scheduledExecutor = defaultScheduledExecutor();
		}
		return scheduledExecutor;
	}

	public static ScheduledExecutorService defaultScheduledExecutor() {
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
				32, new ThreadFactory() {
					private final AtomicInteger counter = new AtomicInteger();

					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r, createName());
						thread.setDaemon(true);
						return thread;
					}

					private String createName() {
						return "oak-scheduled-executor-"
								+ counter.getAndIncrement();
					}
				});
		executor.setKeepAliveTime(1, TimeUnit.MINUTES);
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}
}
