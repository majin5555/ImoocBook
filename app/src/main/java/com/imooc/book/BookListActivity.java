package com.imooc.book;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;



public class BookListActivity extends AppCompatActivity {
    public static        List<String> sNeedReqPermissions = new ArrayList<>();
    private static final int              PERMISSION_RQUEST_CODE = 100;
    static {
        sNeedReqPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private             PermissionUtils  mPermissionUtils;
    private static final String TAG = "BookListActivity";
    private ListView mListView;
    private List<BookListResult.Book> mBooks = new ArrayList<>();
    private AsyncHttpClient mClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);
        //'首先判断当前的权限问题
        mPermissionUtils = new PermissionUtils(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissionUtils.request(sNeedReqPermissions, PERMISSION_RQUEST_CODE, new PermissionUtils.CallBack() {
                @Override
                public void grantAll() {
                    Toast.makeText(BookListActivity.this, "获取了全部权限", Toast.LENGTH_SHORT).show();
                    //finish();
                }

                @Override
                public void denied() {
                    finish();
                    Toast.makeText(BookListActivity.this, "有权限未获取", Toast.LENGTH_SHORT).show();
                }
            });
        }

        mListView = (ListView) findViewById(R.id.book_list_view);
        String url = "http://www.imooc.com/api/teacher?type=10";

        mClient = new AsyncHttpClient();
        mClient.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                final String result = new String(responseBody);

                Gson gson = new Gson();
                BookListResult bookListResult = gson.fromJson(result, BookListResult.class);

                mBooks = bookListResult.getData();

                mListView.setAdapter(new BookListAdapter());


            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }

            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, BookListActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private class BookListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mBooks.size();
        }

        @Override
        public Object getItem(int i) {
            return mBooks.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            final BookListResult.Book book = mBooks.get(position);

            ViewHolder viewHolder = new ViewHolder();
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_book_list_view, null);
                viewHolder.mNameTextView = (TextView) view.findViewById(R.id.name_text_view);
                viewHolder.mButton = (Button) view.findViewById(R.id.book_button);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.mNameTextView.setText(book.getBookname());
            final String path = Environment.getExternalStorageDirectory() + "/imooc/" + book.getBookname() + ".txt";
            final File file = new File(path);
            viewHolder.mButton.setText(file.exists() ? "点击打开" : "点击下载");

            final ViewHolder finalViewHolder = viewHolder;
            viewHolder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 下载的功�?
                    if (file.exists()) {
                        BookActivity.start(BookListActivity.this, path);
                    } else {
                        mClient.addHeader("Accept-Encoding", "identity");
                        mClient.get(book.getBookfile(), new FileAsyncHttpResponseHandler(
                                file) {
                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                                finalViewHolder.mButton.setText("下载失败");
                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, File file) {
                                finalViewHolder.mButton.setText("点击打开");
                            }

                            @Override
                            public void onProgress(long bytesWritten, long totalSize) {
                                super.onProgress(bytesWritten, totalSize);
                                finalViewHolder.mButton.setText(String.valueOf(bytesWritten * 100 / totalSize) + "%");
                            }
                        });

                    }

                }
            });

            return view;
        }

        class ViewHolder {
            public TextView mNameTextView;
            public Button mButton;
        }
    }
}
