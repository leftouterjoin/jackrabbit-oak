package org.apache.jackrabbit.oak.plugins.index.perfdata;

import com.orangesignal.csv.annotation.CsvColumn;
import com.orangesignal.csv.annotation.CsvEntity;

@CsvEntity
public class QueryStats {
	@CsvColumn(name = "ストアの種類", position = 0)
	public String storeType;
	@CsvColumn(name = "インデックスの種類", position = 1)
	public String indexType;
	@CsvColumn(name = "プロパティデータ長", position = 2)
	public int pvalBytes;
	@CsvColumn(name = "投入件数", position = 3)
	public int loadCount;
	@CsvColumn(name = "クエリ文字列", position = 4)
	public String query;
	@CsvColumn(name = "該当した件数", position = 5)
	public long results;
	@CsvColumn(name = "処理時間(μs)", position = 6)
	public long elapsed;
}