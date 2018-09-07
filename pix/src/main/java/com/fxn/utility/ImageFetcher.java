package com.fxn.utility;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.fxn.modals.Img;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by akshay on 06/04/18.
 */

public class ImageFetcher extends AsyncTask<Cursor, Void, ArrayList<Img>> {

    private OnTaskCompleteListener mListener;
    private int mPage = 1;
    private int mItemsPerPage = 6;

    private String mLastMonthText;
    private String mLastWeekText;
    private String mRecentText;

    public ImageFetcher(String lastMonthText, String lastWeekText, String recentText) {
        this.mLastMonthText = lastMonthText;
        this.mLastWeekText = lastWeekText;
        this.mRecentText = recentText;
    }

    @Override
    protected ArrayList<Img> doInBackground(Cursor... cursors) {
        ArrayList<Img> imageList = new ArrayList<>();
        Cursor cursor = cursors[0];
        if (cursor != null) {
            int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int contentUrlIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM");

            Calendar calendar = Calendar.getInstance();
            Date date;
            String dateDifference;
            Calendar lastMonth = Calendar.getInstance();
            Calendar lastWeek = Calendar.getInstance();
            Calendar recent = Calendar.getInstance();
            lastMonth.add(Calendar.DAY_OF_MONTH, -(Calendar.DAY_OF_MONTH));
            lastWeek.add(Calendar.DAY_OF_MONTH, -7);
            recent.add(Calendar.DAY_OF_MONTH, -2);

            String header = "";
            int start = (mPage - 1) * mItemsPerPage;
            int end = start + mItemsPerPage;
            if (cursor.getCount() < end){
                end = cursor.getCount();
            }

            if (start > cursor.getCount()){
                return new ArrayList<>();
            }else if (start > 0){
                cursor.move(start - 1);
            }
            for (int i = start; i < end; i++){
                cursor.moveToNext();
                calendar.setTimeInMillis(cursor.getLong(dateIndex));
                date = calendar.getTime();

                if (calendar.before(lastMonth)) {
                    dateDifference = dateFormat.format(date);
                } else if (calendar.after(lastMonth) && calendar.before(lastWeek)) {
                    dateDifference = mLastMonthText;
                } else if (calendar.after(lastWeek) && calendar.before(recent)) {
                    dateDifference = mLastWeekText;
                } else {
                    dateDifference = mRecentText;
                }

                if (!header.equalsIgnoreCase(dateDifference)) {
                    header = dateDifference;
                    imageList.add(new Img(dateDifference, "", ""));
                }
                Uri curl = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + cursor.getInt(contentUrlIndex));
                imageList.add(new Img(header, curl.toString(), cursor.getString(dataIndex)));
            }
            cursor.close();
        }
        return imageList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mListener != null){
            mListener.onStart();
        }
    }

    @Override
    protected void onPostExecute(ArrayList<Img> imageList) {
        super.onPostExecute(imageList);
        if (mListener != null){
            mListener.onComplete(imageList);
        }
    }

    public void execute(Cursor cursor, int page, int itemsPerPage, OnTaskCompleteListener listener){
        mPage = page;
        mItemsPerPage = itemsPerPage;
        mListener = listener;
        execute(cursor);
    }

    public interface OnTaskCompleteListener{
        void onStart();
        void onComplete(ArrayList<Img> images);
    }

}
