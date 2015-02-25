<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
  -->

The Query Engine
## クエリエンジン

Oak does not index content by default as does Jackrabbit 2. You need to create custom 
indexes when necessary, much like in traditional RDBMSs. If there is no index for a 
specific query, then the repository will be traversed. That is, the query will still 
work but probably be very slow.  
Oakはデフォルトでは、Jackrabbit 2と異なりインデックス化を行ないません。
伝統的なRDBMSsと同様にカスタムインデックスを必要に応じて作成する必要があります。
インデックスが存在しない場合、特定のクエリはリポジトリをトラバースします。
すなわち、そのクエリは動くでしょうが恐らく非常に遅くなります。

Query Indices are defined under the `oak:index` node.  
クエリインデックスは`oak:index`ノード下に定義されます。

Compatibility
### 互換性

Quoting
#### クォート

The query parser is now generally more strict about invalid syntax.
The following query used to work in Jackrabbit 2.x, but not in Oak,
because multiple way to quote the path are used at the same time:  
クエリパーサは現在、無効な構文について、一般的に、より厳格です。
次のようなクエリはJackrabbit 2.xで動いていましたがOakでは動きません。
パスをクォートするために複数の方法を同時に使用しているためです。

    SELECT * FROM [nt:base] AS s 
    WHERE ISDESCENDANTNODE(s, ["/libs/sling/config"])
    
Instead, the query now needs to be:
代わりにクエリは以下のようにする必要があります:

    SELECT * FROM [nt:base] AS s 
    WHERE ISDESCENDANTNODE(s, [/libs/sling/config])
    
Equality for Path Constraints
#### パス制約の等価性

In Jackrabbit 2.x, the following condition was interpreted as a LIKE condition:  
Jackrabbit 2.xでは、以下の条件がLIKE条件と解釈されていました。

    SELECT * FROM nt:base WHERE jcr:path = '/abc/%'
    
Therefore, the query behaves exactly the same as if LIKE was used.
In Oak, this is no longer the case, and such queries search for an exact path match.  
そのため、クエリは、 LIKEを使用した場合とまったく同じように動作します。
Oakでは、このようなことはありません、そして、このようなクエリは、正確なパス一致で検索します。

    
Slow Queries and Read Limits
### スロークエリと読み込み制限

Slow queries are logged as follows:  
スロークエリは次のようにロギングされます。

    *WARN* Traversed 1000 nodes with filter Filter(query=select ...)
    consider creating an index or changing the query

If this is the case, an index might need to be created, or the condition 
of the query might need to be changed to take advantage of an existing index.  
この場合、インデックスを作成される必要がある場合があり、またはクエリの条件を、既存のインデックスを利用するために変更する必要があります。

If a query reads more than 10 thousand nodes in memory, then the query is cancelled
with an UnsupportedOperationException saying that 
"The query read more than 10000 nodes in memory. To avoid running out of memory, processing was stopped."
As a workaround, this limit can be changed using the system property "oak.queryLimitInMemory".  
クエリがメモリ内に複数の1万ノードを読み取る場合、クエリは"The query read more than 10000 nodes in memory. To avoid running out of memory, processing was stopped."
というUnsupportedOperationExceptionとともにキャンセルされます。
回避策として、この制限は"oak.queryLimitInMemory"システムプロパティを使用して変更できます。

If a query traversed more than 100 thousand nodes (for example because there is no index
at all and the whole repository is traversed), then the query is cancelled
with an UnsupportedOperationException saying that 
"The query read or traversed more than 10000 nodes. To avoid affecting other tasks, processing was stopped.".
As a workaround, this limit can be changed using the system property "oak.queryLimitReads".  
クエリは、10万以上のノードをトラバースした場合(インデックスが無い場合、リポジトリ全体をトラバースするという例)、
クエリは"The query read or traversed more than 10000 nodes. To avoid affecting other tasks, processing was stopped."
というUnsupportedOperationExceptionとともにキャンセルされます。
回避策として、この制限は"oak.queryLimitReads"システムプロパティを使用して変更できます。

Full-Text Queries
### フルテキストクエリ

The full-text syntax supported by Jackrabbit Oak is a superset of the JCR specification.
The following syntax is supported within `contains` queries:  
Jackrabbit Oakでサポートするフルテキスト構文は、JCR仕様をサポートしています。
構文は、`contains`クエリの使用をサポートしています。

    FullTextSearch ::= Or
    Or ::= And { ' OR ' And }* 
    And ::= Term { ' ' Term }*
    Term ::= ['-'] { SimpleTerm | PhraseTerm } [ '^' Boost ]
    SimpleTerm ::= Word
    PhraseTerm ::= '"' Word { ' ' Word }* '"'
    Boost ::= <number>
    
Please note that `OR` needs to be written in uppercase.
Characters within words can be escaped using a backslash.  
`OR`はお大文字で記述する事に注意して下さい。
文字はバックスラッシュでエスケープ可能です。

Examples:  
例:

    jcr:contains(., 'jelly sandwich^4')
    jcr:contains(@jcr:title, 'find this')
    
In the first example, the word "sandwich" has weight four times more than the word "jelly."
For details of boosting, see the Apache Lucene documentation about Score Boosting.  
最初の例では、"sandwich"は"jelly"という文字の4倍の重みを持っています。
重み付けについての詳細では、点数重み付けについてのApache Lucene documentationを参照してください。

For compatibility with Jackrabbit 2.x, single quoted phrase queries are currently supported.
That means the query `contains(., "word ''hello world'' word")` is supported.
New applications should not rely on this feature.  
Jackrabbit 2.xとの互換性のため、シングルクォートフレーズ検索は現在サポートされています。
これは、`contains(., "word ''hello world'' word")`がサポートされていることを意味します。
新規のアプリケーションは使用しないで下さい。

Native Queries
### ネイティブクエリ

To take advantage of features that are available in full-text index implementations
such as Apache Lucene and Apache Lucene Solr, so called `native` constraints are supported.
Such constraints are passed directly to the full-text index. This is supported
for both XPath and SQL-2. For XPath queries, the name of the function is `rep:native`,
and for SQL-2, it is `native`. The first parameter is the index type (currently supported
are `solr` and `lucene`). The second parameter is the native search query expression.
For SQL-2, the selector name (if needed) is the first parameter, just before the language.
Examples:  
フルテキストインデックスの実装であるApache LuceneやApache Lucene Solrを利用するために、`native`制約がサポートされています。
この制約は、フルテキストインデックスにダイレクトパスします。
これは、XPathとSQL-2の両方をサポートしています。
XPathクエリでは、機能名は`rep:native`で、SQL-2では`native`です。
最初のパラメータはインデックスタイプ(現在のサポートは`solr`と`lucene`)です。
２つ目のパラメータはネイティブ検索クエリ表現です。
SQL-2では、必要な場合セレクタ名が最初のパラメータで、言語の前に来ます。
例:

    //*[rep:native('solr', 'name:(Hello OR World)')]
    
    select [jcr:path] from [nt:base] 
    where native('solr', 'name:(Hello OR World)')

    select [jcr:path] from [nt:base] as a 
    where native(a, 'solr', 'name:(Hello OR World)')

This also allows to use the Solr [MoreLikeThis](http://wiki.apache.org/solr/MoreLikeThis)
feature. An example query is:  
また、Solr[MoreLikeThis](http://wiki.apache.org/solr/MoreLikeThis)機能を使用することもできます。クエリの例:

    select [jcr:path] from [nt:base] 
    where native('solr', 'mlt?q=id:UTF8TEST&mlt.fl=manu,cat&mlt.mindf=1&mlt.mintf=1')

If no full-text implementation is available, those queries will fail.  
フルテキストの実装が利用可能でない場合、これらのクエリは失敗します。

Similarity Queries
### 類似クエリ

Oak supports similarity queries when using the Lucene full-text index. 
For example, the following query will return nodes that have similar content than
the node /test/a:  
Oakは、Luceneフルテキストインデックスを使用した場合、類似クエリをサポートします。
例として、次のようなクエリは/test/aノード同様のコンテンツを持つノードを返します:

    //element(*, nt:base)[rep:similar(., '/test/a')]
    
Compared to Jackrabbit 2.x, support for rep:similar has the following limitations:
Full-text aggregation is not currently supported.  
Jackrabbit 2.xと比較すると、rep:similarサポートはフルテキストアグリゲーションを現在はサポートしないという制限事項があります。

XPath to SQL2 Transformation
### XPathからSQL2への変形

To support the XPath query language, such queries are internally converted to SQL2.  
XPathクエリ言語のサポートのために、内部的にSQL2に変換されます。

Every conversion is logged in `debug` level under the 
`org.apache.jackrabbit.oak.query.QueryEngineImpl` logger:  
`org.apache.jackrabbit.oak.query.QueryEngineImpl`ロガーが`debug`レベルでは、全変換をロギングします:

    org.apache.jackrabbit.oak.query.QueryEngineImpl Parsing xpath statement: 
        //element(*)[@sling:resourceType = 'slingevent:Lock')]
    org.apache.jackrabbit.oak.query.QueryEngineImpl XPath > SQL2: 
        select [jcr:path], [jcr:score], * from [nt:base] as a 
        where [sling:resourceType] = 'slingevent:Lock' 
        /* xpath: //element(*)[@sling:resourceType = 'slingevent:Lock' 
        and @lock.created < xs:dateTime('2013-09-02T15:44:05.920+02:00')] */

_Each transformed SQL2 query contains the original XPath query as a comment._  
_SQL2クエリ変形ごとに元のXPathクエリをコメントとして含みます。_

When converting from XPath to SQL-2, `or` conditions are automatically converted to
`union` queries, so that indexes can be used for conditions of the form 
`a = 'x' or b = 'y'`.  
XPathからSQL-2への変換時、`or`条件は自動的に`union`クエリに変換されます。これは、`a = 'x' or b = 'y'`形式でインデックスを利用可能にするためです。

Query Processing
### クエリ処理

Internally, the query engine uses a cost based query optimizer that asks all the available
query indexes for the estimated cost to process the query. It then uses the index with the 
lowest cost.  
内部的に、クエリエンジンは、クエリを処理するための推定コストのために利用可能なすべてのクエリー·インデックスを要求し、コストベースのクエリオプティマイザを使用しています。

By default, the following indexes are available:  
デフォルトでは、次のインデックスが利用可能です:

* A property index for each indexed property.  
各インデックス付きプロパティのプロパティインデックス
* A full-text index which is based on Apache Lucene / Solr.  
Apache Lucene / Solrベースのフルテキストインデックス
* A node type index (which is based on an property index for the properties
  jcr:primaryType and jcr:mixins).  
ノードタイプインデックス(jcr:primaryTypeとjcr:mixinsプロパティのプロパティインデックスベース)
* A traversal index that iterates over a subtree.  
サブツリー上を反復するトラバーサルインデックス

If no index can efficiently process the filter condition, the nodes in the repository are 
traversed at the given subtree.  
フィルタ条件処理に有効なインデックスが無い場合、与えられたサブツリーでリポジトリのノードがトラバースされます。

Usually, data is read from the index and repository while traversing over the query 
result. There are exceptions however, where all data is read in memory when the query
is executed: when using a full-text index, and when using an "order by" clause.  
通常、クエリ結果上をトラバースしながら、データはインデックスとリポジトリから読み取られます。
例外として、フルテキストインデックス使用時と"order by"節の使用時、クエリ実行時に全データがメモリに読み込まれるケースがあります。

<a name="property-index"></a>
The Property Index
### プロパティインデックス

Is useful whenever there is a query with a property constraint that is not full-text:  
フルテキストでないプロパティ制約を伴うクエリには有用です:

    SELECT * FROM [nt:base] WHERE [jcr:uuid] = $id

To define a property index on a subtree you have to add an index definition node that:  
サブツリーでプロパティインデックスを定義するためには、インデックス定義ノードを追加追加する必要があります:

* must be of type `oak:QueryIndexDefinition`  
`oak:QueryIndexDefinition`タイプである事
* must have the `type` property set to __`property`__  
__`property`__が設定された`type`プロパティを持つ事
* contains the `propertyNames` property that indicates what properties will be stored in the index.
  `propertyNames` can be a list of properties, and it is optional.in case it is missing, the node name will be used as a property name reference value  
保管されるインデックスを示す`propertyNames`プロパティを含む事。
  オプションとして`propertyNames`はプロパティのリストである事が可能です。不足するケースでは、ノード名がプロパティ名の参照値として使用されます。

_Optionally_ you can specify  
_オプション_設定

* a uniqueness constraint on a property index by setting the `unique` flag to `true`  
プロパティインデックス上のユニーク制約は、`unique`フラグを`true`に設定
* that the property index only applies to a certain node type by setting the `declaringNodeTypes` property  
プロパティインデックスは`declaringNodeTypes`プロパティを設定する事により、特定のノードタイプに適用されます
* the `entryCount` (a long), which is used for the cost estimation (a high entry count means a high cost)  
`entryCount`(long)はコスト推定に使用されます(高エントリーカウントは高コストを意味します)
* the `reindex` flag which when set to `true`, triggers a full content re-index.  
`reindex`フラグを`true`に設定すれば、フルコンテンツの再インデックスをトリガします

Example:  
例:

    {
      NodeBuilder index = root.child("oak:index");
      index.child("uuid")
        .setProperty("jcr:primaryType", "oak:QueryIndexDefinition", Type.NAME)
        .setProperty("type", "property")
        .setProperty("propertyNames", "jcr:uuid")
        .setProperty("declaringNodeTypes", "mix:referenceable")
        .setProperty("unique", true)
        .setProperty("reindex", true);
    }

or to simplify you can use one of the existing `IndexUtils#createIndexDefinition` helper methods:  
または、簡単にするために`IndexUtils#createIndexDefinition`ヘルパーメソッドを使用できます。

    {
      NodeBuilder index = IndexUtils.getOrCreateOakIndex(root);
      IndexUtils.createIndexDefinition(index, "myProp", true, false, ImmutableList.of("myProp"), null);
    }

__Note on `propertyNames`__ Adding a property index definition that contains two or more properties  will only
include nodes that have _all_ specified properties present. This is different than adding a dedicated property
index for each and letting the query engine make use of them.  
__`propertyNames`の注意__ 2つ以上のプロパティを含むプロパティインデックス定義の追加において、全てが存在する特定のプロパティであるノードのみ含む事ができます。
これは、個別のプロパティ·インデックスを追加し、それらのクエリエンジンの使用させようとするのとは異なります。

__Note__ Is is currently not possible to add more than one property index on the same property name, even if it
might be used in various combinations with other property names. This rule is not enforced in any way, but the
behavior is undefined, one of the defined indexes will be updated while the others will simply be ignored by the
indexer which can result in empty result sets at query time.  
__注意__ Isは、それが他のプロパティ名と様々な組み合わせで使用されるかもしれない場合でも、同じプロパティ名を複数のプロパティのインデックスを追加することが出来ません。
このルールはどのような方法で強制されていませんが、動作は定義されておらず、インデックスの内の一つは、他がアップデート中である時である場合、インデクサによって単純に無視され、クエリ時に空の結果セットをもたらすだろう。

Reindexing
#### 再インデックス化

Reindexing a property index happens synchronously by setting the __`reindex`__ flag to __`true`__. This means that the 
first #save call will generate a full repository traversal with the purpose of building the index content and it might
take a long time.  
プロパティインデックスの再インデックス化は、__`reindex`__フラグを__`true`__に設定する事により同期起動されます。
これは、最初の#save呼び出しが、インデックス構築のための完全リポジトリトラバースを生み、長時間要する可能性があります。

Asynchronous reindexing of a property index is available as of OAK-1456. The way this works is by pushing the property 
index updates to a background job and when the indexing process is done, the property definition will be switched back 
to a synchronous updates mode.
To enable this async reindex behaviour you need to first set the __`reindex-async`__ and __`reindex`__ flags to 
__`true`__ (call #save). You can verify the initial setup worked by refreshing the index definition node and looking
for the __`async`__ = __`async-reindex`__ property.
Next you need to start the dedicated background job via a jmx call to the 
__`PropertyIndexAsyncReindex#startPropertyIndexAsyncReindex`__ MBean.  

プロパティインデックスの非同期再インデックス化はOAK-1456で可能となりました。
この作業の方法は、バックグラウンドジョブへのプロパティインデックスアップデートのプッシュと
インデックス化処理の完了時、プロパティ定義は同期アップデートモードへ切り替わります。
非同期インデックス化動作を利用するためには、最初に__`reindex-async`__と__`reindex`__フラグを__`true`__に設定し(#saveを呼び)ます。
__`async`__ = __`async-reindex`__のプロパティを探して、インデックス定義ノードのリフレッシュ動作後に初期設定を確認する事ができます。
次に、jmx経由の__`PropertyIndexAsyncReindex#startPropertyIndexAsyncReindex`__ MBean呼び出しで、専用のバックグラウンドジョブを開始する必要があります。

Example:  
例:

    {
      NodeBuilder index = root.child("oak:index");
      index.child("property")
        .setProperty("reindex-async", true)
        .setProperty("reindex", true);
    }

The Ordered Index
### オーダードインデックス

Extension of the Property index will keep the order of the indexed
property persisted in the repository.  
プロパティインデクスの拡張で、インデックス済みプロパティの順序をリポジトリ内で永続化します。

Used to speed-up queries with `ORDER BY` clause, _equality_ and
_range_ ones.  
`ORDER BY`節、_イコール_と範囲のクエリをスピードアップします。

    SELECT * FROM [nt:base] ORDER BY jcr:lastModified
    
    SELECT * FROM [nt:base] WHERE jcr:lastModified > $date
    
    SELECT * FROM [nt:base] WHERE jcr:lastModified < $date
    
    SELECT * FROM [nt:base]
    WHERE jcr:lastModified > $date1 AND jcr:lastModified < $date2

    SELECT * FROM [nt:base] WHERE [jcr:uuid] = $id

To define a property index on a subtree you have to add an index
definition node that:  
サブツリー上でプロパティインデックスを定義するためには、インデックス定義ノードを追加する必要があります:

* must be of type `oak:QueryIndexDefinition`  
`oak:QueryIndexDefinition`タイプである事
* must have the `type` property set to __`ordered`__  
`type`プロパティが__`ordered`__に設定される事
* contains the `propertyNames` property that indicates what properties
  will be stored in the index.  `propertyNames` has to be a single
  value list of type `Name[]`  
保管されるインデックスを示す`propertyNames`プロパティを含む事。  
`propertyNames`は`Name[]`タイプの単一リスト値である事

_Optionally_ you can specify  
_オプション_設定

* the `reindex` flag which when set to `true`, triggers a full content
  re-index.  
`reindex`フラグを`true`に設定し、フルコンテンツ再インデックス化をトリガします。
* The direction of the sorting by specifying a `direction` property of
  type `String` of value `ascending` or `descending`. If not provided
  `ascending` is the default.  
ソート方向を、`ascending`または`descending`値で、`String`タイプの`direction`プロパティに設定します。
指定しない場合のデフォルトは`ascending`です。
* The index can be defined as asynchronous by providing the property
  `async=async`  
インデックスは、`async=async`プロパティによって非同期として定義する事ができます。

_Caveats_  
_注意事項_

* In case deploying on the index on a clustered mongodb you have to
  define it as asynchronous by providing `async=async` in the index
  definition. This is to avoid cluster merges.  
クラスタ化したmongodbでこのインデックスを配備している場合、インデックス定義の際、`async=async`で非同期として定義する必要があります。
これはクラスタマージを避けるためです。

The Lucene Index
### Luceneインデックス

Refer to [Lucene Index](lucene.html) for details.  
詳細は、[Lucene Index](lucene_JP.html)を参照して下さい。

The Solr Index
### Solrインデックス

The Solr index is mainly meant for full-text search (the 'contains' type of queries):  
Solrインデックスは主にフルテキスト検索のためのものです(クエリの'contains'タイプ):

    //*[jcr:contains(., 'text')]

but is also able to search by path, property restrictions and primary type restrictions.
This means the Solr index in Oak can be used for any type of JCR query.  
それだけでなく、パス検索、プロパティの制約とプライマリタイプの制約で検索することができます。
これは、OakのSolrインデックスがJCRクエリの任意タイプに使用できる事を意味します。

Even if it's not just a full-text index, it's recommended to use it asynchronously (see `Oak#withAsyncIndexing`)
because, in most production scenarios, it'll be a 'remote' index, and therefore network eventual latency / errors would 
have less impact on the repository performance.
To set up the Solr index to be asynchronous that has to be defined inside the index definition, see [OAK-980](https://issues.apache.org/jira/browse/OAK-980)  
単にフルテキストインデックスではなく、非同期的に使用する事を推奨します(`Oak#withAsyncIndexing`参照)
なぜならば、多くの本番シナリオでは、'リモート'インデックスであるため、ネットワークのレイテンシーやエラーはリポジトリパフォーマンスにより影響を与えるであろうからです。
Solrインデックスのセットアップに非同期にすべきで、インデックス定義内で定義されなければなりません。[OAK-980](https://issues.apache.org/jira/browse/OAK-980)を参照して下さい。

(訳注:
[Oak#withAsyncIndexing](http://jackrabbit.apache.org/oak/docs/apidocs/org/apache/jackrabbit/oak/Oak.html#withAsyncIndexing%28%29)  
Enable the asynchronous (background) indexing behavior. Please not that when enabling the background indexer, you need to take care of calling #shutdown on the executor provided for this Oak instance.  
非同期(バックグラウンド)インデックス作成動作を有効にします。バックグラウンドインデクサを有効時には無効にして下さい。Oakインスタンスを提供するエグゼキュータの#shutdown呼び出しに注意する必要があります。)


TODO Node aggregation.  
TODO ノード集約

Index definition for Solr index
##### Solrインデックスのインデックス定義
<a name="solr-index-definition"></a>
The index definition node for a Solr-based index:  
Solrベースインデックスのインデックス定義ノード:

 * must be of type `oak:QueryIndexDefinition`  
`oak:QueryIndexDefinition`タイプである事
 * must have the `type` property set to __`solr`__  
`type`プロパティが__`solr`__に設定される事
 * must contain the `async` property set to the value `async`, this is what sends the
index update process to a background thread.  
`async`値が設定された`async`プロパティを含むこと。これはバックグラウンドスレッドでインデックス更新を処理するものです。

_Optionally_ one can add
_オプション_設定

 * the `reindex` flag which when set to `true`, triggers a full content re-index.  
`reindex`フラグを`true`に設定し、フルコンテンツ再インデックス化をトリガします。

Example:  
例:

    {
      NodeBuilder index = root.child("oak:index");
      index.child("solr")
        .setProperty("jcr:primaryType", "oak:QueryIndexDefinition", Type.NAME)
        .setProperty("type", "solr")
        .setProperty("async", "async")
        .setProperty("reindex", true);
    }
    
Setting up the Solr server
#### Solrサーバの設定
For the Solr index to work Oak needs to be able to communicate with a Solr instance / cluster.
Apache Solr supports multiple deployment architectures:  
OakでSolrインデックスを動作させるためには、Solrインスタンス/クラスタと通信できる必要があります。
Apache Solrは複数のデプロイメント·アーキテクチャをサポートしています。

 * embedded Solr instance running in the same JVM the client runs into  
クライアントが実行されるJVMと同じ組込Solrインスタンス
 * single remote instance  
シングルリモートインスタンス
 * master / slave architecture, eventually with multiple shards and replicas  
複数のシャードやレプリカで構成される、マスタ/スレーブアーキテクチャ
 * SolrCloud cluster, with Zookeeper instance(s) to control a dynamic, resilient set of Solr servers for high 
 availability and fault tolerance  
動的制御のためのZookeeperインスタンスによるSolrCloudクラスタ、高可用で対障害性の弾力性のあるSolrサーバセット

The Oak Solr index can be configured to either use an 'embedded Solr server' or a 'remote Solr server' (being able to 
connect to a single remote instance or to a SolrCloud cluster via Zookeeper).  
Oak Solrインデックスは、'embedded Solr server'または'remote Solr server'(Zookeeper経由でSolrCloudクラスタのシングルリモートインスタンスに接続が可能)の何れかで構成可能です

OSGi environment
##### OSGi環境
All the Solr configuration parameters are described in the 'Solr Server Configuration' section on the 
[OSGi configuration](osgi_config.html) page.

Create an index definition for the Solr index, as described [above](#solr-index-definition).
Once the query index definition node has been created, access OSGi ConfigurationAdmin via e.g. Apache Felix WebConsole:

 1. find the 'Oak Solr indexing / search configuration' item and eventually change configuration properties as needed
 2. find either the 'Oak Solr embedded server configuration' or 'Oak Solr remote server configuration' items depending 
 on the chosen Solr architecture and eventually change configuration properties as needed
 3. find the 'Oak Solr server provider' item and select the chosen provider ('remote' or 'embedded') 

Solr server configurations
##### Solrサーバ構成
Depending on the use case, different Solr server configurations are recommended.  
ユースケースに応じたSolrサーバの構成を推奨します。

Embedded Solr server
###### 組込Solrサーバ
The embedded Solr server is recommended for developing and testing the Solr index for an Oak repository. With that an 
in-memory Solr instance is started in the same JVM of the Oak repository, without HTTP bindings (for security purposes 
as it'd allow HTTP access to repository data independently of ACLs). 
Configuring an embedded Solr server mainly consists of providing the path to a standard [Solr home dir](https://wiki.apache.org/solr/SolrTerminology) 
(_solr.home.path_ Oak property) to be used to start Solr; this path can be either relative or absolute, if such a path 
would not exist then the default configuration provided with _oak-solr-core_ artifact would be put in the given path.
To start an embedded Solr server with a custom configuration (e.g. different schema.xml / solrconfig.xml than the default
 ones) the (modified) Solr home files would have to be put in a dedicated directory, according to Solr home structure, so 
 that the solr.home.path property can be pointed to that directory.  
Solrサーバは開発とテスト時のOakリポジトリでのSolrインデックスに推奨されます。

Single remote Solr server
###### シングルリモートSolrサーバ
A single (remote) Solr instance is the simplest possible setup for using the Oak Solr index in a production environment. 
Oak will communicate to such a Solr server through Solr's HTTP APIs (via [SolrJ](http://wiki.apache.org/solr/Solrj) client).
Configuring a single remote Solr instance consists of providing the URL to connect to in order to reach the [Solr core]
(https://wiki.apache.org/solr/SolrTerminology) that will host the Solr index for the Oak repository via the _solr.http.url_
 property which will have to contain such a URL (e.g. _http://10.10.1.101:8983/solr/oak_). 
All the configuration and tuning of Solr, other than what's described in 'Solr Server Configuration' section of the [OSGi 
configuration](osgi_config.html) page, will have to be performed on the Solr side; [sample Solr configuration]
 (http://svn.apache.org/viewvc/jackrabbit/oak/trunk/oak-solr-core/src/main/resources/solr/) files (schema.xml, 
 solrconfig.xml, etc.) to start with can be found in _oak-solr-core_ artifact.  
シングルリモートSolrサーバインスタンスは、本番環境でOak Solrのインデックスを使用するための最も簡単な設定です。

SolrCloud cluster
###### SolrCloudクラスタ
A [SolrCloud](https://cwiki.apache.org/confluence/display/solr/SolrCloud) cluster is the recommended setup for an Oak 
Solr index in production as it provides a scalable and fault tolerant architecture.
In order to configure a SolrCloud cluster the host of the Zookeeper instance / ensemble managing the Solr servers has 
to be provided in the _solr.zk.host_ property (e.g. _10.1.1.108:9983_) since the SolrJ client for SolrCloud communicates 
directly with Zookeeper.
The [Solr collection](https://wiki.apache.org/solr/SolrTerminology) to be used within Oak is named _oak_, having a replication
 factor of 2 and using 2 shards; this means in the default setup the SolrCloud cluster would have to be composed by at 
 least 4 Solr servers as the index will be split into 2 shards and each shard will have 2 replicas. Such parameters can 
 be changed, look for the 'Oak Solr remote server configuration' item on the [OSGi configuration](osgi_config.html) page.
SolrCloud also allows the hot deploy of configuration files to be used for a certain collection so while setting up the 
 collection to be used for Oak with the needed files before starting the cluster, configuration files can also be uploaded 
 from a local directory, this is controlled by the _solr.conf.dir_ property of the 'Oak Solr remote server configuration'.
For a detailed description of how SolrCloud works see the [Solr reference guide](https://cwiki.apache.org/confluence/display/solr/SolrCloud).  
[SolrCloud](https://cwiki.apache.org/confluence/display/solr/SolrCloud)クラスタは、スケーラブルで対障害性アーキテクチャの本番環境におけるOak Solr インデックスで推奨の設定です。

Differences with the Lucene index
#### Luceneインデックスとの違い
As of Oak version 1.0.0:  
Oak version 1.0.0時点:

* Solr index doesn't support search using relative properties, see [OAK-1835](https://issues.apache.org/jira/browse/OAK-1835).  
Solrインデックスは関連プロパティを使用した検索をサポートしません。[OAK-1835](https://issues.apache.org/jira/browse/OAK-1835)を参照して下さい。
* Solr configuration is mostly done on the Solr side via schema.xml / solrconfig.xml files.  
Solr設定は主にschema.xml / solrconfig.xmlファイルを経由しSolr側で行われます。
* Lucene can only be used for full-text queries, Solr can be used for full-text search _and_ for JCR queries involving
path, property and primary type restrictions.  
Luceneはフルテキストクエリを可能にするのみですが、Solrはフルテキストサーチに加え、関連するパス、プロパティ、プライマリタイプ制約などのJCRクエリも可能です。

The Node Type Index
### ノードタイプインデックス

The `NodeTypeIndex` implements a `QueryIndex` using `PropertyIndexLookup`s on `jcr:primaryType` `jcr:mixinTypes` to evaluate a node type restriction on the filter.
The cost for this index is the sum of the costs of the `PropertyIndexLookup` for queries on `jcr:primaryType` and `jcr:mixinTypes`.  
フィルター上のノードタイプの制限を評価するために`jcr:primaryType` `jcr:mixinTypes`の`PropertyIndexLookup`を使用して` QueryIndex`を実装しています。
このインデックスのためのコストは、 `jcr:primaryType`と`jcr:mixinTypes`上のクエリの`PropertyIndexLookup`のコストの合計です。

Cost Calculation
### コスト計算

Each query index is expected to estimate the worst-case cost to query with the given filter. 
The returned value is between 1 (very fast; lookup of a unique node) and the estimated number of entries to traverse, if the cursor would be fully read, and if there could in theory be one network round-trip or disk read operation per node (this method may return a lower number if the data is known to be fully in memory).  
各インデックスのクエリは、指定されたフィルタでクエリするために、最悪の場合のコスト推定を期待する。
戻り値は、1から(最速: 一意ノードのルックアップ)からエントリーのトラバース推定値の間で、カーソルが完全に読み込まれるか理論的に存在することができれば1つのネットワーク·ラウンドトリップまたはノード毎のディスク読み出し(データがメモリに完全にあることが知られている場合は、この方法はより低い数値を返すことがあります)。

The returned value is supposed to be an estimate and doesn't have to be very accurate. Please note this method is called on each index whenever a query is run, so the method should be reasonably fast (not read any data itself, or at least not read too much data).  
戻り値は見積サポートのためでありそれほど正確である必要はない。 
この方式はクエリの実行の都度インデックス毎に呼び出される事に注意して下さい。だからこの方式は合理的に高速である必要があります(大量データを読み込みは行ないません)

If an index implementation can not query the data, it has to return `Double.POSITIVE_INFINITY`.
インデックス実装がデータをクエリできない場合、`Double.POSITIVE_INFINITY`を戻すことがあります。

Index storage and manual inspection
### インデックスストレージと手動監視

Sometimes there is a need to inspect the index content for debugging (or pure curiosity).
The index content is generally stored as content under the index definition as hidden nodes (this doesn't apply to the solr index).
In order to be able to browse down into an index content you need a low level repository tool that allows NodeStore level access.
There are currently 2 options: the oak-console (command line tool, works will all existing NodeStore implementations) and the oak-explorer
(gui based on java swing, works only on the TarMK), both available as run modes of the [oak-run](https://github.com/apache/jackrabbit-oak/blob/trunk/oak-run/README.md) module  
時々、デバッグ(または純粋な興味)のインデックスコンテンツを検査する必要がある事があります。
インデックスコンテンツは、一般的には(Solrインデックスを除く)隠しノードとしてインデックス定義の下にコンテンツとして保存されます。
インデックスコンテンツを閲覧できるようにするために、NodeStoreレベルアクセス可能な低レベルリポジトリツールが必要です。
現在2つの選択肢があります: oak-console(コマンドラインツールで、存在する全NodeStore実装を操作)とoak-explorer
(java swingベースのGUIで、TarMKでのみ動作)で、双方とも[oak-run](https://github.com/apache/jackrabbit-oak/blob/trunk/oak-run/README.md)モジュールで実行可能です。

The structure of the index is specific to each implementation and is subject to change. What is worth mentioning is that all the _*PropertyIndex_
flavors store the content as unstructured nodes (clear readable text), the _Lucene_ index is stored as binaries, so one would need to export the
entire Lucene directory to the local file system and browse it using a dedicated tool.  
インデックスの構造は、各実装固有で、変更されることがあります。
全ての_*プロパティインデックス_の良さは非構造化ノード(クリア可読テキスト)としてコンテンツ保管されている事、
_Lucene_ インデックスはバイナリ保管であるため、ローカルファイルシステムにLuceneディレクトリ全体をエクスポートし
専用ツールを使って閲覧する必要があるという事は、言及の価値があります。
