package fansirsqi.xposed.sesame.ui.widget;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
/**
 * @author Byseven
 * @date 2025/1/17
 * @apiNote
 */
public class ContentPagerAdapter extends RecyclerView.Adapter<ContentPagerAdapter.ViewHolder> {
    private final Context context;
    private final List<ModelConfig> configs = new ArrayList<>();
    public ContentPagerAdapter(Context context, Map<String, ModelConfig> configMap, String userId) {
        this.context = context;
        // 存储传入的 userId
        this.configs.addAll(configMap.values());
    }
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(Map<String, ModelConfig> configMap) {
        configs.clear();
        configs.addAll(configMap.values());
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);
        return new ViewHolder(scrollView, layout);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelConfig modelConfig = configs.get(position);
        holder.container.removeAllViews();
        for (ModelField<?> field : modelConfig.getFields().values()) {
            View fieldView = field.getView(context);
            if (fieldView != null) {
                holder.container.addView(fieldView);
            }
        }
    }
    @Override
    public int getItemCount() {
        return configs.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        public ViewHolder(@NonNull View itemView, LinearLayout container) {
            super(itemView);
            this.container = container;
        }
    }
}