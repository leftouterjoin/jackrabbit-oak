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

Understanding the node state model
# ノードステートモデルの理解

This article describes the _node state model_ that is the core design
abstraction inside the `oak-core` component. Understanding the node state
model is essential to working with Oak internals and to building custom
Oak extensions.  
ここでは`oak-core`コンポーネントの内部を抽象化した_ノードステートモデル_のコアを説明しています。
ノードステートモデルを理解することは、Oak内部とOak拡張をカスタム構築するために不可欠です。

Background
## 背景

Oak organizes all content in a large tree hierarchy that consists of nodes
and properties. Each snapshot or revision of this content tree is immutable,
and changes to the tree are expressed as a sequence of new revisions. The
MicroKernel of an Oak repository is responsible for managing the content
tree and its revisions.  
Oakは、ノードとプロパティで構成され、大きなツリー階層内のすべてのコンテンツを管理します。
このコンテンツツリーの各スナップショットまたはリビジョンは不変で、ツリーへの変更は、新しいリビジョンのシーケンスとして表現されます。
OakリポジトリのMicroKernelは、コンテンツツリーとそのリビジョンの管理を担当する。

The JSON-based MicroKernel API works well as a part of a remote protocol
but is cumbersome to use directly in oak-core. There are also many cases
where transient or virtual content that doesn't (yet) exist in the
MicroKernel needs to be managed by Oak. The node state model as expressed
in the NodeState interface in oak-core is designed for these purposes. It
provides a unified low-level abstraction for managing all tree content and
lays the foundation for the higher-level Oak API that's visible to clients.  
JSONベースのMicroKernel APIは、リモートプロトコルの一部としてうまく動作しますが、oak-coreで直接使用するのは面倒です。
Oakによって管理する必要のあるMicroKernel内にまだ存在しない一時的または仮想コンテンツが多くあるケースもあります。
oak-coreのNodeStateインタフェースで表されるようなノードステートモデルはこれらの目的のために設計されています。
これは、すべてのツリーコンテンツを管理するための統合された低レベルの抽象化を提供し、クライアントにより高レベルに見えるのOak APIのための基礎を築きます。

The state of a node
## ノードステート

A _node_ in Oak is an unordered collection of named properties and child
nodes. As the content tree evolves through a sequence of revisions, a node
in it will go through a series of different states. A _node state_  then is
an _immutable_ snapshot of a specific state of a node and the subtree beneath
it.
Oakの_ノード_は名前付きプロパティと子ノードの順序不同のコレクションです。
コンテンツツリーは、リビジョンのシーケンスが進化するにつれて、その中のノードは、異なる一連の状態を通過します。
_ノードステート_ はノードの特定の状態とその下のサブツリーの_不変_スナップショットです。

As an example, the following diagram shows two revisions of a content tree,
the first revision consists of five nodes, and in the second revision a
sixth node is added in one of the subtrees. Note how unmodified subtrees
can be shared across revisions, while only the modified nodes and their
ancestors up to the root (shown in yellow) need to be updated to reflect
the change. This way both revisions remain readable at all times without
the implementation having to make a separate copy of the entire repository
for each revision.  
例として、次の図は、コンテンツツリーの2つのリビジョンを示し、最初のリビジョンは、5つのノードで構成され、第2リビジョンで第6ノードがサブツリーの一つに追加されます。
変更済みノードと(黄色で表示)ルート以下のその祖先は変更が反映されて更新されているが、変更されていないサブツリーはリビジョン間で共有することができます。
このように、両リビジョンは、各リビジョンのリポジトリ全体の別々のコピーの作成を実装する必要することなく、読み出しが可能です。

![two revisions of a content tree](nodestate-r1.png?raw=true)

To avoid making a special case of the root node and therefore to make it
easy to write algorithms that can recursively process each subtree as a
standalone content tree, a node state is _unnamed_ and does not contain
information about it's location within a larger content tree. Instead each
property and child node state is uniquely named within a parent node state.
An algorithm that needs to know the path of a node can construct it from
the encountered names as it descends the tree structure.  
ルートノードの特殊なケースを避けるためと、再帰的にスタンドアロンのコンテンツツリーとして各サブツリーを処理することができるアルゴリズムを簡単に記述するために、ノードステートは _unnamed_ で、大きなコンテンツツリー内の場所についての情報は含まれていません。
代わりに、各プロパティと子ノードステートが一意に親ノードステート内で与えられます。
ノードのパスを知っている必要のあるアルゴリズムは、ツリー構造を降下し、遭遇した名前からノード構築することができる。

Since node states are immutable, they are also easy to keep _thread-safe_.
Implementations that use mutable data structures like caches or otherwise
aren't thread-safe by default, are expected to use other mechanisms like
synchronization to ensure thread-safety.  
ノードステートは不変であるので、 _thread-safe_ を維持することが容易です。
キャッシュなどのような変更可能なデータ構造を使用する実装は、デフォルトでスレッドセーフはありません。
スレッドセーフを確保するために、同期のような他のメカニズムを使用することが期待されています。

The NodeState interface
## NodeStateインタフェース

The above design principles are reflected in the `NodeState` interface
in the `org.apache.jackrabbit.oak.spi.state` package of `oak-core`. The
interface consists of three sets of methods:  
上記の設計原理は `oak-core` の `org.apache.jackrabbit.oak.spi.state` パッケージ内の `NodeState` インターフェイスに反映されています。
インタフェースは、メソッドの三組で構成されています:

  * Methods for accessing properties  
	プロパティアクセスメソッド
  * Methods for accessing child nodes  
	子ノードアクセスメソッド
  * The `exists` method for checking whether the node exists or is accessible  
	ノードが存在するかアクセス可能かどうかをチェックするための `exists` メソッド
  * The `builder` method for building modified states  
	状態変更を構築するための `builder` メソッド
  * The `compareAgainstBaseState` method for comparing states  
	状態比較のための `compareAgainstBaseState` メソッド

You can request a property or a child node by name, get the number of
properties or child nodes, or iterate through all of them. Even though
properties and child nodes are accessed through separate methods, they
share the same namespace so a given name can either refer to a property
or a child node, but not to both at the same time.  
あなたは、名前でプロパティまたは子ノードを要求、プロパティまたは子ノードの数を取得することができ、またはそれらのすべてを反復処理することもできます。
プロパティと子ノードが別々のメソッドを介してアクセスされても、与えられた名前で同じ名前空間を共有し、プロパティまたは子ノードを各々参照できますが、同時には不可能です。

Iteration order of properties and child nodes is _unspecified but stable_,
so that re-iterating through the items of a _specific NodeState instance_
will return the items in the same order as before, but the specific ordering
is not defined nor does it necessarily remain the same across different
instances.  
プロパティと子ノードの反復順序は _不特定だが一定_ です。そのため _特定のNodeStateインスタンス_ の項目を再反復すると、以前と同じ順序で項目を返します。
順序の特定は未定義で、必ずしも異なるインスタンス間で同じではない。

The last three methods, `exists`, `builder` and `compareAgainstBaseState`,
are covered in the next sections. See also the `NodeState` javadocs for
more details about this interface and all its methods.  
最後の3メソッド `exists`, `builder`, `compareAgainstBaseState` は次のセクションで説明します。
このインタフェースと全てのメソッドについての詳細は、 `NodeState` javadocsを参照してください。

Existence and iterability of node states
## ノードステートの存在と反復性

The `exists` method makes it possible to always traverse any path regardless
of whether the named content exists or not. The `getChildNode` method returns
a child `NodeState` instance for any given name, and the caller is expected
to use the `exists` method to check whether the named node actually does exist.
The purpose of this feature is to allow paths like `/foo/bar` to be traversed
even if the current user only has read access to the `bar` node but not its
parent `foo`. As a consequence it's even possible to access content like a
fictional `/bar` subtree that doesn't exist at all. A piece of code for
accessing such content could look like this:  
`exists`メソッドは、常にコンテンツが存在するかどうかに関係なく任意のパスをトラバースすることができます。
`getChildNode`メソッドは、任意の名前の子 `NodeState` インスタンスを返し、呼び出し側は、ノードが実際に存在するかどうかをチェックするために`exists`メソッドを使用することが期待されます。
この機能の目的は、`/foo/bar` のようなパスを現在のユーザーが、` bar`ノードのみ読み取りアクセス可能で、親の `foo` が不可である場合でも、トラバース可能にするためです。
結果として、それは全く存在しない架空の`/ bar`サブツリーのようなコンテンツにアクセスすることも可能です。
そのようなコンテンツにアクセスするためのコードの一部はこのようになります：

```java
NodeState root = ...;

NodeState foo = root.getChildNode("foo");
assert !foo.exists();
NodeState bar = foo.getChildNode("bar");
assert bar.exists();

NodeState baz = root.getChildNode("baz");
assert !baz.exists();
```

The following diagram illustrates such a content tree, both as the raw
content that simply exists and as an access controlled view of that tree:  
次の図は、生コンテンツとして単に存在しているコンテンツツリーと、アクセス制御されたビューを示しています。

![content tree with and without access control](nodestate-r2.png?raw=true)

If a node is missing, i.e. its `exists` method returns `false` and thus it
either does not exist at all or is read-protected, one can't list any of
its properties or child nodes. And on the other hand, a non-readable node or
property will only show up when listing the child nodes or properties of its
parent. In other words, a node or a property is _iterable_ only if both it
and its parent are readable. For example, attempts to list properties or
child nodes of the node `foo` will show up empty:
ノードが欠落している場合、`exists`メソッドが`false`を返します、従って、それが存在しないか保護された読み取りで、そのプロパティ、子ノードを何もリストすることはできません。
一方で、読み取り不可ノードまたはプロパティは、子ノードまたはその親のプロパティのリストで現れます。
つまり、ノードまたはプロパティが自身とその親の両方が読み取り可能である場合にのみ_iterable_です。
例えば、`foo`ノードの子プロパティやノードをリストしようとして空にとなります:

```java
assert !foo.getProperties().iterator().hasNext();
assert !foo.getChildNodeEntries().iterator().hasNext();
```

Note that without some external knowledge about the accessibility of a child
node like `bar` there's no way for a piece of code to distinguish between
the existing but non-readable node `foo` and the non-existing node `baz`.  
`bar`のような子ノードのアクセシビリティについて一部の外部知識がなくても、既存のが、読み取り不可ノード`foo`などと無効ノード`baz`を区別するために方法はありませんので注意してください。

Building new node states
## 新しいノードステートの構築

Since node states are immutable, a separate builder interface,
`NodeBuilder`, is used to construct new, modified node states. Calling
the `builder` method on a node state returns such a builder for
modifying that node and the subtree below it.  
ノードステートは不変であるため、別個のビルダーインタフェース`NodeBuilder`を、新規、変更されたノードステートを構成するために使用します。
ノードステートの`builder`メソッドを呼び出すと、そのノードとその下のサブツリーを変更するためのビルダーを返します。

A node builder can be thought of as a _mutable_ version of a node state.
In addition to property and child node access methods like the ones that
are already present in the `NodeState` interface, the `NodeBuilder`
interface contains the following key methods:
ノードビルダは、ノードステートの_mutable_版と考えることができます。
すでに`NodeState`インタフェースに存在しているもののようなプロパティと子ノードのアクセス方法に加えて、` NodeBuilder`インターフェイスは、次の主要なメソッドが含まれています:

  * The `setProperty` and `removeProperty` methods for modifying properties  
	`setProperty`と`removeProperty`メソッドはプロパティを変更します。
  * The `getChildNode` method for accessing or modifying an existing subtree  
	`getChildNode`メソッドは既存のサブツリーをアクセス、変更します。
  * The `setChildNode` and `removeChildNode` methods for adding, replacing
    or removing a subtree  
	`setChildNode`と`removeChildNode`メソッドはサブツリーを追加、交換、削除します。
  * The `exists` method for checking whether the node represented by
    a builder exists or is accessible  
	`exists`はビルダーによって表されるノードが存在するかアクセス可能かどうかをチェックします。
  * The `getNodeState` method for getting a frozen snapshot of the modified
    content tree  
	`getNodeState`メソッドは変更済みコンテンツツリーの凍結スナップショットを取得します。

All the builders acquired from the same root builder instance are linked
so that changes made through one instance automatically become visible in the
other builders. For example:  
1インスタンスを介して行われた変更は自動的に他のビルダーで見えるように、同じルートビルダー·インスタンスから取得したすべてのビルダーがリンクされています。例えば:

```java
NodeBuilder rootBuilder = root.builder();
NodeBuilder fooBuilder = rootBuilder.getChildNode("foo");
NodeBuilder barBuilder = fooBuilder.getChildNode("bar");

assert !barBuilder.getBoolean("x");
fooBuilder.getNodeChild("bar").setProperty("x", Boolean.TRUE);
assert barBuilder.getBoolean("x");

assert barBuilder.exists();
fooBuilder.removeChildNode("bar");
assert !barBuilder.exists();
```

The `getNodeState` method returns a frozen, immutable snapshot of the current
state of the builder. Providing such a snapshot can be somewhat expensive
especially if there are many changes in the builder, so the method should
generally only be used as the last step after all intended changes have
been made. Meanwhile the accessors in the `NodeBuilder` interface can be
used to provide efficient read access to the current state of the tree
being modified.  
`getNodeState`メソッドは、ビルダーの現在の状態の凍結、不変のスナップショットを返します。
そのようなスナップショットを提供することは、この方法は一般的にだけ結局意図の変更が行われた最後のステップとして使用する必要がありますので、ビルダーに多くの変更が存在する場合は特に、やや高価になります。
一方、`NodeBuilder`インタフェースのアクセサは、変更されているツリーの現在の状態の効率的な読み取りアクセスを提供できます。

The node states constructed by a builder often retain an internal reference
to the base state used by the builder. This allows common node state
comparisons to perform really well as described in the next section.  
ビルダーによって構成されたノードステートは、多くの場合、ビルダーによって使用されるベースステートへの内部リファレンスを保持します。
これは共通的なノードステート比較の実行が可能になり、次のセクションで説明します。

Comparing node states
## ノードステート比較

As a node evolves through a sequence of states, it's often important to
be able to tell what has changed between two states of the node. This
functionality is available through the `compareAgainstBaseState` method.
The method takes two arguments:  
ノードは状態の一連の進化するにつれて、ノードの2つの状態の間で変更が分かるようするために重要です。
この機能は、`compareAgainstBaseState`メソッドを介して利用可能です。
このメソッドは2つの引数を取ります。

  * A _base state_ for the comparison. The comparison will report all
    changes necessary for moving from the given base state to the node
    state on which the comparison method is invoked.  
	比較のための_base state_。比較は、比較メソッドが呼び出されているノードステートに与えられたベースステートから移動するために必要なすべての変更を報告します。
  * A `NodeStateDiff` instance to which all detected changes are reported.
    The diff interface contains callback methods for reporting added,
    modified or removed properties or child nodes.  
	`NodeStateDiff`インスタンスはその検出されたすべての変更が報告されます。  
	diffインターフェースは、追加されたプロパティまたは子ノードを変更または削除報告するためのコールバックメソッドが含まれています。

The comparison method can actually be used to compare any two nodes, but the
implementations of the method are typically heavily optimized for the case
when the given base state actually is an earlier version of the same node.
In practice this is by far the most common scenario for node state comparisons,
and can typically be executed in `O(d)` time where `d` is the number of
changes between the two states. The fallback strategy for comparing two
completely unrelated node states can be much more expensive.  
比較メソッドは、実際には2つのノードを比較するために使用することができます。このメソッドの実装は、一般的に重く、与えられたベースステートは実際には同じノードの以前のバージョンである場合に最適化されています。
実際には、これは、ノード状態の比較のためにはるかに最も一般的なシナリオによるものであり、一般的に、2のノードステートの変更数が`d`であるときの`O(d)` 時間で実行可能です。
2つの完全に無関係のノードステートを比較するための後退戦略は、はるかに高価となります。

An important detail of the `NodeStateDiff` mechanism is the `childNodeChanged`
method that will get called if there can be _any_ changes in the subtree
starting at the named child node. The comparison method should thus be able
to efficiently detect differences at any depth below the given nodes. On the
other hand the `childNodeChanged` method is called only for the direct child
node, and the diff implementation should explicitly recurse down the tree
if it wants to know what exactly did change under that subtree. The code
for such recursion typically looks something like this:  
`NodeStateDiff`メカニズムの重要事項は、`childNodeChanged`メソッドで、所定の子ノードから始まるサブツリーで_何らかの_変化があれば呼び出されます。
比較メソッドは、効率的に与えられたノードの下の任意の深さにおける差異を検出することができる必要があります。
一方、`childNodeChanged`メソッドのみが直接子ノードに対して呼び出されます、そのサブツリー下の変更を正確に知りたい場合、diffの実装はツリーを明示的に再帰的降下する必要があります
そのような再帰のためのコードは、典型的に次のようになります。

    public void childNodeChanged(
            String name, NodeState before, NodeState after) {
        after.compareAgainstBaseState(before, ...);
    }

Note that for performance reasons it's possible for the `childNodeChanged`
method to be called in some cases even if there actually are no changes
within that subtree. The only hard guarantee is that if that method is *not*
called for a subtree, then that subtree definitely has not changed, but in
most common cases the `compareAgainstBaseState` implementation can detect
such cases and thus avoid extra `childNodeChanged` calls. However it's
important that diff handlers are prepared to deal with such events.  
`childNodeChanged`メソッドは、実際にはサブツリー内の変更がない場合でも、いくつかのケースで呼び出される可能性があるため、パフォーマンス上の理由から注意してください。
唯一の強固な保証は、そのメソッドが呼び出*されない*場合、サブツリーは間違いなく変更されていないではないということです。しかし、最も一般的なケースでは、`compareAgainstBaseState`の実装はそのようなケースを検出します。これにより余分な`childNodeChanged`呼び出しを回避することができます。
しかし、差分ハンドラはこのようなイベントの対処に備えがあることは重要です。

The commit hook mechanism
## コミットフックメカニズム

A repository typically has various constraints to control what kind of content
is allowed. It often also wants to annotate content changes with additional
modifications like adding auto-created content or updating in-content indices.
The _commit hook mechanism_ is designed for these purposes. An Oak instance
has a list of commit hooks that it applies to all commits. A commit hook is
in full control of what to do with the commit: it can reject the commit,
pass it through as-is, or modify it in any way.  
リポジトリは通常、コンテンツの種類が許可されているものを制御するための様々な制約があります。
それは多くの場合、自動作成されたコンテンツの追加やコンテンツ内インデックス更新といった追加修正によるコンテンツ変更で注意したいでしょう。
_コミットフックメカニズム_は、これらの目的のために設計されています。
Oakインスタンスは、すべてのコミットに適用するコミットフックのリストを有しています。
コミットフックがコミットをどうするかを完全に制御しています。それは、コミットリジェクトも可能で、そのまま受け流すことも、または任意の方法でそれを変更することもできます。

All commit hooks implement the `CommitHook` interface that contains just
a single `processCommit` method:  
すべてのコミットフックは、単一の`processCommit`メソッドを含む`CommitHook`インターフェイスを実装しています:

    NodeState processCommit(NodeState before, NodeState after)
        throws CommitFailedException;

The `before` state is the original revision on which the content changes
being committed are based, and the `after` state contains all those changes.
A `after.compareAgainstBaseState(before, ...)` call can be used to find out
the exact set of changes being committed.

If, based on the content diff or some other inspection of the commit, a
hook decides to reject the commit for example due to a constraint violation,
it can do so by throwing a `CommitFailedException` with an appropriate error
code as outlined in http://wiki.apache.org/jackrabbit/OakErrorCodes.

If the commit is acceptable, the hook can return the after state as-is or
it can make some additional modifications and return the resulting node state.
The returned state is then passed as the after state to the next hook
until all the hooks have had a chance to process the commit. The resulting
final node state is then persisted as a new revision and made available to
other Oak clients.

Commit editors
## コミットエディタ

In practice most commit hooks are interested in the content diff as returned
by the `compareAgainstBaseState` call mentioned above. This call can be
somewhat expensive especially for large commits, so it's not a good idea for
multiple commit hooks to each do a separate diff. A more efficient approach
is to do the diff just once and have multiple hooks process it in parallel.
The _commit editor mechanism_ is used for this purpose. An editor is
essentially a commit hook optimized for processing content diffs.

Instead of a list of separate hooks, the editors are all handled by a single
`EditorHook` instance. This hook handles the details of performing the
content diff and notifying all available editors about the detected content
changes. The editors are provided by a list of `EditorProvider` instances
that implement the following method:

    Editor getRootEditor(
        NodeState before, NodeState after, NodeBuilder builder)
        throws CommitFailedException;

Instead of comparing the given before and after states directly, the provider
is expected to return an `Editor` instance to be used for the comparison.
The before and after states are passed to this method so that the provider
can collect generic information like node type definitions that is needed
to construct the returned editor.

The given `NodeBuilder` instance can be used by the returned editor to make
modifications based on the detected content changes. The builder is based
on the after state, but it is shared by multiple editors so during the diff
processing it might no longer exactly match the after state. Editors within
a single editor hook should generally not attempt to make conflicting changes.

The `Editor` interface is much like the `NodeStateDiff` interface described
earlier. The main differences are that all the editor methods are allowed to
throw `CommitFailedExceptions` and that the child node modification methods
all return a further `Editor` instance.

The idea is that each editor _instance_ is normally used for observing the
changes to just a single node. When there are changes to a subtree below
that node, the relevant child node modification method is expected to
return a new editor instance for observing changes in that subtree. The
editor hook keeps track of these returned "sub-editors" and recursively
notifies them of changes in the relevant subtrees.

If an editor is not interested in changes inside a particular subtree it can
return `null` to notify the editor hook that there's no need to recurse down
that subtree. And if the effect of an editor isn't tied to the location of
the changes within the content tree, it can just return itself. A good example
is a name validator that simply checks the validity of all names regardless
of where they're stored. If the location is relevant, for example when you
need to keep track of the path of the changed node, you can store that
information as internal state of the returned editor instance.

Commit validators
## コミットバリデータ

As mentioned, a common use for commit hooks is to verify that all content
changes preserve the applicable constraints. For example the repository
may want to enforce the integrity of reference properties, the constraints
defined in node types, or simply the well-formedness of the names used.
Such validation is typically based on the content diff of a commit, so
the editor mechanism is a natural match. Additionally, since validation
doesn't imply any further content modifications, the editor mechanism can
be further restricted for this particular case.

The abstract `ValidatorProvider` class and the related `Validator` interface
are based on the respective editor interfaces. The main difference is that
the validator provider drops the `NodeBuilder` argument to make it impossible
for any validators to even accidentally modify the commit being processed.
Thus, even though there's no performance benefit to using the `Validator`
interface instead of `Editor`, it's a good idea to do so whenever possible
to make the intention of your code clearer.
