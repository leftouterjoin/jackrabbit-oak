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
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;

import forstudy.ContextRunner;
import forstudy.TestSpec;
import forstudy.ContextRunner.TestContext;
import forstudy.TestSpec.Confirmatin;

@RunWith(ContextRunner.class)
public class MongoNoIndexPerfPocTest extends PerfTestBase {
	DocumentNodeStore store;
	DB db;

	@Override
	protected String getIndexTypeName() {
		return "non";
	}

	@Override
	protected String getStoreTypeName() {
		return "mongo";
	}

	@Override
	protected Logger getLogger() {
		return  LoggerFactory.getLogger(MongoNoIndexPerfPocTest.class);
	}

	void useMongoDb() {
		try {
			TestContext tc = TestContext.getContext();
			db = new MongoClient("127.0.0.1", 27017)
					.getDB(tc.getTestMethod().getDeclaringClass().getSimpleName());
			db.dropDatabase();
			store = new DocumentMK.Builder().setMongoDB(db).getNodeStore();
		} catch (Throwable th) {
			throw new RuntimeException(th);
		}
	}

	@Override
	protected ContentRepository createRepository() {
		useMongoDb();
		return new Oak(store).with(new InitialContent())
				.with(new OpenSecurityProvider()).createContentRepository();
	}

	@After
	public void teardown() throws Throwable {// ★
		store.dispose();
	}

	@Test
	@TestSpec(
			objective = { "MongoDocumentStore,Index無しの性能測定" },
			confirmatins = {
					@Confirmatin(operation = "テストデータを作成する"),
					@Confirmatin(operation = "データノードサイズを計測する", expected = { "csvにデータ出力されている事" }),
					@Confirmatin(operation = "クエリを実行し処理時間を計測する", expected = { "csvにデータ出力されている事" }),
					})
	public void doPerfTest() throws Exception {
		createData();

		showTreeSizeMongo(db, "/");
		showTreeSizeMongo(db, "/" + PATH_A);
		showTreeSizeMongo(db, "/" + PATH_B);

		showTreeSizeMongo(db, "/" + INDEX_DEFINITIONS_NAME);
		showTreeSizeMongo(db, "/" + INDEX_DEFINITIONS_NAME + "/" + PNAME_A);
		showTreeSizeMongo(db, "/" + INDEX_DEFINITIONS_NAME + "/" + PNAME_B);
		showTreeSizeMongo(db, "/" + INDEX_DEFINITIONS_NAME + "/" + PNAME_A
				+ "/:index/" + String.format(PVAL_FORMAT, 0) + "/" + PATH_A);
		showTreeSizeMongo(db, "/" + INDEX_DEFINITIONS_NAME + "/" + PNAME_B
				+ "/:index/" + String.format(PVAL_FORMAT, 0) + "/" + PATH_B);

		doQeury(false);
	}
}
