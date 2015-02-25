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

Session refresh behavior
## セッションリフレッシュ動作

Oak is based on the MVCC model where each session starts with a snapshot
view of the repository. Concurrent changes from other sessions *are not
visible* to a session until it gets refreshed. A session can be refreshed
either explicitly by calling the ``refresh()`` method or implicitly by
direct-to-workspace methods or by the auto-refresh mode. Also observation
event delivery causes a session to be refreshed.  
Oakは、各セッションがリポジトリのスナップショットビューで開始されるMVCCモデルに基づいています。
他のセッションからの同時変更は、リフレッシュを行うまで*不可視*です。
セッションは、明示的に``refresh()``メソッドを呼び出すか、暗黙的なdirect-to-workspaceメソッド、auto-refresh modeのいずれかによってリフレッシュされます。
また、セッションをリフレッシュによりオブザベーションイベントが配信されます。

By default the auto-refresh mode automatically refreshes all sessions that
have been idle for more than one second, and it's also possible to
explicitly set the auto-refresh parameters. A typical approach would be
for long-lived admin sessions to set the auto-refresh mode to keep the
session always up to date with latest changes from the repository.  
デフォルトでは、auto-refreshモードは自動的に1秒以上アイドル状態になっているすべてのセッションをリフレッシュします。明示的にauto-refreshパラメータを設定することもできます。
長寿命の管理セッションにおいて、リポジトリから変更を最新の状態に維持するために、典型的なアプローチはauto-refreshモードに設定する事です。


Pattern: One session for one request/operation
### パターン: 1リクエスト用/操作ごとに1セッション

One of the key patterns targeted by Oak is a web application that serves
HTTP requests. The recommended way to handle such cases is to use a
separate session for each HTTP request, and never to refresh that session.  
Oakの対象となる主要なパターンの一つは、 WebアプリケーションサーバのHTTPリクエストです。
この場合の推奨方法は、各HTTPリクエストのために個別のセッションを使用しセッションをリフレッシュは行わない事です。

Anti pattern: concurrent session access
### アンチパターン: セッション同時アクセス

Oak is designed to be virtually lock free as long as sessions are not shared
across threads. Don't access the same session instance concurrently from
multiple threads. When doing so Oak will protect its internal data structures
from becoming corrupted but will not make any guarantees beyond that. In
particular violating clients might suffer from lock contentions or deadlocks.  
Oakは、事実上、セッションがスレッド間で共有されないようにロックフリーに設計されています。
複数のスレッドから同時に同じセッションインスタンスにアクセスしないでください。
行った場合、Oakは内部データ構造を破損から保護しますが、それ以上保証はできません。
特に、違反した場合、ロック競合やデッドロックに苦しむ可能性があります。

If Oak detects concurrent write access to a session it will log a warning. 
For concurrent read access the warning will only be logged if `DEBUG` level 
is enabled for `org.apache.jackrabbit.oak.jcr.delegate.SessionDelegate`.
In this case the stack trace of the other session involved will also be 
logged. For efficiency reasons the stack trace will not be logged if 
`DEBUG` level is not enabled.  
Oakがセッションへの同時書き込みアクセスを検出すると、警告がログに記録されます。
`org.apache.jackrabbit.oak.jcr.delegate.SessionDelegate`が`DEBUG`で有効になっている場合、同時読み取りアクセスのみ記録されます。
この場合、関係する他のセッションのスタックトレースもログに記録されます。
効率上の理由から、`DEBUG`レベルが有効になっていない場合、スタックトレースがログに記録されることはありません。

Large number of direct child node
### 大量の子ノード

Oak scales to large number of direct child nodes of a node as long as those
are *not* orderable. For orderable child nodes Oak keeps the order in an
internal property, which will lead to a performance degradation when the list
grows too large. For such scenarios Oak provides the ``oak:Unstructured`` node
type, which is equivalent to ``nt:unstructured`` except that it is not orderable.  
Oakは、*orderableでない*限り、大量の子ノードでもスケールします。
orderableな子ノードのために、Oakは内部プロパティで順序を保持し、リストが大きくなりすぎた場合、パフォーマンスの低下につながります。
そのようなシナリオのために、Oakは``oak:Unstructured``ノードタイプを提供し、これはorderableでない場合を除いた``nt:unstructured``に相当します。

