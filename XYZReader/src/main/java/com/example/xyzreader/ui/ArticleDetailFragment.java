package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;

public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private long itemId;
    private int mutedColor = 0xFF333333;
    private int topInset;
    private int scrollY;
    private int statusBarFullOpacityBottom;
    private boolean isCard = false;

    private Cursor cursor;
    private View rootView;
    private View photoContainerView;
    private ImageView photoImageView;
    private ObservableScrollView observableScrollView;
    private DrawInsetsFrameLayout drawInsetsFrameLayout;
    private ColorDrawable statusBarColorDrawable;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    public ArticleDetailFragment() {
    }

    public static android.support.v4.app.Fragment newInstance(long itemId) {
        Bundle arguments = new Bundle();

        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Objects.requireNonNull(getArguments()).containsKey(ARG_ITEM_ID)) {
            itemId = getArguments().getLong(ARG_ITEM_ID);
        }

        isCard = getResources().getBoolean(R.bool.detail_is_card);

        statusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        drawInsetsFrameLayout = rootView.findViewById(R.id.draw_insets_frame_layout);
        drawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                topInset = insets.top;
            }
        });

        observableScrollView = rootView.findViewById(R.id.scrollview);
        observableScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                scrollY = observableScrollView.getScrollY();
                getActivityCast().onUpButtonFloorChanged(itemId, ArticleDetailFragment.this);
                photoContainerView.setTranslationY((int) (scrollY - scrollY / PARALLAX_FACTOR));
                updateStatusBar();
            }
        });

        photoImageView = rootView.findViewById(R.id.photo);
        photoContainerView = rootView.findViewById(R.id.photo_container);

        statusBarColorDrawable = new ColorDrawable(0);

        rootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat
                        .IntentBuilder
                        .from(Objects.requireNonNull(getActivity()))
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        updateStatusBar();

        return rootView;
    }

    private void updateStatusBar() {
        int color = 0;

        if (photoImageView != null && topInset != 0 && scrollY > 0) {
            float f = progress(scrollY,
                    statusBarFullOpacityBottom - topInset * 3,
                    statusBarFullOpacityBottom - topInset);

            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mutedColor) * 0.9),
                    (int) (Color.green(mutedColor) * 0.9),
                    (int) (Color.blue(mutedColor) * 0.9));
        }

        statusBarColorDrawable.setColor(color);
        drawInsetsFrameLayout.setInsetBackground(statusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min));
    }

    static float constrain(float val) {
        if (val < (float) 0) {
            return (float) 0;
        } else if (val > (float) 1) {
            return (float) 1;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    private void bindViews() {
        if (rootView == null) {
            return;
        }

        TextView titleView = rootView.findViewById(R.id.article_title);
        TextView bylineView = rootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        final TextView bodyView = rootView.findViewById(R.id.article_body);


        bodyView.setTypeface(Typeface.createFromAsset(getResources()
                .getAssets(), "Rosario-Regular.ttf"));

        if (cursor != null) {
            rootView.setAlpha(0);
            rootView.setVisibility(View.VISIBLE);
            rootView.animate().alpha(1);

            titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();

            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + cursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + cursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            bodyView.setText(Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)
                    .substring(0, 2500)
                    .replaceAll("\r\n\r\n", "<br /><br />")
                    .replaceAll("\r\n", " ")
                    .replaceAll("  ", "")));

            final Button readMoreButton = rootView.findViewById(R.id.read_more_button);
            readMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    readMoreButton.setVisibility(View.GONE);
                    bodyView.setText(Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)
                            .replaceAll("\r\n\r\n", "<br /><br />")
                            .replaceAll("\r\n", " ")
                            .replaceAll("  ", "")));
                }
            });

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(cursor.getString(ArticleLoader.Query.PHOTO_URL),
                            new ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(ImageLoader.ImageContainer imageContainer,
                                                       boolean b) {
                                    Bitmap bitmap = imageContainer.getBitmap();
                                    if (bitmap != null) {
                                        Palette p = Palette.generate(bitmap, 12);
                                        mutedColor = p.getDarkMutedColor(0xFF333333);
                                        photoImageView.setImageBitmap(imageContainer.getBitmap());
                                        rootView.findViewById(R.id.meta_bar)
                                                .setBackgroundColor(mutedColor);
                                        updateStatusBar();
                                    }
                                }

                                @Override
                                public void onErrorResponse(VolleyError volleyError) {

                                }
                            });
        } else {
            rootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), itemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }

            return;
        }

        this.cursor = cursor;
        if (this.cursor != null && !this.cursor.moveToFirst()) {
            this.cursor.close();
            this.cursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        cursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (photoContainerView == null || photoImageView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        return isCard
                ? (int) photoContainerView.getTranslationY() + photoImageView.getHeight() - scrollY
                : photoImageView.getHeight() - scrollY;
    }
}
