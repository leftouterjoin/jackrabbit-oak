package forstudy;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.oak.api.Blob;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.commons.json.JsopBuilder;
import org.apache.jackrabbit.oak.json.BlobSerializer;
import org.apache.jackrabbit.oak.json.JsonSerializer;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeBuilder;
import org.apache.jackrabbit.oak.plugins.segment.RecordId;
import org.apache.jackrabbit.oak.plugins.segment.SegmentBlob;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeBuilder;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeState;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;

import forstudy.ContextRunner.TestContext;

public class TestHelpers {
	@Test
	public void test_createFileStore() {
		createFileStore();
	}

	private static final FileStore createFileStore(String className,
			String methodName) {
		try {
			String callerName = className + "." + methodName;
			File f = new File("./FS/" + callerName);
			System.out.format("create dir ... %s¥n", f.getPath());
			FileUtils.forceMkdir(f);
			FileUtils.cleanDirectory(f);
			return new FileStore(f, 1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static final FileStore createFileStore() {
		TestContext tc = TestContext.getContext();
		if (tc != null) {
			return createFileStore(tc.getTestMethod().getDeclaringClass()
					.getName(), tc.getTestMethod().getName());
		}
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		return createFileStore(ste.getClassName(), ste.getMethodName());
	}

	public static NodeState getNodeStateFromTree(Tree t) {
		try {
			NodeBuilder nb = getNodeBuilderFromTree(t);
			Field f = Class.forName("org.apache.jackrabbit.oak.core.SecureNodeBuilder").getDeclaredField("builder");
			f.setAccessible(true);

			MemoryNodeBuilder mnb = (MemoryNodeBuilder) f.get(nb);
			f = MemoryNodeBuilder.class.getDeclaredField("base");
			f.setAccessible(true);
			return (NodeState) f.get(mnb);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static NodeBuilder getNodeBuilderFromTree(Tree t) {
		try {
			Field f = Class.forName(
					"org.apache.jackrabbit.oak.core.MutableTree")
					.getDeclaredField("nodeBuilder");
			f.setAccessible(true);
			return (NodeBuilder) f.get(t);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String convertJsonFromTree(Tree tree) {
		return convertJsonFromNodeState(TestHelpers.getNodeStateFromTree(tree));
	}

	public static String convertJsonFromNodeState(NodeState ns) {
//		String json = JsopBuilder.prettyPrint(JsopDiff.diffToJsop(ns, EmptyNodeState.EMPTY_NODE));
        JsopBuilder builder = new JsopBuilder();
        new JsonSerializer(builder, new BlobSerializer()).serialize(ns);
		return JsopBuilder.prettyPrint(builder.toString());
	}

	public static long calcLucneIndexBlobSize(Tree tree) {
		long l = 0;
		NodeBuilder testNode = getNodeBuilderFromTree(tree);
		for (String s : testNode.getChildNodeNames()) {
			NodeBuilder dataNode = testNode.getChildNode(s);
			l += dataNode.getProperty("blobSize").getValue(Type.LONG);
		}
		return l;
	}

	public static Long[] exploreSize(DB db, String path) {
		Long[] s = { 0l, 0l };
		try {
			String script = IOUtils.toString(new FileReader(
					"../oak-run/src/main/js/oak-mongo.js"));
			CommandResult result = db.doEval(script
					+ ";\n return oak.getChildStats('" + path + "');");
			s[0] = ((BasicDBObject) result.get("retval")).getLong("size");
			s[1] = ((BasicDBObject) result.get("retval")).getLong("count");
			return s;
		} catch (Throwable th) {
			throw new RuntimeException(th);
		}
	}

	public static Long[] exploreSize(SegmentNodeState ns) {
		return exploreSize(ns, new HashMap<RecordIdKey, Long[]>());
	}

	public static Long[] exploreSize(SegmentNodeState ns,
			Map<RecordIdKey, Long[]> sizeCache) {
		RecordIdKey key = new RecordIdKey(ns.getRecordId());
		if (sizeCache.containsKey(key)) {
			return sizeCache.get(key);
		}
		Long[] s = { 0l, 0l, 0l, 0l };

		List<String> names = newArrayList(ns.getChildNodeNames());

		if (names.contains("root")) {
			List<String> temp = newArrayList();
			int poz = 0;
			// push 'root' to the beginning
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext()) {
				String n = iterator.next();
				if (n.equals("root")) {
					temp.add(poz, n);
					poz++;
				} else {
					temp.add(n);
				}
			}
			names = temp;
		}

		for (String n : names) {
			SegmentNodeState k = (SegmentNodeState) ns.getChildNode(n);
			RecordIdKey ckey = new RecordIdKey(k.getRecordId());
			if (sizeCache.containsKey(ckey)) {
				// already been here, record size under 'link'
				Long[] ks = sizeCache.get(ckey);
				s[1] = s[1] + ks[0] + ks[1];
				s[3] = s[3] + ks[0] + ks[3];
			} else {
				Long[] ks = exploreSize(k, sizeCache);
				s[0] = s[0] + ks[0];
				s[1] = s[1] + ks[1];
				s[2] = s[2] + ks[2];
				s[3] = s[3] + ks[3];
			}
		}
		for (PropertyState ps : ns.getProperties()) {
			for (int j = 0; j < ps.count(); j++) {
				if (ps.getType().tag() == Type.BINARY.tag()) {
					Blob b = ps.getValue(Type.BINARY, j);
					boolean skip = b instanceof SegmentBlob
							&& ((SegmentBlob) b).isExternal();
					if (!skip) {
						s[0] = s[0] + b.length();
					}
				} else {
					s[0] = s[0] + ps.size(j);
				}
				s[2]++;
			}
		}
		sizeCache.put(key, s);
		return s;
	}

	public static Long[] exploreSizeOrg(SegmentNodeState ns,
			Map<RecordIdKey, Long[]> sizeCache) {
		RecordIdKey key = new RecordIdKey(ns.getRecordId());
		if (sizeCache.containsKey(key)) {
			return sizeCache.get(key);
		}
		Long[] s = { 0l, 0l };

		List<String> names = newArrayList(ns.getChildNodeNames());

		if (names.contains("root")) {
			List<String> temp = newArrayList();
			int poz = 0;
			// push 'root' to the beginning
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext()) {
				String n = iterator.next();
				if (n.equals("root")) {
					temp.add(poz, n);
					poz++;
				} else {
					temp.add(n);
				}
			}
			names = temp;
		}

		for (String n : names) {
			SegmentNodeState k = (SegmentNodeState) ns.getChildNode(n);
			RecordIdKey ckey = new RecordIdKey(k.getRecordId());
			if (sizeCache.containsKey(ckey)) {
				// already been here, record size under 'link'
				Long[] ks = sizeCache.get(ckey);
				s[1] = s[1] + ks[0] + ks[1];
			} else {
				Long[] ks = exploreSize(k, sizeCache);
				s[0] = s[0] + ks[0];
				s[1] = s[1] + ks[1];
			}
		}
		for (PropertyState ps : ns.getProperties()) {
			for (int j = 0; j < ps.count(); j++) {
				if (ps.getType().tag() == Type.BINARY.tag()) {
					Blob b = ps.getValue(Type.BINARY, j);
					boolean skip = b instanceof SegmentBlob
							&& ((SegmentBlob) b).isExternal();
					if (!skip) {
						s[0] = s[0] + b.length();
					}
				} else {
					s[0] = s[0] + ps.size(j);
				}
			}
		}
		sizeCache.put(key, s);
		return s;
	}

	private static class RecordIdKey {

		private final long msb;
		private final long lsb;
		private final int offset;

		public RecordIdKey(RecordId rid) {
			this.offset = rid.getOffset();
			this.msb = rid.getSegmentId().getMostSignificantBits();
			this.lsb = rid.getSegmentId().getLeastSignificantBits();
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			} else if (object instanceof RecordIdKey) {
				RecordIdKey that = (RecordIdKey) object;
				return offset == that.offset && msb == that.msb
						&& lsb == that.lsb;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return ((int) lsb) ^ offset;
		}
	}
}
