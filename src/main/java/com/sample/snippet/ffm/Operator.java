package com.sample.snippet.ffm;

/**
 * LineFilterクラスが使用する比較演算子enumです。<p/>
 *
 * 各列挙子は下記の演算子に対応します。
 *
 * <ul>
 * <li>EQ : ==</li>
 * <li>GTE : &gt;=</li>
 * <li>GT : &gt;</li>
 * <li>LTE : &lt;=</li>
 * <li>LT : &lt;</li>
 * </ul>
 *
 */
public enum Operator {
	EQ(1),
	GTE(2),
	GT(4),
	LTE(8),
	LT(16);

	/** 比較演算に使用するint値 */
	private final int value;

	/**
	 * 各列挙子を比較演算に使用するint値付きで生成します。<p/>
	 *
	 * @param value 比較演算に使用するint値
	 */
	private Operator(int value) {
		this.value = value;
	}

	/**
	 * 比較演算に使用するint値を返却します。<p/>
	 *
	 * @return 比較演算に使用するint値
	 */
	public int getValue() {
		return this.value;
	}
}
