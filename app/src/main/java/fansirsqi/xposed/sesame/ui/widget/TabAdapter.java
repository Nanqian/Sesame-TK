package fansirsqi.xposed.sesame.ui.widget;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
/**
 * @author Byseven
 * @date 2025/1/17
 * @apiNote
 */
public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {
    private final List<String> titles;
    private final OnTabClickListener listener;
    private int selectedPosition = 0;
    public TabAdapter(List<String> titles, OnTabClickListener listener) {
        this.titles = titles;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        textView.setPadding(16, 16, 16, 16);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(textView);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(titles.get(position));
        holder.textView.setBackgroundColor(position == selectedPosition ? Color.LTGRAY : Color.TRANSPARENT);
        holder.textView.setOnClickListener(v -> {
            listener.onTabClick(position);
            setSelectedPosition(position);
        });
    }
    @Override
    public int getItemCount() {
        return titles.size();
    }
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
    public interface OnTabClickListener {
        void onTabClick(int position);
    }
}