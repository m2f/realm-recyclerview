package co.moonmonkeylabs.realmexamples.realmsearchview;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;

import co.moonmonkeylabs.realmexamples.R;
import co.moonmonkeylabs.realmexamples.realmsearchview.model.Blog;
import co.moonmonkeylabs.realmsearchview.RealmSearchAdapter;
import co.moonmonkeylabs.realmsearchview.RealmSearchView;
import co.moonmonkeylabs.realmsearchview.RealmSearchViewHolder;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class SampleSearchActivity extends AppCompatActivity {

    private RealmSearchView realmSearchView;
    private BlogRecyclerViewAdapter adapter;
    private Realm realm;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_search_activity);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resetRealm();
        loadBlogData();

        realmSearchView = (RealmSearchView) findViewById(R.id.search_view);

        realm = Realm.getInstance(getRealmConfig());
        adapter = new BlogRecyclerViewAdapter(this, realm, "title");
        realmSearchView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.menu_main, menu);

        MenuItem myActionMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        return true;
    }

    private void loadBlogData() {
        SimpleDateFormat formatIn = new SimpleDateFormat("MMMM d, yyyy");
        SimpleDateFormat formatOut = new SimpleDateFormat("MM/d/yy");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        Random random = new Random();
        try {
            JsonParser jsonParserBlog =
                    jsonFactory.createParser(getResources().openRawResource(R.raw.blog));
            List<Blog> entries =
                    objectMapper.readValue(jsonParserBlog, new TypeReference<List<Blog>>() {
                    });

            JsonParser jsonParserEmoji =
                    jsonFactory.createParser(getResources().openRawResource(R.raw.emoji));
            List<String> emojies =
                    objectMapper.readValue(jsonParserEmoji, new TypeReference<List<String>>() {});

            int numEmoji = emojies.size();
            for (Blog blog : entries) {
                blog.setEmoji(emojies.get(random.nextInt(numEmoji)));
                try {
                    blog.setDate(formatOut.format(formatIn.parse(blog.getDate())));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            Realm realm = Realm.getInstance(getRealmConfig());
            realm.beginTransaction();
            realm.copyToRealm(entries);
            realm.commitTransaction();
            realm.close();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load blog data.");
        }
    }

    private RealmConfiguration getRealmConfig() {
        Realm.init(this);
        return new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
    }

    private void resetRealm() {
        Realm.deleteRealm(getRealmConfig());
    }

    public class BlogRecyclerViewAdapter
            extends RealmSearchAdapter<Blog, BlogRecyclerViewAdapter.ViewHolder> {

        public BlogRecyclerViewAdapter(
                Context context,
                Realm realm,
                String filterColumnName) {
            super(context, realm, filterColumnName);
        }

        public class ViewHolder extends RealmSearchViewHolder {

            private BlogItemView blogItemView;

            public ViewHolder(FrameLayout container, TextView footerTextView) {
                super(container, footerTextView);
            }

            public ViewHolder(BlogItemView blogItemView) {
                super(blogItemView);
                this.blogItemView = blogItemView;
            }
        }

        @Override
        public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
            ViewHolder vh = new ViewHolder(new BlogItemView(viewGroup.getContext()));
            return vh;
        }

        @Override
        public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
            final Blog blog = adapterData.get(position);
            viewHolder.blogItemView.bind(blog);
        }

        @Override
        public ViewHolder onCreateFooterViewHolder(ViewGroup viewGroup) {
            View v = inflater.inflate(R.layout.footer_view, viewGroup, false);
            return new ViewHolder(
                    (FrameLayout) v,
                    (TextView) v.findViewById(R.id.footer_text_view));
        }

        @Override
        public void onBindFooterViewHolder(ViewHolder holder, int position) {
            super.onBindFooterViewHolder(holder, position);
            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    }
            );
        }
    }
}
