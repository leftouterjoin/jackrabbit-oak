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

Jackrabbit Oak - the next generation content repository
Jackrabbit Oak - 次世代コンテンツリポジトリ
=======================================================

Jackrabbit Oak is an effort to implement a scalable and performant hierarchical content repository
for use as the foundation of modern world-class web sites and other demanding content applications.
The Oak effort is a part of the [Apache Jackrabbit project](http://jackrabbit.apache.org/). Apache
Jackrabbit is a project of the [Apache Software Foundation](http://www.apache.org/).

Jackrabbit Oakは、現代の世界クラスのウェブサイトやその他の要求の厳しいコンテンツアプリケーションの
基盤として使用するための拡張性とパフォーマンスの高い階層的なコンテンツ・リポジトリを実装するための取り組みです。

Why Oak
何故Oakか
-------

Jackrabbit 2.x is a solid and feature-rich content repository that works well especially for the
needs of traditional web sites and integrated content management applications. However, the trends
in user expectations (especially for personalized, interactive and collaborative content),
application architectures (distributed, loosely coupled, multi-platform solutions with lots of data)
and hardware design (horizontal rather than vertical scaling) have rendered some of the original
Jackrabbit design decisions (which date back almost a decade) obsolete and there is no easy way to
incrementally update the design.

Jackrabbit 2.x は従来のWebサイトと統合されたコンテンツ管理アプリケーションのニーズに特に適していますソリッドで機能豊富なコンテンツリポジトリです。
しかし、ユーザの期待の動向(特に、パーソナライズされたインタラクティブと共同コンテンツ用)、
アプリケーション·アーキテクチャと(むしろ垂直スケーリングより水平)ハードウェア設計
(データの多くが付いて分布し、疎結合された、マルチプラットフォームソリューション)の一部をレンダリングしています
オリジナルJackrabbitの設計廃止された(バックほぼ10年を日付)決定やインクリメンタルなデザインを更新する簡単な方法はありません。

Jackrabbit Oak aims to implement a scalable and performant hierarchical content repository for use
as the foundation of modern world-class web sites and other demanding content applications. The
repository should implement standards like JCR, WebDAV and CMIS, and be easily accessible from
various platforms, especially from JavaScript clients running in modern browser environments. The
implementation should provide more out-of-the-box functionality than typical NoSQL databases while
achieving comparable levels of scalability and performance.

Jackrabbit Oakは、現代の世界クラスのウェブサイトやその他の要求の厳しいコンテンツアプリケーションの
基盤として使用するための拡張性とパフォーマンスの高い階層的なコンテンツリポジトリを実装することを目指しています。
リポジトリは、特に現代のブラウザ環境で実行しているJavaScriptクライアントから、 JCR 、 WebDAVおよびCMISのような標準規格を実装し、さまざまなプラットフォームから簡単にアクセスできる必要があります。
スケーラビリティとパフォーマンスの同等のレベルを達成しながら、実装は、典型的なNoSQLデータベースよりもすぐに使える機能を提供する必要があります。

Work in Progress
進行中の作業
----------------
This documentation is still work in progress. Currently much of the information on Oak is
somewhat spread over different places. If you don't find something here you could also try

このドキュメントは、まだ進行中の作業です。
現在、オーク上の情報の多くは、多少異なる場所に散らばっている。
もし、何か見つけられない場合は、次を試してください。

* the [Oak JIRA](https://issues.apache.org/jira/browse/OAK), specifically [OAK-14]
  (https://issues.apache.org/jira/browse/OAK-14), which lists the known backward compatibility issues,
* the [Oak development list](http://jackrabbit.markmail.org/search/+list:org.apache.jackrabbit.oak-dev),
* the [README files](https://github.com/apache/jackrabbit-oak/blob/trunk/README.md),
* the [Oak presentation](http://goo.gl/zid8V3)
  from the .adaptTo conference 2012,
* [Oak, the architecture of Apache Jackrabbit 3](http://www.slideshare.net/jukka/oak-the-architecture-of-apache-jackrabbit-3).
* [Oak, the architecture of the new Repository](http://www.slideshare.net/MichaelDrig/oak-39377061)
  