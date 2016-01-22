package com.pingshow.amper.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class DigitEditView extends EditText {

	public DigitEditView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before, int after) {
		if (text.length()>17) 
			setTextSize(28);
		else if (text.length()>15) 
			setTextSize(32);
		else if (text.length()>13) 
			setTextSize(36);
		else if (text.length()>11) 
			setTextSize(40);
		else if (text.length()>9) 
			setTextSize(42);
		else if (text.length()>0)
			setTextSize(43);
		else
			setTextSize(16);
		super.onTextChanged(text, start, before, after);
	}
}
