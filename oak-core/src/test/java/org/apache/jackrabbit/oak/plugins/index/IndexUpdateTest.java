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
package org.apache.jackrabbit.oak.plugins.index;

import static org.apache.jackrabbit.JcrConstants.JCR_SYSTEM;
import static org.apache.jackrabbit.JcrConstants.NT_BASE;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.ASYNC_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.ASYNC_REINDEX_VALUE;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_CONTENT_NODE_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.REINDEX_ASYNC_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.REINDEX_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexUtils.createIndexDefinition;
import static org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.JCR_NODE_TYPES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexLookup;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.query.ast.SelectorImpl;
import org.apache.jackrabbit.oak.query.index.FilterImpl;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import forstudy.ContextRunner;
import forstudy.PocMarking;
import forstudy.TestHelpers;
import forstudy.TestSpec;
import forstudy.TestSpec.Confirmatin;

@RunWith(ContextRunner.class)
@PocMarking
public class IndexUpdateTest {

    private static final EditorHook HOOK = new EditorHook(
            new IndexUpdateProvider(new PropertyIndexEditorProvider()));

    FileStore fs;// ★
    NodeStore store;// ★

    private NodeState root;// ★

    private NodeBuilder builder;// ★

    @Before
    public void setup() {
        fs = TestHelpers.createFileStore();// ★
        store = new SegmentNodeStore(fs);// ★
        root = store.getRoot();// ★
        builder = root.builder();// ★
    }

    @After
    public void teardown() throws Throwable {
        store.merge(builder, HOOK, CommitInfo.EMPTY);// ★
        fs.flush();// ★
        fs.close();// ★
    }

    /**
     * Simple Test
     * <ul>
     * <li>Add an index definition</li>
     * <li>Add some content</li>
     * <li>Search & verify</li>
     * </ul>
     *
     */
    @Test
    public void test() throws Exception {
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", true, false, ImmutableSet.of("foo"), null);
        createIndexDefinition(
                builder.child("newchild").child("other")
                        .child(INDEX_DEFINITIONS_NAME), "subIndex", true,
                false, ImmutableSet.of("foo"), null);

        NodeState before = builder.getNodeState();

        // Add nodes
        builder.child("testRoot").setProperty("foo", "abc");
        builder.child("newchild").child("other").child("testChild")
                .setProperty("foo", "xyz");

        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // first check that the index content nodes exist
        checkPathExists(indexed, INDEX_DEFINITIONS_NAME, "rootIndex",
                INDEX_CONTENT_NODE_NAME);
        checkPathExists(indexed, "newchild", "other", INDEX_DEFINITIONS_NAME,
                "subIndex", INDEX_CONTENT_NODE_NAME);

        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("testRoot"), find(lookup, "foo", "abc"));

        PropertyIndexLookup lookupChild = new PropertyIndexLookup(indexed
                .getChildNode("newchild").getChildNode("other"));
        assertEquals(ImmutableSet.of("testChild"),
                find(lookupChild, "foo", "xyz"));
        assertEquals(ImmutableSet.of(), find(lookupChild, "foo", "abc"));

    }

    /**
     * Reindex Test
     * <ul>
     * <li>Add some content</li>
     * <li>Add an index definition with the reindex flag set</li>
     * <li>Search & verify</li>
     * </ul>
     */
    @Test
    public void testReindex() throws Exception {
        builder.child("testRoot").setProperty("foo", "abc");
        builder.child("testRoot1").setProperty("foo", "abce");
        NodeState before = builder.getNodeState();
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", true, false, ImmutableSet.of("foo"), null);

        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // first check that the index content nodes exist
        NodeState ns = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "rootIndex");
        checkPathExists(ns, INDEX_CONTENT_NODE_NAME);
        PropertyState ps = ns.getProperty(REINDEX_PROPERTY_NAME);
        assertNotNull(ps);
        assertFalse(ps.getValue(Type.BOOLEAN));

        // next, lookup
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("testRoot"), find(lookup, "foo", "abc"));
    }

    /**
     * Reindex Test
     * <ul>
     * <li>Add some content & an index definition</li>
     * <li>Update the index def by setting the reindex flag to true</li>
     * <li>Search & verify</li>
     * </ul>
     */
    @Test
    public void testReindex2() throws Exception {
        builder.child("testRoot").setProperty("foo", "abc");

        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", true, false, ImmutableSet.of("foo"), null)
                .removeProperty("reindex");

        NodeState before = builder.getNodeState();
        builder.child(INDEX_DEFINITIONS_NAME).child("rootIndex")
                .setProperty(REINDEX_PROPERTY_NAME, true);
        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // first check that the index content nodes exist
        NodeState ns = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "rootIndex");
        checkPathExists(ns, INDEX_CONTENT_NODE_NAME);
        PropertyState ps = ns.getProperty(REINDEX_PROPERTY_NAME);
        assertNotNull(ps);
        assertFalse(ps.getValue(Type.BOOLEAN));

        // next, lookup
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("testRoot"), find(lookup, "foo", "abc"));
    }

    /**
     * Auto Reindex Test
     * <ul>
     * <li>Add some content</li>
     * <li>Add an index definition without a reindex flag (see OAK-1874)</li>
     * <li>Search & verify</li>
     * </ul>
     */
    @Test
    public void testReindexAuto() throws Exception {
        builder.child("testRoot").setProperty("foo", "abc");
        NodeState before = builder.getNodeState();

        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", false, false, ImmutableSet.of("foo"), null);

        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // first check that the index content nodes exist
        NodeState ns = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "rootIndex");
        checkPathExists(ns, INDEX_CONTENT_NODE_NAME);
        PropertyState ps = ns.getProperty(REINDEX_PROPERTY_NAME);
        assertNotNull(ps);
        assertFalse(ps.getValue(Type.BOOLEAN));

        // next, lookup
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("testRoot"), find(lookup, "foo", "abc"));
    }

    @Test
    public void testReindexHidden() throws Exception {
        // NodeState before = EmptyNodeState.EMPTY_NODE;
        // NodeBuilder builder = before.builder();
        NodeBuilder builder = this.builder;
        NodeState before = builder.getNodeState();
        builder.child(":testRoot").setProperty("foo", "abc");
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", false, false, ImmutableSet.of("foo"), null);
        NodeState after = builder.getNodeState();
        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // first check that the index content nodes exist
        NodeState ns = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "rootIndex");
        NodeState index = checkPathExists(ns, INDEX_CONTENT_NODE_NAME);
        PropertyState ps = ns.getProperty(REINDEX_PROPERTY_NAME);
        assertNotNull(ps);
        assertFalse(ps.getValue(Type.BOOLEAN));
        assertFalse(index.getChildNodeCount(1) > 0);

        before = indexed;
        builder = before.builder();
        builder.child(INDEX_DEFINITIONS_NAME).child("rootIndex")
                .setProperty("reindex", true);
        after = builder.getNodeState();
        indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);
        index = checkPathExists(ns, INDEX_CONTENT_NODE_NAME);
        ps = ns.getProperty(REINDEX_PROPERTY_NAME);
        assertNotNull(ps);
        assertFalse(ps.getValue(Type.BOOLEAN));
        assertFalse(index.getChildNodeCount(1) > 0);
    }

    @Test
    public void testIndexDefinitions() throws Exception {
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "existing", true, false, ImmutableSet.of("foo"), null);

        NodeState before = builder.getNodeState();
        NodeBuilder other = builder.child("test").child("other");
        // Add index definition
        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME), "foo",
                true, false, ImmutableSet.of("foo"), null);
        createIndexDefinition(
                other.child(INDEX_DEFINITIONS_NAME), "index2", true, false,
                ImmutableSet.of("foo"), null);
        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // check that the index content nodes exist
        checkPathExists(indexed, INDEX_DEFINITIONS_NAME, "existing",
                INDEX_CONTENT_NODE_NAME);
        checkPathExists(indexed, "test", "other", INDEX_DEFINITIONS_NAME,
                "index2", INDEX_CONTENT_NODE_NAME);
    }

    @Test
    public void reindexAndIndexDefnChildRemoval_OAK_2117() throws Exception{
        builder.child("testRoot").setProperty("foo", "abc");
        NodeState before = builder.getNodeState();

        NodeBuilder nb = createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", false, false, ImmutableSet.of("foo"), null);
        nb.child("prop1").setProperty("foo", "bar");

        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);

        // first check that the index content nodes exist
        NodeState ns = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "rootIndex");

        //Check index defn child node exist
        checkPathExists(ns, "prop1");
        checkPathExists(ns, INDEX_CONTENT_NODE_NAME);

        // next, lookup
        PropertyIndexLookup lookup = new PropertyIndexLookup(indexed);
        assertEquals(ImmutableSet.of("testRoot"), find(lookup, "foo", "abc"));

    }


    /**
     * Async Reindex Test (OAK-2174)
     * <ul>
     * <li>Add some content</li>
     * <li>Add an index definition with the reindex flag and the reindex-async flag set</li>
     * <li>Run the background async job manually</li>
     * <li>Search & verify</li>
     * </ul>
     */
    @Test
    @TestSpec(objective = { "非同期インデックスリビルドの動作とタイミングを確認する" }, confirmatins = {
            @Confirmatin(operation = "/oak:index/foo/@reindex-async=trueでインデックスを定義する"),
            @Confirmatin(operation = "コンテンツを追加する"),
            @Confirmatin(operation = "コミット(store.merge)する", expected = "インデックス更新が走らない事"),
            @Confirmatin(operation = "PropertyIndexLookupでfind", expected = "見つからない事"),
            @Confirmatin(operation = "AsyncIndexUpdateでrun()", expected = "インデックス更新が走る事"),
            @Confirmatin(operation = "/oak:index/foo/@asyncを取得", expected = "async-reindexである事"),
            @Confirmatin(operation = "AsyncIndexUpdateでrun()", expected = "インデックス更新が走らない事"),
            @Confirmatin(operation = "/oak:index/foo/@asyncを取得", expected = "存在しない事"),
            @Confirmatin(operation = "コンテンツ追加"),
            @Confirmatin(operation = "コミット(store.merge)", expected = "インデックス更新が走らない事"),
            @Confirmatin(operation = "PropertyIndexLookupでfind", expected = "見つかる事") })
    public void testReindexAsync() throws Exception {
        IndexEditorProvider provider = new PropertyIndexEditorProvider();
        EditorHook hook = new EditorHook(new IndexUpdateProvider(provider));

        FileStore fs = TestHelpers.createFileStore();// ★
        NodeStore store = new SegmentNodeStore(fs);// ★
        NodeBuilder builder = store.getRoot().builder();

        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", true, false, ImmutableSet.of("foo"), null)
                .setProperty(REINDEX_ASYNC_PROPERTY_NAME, true);
        builder.child("testRoot").setProperty("foo", "abc");

        // merge it back in
        store.merge(builder, hook, CommitInfo.EMPTY);

        // first check that the async flag exist
        NodeState ns1 = checkPathExists(store.getRoot(),
                INDEX_DEFINITIONS_NAME, "rootIndex");
        assertTrue(ns1.getProperty(REINDEX_PROPERTY_NAME)
                .getValue(Type.BOOLEAN));
        assertTrue(ns1.getProperty(REINDEX_ASYNC_PROPERTY_NAME).getValue(
                Type.BOOLEAN));
        assertEquals(ASYNC_REINDEX_VALUE, ns1.getProperty(ASYNC_PROPERTY_NAME)
                .getValue(Type.STRING));

        AsyncIndexUpdate async = new AsyncIndexUpdate(ASYNC_REINDEX_VALUE,
                store, provider, true);
        int max = 5;
        // same behaviour as PropertyIndexAsyncReindex mbean
        boolean done = false;
        int count = 0;
        while (!done || count >= max) {
            async.run();
            done = async.isFinished();
            count++;
        }

        // first check that the index content nodes exist
        NodeState ns = checkPathExists(store.getRoot(), INDEX_DEFINITIONS_NAME,
                "rootIndex");
        checkPathExists(ns, INDEX_CONTENT_NODE_NAME);
        assertFalse(ns.getProperty(REINDEX_PROPERTY_NAME)
                .getValue(Type.BOOLEAN));
        assertNull(ns.getProperty(ASYNC_PROPERTY_NAME));

        // next, lookup
        PropertyIndexLookup lookup = new PropertyIndexLookup(store.getRoot());
        assertEquals(ImmutableSet.of("testRoot"), find(lookup, "foo",
        "abc"));

        fs.flush();// ★
        fs.close();// ★
    }

    /**
     * OAK-2203 Test reindex behavior on a sync index when the index provider is missing
     * for a given type
     */
    @Test
    public void testReindexSyncMissingProvider() throws Exception {
        EditorHook hook = new EditorHook(new IndexUpdateProvider(
                emptyProvider()));
        NodeState before = builder.getNodeState();

        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", true, false, ImmutableSet.of("foo"), null);
        builder.child(INDEX_DEFINITIONS_NAME).child("azerty");
        builder.child("testRoot").setProperty("foo", "abc");
        NodeState after = builder.getNodeState();

        NodeState indexed = hook.processCommit(before, after, CommitInfo.EMPTY);
        NodeState rootIndex = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "rootIndex");
        PropertyState ps = rootIndex.getProperty(REINDEX_PROPERTY_NAME);
        assertNotNull(ps);
        assertTrue(ps.getValue(Type.BOOLEAN));

        NodeState azerty = checkPathExists(indexed, INDEX_DEFINITIONS_NAME,
                "azerty");
        assertNull("Node should be ignored by reindexer",
                azerty.getProperty(REINDEX_PROPERTY_NAME));
    }

    @Test
    @TestSpec(objective = { "インデックスリビルドでreindexCount属性が加算されている事を確認する" }, confirmatins = {
            @Confirmatin(operation = "/oak:index/rootIndexインデックスを定義する"),
            @Confirmatin(operation = "コミットする", expected = "インデックスが更新される事"),
            @Confirmatin(operation = "/oak:index/rootIndex/@reindexCountを取得する"),
            @Confirmatin(operation = "/oak:index/rootIndex/@reindex=trueに設定する"),
            @Confirmatin(operation = "コミットする", expected = "インデックスが更新される事"),
            @Confirmatin(operation = "/oak:index/rootIndex/@reindexCountを取得する", expected = "値が増えている事"), })
    public void testReindexCount() throws Exception {
        builder.child("testRoot").setProperty("foo", "abc");
        NodeState before = builder.getNodeState();

        createIndexDefinition(builder.child(INDEX_DEFINITIONS_NAME),
                "rootIndex", false, false, ImmutableSet.of("foo"), null);

        NodeState after = builder.getNodeState();

        NodeState indexed = HOOK.processCommit(before, after, CommitInfo.EMPTY);
        long t1 = getReindexCount(indexed);

        NodeBuilder b2 = indexed.builder();
        b2.child(INDEX_DEFINITIONS_NAME).child("rootIndex").setProperty(IndexConstants.REINDEX_PROPERTY_NAME, true);
        indexed = HOOK.processCommit(indexed, b2.getNodeState(), CommitInfo.EMPTY);
        long t2 = getReindexCount(indexed);

        assertTrue(t2 > t1);
    }


    long getReindexCount(NodeState indexed) {
        return indexed.getChildNode(INDEX_DEFINITIONS_NAME)
                .getChildNode("rootIndex")
                .getProperty(IndexConstants.REINDEX_COUNT).getValue(Type.LONG);
    }

    private static IndexEditorProvider emptyProvider() {
        return new IndexEditorProvider() {
            @Override
            public Editor getIndexEditor(String type, NodeBuilder definition,
                    NodeState root, IndexUpdateCallback callback)
                    throws CommitFailedException {
                return null;
            }
        };
    }

    private Set<String> find(PropertyIndexLookup lookup, String name,
            String value) {
        NodeState system = root.getChildNode(JCR_SYSTEM);
        NodeState types = system.getChildNode(JCR_NODE_TYPES);
        NodeState type = types.getChildNode(NT_BASE);
        SelectorImpl selector = new SelectorImpl(type, NT_BASE);
        Filter filter = new FilterImpl(selector, "SELECT * FROM [nt:base]", new QueryEngineSettings());
        return Sets.newHashSet(lookup.query(filter, name,
                PropertyValues.newString(value)));
    }

    static NodeState checkPathExists(NodeState state, String... verify) {
        NodeState c = state;
        for (String p : verify) {
            c = c.getChildNode(p);
            assertTrue(c.exists());
        }
        return c;
    }



}
