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

Repository Construction
# リポジトリ構築

Oak comes with a simple and flexible mechanism for constructing content repositories
for use in embedded deployments and test cases. This article describes this
mechanism. Deployments in managed environments like OSGi should use the native
construction/configuration mechanism of the environment.  
Oakは、組込用途とテストケースにおけるコンテンツリポジトリを構築するための、シンプルで柔軟なメカニズムが付属しています。
この資料では、このメカニズムについて説明します。
OSGiのような管理された環境でのデプロイでは、その環境のネイティブ構築/設定メカニズムを使用する必要があります。

First, we construct a Repository instance.
Both the `Oak` and the `Jcr` classes support `with()` methods, 
so you can easily extend the repository with custom functionality if you like.
To construct an in-memory repository, use:  
最初に、リポジトリインスタンスを作成します。
`Oak`と`Jcr`クラスの双方が`with()`メソッドをサポートしていて、簡単に好きな機能を拡張する事ができます。
インメモリリポジトリを構築する場合:

        Repository repo = new Jcr(new Oak()).createRepository();

To use a MongoDB backend, use:  
MongoDBがバックエンドの場合:

        DB db = new MongoClient("127.0.0.1", 27017).getDB("test2");
        DocumentNodeStore ns = new DocumentMK.Builder().
                setMongoDB(db).getNodeStore();
        Repository repo = new Jcr(new Oak(ns)).createRepository();

To login to the repository and do some work (using 
the default username/password combination), use:  
リポジトリにログインし、何らかの操作を行う(デフォルトではユーザ名/パスワードの組み合わせ)場合:

        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node root = session.getRootNode();
        if (root.hasNode("hello")) {
            Node hello = root.getNode("hello");
            long count = hello.getProperty("count").getLong();
            hello.setProperty("count", count + 1);
            System.out.println("found the hello node, count = " + count);
        } else {
            System.out.println("creating the hello node");
            root.addNode("hello").setProperty("count", 1);
        }
        session.save();
        
To logout and close the backend store, use:  
ログアウトしてバックエンドストアをクローズする場合:
        
        session.logout();
        ns.dispose();
