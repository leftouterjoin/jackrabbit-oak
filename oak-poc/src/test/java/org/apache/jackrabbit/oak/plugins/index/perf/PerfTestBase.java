package org.apache.jackrabbit.oak.plugins.index.perf;

import static org.apache.jackrabbit.oak.api.QueryEngine.NO_BINDINGS;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NODE_TYPE;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.REINDEX_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.TYPE_PROPERTY_NAME;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.Result;
import org.apache.jackrabbit.oak.api.ResultRow;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.IndexUtils;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants;
import org.apache.jackrabbit.oak.plugins.index.lucene.TestUtil;
import org.apache.jackrabbit.oak.plugins.index.perfdata.BlobStats;
import org.apache.jackrabbit.oak.plugins.index.perfdata.LoadStats;
import org.apache.jackrabbit.oak.plugins.index.perfdata.NodeStats;
import org.apache.jackrabbit.oak.plugins.index.perfdata.QueryStats;
import org.apache.jackrabbit.oak.plugins.index.property.OrderedIndex;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeState;
import org.apache.jackrabbit.oak.query.AbstractQueryTest;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.junit.After;
import org.slf4j.Logger;

import com.google.common.base.Stopwatch;
import com.mongodb.DB;
import com.orangesignal.csv.manager.CsvEntityManager;

import forstudy.TestHelpers;

public abstract class PerfTestBase extends AbstractQueryTest {
	private Logger L = getLogger();
	private Stopwatch stopwatch = Stopwatch.createStarted();

	static final int COUNT = 100;
	static final int PVAL_BYTES = 10;

	static final int CARD = COUNT / 100;

	static final String PATH_A = "PATH_A";
	static final String PATH_B = "PATH_B";
	static final String PNAME_A = "PROP_A";
	static final String PNAME_B = "PROP_B";

	static final String NODENAME_FORMAT = "n%09d";
	static final String PVAL_FORMAT = "v%0" + (PVAL_BYTES - 1) + "d";

	private List<LoadStats> loadStats = new ArrayList<LoadStats>();
	private List<NodeStats> nodeStats = new ArrayList<NodeStats>();
	private List<QueryStats> queryStats = new ArrayList<QueryStats>();
	private List<BlobStats> blobStats = new ArrayList<BlobStats>();
	private LoadStats l;
	private NodeStats n;
	private QueryStats q;
	private BlobStats b;

	private void newLoadStats() {
		l = new LoadStats();
		loadStats.add(l);
		l.pvalBytes = PVAL_BYTES;
		l.loadCount = COUNT;
		l.indexType = getIndexTypeName();
		l.storeType = getStoreTypeName();
	}

	private void newNodeStats() {
		n = new NodeStats();
		nodeStats.add(n);
		n.pvalBytes = PVAL_BYTES;
		n.loadCount = COUNT;
		n.indexType = getIndexTypeName();
		n.storeType = getStoreTypeName();
	}

	private void newQueryStats() {
		q = new QueryStats();
		queryStats.add(q);
		q.pvalBytes = PVAL_BYTES;
		q.loadCount = COUNT;
		q.indexType = getIndexTypeName();
		q.storeType = getStoreTypeName();
	}

	private void newBlobStats() {
		b = new BlobStats();
		blobStats.add(b);
		b.pvalBytes = PVAL_BYTES;
		b.loadCount = COUNT;
		b.indexType = getIndexTypeName();
		b.storeType = getStoreTypeName();
	}

	@After
	public void saveCsv() throws Throwable {
		String savePath = "target/";
		String fileName = getStoreTypeName() + "_" + getIndexTypeName();
		new CsvEntityManager().save(loadStats, LoadStats.class).to(
				new File(savePath + "loadStats_" + fileName + "_" + PVAL_BYTES + "B_x_" + COUNT + ".csv"));
		new CsvEntityManager().save(nodeStats, NodeStats.class).to(
				new File(savePath + "nodeStats_" + fileName + "_" + PVAL_BYTES + "B_x_" + COUNT + ".csv"));
		new CsvEntityManager().save(queryStats, QueryStats.class).to(
				new File(savePath + "queryStats_" + fileName + "_" + PVAL_BYTES + "B_x_" + COUNT + ".csv"));
		new CsvEntityManager().save(blobStats, BlobStats.class).to(
				new File(savePath + "blobStats_" + fileName + "_" + PVAL_BYTES + "B_x_" + COUNT + ".csv"));
	}

	@Override
	protected ContentRepository createRepository() {
		return null;
	}

	protected abstract String getIndexTypeName();
	protected abstract String getStoreTypeName();
	protected abstract Logger getLogger();

	protected void createPropertyIndex() throws Exception {
		Tree index = root.getTree("/");
		IndexUtils.createIndexDefinition(
				new NodeUtil(index.getChild(INDEX_DEFINITIONS_NAME)), PNAME_A,
				false, new String[] { PNAME_A }, null, "property");
		IndexUtils.createIndexDefinition(
				new NodeUtil(index.getChild(INDEX_DEFINITIONS_NAME)), PNAME_B,
				false, new String[] { PNAME_B }, null, "property");
		root.commit();
		showIndexDef("/" + INDEX_DEFINITIONS_NAME+ "/" + PNAME_A);
		showIndexDef("/" + INDEX_DEFINITIONS_NAME+ "/" + PNAME_B);
	}

	protected void createOrderedIndex() throws Exception {
		Tree index = root.getTree("/");
		IndexUtils.createIndexDefinition(
				new NodeUtil(index.getChild(INDEX_DEFINITIONS_NAME)), PNAME_A,
				false, new String[] { PNAME_A }, null, OrderedIndex.TYPE);
		IndexUtils.createIndexDefinition(
				new NodeUtil(index.getChild(INDEX_DEFINITIONS_NAME)), PNAME_B,
				false, new String[] { PNAME_B }, null, OrderedIndex.TYPE);
		root.commit();
		showIndexDef("/" + INDEX_DEFINITIONS_NAME+ "/" + PNAME_A);
		showIndexDef("/" + INDEX_DEFINITIONS_NAME+ "/" + PNAME_B);
	}

	protected void createLuceneIndex() throws Exception {
		Tree index = root.getTree("/");
		createLuceneIndexNode(index, PNAME_A);
		createLuceneIndexNode(index, PNAME_B);
	}

	// org.apache.jackrabbit.oak.plugins.index.lucene.Copy_LuceneIndexQueryTest.createTestIndexNode()などを参考。。
	protected void createLuceneIndexNode(Tree index, String pname)
			throws Exception {
		Tree indexDef = index.addChild(INDEX_DEFINITIONS_NAME).addChild(pname);
		indexDef.setProperty(JcrConstants.JCR_PRIMARYTYPE,
				INDEX_DEFINITIONS_NODE_TYPE, Type.NAME);
		indexDef.setProperty(TYPE_PROPERTY_NAME,
				LuceneIndexConstants.TYPE_LUCENE);
		indexDef.setProperty(REINDEX_PROPERTY_NAME, true);

		TestUtil.useV2(indexDef);
		// indexDef.setProperty(LuceneIndexConstants.TEST_MODE, false);
		indexDef.setProperty(LuceneIndexConstants.EVALUATE_PATH_RESTRICTION,
				true);

		Tree props = TestUtil.newRulePropTree(indexDef, "nt:base");
		enableForFullText(props, pname);
		// TestUtil.enableForFullText(props,
		// LuceneIndexConstants.REGEX_ALL_PROPS, true);
		root.commit();
		showIndexDef(indexDef);
	}

	private void showIndexDef(Tree indexDef) {
		showIndexDef(indexDef.getPath());
	}

	private void showIndexDef(String path) {
        L.info("■create index...");
		L.info(path);
		L.info("\n" + TestHelpers.convertJsonFromTree(root.getTree(path)));
	}

	protected void enableForFullText(Tree props, String propName) {
		Tree prop = props.addChild(propName);
		prop.setProperty(LuceneIndexConstants.PROP_NAME, propName);
		prop.setProperty(LuceneIndexConstants.PROP_PROPERTY_INDEX, true);
		// prop.setProperty(LuceneIndexConstants.PROP_IS_REGEX, regex);
		prop.setProperty(LuceneIndexConstants.PROP_NODE_SCOPE_INDEX, true);
		prop.setProperty(LuceneIndexConstants.PROP_ANALYZED, true);
		prop.setProperty(LuceneIndexConstants.PROP_ORDERED, true);
		prop.setProperty(LuceneIndexConstants.PROP_USE_IN_EXCERPT, true);
	}

	protected void createData() {
		Tree t = root.getTree("/");
		L.info("■create data...");
		L.info(String.format("\tcount:\t%d", COUNT));
		L.info(String.format("\tPVAL_FORMAT:\t%s", PVAL_FORMAT));
		stopwatch.reset().start();
		for (int i = 0; i < COUNT; i++) {
			t.addChild(PATH_A).addChild(String.format(NODENAME_FORMAT, i))
					.setProperty(PNAME_A, String.format(PVAL_FORMAT, i));
			t.addChild(PATH_B).addChild(String.format(NODENAME_FORMAT, i))
					.setProperty(PNAME_B, String.format(PVAL_FORMAT, i % CARD));
		}
		L.info(String.format("\taddChild elapsed(µs):\t%1$,3d",
				stopwatch.elapsed(TimeUnit.MICROSECONDS)));
		newLoadStats();
		l.operation = "addChild";
		l.elapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
		stopwatch.reset().start();
		try {
			root.commit();
		} catch (Throwable th) {
			throw new RuntimeException(th);
		}
		L.info(String.format("\tcommit elapsed(µs):\t%1$,3d",
				stopwatch.elapsed(TimeUnit.MICROSECONDS)));
		newLoadStats();
		l.operation = "commit";
		l.elapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
		stopwatch.reset().start();
	}

	private void showFileStoreSize(NodeState rootNodeState, String path) {
		L.info("■size of...");
		L.info(path);
		stopwatch.reset().start();
		Long[] l = TestHelpers.exploreSize((SegmentNodeState) rootNodeState);
		L.info(String.format("\tbytes:\t%1$,3d", l[0]));
		L.info(String.format("\tbytes(no link):\t%1$,3d", l[1]));
		L.info(String.format("\tcount:\t%1$,3d", l[2]));
		L.info(String.format("\tcount(no link):\t%1$,3d", l[3]));
		L.info(String.format("\telapsed(µs):\t\t%1$,3d",
				stopwatch.elapsed(TimeUnit.MICROSECONDS)));
		newNodeStats();
		n.path = path;
		n.bytes = l[0];
		n.bytesNolink = l[1];
		n.nodes = l[2];
		n.nodesNolink = l[3];
		n.elapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
	}

	protected void showTreeSizeMongo(DB db, String path) {
		L.info("■size of...");
		L.info(path);
		stopwatch.reset().start();
		Long[] l = TestHelpers.exploreSize(db, path);
		L.info(String.format("\tbytes:\t%1$,3d", l[0]));
		L.info(String.format("\tcount:\t%1$,3d", l[1]));
		L.info(String.format("\telapsed(µs):\t\t%1$,3d",
				stopwatch.elapsed(TimeUnit.MICROSECONDS)));
		newNodeStats();
		n.path = path;
		n.bytes = l[0];
		n.nodes = l[1];
		n.elapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
	}

	protected void showLuceneIndexSize(String path) {
		L.info("■blob size of...");
		L.info(path);
		stopwatch.reset().start();
		long size = TestHelpers.calcLucneIndexBlobSize(root.getTree(path + "/:data"));
		L.info(String.format("\tbytes:\t%1$,3d", size));
		newBlobStats();
		b.path = path;
		b.bytes = size;
		b.elapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
	}

	protected void showTreeSizeFileStore(String path) {
		Tree t = root.getTree(path);
		if (!t.exists()) {
			L.error("※ not  exists");
			L.error(path);
			return;
		}
		showFileStoreSize(TestHelpers.getNodeStateFromTree(t), path);
	}

	protected void doQeury(boolean hasIndex) {
		doQeury(hasIndex, PNAME_A);
		doQeury(hasIndex, PNAME_B);
	}

	private void doQuery(String sql) {
		L.info("■exec query...", sql);
		L.info(sql);
		stopwatch.reset().start();
		List<String> paths = executeQuery(sql);
		L.info(String.format("\tpaths:\t%d", paths.size()));
		L.info(String.format("\telapsed(µs):\t\t%1$,3d",
				stopwatch.elapsed(TimeUnit.MICROSECONDS)));
		newQueryStats();
		q.query = sql;
		q.results = paths.size();
		q.elapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
	}

	private void doQeury(boolean hasIndex, String pname) {
		setTraversalEnabled(true);//★<=falseの場合index無しの検索で0件

		doQuery("select [jcr:path] from [nt:base] where [" + pname + "] = '"
				+ String.format(PVAL_FORMAT, 1) + "'");

		doQuery("select [jcr:path] from [nt:base] where [" + pname + "] = '"
				+ String.format(PVAL_FORMAT, 3) + "'");

		doQuery("select [jcr:path] from [nt:base] where [" + pname + "] >= '"
				+ String.format(PVAL_FORMAT, 5) + "' and [" + pname + "] < '"
				+ String.format(PVAL_FORMAT, 10) + "'");
	}

    static String formatPlan(String plan) {
        plan = plan.replaceAll(" where ", "\n  where ");
        plan = plan.replaceAll(" inner join ", "\n  inner join ");
        plan = plan.replaceAll(" on ", "\n  on ");
        plan = plan.replaceAll(" and ", "\n  and ");
        return plan;
    }

    protected List<String> executeQuery(String query) {
    	// 元メソッドから実行時間制限とCollection.sortを削除
        List<String> lines = new ArrayList<String>();
        try {
            Result result = executeQuery(query, SQL2, NO_BINDINGS);
            for (ResultRow row : result.getRows()) {
                String r = readRow(row, true);
                if (query.startsWith("explain ")) {
                    r = formatPlan(r);
                }
                lines.add(r);
            }
        } catch (ParseException e) {
            lines.add(e.toString());
        } catch (IllegalArgumentException e) {
            lines.add(e.toString());
        }
        return lines;
    }
}
