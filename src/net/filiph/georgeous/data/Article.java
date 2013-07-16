package net.filiph.georgeous.data;

public class Article {
	public Article(String title, String content, String canonical_url, String thumbnail_url) {
		this.title = title;
		this.content = content;
		this.canonical_url = canonical_url;
		this.thumbnail_url = thumbnail_url;
	}
	
	public String title;
	public String content;
	public String author_guess;
	public String thumbnail_url;
	public String canonical_url;
	public String created_timestamp;
	public String updated_timestamp;
	public int readcount;
	public String[] categories;

	// TODO: implement getters and make this into an upstanding java class
}
