package com.nolanlawson.apptracker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;

public class DrawableUtil {
	
	public static Bitmap convertIconToBitmap(Context context, Drawable drawable) {
	
		int iconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
		return toBitmap(drawable, iconSize, iconSize);
	}
	
	public static Bitmap toBitmap(Drawable drawable, int width, int height) {
		Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas c = new Canvas(bmp);
		drawable.setBounds(new Rect(0,0,width,height));
		drawable.draw(c);

		return bmp;
	}
}
