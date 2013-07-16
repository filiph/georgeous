package net.filiph.georgeous.data;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

public class BlankImageGetter implements ImageGetterWithManageSpace {

	private final Resources mResources;
	private final BitmapDrawable mDrawable;
	
	public BlankImageGetter(Resources resources, DisplayMetrics metrics) {
		mResources = resources;
		
//		mDrawable = new BitmapDrawable(mResources, BitmapFactory.decodeResource(mResources, R.drawable.loader)); - not working because gifs are not supported
		mDrawable = new BitmapDrawable(mResources);
		ImageGetter.setBounds(mDrawable, metrics);
		
	}
	
	@Override
	public Drawable getDrawable(String source) {
		return mDrawable;
	}

	@Override
	public void manageSpace() {
		// No need to implement.
	}
}
