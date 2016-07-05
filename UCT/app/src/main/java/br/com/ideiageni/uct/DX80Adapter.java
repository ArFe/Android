package br.com.ideiageni.uct;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;


/**
 * Created by ariel on 27/06/2016.
 * Adapter específico para Tabela de Memória DX80
 */
public class DX80Adapter extends SimpleAdapter {
    private final LayoutInflater mInflater;

    private int[] mTo;

    private int mResource;

    private boolean[] mVisibility;
    private int[] mValues;
    private String[] mNames;
    private int mHScrollIndex;


    public DX80Adapter(Context context, int resource, String[] from, int[] to,
                       String[] names, int[] values, boolean[] visibility) {
        super(context, null, resource, from, to);
        mTo = to;
        mVisibility = visibility;
        mValues =values;
        mNames = names;
        mResource = resource;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = mInflater.inflate(mResource, parent, false);
        } else {
            v = convertView;
        }

         bindView(position, v);

        return v;
    }

    private void bindView(int position, View view) {

        final int[] to = mTo;
        final int count = to.length;

        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {
                String text;
                if(i>=count - mHScrollIndex) {
                    text = "";
                }else if(i>0){
                    text = String.valueOf(mValues[position*16+i+mHScrollIndex-1]);
                    if(getVisibility(position)) v.setAlpha((float) 1);
                    else  v.setAlpha((float) 0.3);
                } else {
                    v.setAlpha((float) 1);
                    text = mNames[position];
                }


                if (v instanceof TextView) {
                    // Note: keep the instanceof TextView check at the bottom of these
                    // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                    setViewText((TextView) v, text);
                } else {
                    throw new IllegalStateException(v.getClass().getName() + " is not a " +
                            " view that can be bounds by this SimpleAdapter");
                }
            }
        }
    }

    public boolean getVisibility(int position){
        return mVisibility[position];
    }

    public void setHScrollIndex(int hScrollIndex) {mHScrollIndex = hScrollIndex;}

    @Override
    public int getCount() {
        return mNames.length;
    }

}
