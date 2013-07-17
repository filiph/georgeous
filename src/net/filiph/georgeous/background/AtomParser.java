package net.filiph.georgeous.background;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.filiph.georgeous.data.Article;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

/**
 * Parses an Atom XML from the provided InputStream. Returns a List of Articles.
 * 
 * Parser appropriated from http://developer.android.com/training/basics/network-ops/xml.html.
 */
public class AtomParser {
    private static final String ns = null;

    // We don't use namespaces

    public static List<Article> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private static List<Article> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Article> entries = new ArrayList<Article>();

        parser.require(XmlPullParser.START_TAG, ns, "feed");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("entry")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private static Article readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "entry");
        Article article = new Article();
        // TODO: categories
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
            	article.title = readTagContent(parser, "title").trim();
            } else if (name.equals("content")) {
            	article.content = readTagContent(parser, "content");
            	article.author_guess = guessAuthor(article.content);
            } else if (name.equals("published")) {
            	article.published_timestamp = readTagContent(parser, "published");
            } else if (name.equals("updated")) {
            	article.updated_timestamp = readTagContent(parser, "updated");
            } else if (name.equals("media:thumbnail")) {
            	article.thumbnail_url = readTagAttribute(parser, "media:thumbnail", "url");
            } else if (name.equals("link")) {
            	String linkRel = readLinkRel(parser);
            	if (linkRel != null) {
            		article.canonical_url = linkRel;
            	}
            } else if (name.equals("feedburner:origLink")) {
            	// Origlink seems to be the last tag in <entry> and it's a much better URL than <link rel="alternate">. So if we have it
            	// let's go ahead and overwrite.
            	article.canonical_url = readTagContent(parser, "feedburner:origLink");
            } else {
                skip(parser);
            }
        }
        return article;
    }
    
    private static final String POSTED_BY_STRING = "Posted by";
    private static final int MAX_CHARS_WALKED = 400;
    private static final int MAX_CHARS_IN_NAME = 40;
    
    
    private static String guessAuthor(String content) {
    	String guess = null;
    	int maxChars = Math.min(content.length(), MAX_CHARS_WALKED);
    	int i = content.indexOf(POSTED_BY_STRING);
    	if (i == -1) {
    		return null;
    	}
    	int start = i + POSTED_BY_STRING.length() + 1;
    	final String localContent = content.substring(start, maxChars);
    	i = start;
    	// First pass: name is in an <a> tag
    	int endATag = localContent.indexOf("</a>");
    	if (endATag != -1) {
    		int startATag = localContent.indexOf(">", start);
    		if (startATag + 1 < endATag) {
    			guess = localContent.substring(startATag + 1, endATag).trim();
    			if (guess.indexOf("<") == -1 && guess.length() <= MAX_CHARS_IN_NAME) {  // basic check
    				return guess;
    			}
    		}
    	}
    	// Second pass: name is not in a tag
    	int comma = localContent.indexOf(",");
    	if (comma != -1) {
    		guess = localContent.substring(0, comma).trim();
    		if (guess.indexOf("<") == -1 && guess.length() <= MAX_CHARS_IN_NAME) {  // basic check
    			return guess;
    		}
    	}
    	
    	return null;
    }

    private static String readTagContent(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
    	parser.require(XmlPullParser.START_TAG, ns, tagName);
    	String content = readText(parser);
    	parser.require(XmlPullParser.END_TAG, ns, tagName);
    	return content;
    }
    
    private static String readTagAttribute(XmlPullParser parser, String tagName, String attrName) throws IOException, XmlPullParserException {
        String value = null;
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String tag = parser.getName();
        if (tag.equals(tagName)) {
        	value = parser.getAttributeValue(null, attrName);
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return value;
    }
    
    // Processes link tag in the feed and, if it is 'link rel', saves the href.
    private static String readLinkRel(XmlPullParser parser) throws IOException, XmlPullParserException {
        String url = null;
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String tag = parser.getName();
        if (tag.equals("link")) {
        	if (parser.getAttributeValue(null, "rel").equals("alternate")) {
        		url = parser.getAttributeValue(null, "href");
        	}
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return url;
    }

    // For the tags title and summary, extracts their text values.
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                    depth--;
                    break;
            case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
