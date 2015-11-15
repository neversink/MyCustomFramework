package com.example.xiachen.myframework;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by xiachen on 15/11/15.
 */
public class ImgLoaderActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgloader);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment);
        if (fragment == null) {
            fragment = new ListImgsFragment();
            fm.beginTransaction().add(R.id.fragment, fragment).commit();
        }

    }
}
