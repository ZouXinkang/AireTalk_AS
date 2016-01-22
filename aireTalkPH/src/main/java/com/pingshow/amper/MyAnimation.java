package com.pingshow.amper;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class MyAnimation {

	public static float fromAlpha = 1.0f;
	public static float toAlpha = 0.0f;
	public static float fromDegrees =0;
	public static float toDegrees = 360;
	public static int pivotXType = Animation.RELATIVE_TO_SELF;
	public static float pivotXValue = 0.5f;
	public static int pivotYType = Animation.RELATIVE_TO_SELF;
	public static float pivotYValue = 0.5f;
	
	public static float scaleFromX     		= 1.0f;;
	public static float scaleToX 			= 1.5f;
	public static float scaleFromY 			= 1.0f;
	public static float scaleToY 			= 1.5f;
	public static int   scalePivotXType 	= Animation.RELATIVE_TO_SELF;
	public static float scalePivotXValue	= 1.0f;
	public static int   scalePivotYType 	= Animation.RELATIVE_TO_SELF;
	public static float scalePivotYValue 	= 1.0f;
	public static void setScaleAlphaRotate(ImageView fImageView,boolean isAlpha,boolean isRotate,boolean isScale,
			int duration , int startOffset ,  AnimationListener animationListener){
		fImageView.setVisibility(View.VISIBLE);
		AnimationSet animationSet = new AnimationSet(true);
		if(isAlpha){
			AlphaAnimation alpha = new AlphaAnimation(fromAlpha, toAlpha);
			animationSet.addAnimation(alpha);
		}
		if(isRotate){
			RotateAnimation rotate = new RotateAnimation(fromDegrees, toDegrees,
					pivotXType,pivotXValue,pivotYType,pivotYValue);
			animationSet.addAnimation(rotate);
		}
		if(isScale){
			ScaleAnimation scale = new ScaleAnimation(scaleFromX,scaleToX,scaleFromY,scaleToY,
					scalePivotXType,scalePivotXValue,scalePivotYType,scalePivotYValue);
			animationSet.addAnimation(scale);
		}
		animationSet.setDuration(duration);
		animationSet.setStartOffset(startOffset);
		fImageView.startAnimation(animationSet);
		animationSet.setAnimationListener(animationListener);
	}
	
	public static void setAlpha(float setFromAlpha, float setToAlpha){
			fromAlpha = setFromAlpha;
			toAlpha = setToAlpha;
	}
	
	public static void setRotate(float setFromDegrees,float setToDegrees,
			int setPivotXType,float setPivotXValue,int setPivotYType,float setPivotYValue){
			fromDegrees =  setFromDegrees;
			toDegrees =  setToDegrees;
			pivotXType = setPivotXType;
			pivotXValue =  setPivotXValue;
			pivotYType =  setPivotYType;
			pivotYValue = setPivotYValue;
	}
	
	public static void setScale(float setScaleFromX, float setScaleToX, float setScaleFromY,
			float setScaleToY, int setScalePivotXType, float setScalePivotXValue, 
			int setScalePivotYType,	float setScalePivotYValue) {
				scaleFromX = setScaleFromX;
				scaleToX = setScaleToX;
				scaleFromY = setScaleFromY;
				scaleToY = setScaleToY;
				scalePivotXType = setScalePivotXType;
				scalePivotXValue = setScalePivotXValue;
				scalePivotYType = setScalePivotYType;
				scalePivotYValue = setScalePivotYValue;
	}
	
}
