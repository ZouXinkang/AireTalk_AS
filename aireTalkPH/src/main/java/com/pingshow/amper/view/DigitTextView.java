package com.pingshow.amper.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class DigitTextView extends TextView {

	public DigitTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before,
			int after) {
		if (text.length()>17) 
			setTextSize(21);
		else if (text.length()>15) 
			setTextSize(22);
		else if (text.length()>13) 
			setTextSize(23);
		else if (text.length()>11) 
			setTextSize(24);
		else if (text.length()>9) 
			setTextSize(25);
		else 
			setTextSize(26);
		super.onTextChanged(text, start, before, after);
	}
}
