package net.filiph.georgeous.data;

public class Article {

	public Article() {
	}

	public Article(String title, String author_guess, String canonical_url) {
		this.title = title;
		this.author_guess = author_guess;
		this.canonical_url = canonical_url;
	}

	public String title;
	public String content;
	public String author_guess;
	public String thumbnail_url;
	public String canonical_url;
	public String published_timestamp;
	public String updated_timestamp;
	/**
	 * A string with human-readable information (published, author).
	 */
	public String human_info;
	// public int readcount = 0;
	// public String[] categories;
}
