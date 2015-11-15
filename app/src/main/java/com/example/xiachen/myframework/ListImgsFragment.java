package com.example.xiachen.myframework;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.xiachen.myimageloader.ImageLoader;
import com.example.xiachen.myimageloader.Images;

/**
 * Created by xiachen on 15/11/15.
 */
public class ListImgsFragment extends Fragment{
    private GridView mGridView;
    private String[] mUrlStrs = Images.imageThumbUrls;
    private ImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageLoader = ImageLoader.getInstance(3, ImageLoader.Type.LIFO);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_imgs, container, false);
        mGridView = (GridView) view.findViewById(R.id.id_gridview);
        setUpAdapter();
        return view;
    }

    private void setUpAdapter() {
        if (getActivity() == null || mGridView == null) {
            return;
        }

        if (mUrlStrs != null) {
            mGridView.setAdapter(new ListImgItemAdapter(getActivity(), 0, mUrlStrs));
        }
    }

    private class ListImgItemAdapter extends ArrayAdapter<String> {

        public ListImgItemAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_fragment_list_imgs, parent, false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.id_img);
            imageView.setImageResource(R.drawable.pictures_no);
            mImageLoader.loadImage(getItem(position), imageView, true);
            return convertView;
        }
    }
}



