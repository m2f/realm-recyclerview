package co.moonmonkeylabs.realmexamples.realmsearchview;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import co.moonmonkeylabs.realmexamples.R;
import co.moonmonkeylabs.realmexamples.realmsearchview.model.Blog;

/**
 * View for a {@link Blog} model.
 */
public class BlogItemView extends RelativeLayout {

    TextView emoji;
    TextView title;
    TextView date;
    TextView description;

    public BlogItemView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.blog_item_view, this);
        emoji = (TextView) findViewById(R.id.emoji);
        title = (TextView) findViewById(R.id.title);
        date = (TextView) findViewById(R.id.date);
        description = (TextView) findViewById(R.id.description);
    }

    public void bind(Blog blog) {
        emoji.setText(blog.getEmoji());
        title.setText(blog.getTitle());
        date.setText(blog.getDate());
        description.setText(blog.getContent());
    }
}
