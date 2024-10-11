package live.videosdk.rtc.android.java;

import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class EffectsAdapter extends RecyclerView.Adapter<EffectsAdapter.EffectInfoViewHolder> {
    @NonNull
    private List<String> mEffectsList;
    @NonNull
    private OnEffectClickListener mCallback;

    public EffectsAdapter(@NonNull List<String> effectsList) {
        mEffectsList = effectsList;
    }

    public void setOnItemClickListener(OnEffectClickListener callback) {
        mCallback = callback;
    }

    public interface OnEffectClickListener {
        void onEffectClick(String effect);
    }

    @NonNull
    @Override
    public EffectInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.banuba_vbg_item_view, null);
        return new EffectInfoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EffectInfoViewHolder holder, int position) {
        Log.d("TAG", "onBindViewHolder: " + holder);
        String effectName = mEffectsList.get(position);
        holder.bind(effectName);
        holder.itemView.setOnClickListener($ -> mCallback.onEffectClick(effectName));
    }

    @Override
    public int getItemCount() {
        return mEffectsList.size();
    }

    class EffectInfoViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;
        private TextView txtEffectName;

        void bind(String effectName) {
            if (effectName == "off()") {
                mImageView.setImageResource(R.mipmap.unload);
                txtEffectName.setText("No Effect");
            } else {
                File preview = new File(effectName + "/preview.png");
                if (preview.exists()) {
                    mImageView.setImageBitmap(BitmapFactory.decodeFile(preview.getPath()));
                }
                String[] effectnm = effectName.split("/");
                Log.d("TAG", "bind: " + effectnm[effectnm.length - 1]);
                txtEffectName.setText(effectnm[effectnm.length - 1]);
            }
        }

        EffectInfoViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.iconView);
            mImageView.setVisibility(View.GONE);
            txtEffectName = itemView.findViewById(R.id.txtEffectName);
            txtEffectName.setPadding(15,15,15,15);

        }
    }
}
