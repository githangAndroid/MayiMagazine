package com.mayinews.g.home.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.mayinews.g.R;
import com.mayinews.g.home.adapter.ImagesAdapter;
import com.mayinews.g.home.bean.SingleAlbum;
import com.mayinews.g.view.CanotSlidingViewpager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;

public class PhotosActivity extends AppCompatActivity implements View.OnClickListener {
    List<String> data;
    @BindView(R.id.back)
    ImageView back;
    @BindView(R.id.currentPage)
    TextView currentPage;
    @BindView(R.id.totalPage)
    TextView totalPage;
    @BindView(R.id.user_avatar)
    CircleImageView userAvatar;
    @BindView(R.id.tv_comment)
    TextView tvComment;
    @BindView(R.id.tv_coll_count)
    TextView tvCollCount;
    @BindView(R.id.tv_zan_count)
    TextView tvZanCount;
    @BindView(R.id.tv_shared_count)
    TextView tvSharedCount;
    @BindView(R.id.viewPager)
    CanotSlidingViewpager viewPager;
    @BindView(R.id.tob_bottom)
    RelativeLayout tobBottom;
    @BindView(R.id.rootView)
    RelativeLayout rootView;
    @BindView(R.id.rl_comments)
    RelativeLayout rlComments;
    @BindView(R.id.rl_collection)
    RelativeLayout rlCollection;
    @BindView(R.id.rl_zan)
    RelativeLayout rlZan;
    @BindView(R.id.rl_shared)
    RelativeLayout rlShared;
    @BindView(R.id.top_view)
    RelativeLayout topView;
    @BindView(R.id.tv_nodata)
    TextView tvNodata;


    private boolean isShow = false;//图片上下的布局是否显示,默认不显示

    private ImagesAdapter adapter = null;
    private PopupWindow popupWindow = null;

    private PopupWindow commentPopWindow = null;  //评论的pop
    private PopupWindow commentListPopWindow = null;
    private ListView comList;//评论的listView
    private RecyclerView comRecyclerView;//评论页面显示收藏的RecyclerView
    private String avatar = null;//图像的url
    private int imagePosition;

    private PopupWindow window;//水印的pop
    private Handler handler = new Handler();
    private String desc;//描述
    private boolean isShowed;//标记水印水否显示过
    private TextView tvDesc;  //水印的描述

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_photos);
        ButterKnife.bind(this);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPage.setText(position + 1 + "");
                imagePosition = position;
                if (position == 8) {
                    //禁止左滑
                    viewPager.setScrollble(false);
                    //显示
                } else {

                    viewPager.setScrollble(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {


            }


        });


        viewPager.setLeftScrLisener(new CanotSlidingViewpager.LeftScr() {
            @Override
            public void leftScr() {
                //显示Pop
                if (!popupWindow.isShowing()) {
                    popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
                }


            }
        });

        /*
               初始化各种PopWindow
         */
        initPop();     //充Vip的popwindow
        showComPop();   // 发送评论的popWindow
        initListPop();//评论列表的popwindow
        showPopwindow();//水印
        Intent intent = getIntent();

        data = intent.getStringArrayListExtra("images");

        if (data != null) {
            String avatar = intent.getStringExtra("avatar");
            int size = data.size();
            Log.e("TAG", "size" + size);
            totalPage.setText("/" + size);
            desc = intent.getStringExtra("desc");

            Glide.with(this).load(buildGlideUrl("http://static.mayinews.com" + avatar)).into(userAvatar);
            initImageViewPager();
        } else {
            String id = intent.getStringExtra("id");

            //请求数据
            OkHttpUtils.get()
                    .url("http://g.mayinews.com/api/getdoc/id/" + id)
                    .build().execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e, int id) {

                }

                @Override
                public void onResponse(String response, int id) {


                    SingleAlbum singleAlbum = JSON.parseObject(response, SingleAlbum.class);
                    if (singleAlbum.getStatus() == 200) {

                        data = singleAlbum.getResult().getPicture();
                         if(data!=null && data.size()>0){

                             totalPage.setText("/" + data.size());
                             desc = singleAlbum.getResult().getDescription();
                             initImageViewPager();
                         }else{
                             currentPage.setVisibility(View.GONE);
                             window.dismiss();
                             viewPager.setVisibility(View.GONE);
                             //没有数据或者出错
                             tvNodata.setVisibility(View.VISIBLE);
                             tobBottom.setVisibility(View.GONE);
                         }

                    } else {
                        currentPage.setVisibility(View.GONE);
                        window.dismiss();
                        viewPager.setVisibility(View.GONE);
                        //没有数据或者出错
                          tvNodata.setVisibility(View.VISIBLE);
                    }


                }
            });


        }

//        initImageViewPager();
//        initPop();


    }


    //显示水印的
    private void showPopwindow() {

        isShow = true;
        window = new PopupWindow(this);
        final View inflate = View.inflate(this, R.layout.watermark_photo, null);

        window.setContentView(inflate);

        window.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        window.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
//        window.setFocusable(true);
        window.setOutsideTouchable(true);  //点击外部消失
        window.setBackgroundDrawable((new BitmapDrawable()));


        AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0.5f);
        alphaAnimation.setDuration(500);
        alphaAnimation.setFillAfter(true);
        inflate.startAnimation(alphaAnimation);
        tvDesc = (TextView) inflate.findViewById(R.id.describe);

//        backgroundAlpha(0.5f);

        window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {

                tobBottom.setVisibility(View.VISIBLE);

            }
        });

        inflate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                window.dismiss();
            }
        });

    }

//    @Override
//
//    public void onWindowFocusChanged(boolean hasFocus) {
//
//
//        super.onWindowFocusChanged(hasFocus);
//
//        if (hasFocus && !isShowed) {
//
//            //显示popwindow
//            showPopwindow();
//
//            isShowed = true;
//
//        }
//
//    }

    /**
     * 设置添加屏幕的背景透明度
     *
     * @param bgAlpha
     */
    public void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        getWindow().setAttributes(lp);
    }


    @Override

    public void onWindowFocusChanged(boolean hasFocus) {


        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && !isShowed) {

            //显示popwindow
            View view = View.inflate(this, R.layout.activity_photos, null);

            window.showAtLocation(view, Gravity.BOTTOM, 0, 0);

            isShowed = true;

        }

    }

    private void initImageViewPager() {
//        showPopwindow();
        tvDesc.setText(desc);
        adapter = new ImagesAdapter(this, data);


        adapter.setImageListener(new ImagesAdapter.ImageOnClickListener() {

            @Override
            public void onClick(int position) {
                if (isShow) {
                    tobBottom.setVisibility(View.GONE);
                } else {
                    tobBottom.setVisibility(View.VISIBLE);
                }

                isShow = !isShow;
            }


        });
        viewPager.setAdapter(adapter);
    }


    @OnClick({R.id.back, R.id.tv_comment, R.id.rl_comments, R.id.rl_shared})
    public void listener(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.tv_comment:
                Toast.makeText(PhotosActivity.this, "评论", Toast.LENGTH_SHORT).show();
                //显示评论的pop
//                showComPop();
                showCommentPop();
                break;
            case R.id.rl_comments:

                //进入评论的pop,显示在上面状态栏下


                commentListPopWindow.showAsDropDown(topView);
                break;

            case R.id.rl_shared:

//                showShare();
                break;


        }
    }

    //显示评论的pop
    private void showCommentPop() {
        commentPopWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
    }

    private void initListPop() {
        View view = LayoutInflater.from(this).inflate(R.layout.comment_list_pop, null, false);
        commentListPopWindow = new PopupWindow(PhotosActivity.this);
        commentListPopWindow.setContentView(view);
        commentListPopWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        commentListPopWindow.setFocusable(true);
        commentListPopWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        commentListPopWindow.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        comList = (ListView) view.findViewById(R.id.listView);
        comRecyclerView = (RecyclerView) view.findViewById(R.id.com_RecyclerView);
        TextView com = (TextView) view.findViewById(R.id.com_comment);
        com.setOnClickListener(this);


    }

    private void showComPop() {

        View view = LayoutInflater.from(this).inflate(R.layout.comment_pop, null, false);
        commentPopWindow = new PopupWindow(PhotosActivity.this);
        commentPopWindow.setContentView(view);
        commentPopWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        commentPopWindow.setFocusable(true);
        commentPopWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        commentPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        //让popupwindow不被输入法隐藏
        commentPopWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final TextView commentContent = (TextView) view.findViewById(R.id.comment_content);
        TextView send = (TextView) view.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (commentContent.getText().toString().trim().length() > 4) {
                    //评论成功

                } else {

                    Toast.makeText(PhotosActivity.this, "评论最少为5个字数", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void initPop() {
        View view = LayoutInflater.from(this).inflate(R.layout.image_pop, null, false);
        popupWindow = new PopupWindow(PhotosActivity.this);
        popupWindow.setContentView(view);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popupWindow.setFocusable(true);
        popupWindow.setWidth(getScreenWidth(this) * 3 / 4);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView paySingle = (TextView) view.findViewById(R.id.pay_single);
        TextView payVip = (TextView) view.findViewById(R.id.pay_vip);
        TextView collection = (TextView) view.findViewById(R.id.collection);
        TextView cancle = (TextView) view.findViewById(R.id.cancle);
        paySingle.setOnClickListener(this);
        payVip.setOnClickListener(this);
        collection.setOnClickListener(this);
        cancle.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.pay_single:
                Toast.makeText(PhotosActivity.this, "购买单张专辑", Toast.LENGTH_SHORT).show();
                break;
            case R.id.pay_vip:
                Toast.makeText(PhotosActivity.this, "购买Vip", Toast.LENGTH_SHORT).show();
                break;
            case R.id.collection:
                Toast.makeText(PhotosActivity.this, "收藏", Toast.LENGTH_SHORT).show();
                break;
            case R.id.cancle:
                popupWindow.dismiss();
                break;
            case R.id.com_comment:
                //显示评论的pop
                showCommentPop();
                break;


        }

    }


    private GlideUrl buildGlideUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        } else {
            return new GlideUrl(url, new LazyHeaders.Builder().addHeader("Referer", "http://m.mayinews.com").build());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        Log.e("TAG", "onSaveInstanceState");
    }


    @Override
    public void onBackPressed() {

        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (window.isShowing()) {
            window.dismiss();
        }
    }


    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }
}
