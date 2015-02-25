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

Oak Documentation
Oak ドキュメント
=================

The Oak documentation lives as Markdown files in `src/site/markdown` such
that it easy to view e.g. from GitHub. Alternatively the Maven site plugin
can be used to build and deploy a web site as follows:  
Oak ドキュメントは `src/site/markdown` 内のマークダウンファイルで作成します。
そうすれば、GitHubなどで簡単に表示可能となります。

From the reactor do  
リアクタから行う

    mvn clean -Pdoc

to clean any existing site,  
既存のサイトをclean

    mvn site -Pdoc

to build the site **without** Javadoc, and optionally  
必要に応じてサイトを Javadoc を **除いて** ビルドする

    mvn site -Pjavadoc

to add Javadoc.  
Javadoc を追加する場合

   mvn site -Pdoc,javadoc

to generate **both** site and javadocs. Review the site at
`oak-doc/target/site`.  
webサイトと javadocsの **両方** をビルドし、`oak-doc/target/site`を確認します。

Then deploy the site to `http://jackrabbit.apache.org/oak/docs/` using  
サイトを `http://jackrabbit.apache.org/oak/docs/` へデプロイするには

    mvn site-deploy -Pdoc

Finally review the site at `http://jackrabbit.apache.org/oak/docs/index.html`.
To skip the final commit during the deploy phase you can specify
`-Dscmpublish.skipCheckin=true`. You can then review all pending changes in
`oak-doc/target/scmpublish-checkout` and follow up with `svn commit` manually.  
最後に `http://jackrabbit.apache.org/oak/docs/index.html` のサイトを確認します。
デプロイフェーズ中の最後のコミットをスキップするためには、`-Dscmpublish.skipCheckin=true`に設定します。
`oak-doc/target/scmpublish-checkout`の全ての保留中変更をレビューし、手動`svn commit`に従います。

*Note*: `mvn clean` needs to be run as a separate command as otherwise generating
the Javadocs would not work correctly due to issues with module ordering.  
*注意*: `mvn clean`は、モジュール順に正しく発行できないJavaDocの生成とは別のコマンドとして実行する必要があります。

Every committer should be able to deploy the site. No fiddling with
credentials needed since deployment is done via svn commit to
`https://svn.apache.org/repos/asf/jackrabbit/site/live/oak/docs`.
Every committer should be able to deploy the site. 
全こみったはサイトのデプロイができるはずです。
No fiddling with credentials needed since deployment is done via svn commit to `https://svn.apache.org/repos/asf/jackrabbit/site/live/oak/docs`.
`https://svn.apache.org/repos/asf/jackrabbit/site/live/oak/docs`へのsvn commitを通じ行われたデプロイ時から、資格情報は弄らないでください。
