package com.veryworks.android.soundplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import com.veryworks.android.soundplayer.domain.Sound;

import java.util.ArrayList;
import java.util.List;

import static android.app.PendingIntent.getService;

public class SoundService extends Service implements ControlInterface {
    private static final String TAG = "SoundService";

    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    public static MediaPlayer mMediaPlayer = null;
    public static String listType = "";
    public static int position = -1;

    List<Sound> datas = new ArrayList<>();
    Controller controller;

    public SoundService(){
        controller = Controller.getInstance();
        controller.addObserver(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if(intent.getExtras() != null) {
                listType = intent.getExtras().getString(ListFragment.ARG_LIST_TYPE);
                position = intent.getExtras().getInt(ListFragment.ARG_POSITION);
                if(mMediaPlayer == null) {
                    initMedia();
                }
            }
        }

        handleAction(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    // 1. 미디어 플레이어 기본값 설정
    private void initMedia() {
        if(datas.size() < 1){
            switch(listType){
                case ListFragment.TYPE_SONG :
                    datas = DataLoader.getSounds(getBaseContext());
                    break;
                case ListFragment.TYPE_ARTIST :
            }

        }

        // 음원 uri
        Uri musicUri = datas.get(position).music_uri; //TODO datas.get(position).music_uri;

        // 플레이어에 음원 세팅
        mMediaPlayer = MediaPlayer.create(this, musicUri);
        mMediaPlayer.setLooping(false); // 반복여부
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //TODO next();
            }
        });
    }

    // 2. 명령어 실행
    private void handleAction( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;

        String action = intent.getAction();
        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
            controller.play();
        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            controller.pause();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {

        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {

        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            playerStop();
        }
    }

    // Activity 에서의 클릭 버튼 생성
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), SoundService.class );
        intent.setAction( intentAction );
        // PendingIntent : 인텐트를 서비스 밖에서 실행시킬 수 있도록 담아두는 주머니
        PendingIntent pendingIntent = getService(getApplicationContext(), 1, intent, 0);

        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    // 노티바를 생성하는 함수
    private void buildNotification( NotificationCompat.Action action , String action_flag) {
        Sound sound = datas.get(position);

        // Stop intent
        Intent intentStop = new Intent( getApplicationContext(), SoundService.class );
        intentStop.setAction( ACTION_STOP );
        PendingIntent stopIntent = getService(getApplicationContext(), 1, intentStop, 0);

        // 노티바 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder( this );

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle( sound.getTitle() )
                .setContentText( sound.getArtist() );

        // 퍼즈일 경우만 노티 삭제 가능
//        if(ACTION_PAUSE.equals(action_flag)) {
        builder.setDeleteIntent(stopIntent);
        builder.setOngoing(false);
//        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Prev", ACTION_PREVIOUS));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 노티바를 화면에 보여준다
        notificationManager.notify(NOTIFICATION_ID , builder.build());
    }

    private void playerStart(){
        // 노티피케이션 바 생성
        buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ), ACTION_PAUSE );
        mMediaPlayer.start();
    }

    private void playerPause(){
        buildNotification( generateAction( android.R.drawable.ic_media_play, "Play", ACTION_PLAY ), ACTION_PLAY );
        mMediaPlayer.pause();
    }

    private void playerStop(){
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel( NOTIFICATION_ID );
        Intent intent = new Intent( getApplicationContext(), SoundService.class );
        stopService( intent );
    }

    @Override
    public void startPlayer() {
        playerStart();
    }

    @Override
    public void pausePlayer() {
        playerPause();
    }

    @Override
    public void onDestroy() {
        controller.remove(this);
        super.onDestroy();
    }
}
