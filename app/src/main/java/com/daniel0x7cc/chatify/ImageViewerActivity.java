package com.daniel0x7cc.chatify;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.daniel0x7cc.chatify.customviews.TouchImageView;
import com.daniel0x7cc.chatify.utils.Consts;
import com.squareup.picasso.Picasso;

public class ImageViewerActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        String userName = getIntent().getStringExtra(Consts.KEY_USER_NAME);
        String imageUrl = getIntent().getStringExtra(Consts.KEY_URL);

        TouchImageView imageView = (TouchImageView) findViewById(R.id.actImageViewer_imageView);
        Picasso.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_loading_anim)
                .error(R.drawable.placeholder_image_error)
                .into(imageView);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            String title = String.format(getString(R.string.picture_of), userName);
            setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
