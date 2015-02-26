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

## The Blob Store

The Oak BlobStore is similar to the Jackrabbit 2.x DataStore. However, there are a 
few minor problems the BlobStore tries to address.
Because, for the Jackrabbit DataStore:  
OakのBlobStoreはJackrabbit 2.x DataStoreに似ています。しかしながらBlobStoreの小さな問題に対処しようとしています。
なぜならば、Jackrabbit DataStoreは

* a temporary file is created when adding a large binary, 
  even if the binary already exists  
	既存にも関わらず、巨大なバイナリを追加した時、一時ファイルを作成します。

* sharding is slow and complicated because the hash needs to be calculated
  first, before the binary is stored in the target shard (the FileDataStore
  still doesn't support sharding the directory currently)  
	対象シャードにバイナリを格納する前に、ハッシュを最初に計算する必要があるため、シャーディングが遅くかつ複雑です。

* file handles are kept open until the consumer is done reading, which
  complicates the code, and we could potentially get "too many open files"
  when the consumer doesn't close the stream  
	コンシュマが読み取り完了するまで、ファイルハンドルは、開いたままとなります。
	これは、コードを複雑にし、コンシュマがストリームを閉じない場合、潜在的に「あまりにも多くの開いているファイル」状態となります。

* for database based data stores, there is a similar (even worse) problem
  that streams are kept open, which means we need to use connection pooling,
  and if the user doesn't close the stream we could run out of connections  
	データベースベースのDataStoreのため、接続プールを使用する必要があり、
	ユーザはストリームを閉じない場合、接続が不足する可能性がありストリームが
	開いたままにしていることを同じようなさらに悪い問題があります。

* for database based data stores, for some databases (MySQL), binaries are
  fully read in memory, which results in out-of-memory  
	一部のデータベース(MySQLの)のDataStoreで、バイナリは完全にメモリに読み込まれるためアウト·オブ·メモリとなります。

* binaries that are similar are always stored separately no matter what  
	同じようなバイナリにも関わらず別々に保存されます。

Those problems are solved in Oak BlobStores, because binaries are split
into blocks of 2 MB. This is similar to how [DropBox works internally][1].
Blocks are processed in memory so that temp files are never
needed, and blocks are cached. File handles don't need to be kept open.
Sharding is trivial because each block is processed separately.  
これら問題はOak BlobStoresでは解決しています。バイナリは2MBブロックに分割されるためです。
これは [DropBox 内部処理][1] に似ています。一時ファイルは必要されることはなく、ブロックは、メモリ内で処理されキャッシュされます。ファイルハンドルは開いたままにする必要はありません。
各ブロックは別々に処理されるため、シャーディングは些細です。

Binaries that are similar: in the BlobStore, currently, they are stored
separately except if some of the 2 MB blocks match. However, the algorithm
in the BlobStore would allow to re-use all matching parts, because in the
BlobStore, concatenating blob ids means concatenating the data.  
似ているバイナリ: BlobStoreでは、2メガバイトブロック一致のいくつかの場合を除いて、別々に保存されます。
しかし、BlobStoreでのアルゴリズムは、BlobStoreに、Blob IDを連結はデータ連結を意味するので、一致するすべての部品を再利用できるようになります。

Another change was that most DataStore implementations use SHA-1, while
the BlobStore uses SHA-256. Using SHA-256 will be a requirement at some
point, see also http://en.wikipedia.org/wiki/SHA-2 "Federal agencies ... 
must use the SHA-2 family of hash functions for these applications
after 2010". This might affect some potential users.  
もう一つの変更は、BlobStoreはSHA-256で、ほとんどのDataStoreの実装はSHA-1を使用するということでした。
SHA-256は、ある時点で要件になります。http://en.wikipedia.org/wiki/SHA-2 「連邦機関...2010年以降、これらのアプリケーションのためのハッシュ関数のSHA-2ファミリを使用する必要があります」を参照してください。
これは、いくつかの潜在ユーザーに影響を与える可能性があります。

Blob Garbage Collection
### Blob ガベージコレクション

Oak implements a Mark and Sweep based Garbage Collection logic.  
Oakはマークアンドスイープベースのガベージコレクションロジックを実装しています。
 
1. Mark Phase - In this phase the binary references are marked in both
   BlobStore and NodeStore  
	マークフェーズ - このフェーズでは、バイナリの参照は、BlobStoreやNodeStoreの両方にマークされています
    1. Mark BlobStore - GC logic would make a record of all the blob
       references present in the BlobStore. In doing so it would only
       consider those blobs which are older than a specified time 
       interval. So only those blob references are fetched which are 
       last modified say 24 hrs (default) ago.  
		BlobStoreマーク - GCロジックは、BlobStoreに存在するすべてのblob参照を記録します。
		そうすることで、指定した時間間隔よりも古いものblobを対象とします。
		つまり、最終変更が24時間(デフォルト)前のblob参照がフェッチされます。
    2. Mark NodeStore - GC logic would make a record of all the blob
       references which are referred by any node present in NodeStore.
       Note that any blob references from old revisions of node would also be 
       considered as a valid references.  
		NodeStoreマーク - GCロジックはNodeStore内の任意のノードに存在によって参照されるすべてのblob参照の記録になります。
		ノードの古いリビジョンからすべてのblob参照は、有効な参照として対象とされることに注意してください。

2. Sweep Phase - In this phase all blob references form Mark BlobStore phase 
    which were not found in Mark NodeStore part would considered as GC candidates
    and would be deleted.  
	スイープフェーズ - このフェーズでは、すべてのblob参照は、マークBlobStoreフェーズのマークNodeStoreで見つからなかった部分をGCの候補とし削除する。

Support for Jackrabbit 2 DataStore
### Jackrabbit 2 DataStoreのサポート

Jackrabbit 2 used [DataStore][2] to store blobs. Oak supports usage of such 
DataStore via `DataStoreBlobStore` wrapper. This allows usage of `FileDataStore` 
and `S3DataStore` with Oak NodeStore implementations. 

NodeStore and BlobStore
### NodeStoreとBlobStore

Currently Oak provides two NodeStore implementations i.e. `SegmentNodeStore` and `DocumentNodeStore`.
Further Oak ships with multiple BlobStore implementations  
現在Oakは2つのNodeStoreの実装を提供します。i.e. `SegmentNodeStore` と `DocumentNodeStore` です。

1. `FileBlobStore` - Stores the file contents in chunks on file system  
	`FileBlobStore` - ファイルシステム上にチャンクでファイルコンテンツを保存します。
2. `MongoBlobStore` - Stores the file content in chunks in Mongo. Typically used with
   `DocumentNodeStore` when running on Mongo by default  
	`MongoBlobStore` - Mongo上にチャンクでファイルコンテンツを保存します。一般的に Mongoで実行する際、 `DocumentNodeStore` とともに使用します。
3. `FileDataStore` (with wrapper) - Stores the file on file system without breaking it into
   chunks. Mostly used when blobs have to shared between multiple repositories. Also used by 
   default when migrating Jackrabbit 2 repositories to Oak  
	`FileDataStore` (with wrapper) - ファイルシステム上にチャンク無しでファイルコンテンツを保存します。主にblobを複数のリポジトリ間で共有する必要がある際に使用されます。
	OakにJackrabbitの2のリポジトリを移行する場合にもデフォルトで使用されます。
4. `S3DataStore` (with wrapper) - Stores the file in Amazon S3  
	`S3DataStore` (with wrapper) - Amazon S3上にファイルを保存します。

In addition there are some more implementations which are considered **experimental**  
さらに、 **実験的** であると考えているいくつかのより多くの実装があります。

1. `RDBBlobStore` - Stores the file chunks in database
2. `CloudBlobStore` - Stores the file file chunks in cloud storage using the [JClouds BlobStore API][3].
3. `MongoGridFSBlobStore` - Stores the file chunks in Mongo using GridFS support


Depending on NodeStore type and usage requirement these can be configured to use 
a particular BlobStore implementation. For OSGi env refer to [Configuring DataStore/BlobStore]
(../osgi_config.html#config-blobstore)  
NodeStoreタイプと使用要件に応じて、これらは、特定のBlobStore実装を使用するように構成することができます。


#### SegmentNodeStore

By default SegmentNodeStore does not require a BlobStore. Instead the binary content is
directly stored as part of segment blob itself. Depending on requirements one of the following 
can be used  
デフォルトではSegmentNodeStoreはBlobStoreを必要としません。代わりに、バイナリコンテンツを直接セグメントblob自体の一部として保存されます。要件に応じて、以下のいずれかを使用することができます。
 
* FileDataStore - This should be used if the blobs/binaries have to be shared between multiple
  repositories. This would also be used when a JR2 repository is migrated to Oak  
	FileDataStore - blob/バイナリは、複数のリポジトリ間で共有されなければならない場合、これを使用すべきです。JR2リポジトリがOakに移行されたときに使用されます。
* S3DataStore - This should be used when binaries are stored in Amazon S3  
	S3DataStore - バイナリをAmazon S3に保管する必要がある場合に使用します。

#### DocumentNodeStore

By default DocumentNodeStore when running on Mongo uses `MongoBlobStore`. Depending on requirements 
one of the following can be used  
デフォルトのDocumentNodeStoreではMongo上で動作しているときは、 `MongoBlobStore` 使用します。要件に応じて、以下のいずれかを使用することができます。
                  
* MongoBlobStore - Used by default  
	MongoBlobStore - デフォルトで使用します。
* FileDataStore - This should be used if the binaries have to be stored on the file system. This 
  would also be used when a JR2 repository is migrated to Oak  
	FileDataStore - バイナリをファイルシステムに格納する場合に使用されるべきです。JR2リポジトリがOakに移行されたときに使用されます。
* S3DataStore - This should be used when binaries are stored in Amazon S3. Typically used when running
  in Amazon AWS  
	S3DataStore - バイナリをAmazon S3に保管する必要がある場合に使用します。一般的にAmazon AWSで実行する際に使用します。

[1]: http://serverfault.com/questions/52861/how-does-dropbox-version-upload-large-files
[2]: http://wiki.apache.org/jackrabbit/DataStore
[3]: http://jclouds.apache.org/start/blobstore/