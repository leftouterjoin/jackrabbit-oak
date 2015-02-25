/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.index.lucene;

import static org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants.ANALYZERS;
import static org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants.ANL_DEFAULT;

import java.util.Collections;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.query.AbstractQueryTest;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class LuceneIndexQueryJpTest extends AbstractQueryTest {
    @Override
    protected void createTestIndexNode() throws Exception {
        Tree index = root.getTree("/");
        Tree indexDefn = createTestIndexNode(index, LuceneIndexConstants.TYPE_LUCENE);
        TestUtil.useV2(indexDefn);
        indexDefn.setProperty(LuceneIndexConstants.TEST_MODE, true);
        indexDefn.setProperty(LuceneIndexConstants.EVALUATE_PATH_RESTRICTION, true);
        indexDefn.addChild(ANALYZERS).addChild(ANL_DEFAULT).setProperty("class", "org.apache.lucene.analysis.ja.JapaneseAnalyzer");//★

        Tree props = TestUtil.newRulePropTree(indexDefn, "nt:base");
        TestUtil.enableForFullText(props, LuceneIndexConstants.REGEX_ALL_PROPS, true);

        root.commit();
    }

    @Override
    protected ContentRepository createRepository() {
        LowCostLuceneIndexProvider provider = new LowCostLuceneIndexProvider();
        return new Oak().with(new InitialContent())
                .with(new OpenSecurityProvider())
                .with((QueryIndexProvider) provider)
                .with((Observer) provider)
                .with(new LuceneIndexEditorProvider())
                .createContentRepository();
    }

    // 簡易的な。。
    @Test
    public void testTokenizeJPN() throws Exception {
        Tree t = root.getTree("/").addChild("containsJP");
        Tree one = t.addChild("one");
        one.setProperty("t", "This is a sample program to test lucene and Kuromoji.私は東京都の国会議事堂に行きました");
        root.commit();
        // matches
        assertQuery("//*[jcr:contains(., 'this')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., 'kUrOmoJI.')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '私の')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '東京')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '国会')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '私は、東京都の国会議事堂に行きました。')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '東京都 国会 議事堂')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '東京都　国会　議事堂')]", XPATH,
                ImmutableList.of(one.getPath()));
        assertQuery("//*[jcr:contains(., '行く')]", XPATH,
                ImmutableList.of(one.getPath()));
        // no matches
        assertQuery("//*[jcr:contains(., '京都')]", XPATH,
        		Collections.<String>emptyList());
        assertQuery("//*[jcr:contains(., '国')]", XPATH,
        		Collections.<String>emptyList());
        assertQuery("//*[jcr:contains(., 'の')]", XPATH,
        		Collections.<String>emptyList());
        assertQuery("//*[jcr:contains(., '会議')]", XPATH,
        		Collections.<String>emptyList());
        assertQuery("//*[jcr:contains(., '議事')]", XPATH,
        		Collections.<String>emptyList());
    }

    @Test
    public void testTokenizeJPNSQL() throws Exception {
        Tree t = root.getTree("/").addChild("containsJPSQL");
        Tree one = t.addChild("one");
        one.setProperty("t", "This is a sample program to test lucene and Kuromoji.私は東京都の国会議事堂に行きました");
        root.commit();
        // matches
        assertQuery("select * from [nt:base] as a where contains(*, '東京')", SQL2,
                ImmutableList.of(one.getPath()));
        // no matches
        assertQuery("select * from [nt:base] as a where contains(*, '京都')", SQL2,
        		Collections.<String>emptyList());
    }
}
