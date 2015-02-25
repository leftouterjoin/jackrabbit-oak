package org.apache.jackrabbit.oak.plugins.index.perfdata;

import com.orangesignal.csv.annotation.CsvColumn;
import com.orangesignal.csv.annotation.CsvEntity;

@CsvEntity
public class NodeStats {
	@CsvColumn(name = "ストアの種類", position = 0)
	public String storeType;
	@CsvColumn(name = "インデックスの種類", position = 1)
	public String indexType;
	@CsvColumn(name = "プロパティデータ長", position = 2)
	public int pvalBytes;
	@CsvColumn(name = "投入件数", position = 3)
	public int loadCount;
	@CsvColumn(name = "パス", position = 4)
	public String path;
	@CsvColumn(name = "ノードサイズ", position = 5)
	public long bytes;
	@CsvColumn(name = "ノード数", position = 6)
	public long nodes;
	@CsvColumn(name = "ノードサイズ(リンク無)", position = 7)
	public long bytesNolink = -1;
	@CsvColumn(name = "ノード数(リンク無)", position = 8)
	public long nodesNolink = -1;
	@CsvColumn(name = "処理時間(μs)", position = 9)
	public long elapsed;
}