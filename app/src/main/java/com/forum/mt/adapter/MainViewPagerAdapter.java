package com.forum.mt.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.forum.mt.ui.HomeFragment;
import com.forum.mt.ui.ForumFragment;
import com.forum.mt.ui.MessageFragment;
import com.forum.mt.ui.UserFragment;

/**
 * 主页面ViewPager2适配器
 */
public class MainViewPagerAdapter extends FragmentStateAdapter {
    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new ForumFragment();
            case 2:
                return new MessageFragment();
            case 3:
                return new UserFragment();
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
