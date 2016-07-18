package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.UUID;

import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import co.moonmonkeylabs.realmrecyclerview.example.models.Message;
import co.moonmonkeylabs.realmrecyclerview.example.models.TodoItem;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

public class ChatActivity extends RealmBaseActivity {

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        resetRealm();
        realm = Realm.getInstance(getRealmConfig());
        RealmResults<Message> messages = realm
                .where(Message.class)
                .findAllSorted("timestamp", Sort.ASCENDING);
        ChatAdapter chatAdapter =
                new ChatAdapter(this, messages);
        RealmRecyclerView realmRecyclerView =
                (RealmRecyclerView) findViewById(R.id.chat_list_rv);
        realmRecyclerView.setAdapter(chatAdapter);

        final EditText chatInput = (EditText) findViewById(R.id.chat_send_input);
        Button chatSendBtn = (Button) findViewById(R.id.chat_send_btn);
        chatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMessage(chatInput.getText().toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    public void addMessage(String messageText) {
        realm.beginTransaction();
        Message message = realm.createObject(Message.class);
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessage(messageText);
        message.setTimestamp(System.currentTimeMillis());
        realm.commitTransaction();
    }

    public class ChatAdapter extends RealmBasedRecyclerViewAdapter<Message, ChatAdapter.MessageViewHolder> {

        public ChatAdapter(Context context, RealmResults<Message> realmResults) {
            super(context, realmResults, true, true);
        }

        @Override
        public MessageViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int i) {
            View v = inflater.inflate(R.layout.chat_item_view, viewGroup, false);
            return new MessageViewHolder((FrameLayout) v);
        }

        @Override
        public void onBindRealmViewHolder(MessageViewHolder messageViewHolder, int position) {
            final Message message = realmResults.get(position);
            messageViewHolder.todoTextView.setText(message.getMessage());
        }

        public class MessageViewHolder extends RealmViewHolder {
            public TextView todoTextView;
            public MessageViewHolder(FrameLayout container) {
                super(container);
                this.todoTextView = (TextView) container.findViewById(R.id.todo_text_view);
            }
        }
    }
}
