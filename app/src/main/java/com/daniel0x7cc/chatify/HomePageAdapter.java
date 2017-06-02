package com.daniel0x7cc.chatify;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;


public class HomePageAdapter extends FragmentStatePagerAdapter {

    private final Fragment[] fragments = new Fragment[2];
    private String tabTitles[] = new String[]{"Usuários", "Conversas"};

    HomePageAdapter(FragmentManager paramFragmentManager) {
        super(paramFragmentManager);
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (fragments[position] == null) {
                    fragments[position] = SearchPeopleFragment.newInstance();
                }
                return fragments[position];
            case 1:
                if (fragments[position] == null) {
                    fragments[position] = ChatsFragment.newInstance();
                }
                return fragments[position];
            default:
                return null;
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }



}