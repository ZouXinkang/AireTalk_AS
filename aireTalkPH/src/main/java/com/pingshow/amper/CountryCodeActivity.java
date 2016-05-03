package com.pingshow.amper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.util.LBMUtil;

/**
 * Created by jack on 2016/3/2.
 */
public class CountryCodeActivity extends Activity {

    private ListView lv_countrycode;
    private CountryCodeAdapter countryCodeAdapter;
    private String[] countryCodeArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.countrycode);
        countryCodeArray = getResources().getStringArray(R.array.phone_code_list);
        lv_countrycode = (ListView) findViewById(R.id.lv_countrycode);

        if (countryCodeAdapter == null) {
            countryCodeAdapter = new CountryCodeAdapter();
            lv_countrycode.setAdapter(countryCodeAdapter);
        } else {
            countryCodeAdapter.notifyDataSetChanged();
        }

        lv_countrycode.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String s = countryCodeArray[position];
                String[] array = s.split("\\+");
                Intent intent = new Intent();
                intent.putExtra("country",array[0]);
                intent.putExtra("iso",array[1]);
                setResult(1, intent);
                finish();
            }
        });
    }

    private class CountryCodeAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return countryCodeArray.length;
        }

        @Override
        public Object getItem(int position) {
            return countryCodeArray[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(CountryCodeActivity.this, R.layout.countrycode_item, null);
                holder.tv_countrycode_detail = (TextView)convertView.findViewById(R.id.tv_countrycode_detail);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }
            holder.tv_countrycode_detail.setText(countryCodeArray[position]);
            return convertView;
        }
    }

    class ViewHolder {
        TextView tv_countrycode_detail;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
