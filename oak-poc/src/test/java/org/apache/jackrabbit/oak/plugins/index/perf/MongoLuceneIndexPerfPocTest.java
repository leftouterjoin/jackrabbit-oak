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
package org.apache.jackrabbit.oak.plugins.index.perf;

import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.plugins.index.lucene.LowCostLuceneIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import forstudy.ContextRunner;
import forstudy.TestSpec;
import forstudy.TestSpec.Confirmatin;

@RunWith(ContextRunner.class)
public class MongoLuceneIndexPerfPocTest extends MongoNoIndexPerfPocTest {
	@Override
	protected String getIndexTypeName() {
		return "lucene";
	}

	@Override
	protected Logger getLogger() {
		return  LoggerFactory.getLogger(MongoLuceneIndexPerfPocTest.class);
	}

	@Override
	protected ContentRepository createRepository() {
		useMongoDb();
		LowCostLuceneIndexProvider provider = new LowCostLuceneIndexProvider();
		return new Oak(store).with(new InitialContent())
				.with(new OpenSecurityProvider())
				.with((QueryIndexProvider) provider).with((Observer) provider)
				.with(new LuceneIndexEditorProvider())
				.createContentRepository();
	}

	@Override
	protected void createTestIndexNode() throws Exception {
		createLuceneIndex();
	}

	@Test
	@TestSpec(
			objective = { "MongoDocumentStore,LuceneIndexの性能測定" },
			confirmatins = {
					@Confirmatin(operation = "テストデータを作成する"),
					@Confirmatin(operation = "データノードサイズを計測する", expected = { "csvにデータ出力されている事" }),
					@Confirmatin(operation = "インデックスノードサイズを計測する", expected = { "csvにデータ出力されている事" }),
					@Confirmatin(operation = "クエリを実行し処理時間を計測する", expected = { "csvにデータ出力されている事" }),
					@Confirmatin(operation = "LuceneのBlobSizeを計測する", expected = { "csvにデータ出力されている事" }),
					})
	public void doPerfTest() throws Exception {
		super.doPerfTest();
		showLuceneIndexSize("/" + INDEX_DEFINITIONS_NAME+ "/" + PNAME_A);
		showLuceneIndexSize("/" + INDEX_DEFINITIONS_NAME+ "/" + PNAME_B);
	}
}
