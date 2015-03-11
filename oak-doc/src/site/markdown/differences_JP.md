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

Backward compatibility
後方互換性
======================

Oak implements the JCR API and we expect most applications to work out of the box. However, the Oak
code base is very young and not yet on par with Jackrabbit 2. Some of the more obscure parts of JCR
are not (yet) implemented. If you encounter a problem running your application on Oak, please cross
check against Jackrabbit 2 before reporting an issue against Oak.  
Oak はJCR APIと、箱から出してすぐ動くアプリを実装しています。
しかしながら、Oak のコードベースはまだ非常に若くまだJackrabbit 2には及びません。
いくつかのJCRの曖昧な部分は、まだ実装されていません。
Oakをアプリケーションで動かしていて問題をみつけたら、Oakにissueを上げる前にJackrabbit 2とクロスチェックを行ってください。
 
Reporting issues
報告されている問題
================

If you encounter a problem where functionality is missing or Oak does not behave as expected please
check whether this is a [known change in behaviour](https://issues.apache.org/jira/browse/OAK-14) or
a [known issue](https://issues.apache.org/jira/browse/OAK). If in doubt ask on the [Oak dev list]
(http://oak.markmail.org/). Otherwise create a [new issue](https://issues.apache.org/jira/browse/OAK).  
機能の欠落や期待通り動かないという問題をみつけたら、[known change in behaviour](https://issues.apache.org/jira/browse/OAK-14) または
[known issue](https://issues.apache.org/jira/browse/OAK)をチェックしてみて下さい。
疑わしい場合はメーリングリスト [Oak dev list](http://oak.markmail.org/)で訪ねましょう。
解決しなければ [new issue](https://issues.apache.org/jira/browse/OAK)を作成して下さい。

Notable changes
注目すべき変更
===============

This section gives a brief overview of the most notable changes in Oak with respect to Jackrabbit 2.
These changes are generally caused by overall design decisions carefully considering the benefits
versus the potential backward compatibility issues.  
このセクションでは、 Jackrabbit 2に関して最も注目すべき変更について簡単な概要を紹介します。
これらの変更点は、潜在的な後方互換の問題についてのメリットを注意深く考慮し決定されたものです。

Session state and refresh behaviour
セッションステートとリフレッシュ動作
-----------------------------------

In Jackrabbit 2 sessions always reflects the latest state of the repository. With Oak a session
reflects a stable view of the repository from the time the session was acquired ([MVCC model](http://en.wikipedia.org/wiki/MVCC)). This is a fundamental design aspect for achieving the
distributed nature of an Oak repository. A rarely encountered side effect of this is that sessions
expose [write skew](architecture/transactional-model.html).  
Jackrabbit 2ではセッションは常にリポジトリの最新状態を反映していました。
Oak のセッションはセッション取得時のリポジトリについて静止視点を反映しています ([MVCC model](http://en.wikipedia.org/wiki/MVCC))。
これはOakリポジトリの分散性を達成するための基礎デザインの側面です。
これは、セッションがまれに[write skew](architecture/transactional-model.html)異常(訳注:２つのトランザクションで同じデータセットを読み込んでいて互い違いに更新を行うこと(不一致)。読み取り一貫性、Snapshot分離レベル、MVCCであるDB、OracleDatabaseなどに見られる)に遭遇するという副作用があります。

This change can cause subtle differences in behavior when two sessions perform modifications
relying on one session seeing the other session's changes. Oak requires explicit calls to
`Session.refresh()`in this case.  
この変更は、2つのセッションの変更操作時の相互作用においての振る舞いの微妙な違いとなります。
Oakではこの場合、明示的に`Session.refresh()`を呼び出す事ができます。

> *Note*: To ease migration to Oak, sessions being idle for more than one minute will log a warning
> to the log file. Furthermore sessions are automatically synchronised to reflect the same state
> across accesses within a single thread. That is, an older session will see the changes done
> through a newer session given both sessions are accessed from within the same thread.  
> *Note*: Oakへの移行を容易にするために、1分以上アイドル状態のセッションは、ログファイルに警告を記録します。
> さらに、セッションは同じ状態を反映するためにシングルスレッド内で自動的に同期されます。
> すなわち、複数セッションは同一スレッド内からアクセスとなるため、古いセッションは新しいセッションを介して行われた変更が分かります。
>
> Automatic session synchronisation is a transient feature and will most probably be removed in
> future versions of Oak. See [OAK-803](https://issues.apache.org/jira/browse/OAK-803) for further
> details regarding session backwards compatibility and
> [OAK-960](https://issues.apache.org/jira/browse/OAK-960) regarding in thread session
> synchronisation.
>
> The `SessionMBean` provides further information on when a session is refreshed and wheter
> a refresh will happen on the next access.  
> 自動セッション同期は一過性機能であり、おそらくOakの将来のバージョンでは削除されます。
> セッションの後方互換性に関する詳細については[OAK-803](https://issues.apache.org/jira/browse/OAK-803)を
> セッション同期スレッドについては[OAK-960](https://issues.apache.org/jira/browse/OAK-960)を確認して下さい。
> `SessionMBean` は、セッションが更新されたときに、リフレッシュは、次のアクセスで発生するかどうか、さらに情報を提供します。

On Oak `Item.refresh()` is deprecated and will always cause an `Session.refresh()`. The former call
will result in a warning written to the log in order to facilitate locating trouble spots.  
Oakでは、`Item.refresh()`は非推奨で`Session.refresh()`を推奨しています。
前者の呼び出しは、問題特定のために警告がログに書き込まれます。

On Oak `Item.save()` is deprecated and will per default log a warning and fall back to
`Session.save()`. This behaviour can be tweaked with `-Ditem-save-does-session-save=false` in which
case no fall back to `Session#save()` will happen but an `UnsupportedRepositoryException` is thrown
if the sub-tree rooted at the respective item does not contain all transient changes. See
[OAK-993](https://issues.apache.org/jira/browse/OAK-993) for details.  
Oakでは、`Item.save()`は非推奨で警告がログに書き込まれ`Session.save()`に置き換えられます。
この振る舞いは、`-Ditem-save-does-session-save=false`で無効化でき、その場合は、`Session#save()`の置き換えは行われず、
それぞれのアイテムをルートとするサブツリーにすべての変更が含まれていない場合、`UnsupportedRepositoryException`がthrowされます。
詳細は[OAK-993](https://issues.apache.org/jira/browse/OAK-993)を確認して下さい。

Query
クエリ
-----

Oak does not index content by default as does Jackrabbit 2. You need to create custom indexes when
necessary, much like in traditional RDBMSs. If there is no index for a specific query then the
repository will be traversed. That is, the query will still work but probably be very slow.
See the [query overview page](query.html) for how to create a custom index.  
Oakはデフォルトでは、Jackrabbit 2と異なりインデックス化を行ないません。
伝統的なRDBMSsと同様にカスタムインデックスを必要に応じて作成する必要があります。

If there is no index for a specific query then the repository will be traversed. 
That is, the query will still work but probably be very slow. 
See the [query overview page](query.html) for how to create a custom index.  
インデックスが存在しない場合、特定のクエリはリポジトリをトラバースします。
すなわち、そのクエリは動くでしょうが恐らく非常に遅くなります。
カスタムインデックスの作成方法は[query overview page](query.html)を参照して下さい。

There were some smaller bugfixes in the query parser which might lead to incompatibility.
See the [query overview page](query.html) for details.  
クエリパーサに幾つかのバグフィックスがあり、これは非互換性につながるかもしれません。
詳細は[query overview page](query.html)を参照して下さい。


Observation
オブザベーション
-----------
* `Event.getInfo()` contains the primary and mixin node types of the associated parent node of the 
  event. The key `jcr:primaryType` maps to the primary type and the key `jcr:mixinTypes` maps to an 
  array containing the mixin types.  
  `Event.getInfo()`はイベントの関連する親ノードのプライマリおよびミックスインのノードタイプが含まれています。
  キー`jcr:primaryType`がプライマリタイプにマップされ、キー`jcr:mixinTypes`がミックスタイプを含む配列にマップします。

* `Event.getUserId()`, `Event.getUserData()`and `Event.getDate()` will only be available for locally
  generated events (i.e. on the same cluster node). To help identifying potential trouble spots,
  calling any of these methods without a previous call to `JackrabbitEvent#isExternal()` will write
  a warning to the log file.  
  `Event.getUserId()`, `Event.getUserData()`および `Event.getDate()`は、ローカルに生成されたイベント(i.e. 同一クラスタのノード上)で利用可能です。
  潜在的なトラブルスポットを特定しやすくするために、`JackrabbitEvent#isExternal()`の呼び出しを行わずにこれらのメソッドが呼び出すと警告がログファイルに書き込まれます。

* Push notification mechanisms like JCR observation weight heavy on distributed systems. Therefore,
  if an application requirement is not actually an "eventing problem" consider using different means
  like query and custom indexes.
  [Apache Sling](http://sling.apache.org) identified and classified common [usage patterns]
  (https://cwiki.apache.org/confluence/display/SLING/Observation+usage+patterns) of observation and
  recommendations on alternative solutions where applicable.  
  分散システム上のヘビーなJCRオブザベーションの様なPush notificationメカニズム。
  従って、アプリケーション要件が、実際に「イベント問題」ではない場合、クエリ、カスタムインデックスのような別の手段を使用することを検討してください。
  [Apache Sling](http://sling.apache.org)が一般的なオブザベーションの[usage patterns](https://cwiki.apache.org/confluence/display/SLING/Observation+usage+patterns)と代替ソリューション提言の特定と分類をしています。

* Event generation is done by looking at the difference between two revisions of the persisted
  content trees. Items not present in a previous revision but present in the current revision are
  reported as `Event.NODE_ADDED` and `Event.PROPERTY_ADDED`, respectively. Items present in a
  previous revision but not present in the current revision are reported as `Event.NODE_REMOVED` and
  `Event.PROPERTY_REMOVED`, respectively. Properties that changed in between the previous revision
  and the current revision are reported as `PROPERTY_CHANGED`. As a consequence operations that
  cancelled each others in between the previous revision and the current revision are not reported.
  Furthermore the order of the events depends on the underlying implementation and is not specified.
  In particular there are some interesting consequences:  
  イベントの生成は、永続化されたコンテンツツリーの2つのリビジョンの違いを見ることによって行われます。
  前リビジョンで存在せず現リビジョンで存在するアイテムは、`Event.NODE_ADDED`と`Event.PROPERTY_ADDED`でそれぞれ報告されます。
  前リビジョンで存在し現リビジョンで存在しないアイテムは、`Event.NODE_REMOVED`と`Event.PROPERTY_REMOVED`でそれぞれ報告されます。
  前リビジョンと現リビジョン間で変更されたプロパティはPROPERTY_CHANGEDで報告されます。
  前リビジョンと現リビジョン間で相殺される操作は報告されません。
  さらにイベントの順序は実装依存で規定されていません。
  特にいくつかの興味深い結果があります:

    * Touched properties: Jackrabbit 2 used to generate a `PROPERTY_CHANGED` event when touching a
      property (i.e. setting a property to its current value). Oak keeps closer to the specification
      and [omits such events](https://issues.apache.org/jira/browse/OAK-948). More generally removing
      a subtree and replacing it with the same subtree will not generate any event.  
      Touched properties: Jackrabbit 2は、`PROPERTY_CHANGED`イベントの生成のために使用していました(i.e. プロパティに現在値を設定する)。
      Oakはその仕様に近づけており[そのようなイベントを除外しています](https://issues.apache.org/jira/browse/OAK-948).
      もっと一般的にサブツリーの削除と同一サブツリーによる置換えはイベントの生成を行ないません。

    * Removing a referenceable node and adding it again will result in a `PROPERTY_CHANGED` event for
      `jcr:uuid`; the same applies for other built-in protected and mandatory properties
      such as e.g. jcr:versionHistory if the corresponding versionable node
      was removed and a versionable node with the same name is being created.  
      参照可能ノードの削除とその追加は再び`jcr:uuid`の`PROPERTY_CHANGED`イベントとなります。
      他の組込保護されたものや必須プロパティも同様で、jcr:versionHistoryといったもので、バージョン管理に対応するノードを除去して、同じ名前のバージョン管理可能なノードが作成されている場合です。

    * Limited support for `Event.NODE_MOVED`:  
      `Event.NODE_MOVED`の限定サポート:

      + A node that is added and subsequently moved will not generate a `Event.NODE_MOVED`
        but a `Event.NODE_ADDED` for its final location.  
        追加された後で移動されたノードは、`Event.NODE_MOVED`を生成せず、最終箇所の`Event.NODE_ADDED`となります。

      + A node that is moved and subsequently removed will not generate a `Event.NODE_MOVED`
        but a `Event.NODE_REMOVED` for its initial location.  
        移動された後で削除されたノードは、`Event.NODE_MOVED`を生成せず初期箇所の`Event.NODE_REMOVED`となります。

      + A node that is moved and subsequently moved again will only generate a single
        `Event.NODE_MOVED` reporting its initial location as `srcAbsPath` and its
         final location as `destAbsPath`.  
        移動された後で再び移動されたノードは1つの`Event.NODE_MOVED`のみ生成され、`srcAbsPath`として初期箇所、`destAbsPath`として最終箇所が報告します。

      + A node whose parent was moved and that moved itself subsequently reports its initial
        location as `srcAbsPath` instead of the location it had under the moved parent.  
        親ノードが移動され、それ自信が後で移動されたノードは、`srcAbsPath`として初期箇所がそれが移動した親の下に持っていた場所を代わりに報告します。

      + A node that was moved and subsequently its parent is moved will report its final
        location as `destAbsPath` instead of the location it had before its parent moved.  
        移動した後でその親が移動されたノードは、`destAbsPath`として最終箇所がその移動された親の前に持っていた場所を代わりに報告します。

      + Removing a node and adding a node with the same name at the same parent will be
        reported as `NODE_MOVED` event as if it where caused by `Node.orderBefore()` if
        the parent node is orderable and the sequence of operations caused a change in
        the order of the child nodes.  
        移動ノードと追加ノードが同じ親で同じ名前であるノードは、
        親ノードが順序付け可能で動作シーケンスが子ノードの順序の変化に基づく場合、
        `Node.orderBefore()`によって引き起こされる場合、`NODE_MOVED`イベントが報告されます。

      + The exact sequence of `Node.orderBefore()` will not be reflected through `NODE_MOVED`
        events: given two child nodes `a` and `b`, ordering `a` after `b` may be reported as
        ordering `b` before `a`.  
        `Node.orderBefore()`の完全な順序は、`NODE_MOVED`を通じて反映されません。
        与えられた2つの子ノード`a`と`b`について、`a`が`b`後の順でも`b`は`a`の前の順に報告されることがあります。

* The sequence of differences Oak generates observation events from is guaranteed to contain the
  before and after states of all cluster local changes. This guarantee does not hold for cluster
  external changes. That is, cancelling operations from cluster external events might not be
  reported event though they stem from separate commits (`Session.save()`).  
  差異のシーケンスを、Oakは全てのクラスターのローカル変更の前後の状態が含まれる事が保証してオブザベーションイベントを生成します。
  これはクラスタ外の変更を保持する事を保証するものではありません。
  すなわち、クラスタ外イベントからのキャンセル操作は、それらは別々のコミット(`Session.save()`)にから生じるイベントであるものの、報告できない場合があります。

* Unregistering an observation listener blocks for no more than one second. If a pending
  `onEvent()` call does not complete by then a warning is logged and the listener will be
  unregistered without further waiting for the pending `onEvent()` call to complete.
  See [OAK-1290](https://issues.apache.org/jira/browse/OAK-1290) and
  [JSR_333-74](https://java.net/jira/browse/JSR_333-74) for further information.  
  オブザベーションリスナブロックの登録解除は1秒もかかりません。
  保留中の`onEvent()`の呼び出しがそれまでに完了しない場合、警告がログに記録され、リスナは保留中の`onEvent()`の呼び出し完了をさらに待つことなく、登録解除されます。
  詳細については[OAK-1290](https://issues.apache.org/jira/browse/OAK-1290)と[JSR_333-74](https://java.net/jira/browse/JSR_333-74)を参照してください。

* See [OAK-1459](https://issues.apache.org/jira/browse/OAK-1459) introduced some differences
  in what events are dispatch for bulk operations (moving and deleting sub-trees):  
  [OAK-1459](https://issues.apache.org/jira/browse/OAK-1459)では、一括操作(サブツリーの移動と削除)のイベントが割り当ているものでいくつかの違いについて紹介しています:

<table>
<tr>
<th>Operation</th>
<th>Jackrabbit 2</th>
<th>Oak</th>
</tr>
<tr>
<td>add sub-tree</td>
<td>NODE_ADDED event for every node in the sub-tree</td>
<td>NODE_ADDED event for every node in the sub-tree</td>
</tr>
<tr>
<td>remove sub-tree</td>
<td>NODE_REMOVED event for every node in the sub-tree</td>
<td>NODE_REMOVED event for the root of the sub-tree only</td>
</tr>
<tr>
<td>move sub-tree</td>
<td>NODE_MOVED event, NODE_ADDED event for the root of the sub-tree only,
    NODE_REMOVED event for every node in the sub-tree</td>
<td>NODE_MOVED event, NODE_ADDED event for the root of the sub-tree only,
    NODE_REMOVED event for the root of the sub-tree only</td>
</tr>
</table>

Binary streams
バイナリストリーム
--------------

In Jackrabbit 2 binary values were often (though not always) stored in
or spooled into a file in the local file system, and methods like
`Value.getStream()` would thus be backed by `FileInputStream` instances.
As a result the `available()` method of the stream would typically return
the full count of remaining bytes, regardless of whether the next `read()`
call would block to wait for disk IO.  
Jackrabbit 2ではバイナリ値は(常にではないが)にストアするか、またはローカルファイルシステム内のファイルにスプールされ、
`Value.getStream()`のようなメソッドは`FileInputStream`インスタンスが裏にいることとなっていたでしょう。
その結果、一般的にに残りのバイトのフルカウントを返すストリームの`available()`メソッドは
次に`read()`を呼び出すかに関係なく、ディスクIO待ちでブロックされます。

In Oak binaries are typically stored in an external database or (with the
TarMK) using a custom data structure in the local file system. The streams
returned by Oak are therefore custom `InputStream` subclasses that implement
the `available()` method based on whether the next `read()` call will return
immediately or if it needs to block to wait for the underlying IO operations.  
Oakではバイナリを一般的に外部データベースか(TarMK)ファイルシステムのカスタムデータ構造にストアします。
Oakから返されたストリームは、カスタム`InputStream`サブクラスであるため、
`available()`メソッドは、次の`read()`呼び出しで即時応答するか、根本的なIO操作待ちでブロックする必要がある場合かに基づいて動作します。

This difference may affect some clients that make the incorrect assumption
that the `available()` method will always return the number of remaining
bytes in the stream, or that the return value is zero only at the end of the
stream. Neither assumption is correctly based on the `InputStream` API
contract, so such client code needs to be fixed to avoid problems with Oak.  
この差異は、幾つかのクライアントは、`available()` メソッドはストリーム残りのバイト数を常に返す、もしくは戻り値は0であるのはストリームの終わりだけという間違った仮定に影響を及ぼすでしょう。
`InputStream` API契約に基づき、どちらの仮定も正しく、そのように、クライアントコードはOakの問題を回避するために修正する必要がある。

Locking
ロッキング
-------

Oak does not support the strict locking semantics of Jackrabbit 2.x. Instead
a "fuzzy locking" approach is used with lock information stored as normal
content changes. If a `mix:lockable` node is marked as holding a lock, then
the code treats it as locked, regardless of what other concurrent sessions
that might see different versions of the node see or do. Similarly a lock token
is simply the path of the locked node.  
Oakは、Jackrabbit 2.xの厳格なロッキングセマンティクスをサポートしません。
代わりに、"fuzzy locking"アプローチが通常のコンテンツの変更として格納ロック情報と共に使用されます。
`mix:lockable`ノードがロックを保持しているとマーキングされている場合、ロックされているとして扱い、
ノードの異なるバージョンの処理を行っている他の並列セッションには無関係です。
同様に、ロック·トークンは、単純にロックされたノードのパスです。

This fuzzy locking should not be used or relied as a tool for synchronizing
the actions of two clients that are expected to access the repository within
a few seconds of each other. Instead this feature is mostly useful as a higher
level tool, for example a human author could use a lock to mark a document as
locked for a few hours or days during which other users will not be able to
modify the document.  
このfuzzy lockingは、互いに数秒以内にリポジトリにアクセスすることが期待されている
2のクライアントの動作を同期させるための信頼できるツールとして使用するべきではありません。
代わりに、この機能は主に便利なより高いレベルのツールで、
たとえば著者の人間は他のユーザーがドキュメントを変更することができないように、
数時間または数日のためにロックとしてドキュメントをマークするためにロックを使用することができます。

Same name siblings
同じ名前の兄弟
------------------

Same name siblings (SNS) are deprecated in Oak. We figured that the actual benefit supporting same
name siblings as mandated by JCR is dwarfed by the additional implementation complexity. Instead
there are ideas to implement a feature for automatic [disambiguation of node names](https://issues.apache.org/jira/browse/OAK-129).  
Oakでは同じ名前の兄弟(SNS)は非推奨です。
JCR定義の同じ名前の兄弟サポートのメリットは、実際、追加の実装の複雑さより小さいと結論付けました。
代わりに、自動化機能[ノード名の曖昧さ回避](https://issues.apache.org/jira/browse/OAK-129)の実装のアイデアがあります。

In the meanwhile we have [basic support](https://issues.apache.org/jira/browse/OAK-203) for same
name siblings but that might not cover all cases.  
それは、同じ名前の兄弟のための[基本的なサポート](https://issues.apache.org/jira/browse/OAK-203)であり、すべてのケースをカバーしていない場合があります。


XML Import
XMLインポート
----------

The import behavior for
[`IMPORT_UUID_CREATE_NEW`](http://www.day.com/maven/jsr170/javadocs/jcr-2.0/javax/jcr/ImportUUIDBehavior.html#IMPORT_UUID_CREATE_NEW)
in Oak is implemented slightly different compared to Jackrabbit. Jackrabbit 2.x only creates a new
UUID when it detects an existing conflicting node with the same UUID. Oak always creates a new UUID,
even if there is no conflicting node. The are mainly two reasons why this is done in Oak:  
Oakの[`IMPORT_UUID_CREATE_NEW`](http://www.day.com/maven/jsr170/javadocs/jcr-2.0/javax/jcr/ImportUUIDBehavior.html#IMPORT_UUID_CREATE_NEW)
のインポート動作はJackrabbitのに比べて若干異なる実装がされています。
Jackrabbit 2.xは、同じUUIDを持つ既存の競合するノードを検出したときに、新しいUUIDを作成します。
Oakは常に競合するノードが存在しない場合でも、新しいUUIDを作成します。
このOakにおける主な理由は2つあります:

* The implementation in Oak is closer to what the JCR specification says: *Incoming nodes are
assigned newly created identifiers upon addition to the workspace. As a result, identifier
collisions never occur.*  
Oakの実装をJCR仕様に近づけた: *入ってきたノードはワークスペース上で識別子を新規作成し割り当てられたものである。結果として識別子が衝突することは発生しない*

* Oak uses a MVCC model where a session operates on a snapshot of the repository. It is therefore
very difficult to ensure new UUIDs only in case of a conflict. Based on the snapshot view of a
session, an existing node with a conflicting UUID may not be visible until commit.  
Oakは、リポジトリのスナップショット上のセッション操作にMVCCモデルを使用しています。
それは競合が発生した場合、新たなUUIDを確保することが非常に困難です。
セッションのスナップショットビューに基づくため、UUIDが競合する事がコミットされるまで分からないからです。


Identifiers
識別子
-----------

In contrast to Jackrabbit 2.x, only referenceable nodes in Oak have a UUID assigned. With Jackrabbit
2.x the UUID is only visible in content when the node is referenceable and exposes the UUID as a
`jcr:uuid` property. But using `Node.getIdentifier()`, it is possible to get the UUID of any node.
With Oak this method will only return a UUID when the node is referenceable, otherwise the
identifier is the UUID of the nearest referenceable ancestor with the relative path to the node.  
Jackrabbit 2.xと対象的に, Oakの参照可能ノードにのみUUIDを持つように割り当てられます。
Jackrabbit 2.xはUUIDはコンテンツ ノードが参照可能な時のみ可視であり、`jcr:uuid`プロパティとしてUUIDが公開されます。
しかし`Node.getIdentifier()`は使用により、任意のノードのUUIDを取得することが可能です。
Oakのこのメソッドは、ノードが参照可能な時にのみUUIDを返し、そうでなければ、その識別子は、そのノードへの相対パスに最も近い参照可能な祖先のUUIDとなります。

Manually adding a property with the name `jcr:uuid` to a non referenceable node might have
unexpected effects as Oak maintains an unique index on `jcr:uuid` properties. As the namespace
`jcr` is reserved, doing so is strongly discouraged.  
`jcr:uuid`という名前で主導でプロパティを追加すると、非参照可能ノードとなり、Oakの`jcr:uuid`プロパティに
対するユニークインデックスのメンテナンスで予期しない影響があるかもしれません。

As the namespace `jcr` is reserved, doing so is strongly discouraged.  
`jcr`ネームスペースは予約済みで、使用することは強く禁止します。

Versioning
バージョンニング
----------

* Because of the different identifier implementation in Oak, the value of a `jcr:frozenUuid` property
on a frozen node will not always be a UUID (see also section about Identifiers). The property
reflects the value returned by `Node.getIdentifier()` when a node is copied into the version storage
as a frozen node. This also means a node restored from a frozen node will only have a `jcr:uuid`
when it is actually referenceable.  
Oakの識別子の実装の違いにより、凍結ノード上の`jcr:frozenUuid`プロパティの値は常にUUID (see also section about Identifiers)でありません。
このプロパティは、ノードは、凍結されたノードとしてバージョン·ストレージにコピーされたときの`Node.getIdentifier()`の返した値を参照しています。
これはまた、凍結ノードから復元されたノードは、実際に参照可能となった時にのみ`jcr:uuid`持つことを意味しています。

* Oak does currently not implement activities (`OPTION_ACTIVITIES_SUPPORTED`), configurations and
baselines (`OPTION_BASELINES_SUPPORTED`).  
Oakは現在、アクティビティ(`OPTION_ACTIVITIES_SUPPORTED`)、構成、ベースライン(`OPTION_BASELINES_SUPPORTED`)の実装を行っていません。

* Oak does currently not implement the various variants of `VersionManager.merge` but throws an
`UnsupportedRepositoryOperationException` if such a method is called.  
Oakは現在、`VersionManager.merge`の様々なバージョンを実装しておらず、関連するメソッドが呼ばれた場合`UnsupportedRepositoryOperationException`をthrowします。

Security
セキュリティ
--------

* [Authentication](security/authentication/differences.html)  
  [認証](security/authentication/differences.html)
* [AccessControl Management](security/accesscontrol/differences.html)  
  [アクセス制御](security/accesscontrol/differences.html)
* [Permission Evaluation](security/permission/differences.html)  
  [パーミッション評価](security/permission/differences.html)
* [Privilege Management](security/privilege/differences.html)  
  [特権管理](security/privilege/differences.html)
* [Principal Management](security/principal/differences.html)  
  [プリンシパル管理](security/principal/differences.html)
* [User Management](security/user/differences.html)  
  [ユーザー管理](security/user/differences.html)

Workspaces
ワークスペース
----------

An Oak repository only has one default workspace.  
Oakリポジトリは単一のデフォルトワークスペースのみ持ちます。

