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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jcr.query.Query;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.plugins.index.IndexUtils;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.query.AbstractQueryTest;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import forstudy.ContextRunner;
import forstudy.PocMarking;
import forstudy.TestHelpers;
import forstudy.TestSpec;
import forstudy.TestSpec.Confirmatin;

/**
 * <code>RelativePathTest</code>...
 */
@RunWith(ContextRunner.class)
@PocMarking
public class RelativePathTest extends AbstractQueryTest {
	FileStore fs;//★
	NodeStore store;//★

    @Override
    protected ContentRepository createRepository() {
        fs = TestHelpers.createFileStore();//★
        store = new SegmentNodeStore(fs);//★
        return new Oak(store).with(new InitialContent())
                .with(new RepositoryInitializer() {
                    @Override
                    public void initialize(NodeBuilder builder) {
                        NodeBuilder index = IndexUtils.getOrCreateOakIndex(builder);
                        IndexUtils.createIndexDefinition(index, "myProp", true,
                                false, ImmutableList.<String>of("myProp"), null);
                        IndexUtils.createIndexDefinition(index, "myProp1", true,
                                false, ImmutableList.<String>of("myProp1"), null);
                    }
                })
                .with(new OpenSecurityProvider())
                .with(new PropertyIndexProvider())
                .with(new PropertyIndexEditorProvider())
                .createContentRepository();
    }

    @After
    public void teardown() throws Throwable {//★
        fs.flush();//★
        fs.close();//★
    }

    @Test
	@TestSpec(
			objective = { "ノード/属性の相対パス指定によるインデックスの使用を確認する" },
			precondition = {"myPropプロパティインデックスが定義されている"},
			confirmatins = {
					@Confirmatin(operation = "コンテンツを追加する"),
					@Confirmatin(operation = "コミットする"),
					@Confirmatin(operation = "トラバースを不可にする"),
					@Confirmatin(operation = "条件(ノード/属性 is not null)クエリする", expected = "結果が得られる"),
					@Confirmatin(operation = "条件(ノード/属性 is not null)をexplainする", expected = "myPropプロパティインデックスが指定されている"),
					@Confirmatin(operation = "条件(ノード/属性 = 'foo')クエリする", expected = "結果が得られる"),
			})
    public void query() throws Exception {
        Tree t = root.getTree("/");
        t.addChild("a").addChild("n").setProperty("myProp", "foo");
        t.addChild("b").addChild("n").setProperty("myProp", "bar");
        t.addChild("c").addChild("x").setProperty("myProp", "foo");
        t.setProperty("myProp", "foo");
        root.commit();
        setTraversalEnabled(false);
        assertQuery("select [jcr:path] from [nt:base] where [n/myProp] is not null",
                ImmutableList.of("/a", "/b"));

        List<String> lines = executeQuery(
                "explain select [jcr:path] from [nt:base] where [n/myProp] is not null",
                Query.JCR_SQL2);
        assertEquals(1, lines.size());
        // make sure it used the property index
        assertTrue(lines.get(0).contains("property myProp"));

        assertQuery(
                "select [jcr:path] from [nt:base] where [n/myProp] = 'foo'",
                ImmutableList.of("/a"));
        setTraversalEnabled(false);
    }

    @Test
    public void ptest() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        long millis = 0;

        Tree t = root.getTree("/");

		stopwatch.reset().start();
        for (int i = 0; i < 100000; i++) {
            t.addChild("a").addChild("n1-" + i).setProperty("myProp", "x" + i % 20);
            t.addChild("b").addChild("n2-" + i).setProperty("youProp", "x" + i % 20);
            t.addChild("c").addChild("n3-" + i).setProperty("myProp1", "x" + i);
            t.addChild("d").addChild("n4-" + i).setProperty("youProp1", "x" + i);
		}
		millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		System.out.printf("elapsed: %s\n", stopwatch.toString());

		stopwatch.reset().start();
        root.commit();
 		millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
 		System.out.printf("elapsed: %s\n", stopwatch.toString());

 		setTraversalEnabled(true);

        List<String> paths = null;

        {
			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [youProp] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("youProp elapsed: %s\n", stopwatch.toString());

			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [youProp] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("youProp elapsed: %s\n", stopwatch.toString());
        }

        {
			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [myProp] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("myProp elapsed: %s\n", stopwatch.toString());

			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [myProp] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("myProp elapsed: %s\n", stopwatch.toString());
        }


        {
			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [youProp1] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("youProp1 elapsed: %s\n", stopwatch.toString());

			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [youProp1] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("youProp1 elapsed: %s\n", stopwatch.toString());
        }

        {
			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [myProp1] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("myProp1 elapsed: %s\n", stopwatch.toString());

			stopwatch.reset().start();
	        paths = executeQuery("select [jcr:path] from [nt:base] where [myProp1] = 'x1'", SQL2, true, false);
			millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			System.out.printf("myProp1 elapsed: %s\n", stopwatch.toString());
		}
	}
}
