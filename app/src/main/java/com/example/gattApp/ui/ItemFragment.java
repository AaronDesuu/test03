package com.example.gattApp.ui;

import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class ItemFragment extends Fragment {
    public interface messageManager {
        void fragment(ItemFragment fragment);
        int fragmentMessage(final int message_id);
        int messageID();
        void Parameter1(final String argument);
        void Parameter2(final String argument);
        void Multiple(final byte [] list);
        int setInterval(final boolean enable);
        void Sound(final int id);
        void showToast(String text);
    }
    public void Progress(final String msg, final int now, final int end){
    };
    public void DeviceInfo(final String msg){
    };
    public void DataArrived(final ArrayList<String> in, final boolean last) {
    };
    public void SetData(final ArrayList<String> in) {
    };
    public String getData() {
        return "";
    };

    public void setAnime(Button btn) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(100);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        btn.startAnimation(animationSet);
    }
}
