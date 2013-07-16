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

    // This class represents a single entry (post) in the XML feed.
    // It includes the data members "title," "link," and "summary."
    public static class Entry {
        public final String title;
        public final String link;
        public final String summary;

        private Entry(String title, String summary, String link) {
            this.title = title;
            this.summary = summary;
            this.link = link;
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private static Article readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "entry");
        String title = null;
        String content = null;
        String canonical_url = null;
        String thumbnail_url = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readTitle(parser).trim();
            } else if (name.equals("content")) {
                content = readContent(parser);
            } else if (name.equals("media:thumbnail")) {
                thumbnail_url = readThumbnailUrl(parser);
            } else if (name.equals("feedburner:origLink")) {
            	canonical_url = readOrigLink(parser);
            } else {
                skip(parser);
            }
        }
        return new Article(title, content, canonical_url, thumbnail_url);
    }

    // Processes title tags in the feed.
    private static String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    // Processes feedburner urls in the feed.
    private static String readOrigLink(XmlPullParser parser) throws IOException, XmlPullParserException {
    	parser.require(XmlPullParser.START_TAG, ns, "feedburner:origLink");
    	String title = readText(parser);
    	parser.require(XmlPullParser.END_TAG, ns, "feedburner:origLink");
    	return title;
    }

    // Processes link tags in the feed.
    private static String readThumbnailUrl(XmlPullParser parser) throws IOException, XmlPullParserException {
        String url = "";
        parser.require(XmlPullParser.START_TAG, ns, "media:thumbnail");
        String tag = parser.getName();
        if (tag.equals("media:thumbnail")) {
            url = parser.getAttributeValue(null, "url");
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, "media:thumbnail");
        return url;
    }

    // Processes summary tags in the feed.
    private static String readContent(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "content");
        String content = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "content");
        return content;
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
