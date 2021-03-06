package org.ar.armeet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lzf.easyfloat.EasyFloat;
import com.lzf.easyfloat.enums.ShowPattern;
import com.lzf.easyfloat.interfaces.OnFloatCallbacks;
import com.lzf.easyfloat.interfaces.OnInvokeView;
import com.lzf.easyfloat.interfaces.OnPermissionResult;
import com.lzf.easyfloat.permission.PermissionUtils;

import org.ar.common.enums.ARNetQuality;
import org.ar.common.enums.ARVideoCommon;
import org.ar.common.utils.ARAudioManager;
import org.ar.common.utils.AR_AudioManager;
import org.ar.meet_kit.ARMeetEngine;
import org.ar.meet_kit.ARMeetEvent;
import org.ar.meet_kit.ARMeetKit;
import org.ar.meet_kit.ARMeetOption;
import org.ar.meet_kit.ARMeetType;
import org.ar.meet_kit.ARMeetZoomMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoRenderer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class MeetingActivity extends BaseActivity implements View.OnClickListener {

    View space;
    RelativeLayout rlVideo, rl_log_layout;
    ImageButton ibCamera, ibLog, ibtn_close_log;
    Button ibVideo, ibAudio, ibHangUp;
    RecyclerView rvLog;
    TextView tvRoomId;
    LinearLayout ll_bottom_layout;
    private LogAdapter logAdapter;
    private ARVideoView mVideoView;
    private ARMeetKit mMeetKit;
    private String meetId = "";
    private String userId = (int) ((Math.random() * 9 + 1) * 100000) + "";
    ARAudioManager arAudioManager;

    @Override
    public int getLayoutId() {
        return R.layout.activity_meeting;
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        space = findViewById(R.id.view_space);
        mImmersionBar.titleBar(space).init();
        tvRoomId = findViewById(R.id.tv_room_id);
        rvLog = findViewById(R.id.rv_log);
        rlVideo = findViewById(R.id.rl_video);
        ibCamera = findViewById(R.id.btn_camare);
        rl_log_layout = findViewById(R.id.rl_log_layout);
        ibtn_close_log = findViewById(R.id.ibtn_close_log);
        ll_bottom_layout = findViewById(R.id.ll_bottom_layout);
        ibVideo = findViewById(R.id.ib_video);
        ibAudio = findViewById(R.id.ib_audio);
        ibLog = findViewById(R.id.btn_log);
        ibHangUp = findViewById(R.id.ib_leave);
        ibCamera.setOnClickListener(this);
        ibVideo.setOnClickListener(this);
        ibAudio.setOnClickListener(this);
        ibLog.setOnClickListener(this);
        ibHangUp.setOnClickListener(this);
        ibtn_close_log.setOnClickListener(this);
        logAdapter = new LogAdapter();
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter.bindToRecyclerView(rvLog);
        meetId = getIntent().getStringExtra("meet_id");
        tvRoomId.setText("房间ID：" + meetId);
        mVideoView = new ARVideoView(rlVideo, ARMeetEngine.Inst().Egl(), this);

        mVideoView.setVideoViewLayout(false, Gravity.CENTER, LinearLayout.HORIZONTAL);
        //获取配置类
        ARMeetOption option = ARMeetEngine.Inst().getARMeetOption();
        //设置默认为前置摄像头
        option.setDefaultFrontCamera(true);
        //设置视频分辨率
        option.setVideoProfile(ARVideoCommon.ARVideoProfile.ARVideoProfile480x640);
        //设置会议类型
        option.setMeetType(ARMeetType.Normal);
        option.setMediaType(ARVideoCommon.ARMediaType.Video);
        mMeetKit = new ARMeetKit(arMeetEvent);
        mMeetKit.setFrontCameraMirrorEnable(true);
        VideoRenderer localVideoRender = mVideoView.openLocalVideoRender();
        mMeetKit.setLocalVideoCapturer(localVideoRender.GetRenderPointer());
        mMeetKit.joinRTCByToken("", meetId, userId, getUserInfo());
        arAudioManager = ARAudioManager.create(this);
        arAudioManager.start(new ARAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(ARAudioManager.AudioDevice audioDevice, Set<ARAudioManager.AudioDevice> set) {

            }
        });


    }

    public String getUserInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("MaxJoiner", 6);
            jsonObject.put("userId", userId);
            jsonObject.put("nickName", "android" + userId);
            jsonObject.put("headUrl", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camare:
                mMeetKit.switchCamera();//翻转摄像头
                if (ibCamera.isSelected()) {
                    ibCamera.setSelected(false);
                } else {
                    ibCamera.setSelected(true);
                }
                logAdapter.addData("方法：翻转摄像头");
                break;
            case R.id.ib_audio:
                if (ibAudio.isSelected()) {
                    ibAudio.setSelected(false);
                    mMeetKit.setLocalAudioEnable(true);//允许本地音频传输
                } else {
                    ibAudio.setSelected(true);
                    mMeetKit.setLocalAudioEnable(false);//禁止本地音频传输
                }
                logAdapter.addData("方法：" + (ibAudio.isSelected() ? "本地音频传输关闭" : "本地音频传输开启"));
                break;
            case R.id.ib_video:
                if (ibVideo.isSelected()) {
                    mMeetKit.setLocalVideoEnable(true);//允许本地视频传输
                    ibVideo.setSelected(false);
                } else {
                    ibVideo.setSelected(true);
                    mMeetKit.setLocalVideoEnable(false);//禁止本地视频传输
                }
                logAdapter.addData("方法：" + (ibVideo.isSelected() ? "本地视频传输关闭" : "本地视频传输开启"));
                break;
            case R.id.btn_log:
                rl_log_layout.setVisibility(View.VISIBLE);
                break;
            case R.id.ibtn_close_log:
                rl_log_layout.setVisibility(View.GONE);
                break;
            case R.id.ib_leave:
                if (mMeetKit != null) {
                    mMeetKit.clean();
                }
                finishAnimActivity();
                break;
        }
    }



    ARMeetEvent arMeetEvent = new ARMeetEvent() {
        @Override
        public void onRTCJoinMeetOK(final String roomId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCJoinMeetOK 加入房间成功 ID：" + roomId);
                }
            });
        }

        @Override
        public void onRTCJoinMeetFailed(final String roomId, final int code, final String reason) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCJoinMeetFailed 加入房间失败 ID：" + roomId + "code:" + code + "reason:" + reason);
                    if (code == 701) {
                        Toast.makeText(MeetingActivity.this, "会议人数已满", Toast.LENGTH_SHORT).show();
                        finishAnimActivity();
                    }
                }
            });
        }

        @Override
        public void onRTCLeaveMeet(final int code) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCLeaveMeet 离开房间 code：" + code);
                }
            });
        }

        @Override
        public void onRTCOpenRemoteVideoRender(final String peerId, final String publishId, final String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCOpenRemoteVideoRender 远程视频流接入即将渲染显示 publishId：" + publishId + "\n peerId:" + peerId + "user:" + userId);
                    final VideoRenderer render = mVideoView.openRemoteVideoRender(publishId);
                    if (null != render) {
                        mMeetKit.setRemoteVideoRender(publishId, render.GetRenderPointer());
                    }
                }
            });
        }

        @Override
        public void onRTCCloseRemoteVideoRender(final String peerId, final String publishId, final String userId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCCloseRemoteVideoRender 远程视频流关闭 publishId：" + publishId + "\n peerId:" + peerId + "user:" + userId);
                    if (mMeetKit != null && mVideoView != null) {
                        mVideoView.removeRemoteRender(publishId);
                        mMeetKit.setRemoteVideoRender(publishId, 0);
                    }

                }
            });
        }

        @Override
        public void onRTCOpenScreenRender(final String peerId, final String publishId, final String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCOpenScreenRender 屏幕共享流接入即将渲染显示 publishId：" + publishId + "\n peerId:" + peerId + "user:" + userId);
                    final VideoRenderer render = mVideoView.openRemoteVideoRender("ScreenShare");
                    if (null != render) {
                        mMeetKit.setRemoteVideoRender(publishId, render.GetRenderPointer());
                    }
                }
            });
        }

        @Override
        public void onRTCCloseScreenRender(final String peerId, final String publishId, final String userId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCCloseScreenRender 屏幕共享流关闭 publishId：" + publishId + "\n peerId:" + peerId + "user:" + userId);
                    if (mMeetKit != null && mVideoView != null) {
                        mVideoView.removeRemoteRender("ScreenShare");
                        mMeetKit.setRemoteVideoRender(publishId, 0);
                    }
                }
            });
        }


        @Override
        public void onRTCOpenRemoteAudioTrack(final String peerId, final String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRtcOpenRemoteAudioTrack 远程音频流接入 peerId:" + peerId + "user:" + userId);
                }
            });
        }

        @Override
        public void onRTCCloseRemoteAudioTrack(final String peerId, final String userId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRtcOpenRemoteAudioTrack 远程音频流离开 peerId:" + peerId + "user:" + userId);
                }
            });
        }

        @Override
        public void onRTCLocalAudioPcmData(String peerId, byte[] data, int nLen, int nSampleHz, int nChannel) {

        }

        @Override
        public void onRTCRemoteAudioPcmData(String peerId, byte[] data, int nLen, int nSampleHz, int nChannel) {

        }

        @Override
        public void onRTCRemoteAVStatus(final String peerId, final boolean bAudio, final boolean bVideo) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCRemoteAVStatus 远程用户音视频状态  peerId:" + peerId + "bAudio:" + bAudio + "bVideo:" + bVideo);
                }
            });
        }

        @Override
        public void onRTCLocalAVStatus(final boolean bAudio, final boolean bVideo) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCLocalAVStatus 本地音视频状态 bAudio:" + bAudio + "bVideo:" + bVideo);
                }
            });
        }

        @Override
        public void onRTCRemoteAudioActive(final String peerId, String userId, final int nLevel, int nTime) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }


        @Override
        public void onRTCLocalAudioActive(final int nLevel, int nTime) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onRTCRemoteNetworkStatus(final String publishId, String userId, final int nNetSpeed, final int nPacketLost, final ARNetQuality netQuality) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    if (mVideoView != null) {
//                        mVideoView.updateRemoteNetStatus(publishId, String.valueOf(nNetSpeed / 1024 * 8));
//                    }

                }
            });
        }

        @Override
        public void onRTCLocalNetworkStatus(final int nNetSpeed, final int nPacketLost, final ARNetQuality netQuality) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    if (mVideoView != null) {
//                        mVideoView.updateLocalNetStatus(String.valueOf(nNetSpeed / 1024 * 8));
//                    }
                }
            });
        }

        @Override
        public void onRTCConnectionLost() {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCConnectionLost 网络丢失....");
                }
            });
        }

        @Override
        public void onRTCUserMessage(String userId, final String userName, String headUrl, final String message) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCUserMessage 收到消息 " + "\n姓名：" + userName + "\n消息：" + message);
                }
            });
        }

        @Override
        public void onRTCShareEnable(final boolean success) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCShareEnable 分享通道开启关闭结果" + success);
                }
            });
        }

        @Override
        public void onRTCShareOpen(final int type, final String shareInfo, String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCShareOpen 分享通道开启 shareInfo:" + shareInfo + "==========" + type);
                }
            });
        }

        @Override
        public void onRTCShareClose() {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCShareClose 分享通道关闭");
                }
            });
        }

        @Override
        public void onRTCHosterOnline(final String peerId, final String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCHosterOnline 主持人上线 peerId:" + peerId);
                }
            });
        }

        @Override
        public void onRTCHosterOffline(final String peerId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRTCHosterOnline 主持人下线 peerId:" + peerId);
                }
            });
        }

        @Override
        public void onRTCTalkOnlyOn(String peerId, String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        @Override
        public void onRTCTalkOnlyOff(String peerId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        @Override
        public void onRtcUserCome(final String peerId, String publishId, String userId, String userData) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRtcUserCome 有人进入 peerId:" + peerId);
                }
            });
        }

        @Override
        public void onRtcUserOut(final String peerId, String publishId, String userId) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAdapter.addData("回调：onRtcUserOut 有人退出 peerId:" + peerId);
                }
            });
        }

        @Override
        public void onRTCZoomPageInfo(ARMeetZoomMode zoomMode, int allPages, int curPage, int allRender, int screenIndex, int num) {
            MeetingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.removeLocalVideoRender();
            mVideoView.removeAllRemoteRender();
        }
        if (arAudioManager != null) {
            arAudioManager.stop();
            arAudioManager = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ll_bottom_layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setBottomHeight(ll_bottom_layout.getMeasuredHeight());
            }
        }, 100);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    public boolean moveTaskToBack(boolean nonRoot) {
        return super.moveTaskToBack(nonRoot);
    }
}
