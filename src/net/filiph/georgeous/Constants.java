package net.filiph.georgeous;

public class Constants {
	public static final String FEED_RESULT_INTENT = "net.filiph.georgeous.FEED_RESULT";
	/**
	 * Showed when images for the article (_id given by long extra) are cached
	 * and ready.
	 */
	public static final String IMAGES_CACHED_INTENT = "net.filiph.georgeous.IMAGES_CACHED";

	public static final String GET_ARTICLES_INTENT = "net.filiph.georgeous.GET_ARTICLES";
	public static final String GET_ARTICLE_IMAGES_INTENT = "net.filiph.georgeous.GET_ARTICLE_IMAGES";

	public static final String FEED_RESULT_CODE = "net.filiph.georgeous.FEED_RESULT_CODE";

	public static final int FEED_RESULT_NEW_ARTICLES = 1;
	public static final int FEED_RESULT_NO_NEW_ARTICLES = 2;
	public static final int FEED_RESULT_DATABASE_ERROR = 5;
	public static final int FEED_RESULT_NET_ERROR = 6;
	public static final int FEED_RESULT_OTHER_ERROR = 7;

	public static final String ARTICLE_ID_EXTRA = "net.filiph.georgeous.ARTICLE_ID";
	public static final String ARTICLE_CONTENT_EXTRA = "net.filiph.georgeous.ARTICLE_CONTENT";
}
