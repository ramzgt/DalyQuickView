package com.daly.daly.dalyquickview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// custom UI by Andrew P Ramsey for Daly Computers 2014
public class AutoCompleteEditText extends AutoCompleteTextView {

    public ArrayAdapter<String> adapter;
    private String startAtSymbol = "";

    public AutoCompleteEditText(Context context){
        this(context, null);
    }
    
    public AutoCompleteEditText(Context context, AttributeSet attrs){
        super(context,attrs);
        init();
    }
    
    public AutoCompleteEditText(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
	adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());
        setAdapter(adapter);
        setDropDownWidth(WindowManager.LayoutParams.WRAP_CONTENT);
    }
    
    public void setStartAtSymbol(String symbol){
        startAtSymbol = symbol;
    }
    
    public void setAutoCompleteList(String[] dataList){
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, dataList);
        setAdapter(adapter);
    }

    @Override
    public boolean enoughToFilter() {
        if(getText() != null){
            return getText().length() != 0;
        }
        return true;
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode){
        // This is the code fix for the 3-4-5-6 no dropdown issue.
    	// MUST override filter to allow raw results to be displayed
    	//Log.w("performFiltering()",""+text.toString());
	    	/*String beforeCursor = getText().toString().substring(0, getSelectionStart());
	        Pattern pattern = Pattern.compile(getRegularExpression());
	        Matcher matcher = pattern.matcher(beforeCursor);
	        if (matcher.find()) {
	            text = matcher.group(0);
	        }
	    	super.performFiltering(text, keyCode);*/
    }
	
    /*@Override
    protected void replaceText(CharSequence text){
        String beforeCursor = getText().toString().substring(0, getSelectionStart());
        String afterCursor = getText().toString().substring(getSelectionStart());

        Pattern pattern = Pattern.compile("#\\S*");
        Matcher matcher = pattern.matcher(beforeCursor);
        StringBuffer sb = new StringBuffer();
        int matcherStart = 0;
        while (matcher.find()) {
            int curPos = getSelectionStart();
            if(curPos > matcher.start() &&
                    curPos <= matcher.end()){
                matcherStart = matcher.start();
                matcher.appendReplacement(sb, text.toString()+" ");
            }
        }
        matcher.appendTail(sb);
        setText(sb.toString()+afterCursor);
        setSelection(matcherStart + text.length()+1);
    	super.replaceText(text);
    }*/
    
    private String getRegularExpression(){
        return startAtSymbol+"\\S*\\z";
    }
    
    
}