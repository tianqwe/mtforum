package com.forum.mt.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.forum.mt.ui.NotificationListFragment;
import com.forum.mt.ui.PmListFragment;

/**
 * 消息页面ViewPager适配器
 */
public class MessagePagerAdapter extends FragmentStateAdapter {
    
    private static final int TAB_COUNT = 2;
    private static final int POSITION_PM = 0;          // 私信
    private static final int POSITION_NOTIFICATION = 1; // 通知
    
    public MessagePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case POSITION_PM:
                return PmListFragment.newInstance();
            case POSITION_NOTIFICATION:
                return NotificationListFragment.newInstance();
            default:
                return PmListFragment.newInstance();
        }
    }
    
    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}
