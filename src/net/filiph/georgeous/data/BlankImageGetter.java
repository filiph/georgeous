package net.filiph.georgeous.data;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

/**
 * An Html.ImageGetter that returns an empty Drawable. It may be extended to 
 * show a placeholder image or to set bounds for the image if we know them.  
 */
public class BlankImageGetter implements ImageGetterWithManageSpace {

	public BlankImageGetter(Resources resources, DisplayMetrics metrics) {
		mResources = resources;

		// mDrawable = new BitmapDrawable(mResources,
		// BitmapFactory.decodeResource(mResources, R.drawable.loader)); - not
		// working because gifs are not supported
		mDrawable = new BitmapDrawable(mResources);
		ImageGetter.setBounds(mDrawable, metrics);
	}

	private final Resources mResources;

	private final BitmapDrawable mDrawable;

	@Override
	public Drawable getDrawable(String source) {
		return mDrawable;
	}

	@Override
	public void manageSpace() {
		// No need to implement.
	}
}
