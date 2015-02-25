/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.index.property;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_SYSTEM;
import static org.apache.jackrabbit.JcrConstants.NT_BASE;
import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexUtils.createIndexDefinition;
import static org.apache.jackrabbit.oak.plugins.index.counter.NodeCounterEditor.COUNT_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.JCR_NODE_TYPES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.IndexUpdateProvider;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.query.ast.SelectorImpl;
import org.apache.jackrabbit.oak.query.index.FilterImpl;
import org.apache.jackrabbit.oak.query.index.TraversingIndex;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import forstudy.PocMarking;
import forstudy.TestHelpers;
import forstudy.TestSpec;
import forstudy.TestSpec.Confirmatin;

/**
 * Test the Property2 index mechanism.
 */
@PocMarking
public class PropertyIndexTest {
	public PropertyIndexTest SRC_REF;

    private static final int MANY = 100;

    private static final EditorHook HOOK = new EditorHook(
            new IndexUpdateProvider(new PropertyIndexEditorProvider()));

    @Test
	@TestSpec(
			objective = { "要素数に応じたコスト見積を確認する" },
			confirmatins = {
					@Confirmatin(operation = "インデックスを追加する"),
					@Confirmatin(operation = "複数のコンテンツを追加する"),
					@Confirmatin(operation = "コミットする"),
					@Confirmatin(operation = "プロパティ値毎にプロパティインデックス参照を行ないコストを取得する", expected = "該当するコンテンツに応じたコストである事"),
			})
    public void costEstimation() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        NodeBuilder index = createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME), "foo",
                true, false, ImmutableSet.of("foo"), null);
        // disable the estimation
        index.setProperty("entryCount", -1);
        NodeState before = builder.getNodeState();

        // Add some content and process it through the property index hook
        for (int i = 0; i < MANY; i++) {
            builder.child("n" + i).setProperty("foo", "x" + i % 20);
        }
        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_BASE);

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        double cost;

        cost = lookup.getCost(f, "foo", PropertyValues.newString("x1"));
        assertTrue("cost: " + cost, cost >= 6.5 && cost <= 7.5);

        cost = lookup.getCost(f, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2")));
        assertTrue("cost: " + cost, cost >= 11.5 && cost <= 12.5);

        cost = lookup.getCost(f, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2", "x3", "x4", "x5")));
        assertTrue("cost: " + cost, cost >= 26.5 && cost <= 27.5);

        cost = lookup.getCost(f, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x0")));
        assertTrue("cost: " + cost, cost >= 51.5 && cost <= 52.5);

        cost = lookup.getCost(f, "foo", null);
        assertTrue("cost: " + cost, cost >= MANY);

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    /**
     * This is essentially same test as {@link #costEstimation()} with one difference that it uses
     * path constraint in query and creates similar trees under 2 branches {@code path1} and {@code path2}.
     * The cost estimation is then verified to be same as that in {@code costEstimation} for query under {@code path1}
     * @throws Exception
     */
    @Test
	@TestSpec(
			objective = { "要素数に応じたコスト見積を確認する" },
			confirmatins = {
					@Confirmatin(operation = "インデックスを追加する"),
					@Confirmatin(operation = "複数のコンテンツを追加する"),
					@Confirmatin(operation = "コミットする"),
					@Confirmatin(operation = "プロパティ値毎にパス制限有りプロパティインデックス参照を行ないコストを取得する", expected = "該当するコンテンツに応じたコストである事"),
					@Confirmatin(operation = "プロパティ値毎にパス制限無しプロパティインデックス参照を行ないコストを取得する", expected = "「パス制限有りコスト < パス制限無し」である事"),
			})
    public void pathBasedCostEstimation() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        NodeBuilder index = createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME), "foo",
                true, false, ImmutableSet.of("foo"), null);
        // disable the estimation
        index.setProperty("entryCount", -1);
        builder.setProperty(COUNT_PROPERTY_NAME, (long) MANY * 2, Type.LONG);
        NodeState before = builder.getNodeState();

        NodeBuilder path1 = builder.child("path1");
        NodeBuilder path2 = builder.child("path2");
        // Add some content and process it through the property index hook
        for (int i = 0; i < MANY; i++) {
            path1.child("n" + i).setProperty("foo", "x" + i % 20);
            path2.child("n" + i).setProperty("foo", "x" + i % 20);
        }
        path1.setProperty(COUNT_PROPERTY_NAME, (long) MANY, Type.LONG);
        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_BASE);
        f.restrictPath("/path1", Filter.PathRestriction.ALL_CHILDREN);

        FilterImpl f_ = createFilter(indexed, NT_BASE);//★パス制限なし

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        double cost, cost_;

        cost = lookup.getCost(f, "foo", PropertyValues.newString("x1"));
        assertTrue("cost: " + cost, cost >= 7.5 && cost <= 8.5);
        cost_ = lookup.getCost(f_, "foo", PropertyValues.newString("x1"));//★
        assertTrue(cost < cost_);//★

        cost = lookup.getCost(f, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2")));
        assertTrue("cost: " + cost, cost >= 14.5 && cost <= 15.5);
        cost_ = lookup.getCost(f_, "foo", PropertyValues.newString(
        		Arrays.asList("x1", "x2")));//★
        assertTrue(cost < cost_);//★

        cost = lookup.getCost(f, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2", "x3", "x4", "x5")));
        assertTrue("cost: " + cost, cost >= 34.5 && cost <= 35.5);
        cost_ = lookup.getCost(f_, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2", "x3", "x4", "x5")));//★
        assertTrue(cost < cost_);//★

        cost = lookup.getCost(f, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x0")));
        assertTrue("cost: " + cost, cost >= 81.5 && cost <= 82.5);
        cost_ = lookup.getCost(f_, "foo", PropertyValues.newString(
                Arrays.asList("x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x0")));//★
        assertTrue(cost < cost_);//★

        cost = lookup.getCost(f, "foo", null);
        assertTrue("cost: " + cost, cost >= MANY);
        cost_ = lookup.getCost(f_, "foo", null);//★
        assertTrue(cost < cost_);//★

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    @Test
    public void costMaxEstimation() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME), "foo",
                true, false, ImmutableSet.of("foo"), null);
        NodeState before = builder.getNodeState();

        // 100 nodes in the index:
        // with a single level /content cost is 121
        // adding a second level /content/data cost is133

        // 101 nodes in the index:
        // with a single level /content cost is 121
        // adding a second level /content/data cost is 133

        // 100 nodes, 12 levels deep, cost is 345
        // 101 nodes, 12 levels deep, cost is 345

        // threshold for estimation (PropertyIndexLookup.MAX_COST) is at 100
        int nodes = 101;
        int levels = 12;

        NodeBuilder data = builder;
        for (int i = 0; i < levels; i++) {
            data = data.child("l" + i);
        }
        for (int i = 0; i < nodes; i++) {
            NodeBuilder c = data.child("c_" + i);
            c.setProperty("foo", "azerty");
        }
        // add more nodes (to make traversal more expensive)
        for (int i = 0; i < 10000; i++) {
            data.child("cx_" + i);
        }
        NodeState after = builder.getNodeState();
        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_BASE);

        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        double cost = lookup.getCost(f, "foo",
                PropertyValues.newString("azerty"));
        double traversal = new TraversingIndex().getCost(f, indexed);

        assertTrue("Estimated cost for " + nodes
                + " nodes should not be higher than traversal (" + cost + " < " + traversal + ")",
                cost < traversal);

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    @Test
	@TestSpec(
			objective = { "プロパティ参照時のインデックスの使用を確認する" },
			confirmatins = {
					@Confirmatin(operation = "インデックスを追加する"),
					@Confirmatin(operation = "プロパティに単一値、複数値を含むコンテンツを追加する"),
					@Confirmatin(operation = "ダミーのコンテンツを追加する"),
					@Confirmatin(operation = "コミットする"),
					@Confirmatin(operation = "プロパティ値毎にプロパティインデックス参照を行なう", expected = "該当するコンテンツが取得される事"),
					@Confirmatin(operation = "プロパティ値毎にプロパティインデックス参照を行ないコストを取得する", expected = "該当するコンテンツに応じたコストである事"),
			})
    public void testPropertyLookup() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        NodeBuilder index = createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME), "foo",
                true, false, ImmutableSet.of("foo"), null);
        index.setProperty("entryCount", -1);
        NodeState before = builder.getNodeState();

        // Add some content and process it through the property index hook
        builder.child("a").setProperty("foo", "abc");
        builder.child("b").setProperty("foo", Arrays.asList("abc", "def"),
                Type.STRINGS);
        // plus lots of dummy content to highlight the benefit of indexing
        for (int i = 0; i < MANY; i++) {
            builder.child("n" + i).setProperty("foo", "xyz");
        }
        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_BASE);

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("a", "b"), find(lookup, "foo", "abc", f));
        assertEquals(ImmutableSet.of("b"), find(lookup, "foo", "def", f));
        assertEquals(ImmutableSet.of(), find(lookup, "foo", "ghi", f));
        assertEquals(MANY, find(lookup, "foo", "xyz", f).size());
        assertEquals(MANY + 2, find(lookup, "foo", null, f).size());

        double cost, costBefore = MANY;//★
        cost = lookup.getCost(f, "foo", PropertyValues.newString(Arrays.asList("abc", "def")));//★
        assertTrue("cost: " + cost, cost <= costBefore);//★5.0
        costBefore = cost;
        cost = lookup.getCost(f, "foo", PropertyValues.newString("abc"));
        assertTrue("cost: " + cost, cost <= costBefore);//★4.0
        costBefore = cost;
        cost = lookup.getCost(f, "foo", PropertyValues.newString("def"));
        assertTrue("cost: " + cost, cost <= costBefore);//★3.0
        costBefore = cost;
        cost = lookup.getCost(f, "foo", PropertyValues.newString("hoge"));
        assertTrue("cost: " + cost, cost <= costBefore);//★2.0

        cost = lookup.getCost(f, "foo", PropertyValues.newString("xyz"));
        assertTrue("cost: " + cost, cost >= MANY);
        cost = lookup.getCost(f, "foo", null);
        assertTrue("cost: " + cost, cost >= MANY);

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    @Test
	@TestSpec(
			objective = { "パス制限時の検索を確認する" },
			confirmatins = {
					@Confirmatin(operation = "インデックスを追加する"),
					@Confirmatin(operation = "プロパティ値が重複する、複数のコンテンツを追加する"),
					@Confirmatin(operation = "コミットする"),
					@Confirmatin(operation = "restrictPathを指定してプロパティインデックス参照する", expected = "指定パスのコンテンツが取得される事"),
			})
    public void testPathAwarePropertyLookup() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME), "foo",
                true, false, ImmutableSet.of("foo"), null);
        NodeState before = builder.getNodeState();

        // Add some content and process it through the property index hook
        builder.child("a").setProperty("foo", "abc");
        builder.child("b").setProperty("foo", "abc");

        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_BASE);
        f.restrictPath("/a", Filter.PathRestriction.ALL_CHILDREN);

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("a"), find(lookup, "foo", "abc", f));

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    private static Set<String> find(PropertyIndexLookup lookup, String name,
            String value, Filter filter) {
        return Sets.newHashSet(lookup.query(filter, name, value == null ? null
                : PropertyValues.newString(value)));
    }

    @Test
    public void testCustomConfigPropertyLookup() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "fooIndex", true, false, ImmutableSet.of("foo", "extrafoo"),
                null);
        NodeState before = builder.getNodeState();

        // Add some content and process it through the property index hook
        builder.child("a").setProperty("foo", "abc")
                .setProperty("extrafoo", "pqr");
        builder.child("b").setProperty("foo", Arrays.asList("abc", "def"),
                Type.STRINGS);
        // plus lots of dummy content to highlight the benefit of indexing
        for (int i = 0; i < MANY; i++) {
            builder.child("n" + i).setProperty("foo", "xyz");
        }
        NodeState after = builder.getNodeState();

        // Add an index
        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_BASE);

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("a", "b"), find(lookup, "foo", "abc", f));
        assertEquals(ImmutableSet.of("b"), find(lookup, "foo", "def", f));
        assertEquals(ImmutableSet.of(), find(lookup, "foo", "ghi", f));
        assertEquals(MANY, find(lookup, "foo", "xyz", f).size());
        assertEquals(ImmutableSet.of("a"), find(lookup, "extrafoo", "pqr", f));

        try {
            assertEquals(ImmutableSet.of(), find(lookup, "pqr", "foo", f));
            fail();
        } catch (IllegalArgumentException e) {
            // expected: no index for "pqr"
        }

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-666">OAK-666:
     *      Property2Index: node type is used when indexing, but ignored when
     *      querying</a>
     */
    @Test
    public void testCustomConfigNodeType() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        NodeBuilder index = builder.child(INDEX_DEFINITIONS_NAME);
        createIndexDefinition(index, "fooIndex", true, false,
                ImmutableSet.of("foo", "extrafoo"),
                ImmutableSet.of(NT_UNSTRUCTURED));
        createIndexDefinition(index, "fooIndexFile", true, false,
                ImmutableSet.of("foo"), ImmutableSet.of(NT_FILE));
        NodeState before = builder.getNodeState();

        // Add some content and process it through the property index hook
        builder.child("a")
                .setProperty(JCR_PRIMARYTYPE, NT_UNSTRUCTURED, Type.NAME)
                .setProperty("foo", "abc");
        builder.child("b")
                .setProperty(JCR_PRIMARYTYPE, NT_UNSTRUCTURED, Type.NAME)
                .setProperty("foo", Arrays.asList("abc", "def"), Type.STRINGS);
        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(indexed, NT_UNSTRUCTURED);

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("a", "b"), find(lookup, "foo", "abc", f));
        assertEquals(ImmutableSet.of("b"), find(lookup, "foo", "def", f));
        assertEquals(ImmutableSet.of(), find(lookup, "foo", "ghi", f));

        try {
            assertEquals(ImmutableSet.of(), find(lookup, "pqr", "foo", f));
            fail();
        } catch (IllegalArgumentException e) {
            // expected: no index for "pqr"
        }

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    private static FilterImpl createFilter(NodeState root, String nodeTypeName) {
        NodeState system = root.getChildNode(JCR_SYSTEM);
        NodeState types = system.getChildNode(JCR_NODE_TYPES);
        NodeState type = types.getChildNode(nodeTypeName);
        SelectorImpl selector = new SelectorImpl(type, nodeTypeName);
        return new FilterImpl(selector, "SELECT * FROM [" + nodeTypeName + "]", new QueryEngineSettings());
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-666">OAK-666:
     *      Property2Index: node type is used when indexing, but ignored when
     *      querying</a>
     */
    @Test
    public void testCustomConfigNodeTypeFallback() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        NodeBuilder index = builder.child(INDEX_DEFINITIONS_NAME);
        createIndexDefinition(
                index, "fooIndex", true, false,
                ImmutableSet.of("foo", "extrafoo"), null);
        createIndexDefinition(
                index, "fooIndexFile", true, false,
                ImmutableSet.of("foo"), ImmutableSet.of(NT_FILE));
        NodeState before = builder.getNodeState();

        // Add some content and process it through the property index hook
        builder.child("a")
                .setProperty(JCR_PRIMARYTYPE, NT_UNSTRUCTURED, Type.NAME)
                .setProperty("foo", "abc");
        builder.child("b")
                .setProperty(JCR_PRIMARYTYPE, NT_UNSTRUCTURED, Type.NAME)
                .setProperty("foo", Arrays.asList("abc", "def"), Type.STRINGS);
        NodeState after = builder.getNodeState();

        // Add an index
        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        FilterImpl f = createFilter(after, NT_UNSTRUCTURED);

        // Query the index
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("a", "b"), find(lookup, "foo", "abc", f));
        assertEquals(ImmutableSet.of("b"), find(lookup, "foo", "def", f));
        assertEquals(ImmutableSet.of(), find(lookup, "foo", "ghi", f));

        try {
            assertEquals(ImmutableSet.of(), find(lookup, "pqr", "foo", f));
            fail();
        } catch (IllegalArgumentException e) {
            // expected: no index for "pqr"
        }

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    @Test(expected = CommitFailedException.class)
	@TestSpec(
			objective = { "ノード/属性の相対パス指定によるインデックスの使用を確認する" },
			confirmatins = {
					@Confirmatin(operation = "ユニーク指定のインデックスを追加する"),
					@Confirmatin(operation = "一部プロパティ値が重複する複数のコンテンツを追加する"),
					@Confirmatin(operation = "コミットする", expected = "CommitFailedExceptionがスローされる事"),
			})
    public void testUnique() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(
                builder.child(INDEX_DEFINITIONS_NAME),
                "fooIndex", true, true, ImmutableSet.of("foo"), null);
        NodeState before = builder.getNodeState();
        builder.child("a").setProperty("foo", "abc");
        builder.child("b").setProperty("foo", Arrays.asList("abc", "def"),
                Type.STRINGS);
        NodeState after = builder.getNodeState();

        try {
            HOOK.processCommit(before, after, CommitInfo.EMPTY); // should throw
        } finally {
            store.merge(builder, HOOK, CommitInfo.EMPTY);//★
            fs.flush();//★
            fs.close();//★
        }
    }

    @Test
    public void testUniqueByTypeOK() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "fooIndex", true, true, ImmutableSet.of("foo"),
                ImmutableSet.of("typeFoo"));
        NodeState before = builder.getNodeState();
        builder.child("a").setProperty(JCR_PRIMARYTYPE, "typeFoo", Type.NAME)
                .setProperty("foo", "abc");
        builder.child("b").setProperty(JCR_PRIMARYTYPE, "typeBar", Type.NAME)
                .setProperty("foo", "abc");
        NodeState after = builder.getNodeState();

        HOOK.processCommit(before, after, CommitInfo.EMPTY); // should not throw

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    @Test(expected = CommitFailedException.class)
    public void testUniqueByTypeKO() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "fooIndex", true, true, ImmutableSet.of("foo"),
                ImmutableSet.of("typeFoo"));
        NodeState before = builder.getNodeState();
        builder.child("a").setProperty(JCR_PRIMARYTYPE, "typeFoo", Type.NAME)
                .setProperty("foo", "abc");
        builder.child("b").setProperty(JCR_PRIMARYTYPE, "typeFoo", Type.NAME)
                .setProperty("foo", "abc");
        NodeState after = builder.getNodeState();

        try {
            HOOK.processCommit(before, after, CommitInfo.EMPTY); // should throw
        } finally {
            store.merge(builder, HOOK, CommitInfo.EMPTY);//★
            fs.flush();//★
            fs.close();//★
        }
    }

    @Test
    public void testUniqueByTypeDelete() throws Exception {
        FileStore fs = TestHelpers.createFileStore();//★
        NodeStore store = new SegmentNodeStore(fs);//★

        // Add index definition
        NodeBuilder builder = store.getRoot().builder();//★
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "fooIndex", true, true, ImmutableSet.of("foo"),
                ImmutableSet.of("typeFoo"));
        builder.child("a").setProperty(JCR_PRIMARYTYPE, "typeFoo", Type.NAME)
                .setProperty("foo", "abc");
        builder.child("b").setProperty(JCR_PRIMARYTYPE, "typeBar", Type.NAME)
                .setProperty("foo", "abc");
        NodeState before = builder.getNodeState();
        builder.getChildNode("b").remove();
        NodeState after = builder.getNodeState();

        HOOK.processCommit(before, after, CommitInfo.EMPTY); // should not throw

        store.merge(builder, HOOK, CommitInfo.EMPTY);//★
        fs.flush();//★
        fs.close();//★
    }

    @Test
    public void ptest() throws Exception {
		FileStore fs = TestHelpers.createFileStore();// ★
		NodeStore store = new SegmentNodeStore(fs);// ★

		// Add index definition
		NodeBuilder builder = store.getRoot().builder();// ★
		NodeBuilder index = createIndexDefinition(
				builder.child(INDEX_DEFINITIONS_NAME), "foo", true, false,
				ImmutableSet.of("foo"), null);
		// disable the estimation
		index.setProperty("entryCount", -1);
		NodeState before = builder.getNodeState();

		// Add some content and process it through the property index hook
		for (int i = 0; i < 100000; i++) {
			builder.child("n" + i).setProperty("foo", "x" + i % 20);
			builder.child("m" + i).setProperty("bar", "x" + i % 20);
		}
		NodeState after = builder.getNodeState();

		NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

		FilterImpl f = createFilter(indexed, NT_BASE);

		// Query the index
		PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);

		Stopwatch stopwatch = Stopwatch.createStarted();
        long millis;

		assertEquals(5000, find(lookup, "foo", "x19", f).size());
		millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		System.out.printf("elapsed: %s\n", stopwatch.toString());

//		assertEquals(5000, find(lookup, "bar", "x1", f).size());
		long l = find(lookup, "bar", "x1", f).size();
		millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		System.out.printf("elapsed: %s\n", stopwatch.toString());

		double cost1, cost2;

		cost1 = lookup.getCost(f, "foo", PropertyValues.newString("x1"));
		cost2 = lookup.getCost(f, "bar", PropertyValues.newString("x1"));

		cost1 = lookup.getCost(f, "foo", null);
		cost2 = lookup.getCost(f, "bar", null);

		store.merge(builder, HOOK, CommitInfo.EMPTY);// ★
		fs.flush();// ★
		fs.close();// ★
	}
}
