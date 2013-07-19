package net.filiph.georgeous.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * An Html.ImageGetter that also caches images that it fetches.
 */
public class ImageGetter implements ImageGetterWithManageSpace {
    private static final String TAG = "ImageGetter";

    private static final long MAX_SIZE_OF_CACHE = 5000000; // 5MB self-imposed
                                                           // quota

    /**
     * Sets the bounds of the image according to given DisplayMetrics.
     */
    public static void setBounds(BitmapDrawable d, DisplayMetrics metrics) {
        if (d == null) return;
        int width, height;
        int originalWidthScaled = (int) (d.getIntrinsicWidth() * metrics.density);
        int originalHeightScaled = (int) (d.getIntrinsicHeight() * metrics.density);
        if (originalWidthScaled > metrics.widthPixels) {
            height = d.getIntrinsicHeight() * metrics.widthPixels / d.getIntrinsicWidth();
            width = metrics.widthPixels;
        } else {
            height = originalHeightScaled;
            width = originalWidthScaled;
        }

        d.setBounds(0, 0, width, height);
    }

    /**
     * Creates and sets up the ImageGetter.
     * 
     * @param externalCacheDir
     *            Directory for caching.
     * @param resources
     *            Activity's Resources.
     * @param metrics
     *            Activity's DisplayMetrics.
     * @param cachingOnly
     *            True if the ImageGetter is called solely to cache the fetched images, without
     *            intention to display them right away. This allows for some basic optimization.
     * @throws IllegalStateException
     */
    public ImageGetter(File externalCacheDir, Resources resources, DisplayMetrics metrics,
            boolean cachingOnly) throws IllegalStateException {
        mExternalCacheDir = externalCacheDir;
        mResources = resources;
        mMetrics = metrics;
        mCachingOnly = cachingOnly;

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (mExternalStorageAvailable && mExternalCacheDir != null) {
            cacheFilenames = new HashMap<String, String>();
            String[] filenames = mExternalCacheDir.list();
            for (String filename : filenames) {
                String hash = filename.replaceFirst("[.][^.]+$", "");
                cacheFilenames.put(hash, filename);
            }
        }

        if (cachingOnly && !mExternalStorageWriteable) {
            throw new IllegalStateException(
                    "Trying to create ImageGetter for caching when external "
                            + "storage is unavailable.");
        }
    }

    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;

    /**
     * Whether or not to load images from web when they are not available cached. Normally set to
     * true, but can be set to false when caller uses ImageGetter to cache images (and so doesn't
     * need to show them).
     */
    final boolean mCachingOnly;
    File mExternalCacheDir;

    Resources mResources;
    DisplayMetrics mMetrics;

    /**
     * A Map of image URL hashes and the images respective filenames in the cache.
     */
    HashMap<String, String> cacheFilenames;

    @Override
    public Drawable getDrawable(String url) {
        BitmapDrawable bmp = null;

        if (mExternalStorageAvailable && mExternalCacheDir != null) {
            bmp = getBitmapFromCache(url);
        }

        if (bmp == null) {
            // Drawable not in cache.
            try {
                InputStream in = new java.net.URL(url).openStream();
                boolean cached = cacheImageStream(in, url);
                in.close();
                if (cached && !mCachingOnly) {
                    // Now that we have the file offline, build the drawable.
                    bmp = getBitmapFromCache(url);
                } else {
                    if (!mCachingOnly) {
                        // Try again without caching.
                        in = new java.net.URL(url).openStream();
                        bmp = new BitmapDrawable(mResources, new PatchInputStream(in));
                        in.close();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null; // TODO: return a default X pic?
            } catch (IOException e) {
                e.printStackTrace();
                return null; // TODO: return a default X pic?
            }
        }

        if (bmp != null) {
            setBounds(bmp);
            // TODO: save the bounds somewhere so we can invoke them when
            // creating a blank image?
        }
        return bmp;
    }

    /**
     * This method deletes cached files over quota.
     */
    @Override
    public void manageSpace() {
        if (mExternalStorageAvailable && mExternalCacheDir != null) {
            cacheFilenames = new HashMap<String, String>();
            File[] files = mExternalCacheDir.listFiles();
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    // Sort files by lastModified, descending.
                    return -Long.valueOf(lhs.lastModified()).compareTo(rhs.lastModified());
                }
            });
            long cummulativeSize = 0;
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                long fileSize = f.length();
                cummulativeSize += fileSize;
                if (cummulativeSize > MAX_SIZE_OF_CACHE) {
                    Log.v(TAG, "Deleting file " + f.getAbsolutePath());
                    f.delete();
                    cummulativeSize -= fileSize;
                }
            }

        }
    }

    /**
     * Tries to find the image in the cache.
     */
    private BitmapDrawable getBitmapFromCache(String url) {
        String hash = getUrlHash(url);

        assert (cacheFilenames != null);

        if (cacheFilenames.containsKey(hash)) {
            String filename = cacheFilenames.get(hash);
            File file = new File(mExternalCacheDir, filename);
            assert (file.exists()); // Should exist since it's in the HashMap created moments ago.
            return new BitmapDrawable(mResources, file.getAbsolutePath());
        } else {
            // Did not find the cashed file.
            return null;
        }
    }

    /**
     * Extracts the filename extension from the url.
     */
    private String getExtensionFromUrl(String url) {
        return "image"; // Let the BitmapImageFactory do the work of recognizing
                        // file type. We don't need an extension for that.
    }

    /**
     * Constructs the filename for the local cache file of the image at the given url.
     */
    private String getRelativeCacheFilename(String url) {
        return getUrlHash(url) + "." + getExtensionFromUrl(url);
    }

    /**
     * Creates the hash to use for the cache filename.
     */
    private String getUrlHash(String url) {
        return Integer.toHexString(url.hashCode());
    }

    /**
     * Sets the bounds of the Drawable according to metrics.
     */
    private void setBounds(BitmapDrawable d) {
        assert (mMetrics != null);
        ImageGetter.setBounds(d, mMetrics);
    }

    /**
     * Reads from the stream and saves it to the local filesystem.
     * 
     * @return True on success, false on failure.
     */
    private boolean cacheImageStream(InputStream in, String url) {
        if (!mExternalStorageAvailable || !mExternalStorageWriteable || mExternalCacheDir == null) {
            return false;
        }
        try {
            final File file = new File(mExternalCacheDir, getRelativeCacheFilename(url));
            final OutputStream output = new FileOutputStream(file);
            try {
                try {
                    final byte[] buffer = new byte[1024];
                    int read;

                    while ((read = in.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }

                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        cacheFilenames.put(getUrlHash(url), getRelativeCacheFilename(url));
        return true;
    }

    /**
     * Fixing BitmapFactory.decodeStream() as per
     * https://code.google.com/p/android/issues/detail?id=6066
     */
    private class PatchInputStream extends FilterInputStream {
        public PatchInputStream(InputStream in) {
            super(in);
        }

        @Override
        public long skip(long n) throws IOException {
            long m = 0L;
            while (m < n) {
                long _m = in.skip(n - m);
                if (_m == 0L) break;
                m += _m;
            }
            return m;
        }
    }
}
