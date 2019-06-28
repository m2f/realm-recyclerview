package co.moonmonkeylabs.realmexamples.realmrecyclerview;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import co.moonmonkeylabs.realmexamples.R;
import co.moonmonkeylabs.realmexamples.realmrecyclerview.models.Message;
import co.moonmonkeylabs.realmrecyclerview.DividerItemDecoration;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

public class LoadingListActivity extends RealmBaseActivity {

    private Realm realm;
    private static List<String> quotes = Arrays.asList(
            "Always borrow money from a pessimist. He won’t expect it back.",
            "Dogs have masters. Cats have staff.",
            "The best way to lie is to tell the truth . . . carefully edited truth.",
            "If at first you don’t succeed . . . so much for skydiving.",
            "A bargain is something you don’t need at a price you can’t resist.",
            "My mother never saw the irony in calling me a son-of-a-bitch.",
            "God gave us our relatives; thank God we can choose our friends.",
            "Women who seek to be equal with men lack ambition.",
            "If you do a job too well, you’ll get stuck with it.",
            "Insanity is hereditary. You get it from your children.",
            "Always borrow money from a pessimist. He won’t expect it back.",
            "Dogs have masters. Cats have staff.",
            "The best way to lie is to tell the truth . . . carefully edited truth.",
            "If at first you don’t succeed . . . so much for skydiving.",
            "A bargain is something you don’t need at a price you can’t resist.",
            "My mother never saw the irony in calling me a son-of-a-bitch.",
            "God gave us our relatives; thank God we can choose our friends.",
            "Women who seek to be equal with men lack ambition.",
            "If you do a job too well, you’ll get stuck with it.",
            "Insanity is hereditary. You get it from your children.");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_list);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if(null != getSupportActionBar()) {
            getSupportActionBar().setTitle("Loading List");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        resetRealm();
        realm = Realm.getInstance(getRealmConfig());

        final LoadingListAdapter loadingListAdapter = new LoadingListAdapter(this, null);

        final RealmRecyclerView realmRecyclerView = (RealmRecyclerView) findViewById(R.id.list_rv);
        realmRecyclerView.setAdapter(loadingListAdapter);
        realmRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        final Handler handler = new Handler();
        addMessages();
        final ProgressBar p = (ProgressBar) findViewById(R.id.loading_list);
        p.setVisibility(View.VISIBLE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadingListAdapter.setAdapterData(
                        realm.where(Message.class)
                                .sort("timestamp", Sort.ASCENDING)
                                .findAll());
                p.setVisibility(View.GONE);
            }
        }, 3000);
    }

    private void addMessages() {
        for (String quote: quotes) {
            realm.beginTransaction();
            Message message = realm.createObject(Message.class, UUID.randomUUID().toString());
            message.setMessage(quote);
            message.setTimestamp(System.currentTimeMillis());
            realm.commitTransaction();
        }
    }

    public class LoadingListAdapter extends RealmBasedRecyclerViewAdapter<Message, LoadingListAdapter.QuoteViewHolder> {

        public LoadingListAdapter(Context context, RealmResults<Message> realmResults) {
            super(context, realmResults, true, true);
        }

        @Override
        public QuoteViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int i) {
            View v = inflater.inflate(R.layout.loading_item_view, viewGroup, false);
            return new QuoteViewHolder((FrameLayout) v);
        }

        @Override
        public void onBindRealmViewHolder(QuoteViewHolder quoteViewHolder, int position) {
            final Message message = adapterData.get(position);
            quoteViewHolder.quoteTextView.setText(message.getMessage());
        }

        public class QuoteViewHolder extends RealmViewHolder {
            public TextView quoteTextView;
            public QuoteViewHolder(FrameLayout container) {
                super(container);
                this.quoteTextView = (TextView) container.findViewById(R.id.quote_text_view);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != realm) {
            realm.close();
        }
    }
}
