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

Transactional model of sessions
セッションのトランザクショナルモデル
================================
Sessions in Oak are based on a multi version concurrency control model using snapshot isolation with
a relaxed first committer wins strategy. That is, on login each session is under the impression of
operating on its own copy of the repository. Modifications from other sessions do not affect the
current session. With the relaxed first committer wins strategy a later session will fail on save
when it contains operations which are incompatible with the operations of an earlier session which
saved successfully. This is different from the standard first committer wins strategy where failure
would occur on conflicting operations rather than on incompatible operations. Incompatible is weaker
than conflict since two write operation on the same item do conflict but are not incompatible. The
details of what incompatible means are specified by `NodeStore.rebase()` and `MicroKernel.rebase()`.  
Oakのセッション制御は、簡易first-committer-wins戦略のスナップショット分離を使用したマルチバージョン同時実行制御モデルです。
すなわち、各セッションログインはリポジトリの独自のコピー上で操作する感覚です。
他のセッションからの変更は、現在のセッションには影響しません。
簡易first-committer-wins戦略では、後セッションは、保存に成功した前セッション操作と互換性の無いコンテンツ保存操作時に失敗します。
これは、非互換操作ではなく競合操作で失敗する標準first-committer-wins戦略とは異なります。
非互換は、非互換ではなく同じアイテムの2つの書き込み操作による競合よりも弱いです。
非互換の意味についての詳細は、`NodeStore.rebase()`と`MicroKernel.rebase()`で規定されています。

Snapshot isolation exhibits [write skew](http://research.microsoft.com/apps/pubs/default.aspx?id=69541)
which can be problematic for some application level consistency requirements. Consider the following
sequence of operations:  
スナップショット分離は[write skew](http://research.microsoft.com/apps/pubs/default.aspx?id=69541)で説明されていて、
これは、いくつかのアプリケーションレベルの整合性の要件については問題となり得ます。
次の一連の操作を考えてください

    session1.getNode("/testNode").setProperty("p1", -1);
    check(session1);
    session1.save();

    session2.getNode("/testNode").setProperty("p2", -1);
    check(session2);
    session2.save();

    Session session3 = repository.login();
    check(session3);

The check method enforces an application logic constraint which says that the sum of the properties
`p1` and `p2` must not be negative. While session1 and session2 each enforce this constraint before
saving, the constraint might not hold globally for session3.  
checkメソッドは、プロパティ`p1`と`p2`の和が負であってはならないと言うアプリケーションロジック制約を強制するものです。
session1とsession2は、それぞれ保存前にこの制約を強制しながら、制約はsession3にグローバルに保持しない場合があります。

See `CompatibilityIssuesTest.sessionIsolation` for a test case demonstrating this in runnable code.  
この実行可能なコードのテストケース用の`CompatibilityIssuesTest.sessionIsolation`を見て下さい。

