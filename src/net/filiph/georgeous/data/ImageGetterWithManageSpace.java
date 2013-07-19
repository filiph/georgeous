package net.filiph.georgeous.data;

import android.text.Html;

/**
 * A common interface for Html.ImageGetter classes that also cache files and
 * therefore need to manage their internal storage self-imposed quota. 
 */
public interface ImageGetterWithManageSpace extends Html.ImageGetter {
	/**
	 * Tells the ImageGetter to delete files above the self-imposed quota.
	 */
	public void manageSpace();
}
